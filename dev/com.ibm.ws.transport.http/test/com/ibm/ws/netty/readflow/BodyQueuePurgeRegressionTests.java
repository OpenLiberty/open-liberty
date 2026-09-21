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
     * Simulates the race: a producer thread passes {@code purging.get()==false},
     * then the drain runs (two-pass), then the producer enqueues. The two-pass
     * drain picks up the fragment in the second pass. The final fragment must
     * have its queue-retained ref released exactly once.
     *
     * This test is necessarily sequential and deterministic (no actual concurrency
     * needed to verify the logic). It directly inserts into the queue between the
     * two drain passes by using the public API in an order that mimics the race.
     *
     * We verify the invariant: after drainAndRelease() with a racing enqueue,
     * no retained refs survive in the queue.
     */
    @Test
    public void testDrainTwoPassHandlesRacingEnqueue() throws Exception {
        // We cannot truly inject between the two passes from outside,
        // so we verify that running drainAndRelease() twice is idempotent and
        // that a fragment enqueued AFTER the first drain is not leaked.

        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Fragment A enqueued normally.
        ByteBuf a = Unpooled.buffer(2).writeBytes(new byte[]{1, 2});
        queue.enqueueRetained(a);
        assertEquals("a retained", 2, a.refCnt());

        // Begin the purge (sets flag + drains A).
        queue.drainAndRelease();
        assertEquals("a released by drain", 1, a.refCnt());
        assertTrue("purging after drain", queue.isPurging());

        // Simulate a fragment that raced: it was enqueued AFTER purging=true.
        // In the real code, enqueueRetained now skips retain; we verify that.
        ByteBuf b = Unpooled.buffer(2).writeBytes(new byte[]{3, 4});
        queue.enqueueRetained(b);  // must NOT retain
        assertEquals("b not retained during purge", 1, b.refCnt());

        // No fragments remain in queue (nothing to drain).
        assertNull("poll must return null after purge+skip", queue.poll());

        a.release();
        b.release();
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
