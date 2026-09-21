/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.readflow;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.ibm.ws.http.netty.message.BodyQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Regression tests for the BodyQueue purge-path defects described in the
 * review findings.
 *
 * <h3>Finding 1 – Purge-path double release</h3>
 * The old code called {@code ReferenceCountUtil.safeRelease(buf)} when purging,
 * releasing a reference the queue did not own. The dispatcher's {@code finally}
 * block already releases {@code HttpContent}, which owns the underlying buffer.
 * Two releases → illegal reference count. Fix: do NOT release when discarding.
 *
 * <h3>Finding 2 – Fully-received but application-unread bodies</h3>
 * When the body was fully received ({@code signalEos()} called) but the
 * application never polled any fragment, retained queue fragments leaked.
 * Fix: {@code drainAndRelease()} must release retained refs even when EOS
 * was already set.
 *
 * <h3>Finding 3 – Purge race against concurrent enqueue</h3>
 * A producer that passed the {@code purging.get()==false} check before
 * {@code drainAndRelease()} set the flag could enqueue after the drain loop.
 * Fix: two-pass drain. Also, readers blocked inside {@code awaitChange()} must
 * see the purge flag after wakeup.
 *
 * <h3>Finding 5 – Cumulative byte accounting during purge</h3>
 * The old purge branch returned before incrementing {@code bytesRead}, so size
 * limits could be bypassed. Fix: count before routing decision.
 */
public class BodyQueuePurgeRegressionTests {

    // -----------------------------------------------------------------------
    // Finding 1: correct reference counting in the purge discard path
    // -----------------------------------------------------------------------

    /**
     * The caller owns the buffer reference and will release it. When
     * {@code drainAndRelease} has set {@code purging=true}, a subsequent
     * {@code enqueueRetained} must NOT release the buffer — only skip the
     * retain-and-enqueue step. After the call, refCnt must still be 1.
     */
    @Test
    public void testEnqueueRetainedDuringPurgeDoesNotReleaseCallerBuffer() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        queue.drainAndRelease(); // sets purging=true, drains nothing

        ByteBuf buf = Unpooled.buffer(4);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        assertEquals("sanity: refCnt=1 before enqueue", 1, buf.refCnt());

        // Simulate caller doing enqueueRetained(content.content()) with a
        // finally block that will release content (which releases buf).
        queue.enqueueRetained(buf);

        // The queue must NOT have released buf; caller still owns it.
        assertEquals("refCnt must still be 1 after purge discard", 1, buf.refCnt());
        // Caller releases — must reach 0 cleanly (no IllegalReferenceCountException).
        assertTrue("release by caller must succeed", buf.release());
        assertEquals("refCnt 0 after caller release", 0, buf.refCnt());
    }

    /**
     * Converse: when NOT purging, {@code enqueueRetained} retains the buffer.
     * The queue then owns that extra ref, and {@code drainAndRelease} must
     * release it (exactly once). After drain, the original caller can release
     * its own ref normally.
     */
    @Test
    public void testEnqueueRetainedNotPurgingRetainsBuffer() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf buf = Unpooled.buffer(4);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        assertEquals("refCnt before enqueue", 1, buf.refCnt());

        queue.enqueueRetained(buf);
        assertEquals("refCnt=2 after enqueue (queue holds one ref)", 2, buf.refCnt());

        // Queue drains and releases its retained reference.
        queue.drainAndRelease();
        assertEquals("refCnt=1 after drain (caller still holds theirs)", 1, buf.refCnt());

        // Caller releases — clean.
        assertTrue("caller release succeeds", buf.release());
        assertEquals("refCnt=0 after caller release", 0, buf.refCnt());
    }

    /**
     * Multiple fragments enqueued, then drained: each queue-owned reference must
     * be released exactly once.
     */
    @Test
    public void testDrainReleasesAllQueuedFragmentsExactlyOnce() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf a = Unpooled.buffer(2);
        a.writeBytes(new byte[]{1, 2});
        ByteBuf b = Unpooled.buffer(2);
        b.writeBytes(new byte[]{3, 4});

        queue.enqueueRetained(a);
        queue.enqueueRetained(b);
        assertEquals("a: refCnt=2", 2, a.refCnt());
        assertEquals("b: refCnt=2", 2, b.refCnt());

        queue.drainAndRelease();

        // Queue's retained refs released; callers' refs still alive.
        assertEquals("a: refCnt=1 after drain", 1, a.refCnt());
        assertEquals("b: refCnt=1 after drain", 1, b.refCnt());

        // Purging flag set: new arrivals are discarded without retain.
        assertTrue("isPurging after drainAndRelease", queue.isPurging());

        a.release();
        b.release();
    }

    // -----------------------------------------------------------------------
    // Finding 2: fully-received but application-unread bodies are drained
    // -----------------------------------------------------------------------

    /**
     * EOS is signalled (body fully received) but fragments were never polled.
     * {@code drainAndRelease()} must still release the retained refs.
     *
     * Note: {@code isEos()} returns {@code true} only when BOTH the EOS flag is
     * set AND the queue is empty (the application-facing "no more data" predicate).
     * Here we verify that the EOS flag was accepted and that the retained buffer
     * ref is still present before the drain.
     */
    @Test
    public void testDrainReleasesFragmentsWhenEosAlreadySet() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf buf = Unpooled.buffer(3);
        buf.writeBytes(new byte[]{10, 20, 30});
        queue.enqueueRetained(buf);
        queue.signalEos();

        // isEos() == false because the queue is non-empty (fragment not polled).
        // The EOS flag is set but signalEos() docs say isEos() = eos && queue.isEmpty().
        assertFalse("isEos() is false while fragment is still queued", queue.isEos());
        assertEquals("buf retained by queue (refCnt=2)", 2, buf.refCnt());

        // drainAndRelease must drain the retained fragment even when EOS was set.
        queue.drainAndRelease();

        // Queue now empty; isEos() becomes true.
        assertTrue("isEos() true after drain (queue empty + eos flag)", queue.isEos());
        assertEquals("buf queue-ref released (refCnt=1)", 1, buf.refCnt());
        buf.release(); // caller releases
    }

    /**
     * EOS already set and queue already empty (body was fully consumed by the
     * application): {@code drainAndRelease()} must be safe (no-op on the drain,
     * sets purging).
     */
    @Test
    public void testDrainOnEmptyEosQueueIsSafe() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        queue.signalEos();
        // poll empties the queue (simulating app reading).

        // No exception; purging flag set.
        queue.drainAndRelease();
        assertTrue("isPurging after drain on empty EOS queue", queue.isPurging());
    }

    // -----------------------------------------------------------------------
    // Finding 3: purge race against concurrent enqueue
    // -----------------------------------------------------------------------

    /**
     * Ordering A: enqueue completes, THEN drain runs.
     *
     * The event-loop serialization model guarantees these two operations are
     * mutually exclusive on the same thread, so the drain always sees any buffer
     * that was enqueued before it ran.  This test verifies that invariant by
     * calling them sequentially in order A→B: enqueue then drain.
     *
     * After drain: the queue-retained ref must be released; the caller's ref
     * must survive; no buffer is stranded.
     */
    @Test
    public void testEnqueueBeforeDrainBufferReleasedByDrain() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf a = Unpooled.buffer(2).writeBytes(new byte[]{1, 2});
        queue.enqueueRetained(a);
        assertEquals("queue acquired retain: refCnt=2", 2, a.refCnt());

        // Drain runs after enqueue completes — picks up A.
        queue.drainAndRelease();

        assertEquals("drain released queue ref: refCnt=1", 1, a.refCnt());
        assertTrue("isPurging after drain", queue.isPurging());
        assertNull("queue empty after drain", queue.poll());

        // Caller releases its own ref cleanly.
        assertTrue("caller release succeeds", a.release());
        assertEquals("refCnt=0 after caller release", 0, a.refCnt());
    }

    /**
     * Ordering B: drain runs, THEN enqueue is called.
     *
     * This is the critical case the old two-pass code tried (and failed) to
     * handle.  With the event-loop serialization model these two operations
     * cannot interleave: drainAndRelease() and enqueueRetained() both run on
     * the event loop, so "drain runs then enqueue is called" is a sequential,
     * not concurrent, ordering.
     *
     * After drain: purging=true.  A subsequent enqueueRetained must discard
     * without retaining.  The caller's ref must remain at 1 — no double-release,
     * no stranded queue-owned ref.
     */
    @Test
    public void testDrainBeforeEnqueueDiscardWithNoRetain() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Drain on an empty queue — sets purging=true.
        queue.drainAndRelease();
        assertTrue("purging=true after drain", queue.isPurging());

        // Producer calls enqueueRetained after drain: must discard, not retain.
        ByteBuf b = Unpooled.buffer(2).writeBytes(new byte[]{3, 4});
        queue.enqueueRetained(b);

        assertEquals("no retain during purge: refCnt=1", 1, b.refCnt());
        assertNull("queue empty: poll returns null", queue.poll());

        // Caller releases cleanly.
        assertTrue("caller release succeeds", b.release());
        assertEquals("refCnt=0 after caller release", 0, b.refCnt());
    }

    /**
     * Multiple buffers enqueued, then drain: every queue-owned ref released.
     * After drain, a late enqueueRetained discards without stranding anything.
     * Byte accounting includes all fragments regardless of purge.
     */
    @Test
    public void testDrainReleasesAllThenLateEnqueueDiscards() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf a = Unpooled.buffer(4).writeBytes(new byte[]{1, 2, 3, 4});
        ByteBuf b = Unpooled.buffer(3).writeBytes(new byte[]{5, 6, 7});
        queue.enqueueRetained(a);
        queue.enqueueRetained(b);
        assertEquals("a: refCnt=2", 2, a.refCnt());
        assertEquals("b: refCnt=2", 2, b.refCnt());
        assertEquals("7 bytes accounted", 7L, queue.bytesRead());

        // Drain: both queue refs released.
        queue.drainAndRelease();
        assertEquals("a: refCnt=1 after drain", 1, a.refCnt());
        assertEquals("b: refCnt=1 after drain", 1, b.refCnt());

        // Late arrival discarded without retain.
        ByteBuf c = Unpooled.buffer(2).writeBytes(new byte[]{8, 9});
        queue.enqueueRetained(c);
        assertEquals("c: refCnt=1 (not retained)", 1, c.refCnt());
        // Byte accounting includes the discarded fragment.
        assertEquals("9 bytes including purge-discarded", 9L, queue.bytesRead());

        a.release();
        b.release();
        c.release();
    }

    /**
     * Reader blocked in {@code awaitChange()} sees the purge flag after
     * {@code drainAndRelease()} signals and returns false.
     *
     * This is a deterministic multi-thread test: the reader waits on the
     * signal lock; drainAndRelease() wakes it; the reader re-checks purging.
     */
    @Test(timeout = 5000)
    public void testAwaitChangeWakesUpOnPurge() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        CountDownLatch readerStarted = new CountDownLatch(1);
        CountDownLatch readerDone    = new CountDownLatch(1);
        AtomicBoolean seenPurge      = new AtomicBoolean(false);

        Thread reader = new Thread(() -> {
            try {
                // Get a token before drainAndRelease is called.
                long token = queue.signalToken();
                readerStarted.countDown();
                // This will block until signalChange() is called by drainAndRelease.
                token = queue.awaitChange(token);
                // After waking, caller (HttpInputStreamImpl) would check isPurging().
                seenPurge.set(queue.isPurging());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readerDone.countDown();
            }
        }, "test-reader");

        reader.setDaemon(true);
        reader.start();
        readerStarted.await();

        // Give reader time to actually enter wait.
        Thread.sleep(50);

        // Trigger purge — must wake the reader.
        queue.drainAndRelease();

        readerDone.await();
        assertTrue("reader must see isPurging() after wakeup", seenPurge.get());
    }

    // -----------------------------------------------------------------------
    // Finding 2: reader/purge interleavings — awaitChange predicate includes
    // !purging so all three timing windows are covered deterministically.
    // -----------------------------------------------------------------------

    /**
     * Interleaving B1 — reader already blocked in {@code awaitChange()} when
     * purge begins.
     *
     * The reader captures its token before drain starts, then enters
     * {@code wait()}. {@code drainAndRelease()} calls {@code signalChange()},
     * which wakes the reader. The {@code !purging} predicate in
     * {@code awaitChange} causes it to exit.
     *
     * This case was handled correctly by the old code (the signal woke the
     * reader); it is retained here to guard regressions.
     */
    @Test(timeout = 5000)
    public void testReaderAlreadyWaitingWhenPurgeBegins() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        CountDownLatch insideWait  = new CountDownLatch(1);
        CountDownLatch readerDone  = new CountDownLatch(1);
        AtomicBoolean  seenPurge   = new AtomicBoolean(false);
        AtomicBoolean  interrupted = new AtomicBoolean(false);

        Thread reader = new Thread(() -> {
            try {
                long token = queue.signalToken();
                insideWait.countDown();        // signal: about to wait
                queue.awaitChange(token);      // blocks here
                seenPurge.set(queue.isPurging());
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                readerDone.countDown();
            }
        }, "b1-reader");

        reader.setDaemon(true);
        reader.start();
        insideWait.await();
        Thread.sleep(30);                      // ensure reader is in Object.wait()

        queue.drainAndRelease();               // wakes reader via signalChange()

        readerDone.await();
        assertFalse("no interruption", interrupted.get());
        assertTrue("reader sees purging after wakeup", seenPurge.get());
    }

    /**
     * Interleaving B2 — purge occurs after the entry {@code isPurging()} check
     * but before {@code signalToken()} is called.
     *
     * The reader sees {@code purging=false} at the fast-path entry check, then
     * drain completes (setting {@code purging=true} and incrementing the signal),
     * and only then does the reader capture the token.  Without {@code !purging}
     * in the predicate, {@code awaitChange} would observe {@code signal==token}
     * and block forever — no further signal is ever sent.
     *
     * With the fix, {@code awaitChange} exits immediately because
     * {@code !purging} is already false when evaluated.
     *
     * This is the exact window described in Finding 2. The test uses a
     * {@link CountDownLatch} pair to inject the drain at precisely this point.
     */
    @Test(timeout = 5000)
    public void testPurgeAfterEntryCheckBeforeTokenCapture() throws Exception {
        // Subclass that lets us interpose between isPurging() and signalToken().
        CountDownLatch afterEntryCheck   = new CountDownLatch(1);
        CountDownLatch drainComplete     = new CountDownLatch(1);
        AtomicBoolean  exited            = new AtomicBoolean(false);
        AtomicBoolean  interrupted       = new AtomicBoolean(false);

        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Simulate the isPurging() entry check passing (returns false),
        // then signal the drain thread to run, then call signalToken().
        Thread reader = new Thread(() -> {
            try {
                // Step 1: entry check (simulated — queue.isPurging() == false here)
                assertFalse("isPurging false at entry check", queue.isPurging());

                // Step 2: yield to allow drain to run
                afterEntryCheck.countDown();
                drainComplete.await();          // wait for drain to finish

                // Step 3: capture token AFTER drain incremented signal
                long token = queue.signalToken();

                // Step 4: awaitChange — with !purging in predicate this must
                // return immediately because purging is now true
                queue.awaitChange(token);
                exited.set(true);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        }, "b2-reader");

        reader.setDaemon(true);
        reader.start();

        afterEntryCheck.await();       // reader has passed entry check
        queue.drainAndRelease();       // sets purging=true, increments signal
        drainComplete.countDown();     // allow reader to proceed

        reader.join(3000);
        assertFalse("reader thread must not still be alive", reader.isAlive());
        assertFalse("no interruption", interrupted.get());
        assertTrue("awaitChange must exit immediately when purging=true", exited.get());
    }

    /**
     * Interleaving B3 — purge occurs after {@code signalToken()} but before
     * {@code awaitChange} enters {@code wait()}.
     *
     * The reader captures the token, then drain runs (sets {@code purging=true}
     * and increments the signal), then the reader calls {@code awaitChange(token)}.
     * At this point {@code signal > token}, so the while predicate is false
     * immediately and {@code wait()} is never entered — the reader exits promptly.
     *
     * This case is handled by the existing {@code signal != lastToken} part of the
     * predicate; the {@code !purging} clause provides defence-in-depth for the
     * window where the signal happens to match (e.g. on a wrapped counter), but
     * the fundamental exit path here is the token mismatch.
     */
    @Test(timeout = 5000)
    public void testPurgeAfterTokenCaptureBeforeAwait() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Capture token first.
        long token = queue.signalToken();

        // Drain runs — sets purging=true and increments signal.
        queue.drainAndRelease();

        // Now call awaitChange. Since signal > token, the predicate is false
        // immediately and no wait() is entered.
        AtomicBoolean exited       = new AtomicBoolean(false);
        AtomicBoolean interrupted  = new AtomicBoolean(false);

        Thread reader = new Thread(() -> {
            try {
                queue.awaitChange(token);
                exited.set(true);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        }, "b3-reader");

        reader.setDaemon(true);
        reader.start();
        reader.join(2000);

        assertFalse("reader thread must exit promptly", reader.isAlive());
        assertFalse("no interruption", interrupted.get());
        assertTrue("awaitChange must exit (token mismatch or purging)", exited.get());
    }

    /**
     * Interleaving B4 — partial buffer left in {@link
     * com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl} when purge
     * begins: the buffer must be released and the reader must not block.
     *
     * This is the "partially consumed stream-owned buffer" case from Finding 2.
     * We simulate it at the BodyQueue level: the reader has already polled a
     * fragment (owns a retained ref), then purge fires. The reader must release
     * the buffer and return false, not block.
     *
     * The BodyQueue itself does not hold the stream buffer — that is owned by
     * HttpInputStreamImpl. This test verifies that awaitChange exits when
     * purging=true so that HttpInputStreamImpl's post-wait purge check fires
     * promptly and can release its buffer without requiring another fragment.
     */
    @Test(timeout = 5000)
    public void testReaderWithConsumedBufferExitsOnPurge() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Enqueue a fragment (simulating one already delivered to the stream buffer).
        ByteBuf frag = Unpooled.buffer(4).writeBytes(new byte[]{1, 2, 3, 4});
        queue.enqueueRetained(frag);

        // Simulate the reader having polled it (queue-retained ref transferred).
        ByteBuf polled = queue.poll();
        assertNotNull("polled fragment exists", polled);
        assertEquals("polled ref is queue-retained ref", 2, frag.refCnt()); // caller + queue->polled

        // Queue is now empty; reader would call awaitChange waiting for more data.
        CountDownLatch aboutToWait = new CountDownLatch(1);
        CountDownLatch readerDone  = new CountDownLatch(1);
        AtomicBoolean  exited      = new AtomicBoolean(false);

        Thread reader = new Thread(() -> {
            try {
                long token = queue.signalToken();
                aboutToWait.countDown();
                queue.awaitChange(token); // blocks until signal or purge
                exited.set(true);
                // In the real code HttpInputStreamImpl releases its buffer here.
                polled.release();         // simulate buffer release on purge exit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readerDone.countDown();
            }
        }, "b4-reader");

        reader.setDaemon(true);
        reader.start();
        aboutToWait.await();
        Thread.sleep(30); // let reader enter wait()

        queue.drainAndRelease(); // wakes reader; sets purging=true
        readerDone.await();

        assertTrue("reader exited promptly via purge wakeup", exited.get());
        // polled ref released by reader; caller ref still alive.
        assertEquals("frag: caller ref survives", 1, frag.refCnt());
        frag.release(); // caller release
    }

    // -----------------------------------------------------------------------
    // Finding 5: cumulative byte accounting during purge
    // -----------------------------------------------------------------------

    /**
     * Bytes discarded by the purge path must still be counted by
     * {@code bytesRead()}. Multiple fragments — first two enqueued normally,
     * then purge starts, last fragment arrives and is discarded. The total
     * must include all three fragments.
     */
    @Test
    public void testBytesReadCountsDiscardedPurgeFragments() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Fragment 1 (10 bytes) — normal enqueue.
        ByteBuf f1 = Unpooled.buffer(10).writeBytes(new byte[10]);
        queue.enqueueRetained(f1);

        // Fragment 2 (15 bytes) — normal enqueue.
        ByteBuf f2 = Unpooled.buffer(15).writeBytes(new byte[15]);
        queue.enqueueRetained(f2);

        assertEquals("bytesRead after 2 fragments", 25L, queue.bytesRead());

        // Begin purge — drains f1 and f2 from the queue.
        queue.drainAndRelease();

        // Fragment 3 (20 bytes) — arrives after purge; discarded without retain.
        ByteBuf f3 = Unpooled.buffer(20).writeBytes(new byte[20]);
        queue.enqueueRetained(f3); // caller will release f3 itself

        // bytesRead must include ALL 45 bytes.
        assertEquals("bytesRead includes purged fragment", 45L, queue.bytesRead());

        // Cleanup caller refs.
        f1.release();
        f2.release();
        f3.release();
    }

    /**
     * The size limit check in the dispatcher uses {@code queue.bytesRead() + sizeOfCurrentChunk}.
     * Verify that multiple individually-acceptable fragments whose combined size
     * exceeds the limit are correctly counted even when later fragments arrive
     * after purge begins.
     *
     * This test validates the accounting model; the actual rejection decision
     * lives in the dispatcher.
     */
    @Test
    public void testCumulativeBytesTrackAcrossPurgeBoundary() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Enqueue two 100-byte fragments before purge.
        ByteBuf a = Unpooled.buffer(100).writeBytes(new byte[100]);
        ByteBuf b = Unpooled.buffer(100).writeBytes(new byte[100]);
        queue.enqueueRetained(a);
        queue.enqueueRetained(b);
        assertEquals("200 bytes after two fragments", 200L, queue.bytesRead());

        // Start purge — drains a and b, sets purging=true.
        queue.drainAndRelease();

        // Third 50-byte fragment arrives during purge — discarded but must be counted.
        ByteBuf c = Unpooled.buffer(50).writeBytes(new byte[50]);
        queue.enqueueRetained(c); // discarded (not retained), but bytes counted

        // 100 + 100 + 50 = 250 total
        assertEquals("250 bytes including purge-discarded fragment", 250L, queue.bytesRead());

        a.release();
        b.release();
        c.release(); // caller release only (no queue ref acquired during purge)
    }

    /**
     * {@code bytesRead()} returns 0 on a fresh queue and increments correctly
     * through a sequence of enqueue → poll → purge → discard.
     */
    @Test
    public void testBytesReadZeroOnFreshQueue() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        assertEquals("zero on fresh queue", 0L, queue.bytesRead());

        ByteBuf buf = Unpooled.buffer(7).writeBytes(new byte[7]);
        queue.enqueueRetained(buf);
        assertEquals("7 after enqueue", 7L, queue.bytesRead());

        // Poll removes from queue but bytesRead does not decrease.
        ByteBuf polled = queue.poll();
        assertNotNull(polled);
        assertEquals("7 after poll (cumulative)", 7L, queue.bytesRead());
        polled.release(); // release queue-retained ref
        buf.release();    // release caller ref

        // Enqueue another.
        ByteBuf buf2 = Unpooled.buffer(3).writeBytes(new byte[3]);
        queue.enqueueRetained(buf2);
        assertEquals("10 cumulative", 10L, queue.bytesRead());

        queue.drainAndRelease();
        assertEquals("10 after drain", 10L, queue.bytesRead());

        // Discard during purge.
        ByteBuf buf3 = Unpooled.buffer(5).writeBytes(new byte[5]);
        queue.enqueueRetained(buf3);
        assertEquals("15 including purge-discarded", 15L, queue.bytesRead());

        buf2.release();
        buf3.release();
    }

    // -----------------------------------------------------------------------
    // Edge cases: drainAndRelease idempotency and error/EOS signalling
    // -----------------------------------------------------------------------

    /**
     * Calling {@code drainAndRelease()} twice must not double-release any buffer
     * and must leave the queue in a consistent purging state.
     */
    @Test
    public void testDrainAndReleaseIsIdempotent() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf buf = Unpooled.buffer(4).writeBytes(new byte[]{1, 2, 3, 4});
        queue.enqueueRetained(buf);
        assertEquals("refCnt=2 after enqueue", 2, buf.refCnt());

        queue.drainAndRelease();
        assertEquals("refCnt=1 after first drain", 1, buf.refCnt());

        // Second drain — must not release again (queue is empty).
        queue.drainAndRelease();
        assertEquals("refCnt=1 after second drain (idempotent)", 1, buf.refCnt());
        assertTrue("still purging", queue.isPurging());

        buf.release();
    }

    /**
     * Error-signalled queue: {@code drainAndRelease()} must release retained
     * fragments even if an error was previously signalled.
     */
    @Test
    public void testDrainReleasesFragmentsEvenAfterErrorSignal() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf buf = Unpooled.buffer(2).writeBytes(new byte[]{7, 8});
        queue.enqueueRetained(buf);
        queue.signalError(new RuntimeException("simulated"));

        assertEquals("refCnt=2 after enqueue+error", 2, buf.refCnt());

        queue.drainAndRelease();

        assertEquals("refCnt=1 after drain (queue ref released)", 1, buf.refCnt());
        buf.release();
    }
}
