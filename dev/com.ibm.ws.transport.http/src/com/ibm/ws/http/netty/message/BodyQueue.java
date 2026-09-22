package com.ibm.ws.http.netty.message;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

final public class BodyQueue {

    private static final int DEFAULT_HIGH = 256 * 1024;
    private static final int DEFAULT_LOW = 64 * 1024;

    private final ConcurrentLinkedQueue<ByteBuf> queue = new ConcurrentLinkedQueue<>();
    private final int lowWater, highWater;
    private final AtomicInteger buffered = new AtomicInteger();
    private volatile boolean eos;
    private volatile Throwable error;
    private final ByteBufAllocator allocator;

    /**
     * Total bytes received, counting every fragment regardless of whether it
     * was queued or discarded by the purge path.  Incremented unconditionally
     * in {@link #enqueueRetained} before the routing decision so that
     * size-limit enforcement is accurate across the purge boundary.
     *
     * <p>Written only on the event loop; read from worker threads for limit
     * checks. Declared {@code volatile} to ensure worker-thread reads see the
     * latest value without an additional memory barrier.
     */
    private volatile long bytesReceived;

    /**
     * Set to {@code true} once the purge lifecycle begins.
     *
     * <h3>Thread model and race freedom</h3>
     * Both {@link #enqueueRetained} and {@link #drainAndRelease} run on the
     * same Netty I/O event loop (single-threaded). Because they are serialised
     * by the event loop's execution model, no concurrent modification of this
     * flag or the queue is possible — no lock is required for the
     * enqueue/purge transition.
     *
     * <p>Declared {@code volatile} solely so that worker-thread callers of
     * {@link #isPurging()} (e.g. {@code HttpInputStreamImpl} on an application
     * thread after waking from {@link #awaitChange}) see the most recently
     * written value promptly, without a monitor acquire.
     *
     * <h3>Ownership contract</h3>
     * <ul>
     *   <li>The caller of {@link #enqueueRetained} owns the incoming reference.
     *       The queue must not release a reference it did not acquire.</li>
     *   <li>When purging, the queue skips retain-and-enqueue.  The caller's
     *       {@code finally} block releases the enclosing {@code HttpContent}.</li>
     *   <li>When not purging, the queue retains the buffer and owns that
     *       reference until it is transferred via {@link #poll()} or released
     *       by {@link #drainAndRelease()}.</li>
     * </ul>
     */
    private volatile boolean purging = false;

    private final Object signalLock = new Object();
    private long signal;

    public BodyQueue(ByteBufAllocator allocator) {
        this(allocator, DEFAULT_HIGH, DEFAULT_LOW);
    }

    public BodyQueue(ByteBufAllocator allocator, int high, int low) {
        this.allocator = allocator;
        this.highWater = Math.max(high, low);
        this.lowWater = low;
    }

    /**
     * Called by the dispatcher on the Netty I/O event loop when an
     * {@link io.netty.handler.codec.http.HttpContent} fragment arrives.
     *
     * <p>Must be called on the event loop. Because {@link #drainAndRelease}
     * also runs on the event loop, the two methods are mutually exclusive by
     * the event loop's single-threaded execution model — no lock is needed.
     *
     * <h3>Ownership</h3>
     * The caller owns {@code buf} and will release the enclosing
     * {@code HttpContent} in its {@code finally} block.  This method either:
     * <ul>
     *   <li>retains {@code buf} before enqueuing so the queue holds its own
     *       reference, OR</li>
     *   <li>skips enqueue when purging — the caller's release handles cleanup.
     *       The queue must NOT call an extra release.</li>
     * </ul>
     *
     * @param buf the buffer to enqueue; the caller retains ownership.
     */
    public void enqueueRetained(ByteBuf buf) {
        int readable = buf.readableBytes();

        // Count unconditionally so size-limit enforcement is accurate whether
        // or not this fragment is actually retained.
        bytesReceived += readable;

        // If a purge is in progress the queue does not acquire a reference.
        // The caller's finally block releases the enclosing HttpContent.
        // Do NOT call release() here: we did not retain, so we do not release.
        if (purging) {
            return;
        }
        queue.add(buf.retain());
        buffered.addAndGet(readable);
        signalChange();
    }

    public ByteBuf poll() {
        ByteBuf b = queue.poll();
        if (b != null) {
            buffered.addAndGet(-b.readableBytes());
        }
        return b;
    }

    public boolean wantsInput() {
        return error == null && !eos && buffered.get() < lowWater;
    }

    public boolean isEos() {
        return eos && queue.isEmpty();
    }

    private void signalChange() {
        synchronized (signalLock) {
            signal++;
            signalLock.notifyAll();
        }
    }

    public long signalToken() {
        synchronized (signalLock) {
            return signal;
        }
    }

    /**
     * Blocks until the queue state changes from {@code lastToken}, EOS or an
     * error is signalled, or the queue enters purge mode.
     *
     * <p>Including {@code !purging} in the predicate prevents a missed-wakeup
     * when purge completes between the caller's {@link #isPurging()} check and
     * this method's entry into {@code wait()}:
     *
     * <pre>
     *   Reader:  isPurging() → false          (entry check in fillFromStreaming passes)
     *   Event loop: drainAndRelease()         (purging=true, signal++)
     *   Reader:  token = signalToken()        (captures post-purge token value)
     *   Reader:  awaitChange(token)           (without !purging: signal==token → waits forever)
     * </pre>
     *
     * <p>With {@code !purging} in the predicate the condition is false at entry
     * and the reader returns immediately.  Callers must re-check
     * {@link #isPurging()} after returning.
     */
    public long awaitChange(long lastToken) throws InterruptedException {
        synchronized (signalLock) {
            while (signal == lastToken && !eos && error == null && !purging) {
                signalLock.wait();
            }
            return signal;
        }
    }

    /**
     * Returns the total bytes received, including fragments discarded during
     * purge, for use in cumulative body-size limit enforcement.
     */
    public long bytesRead() {
        return bytesReceived;
    }

    public void signalEos() {
        eos = true;
        signalChange();
    }

    public void signalError(Throwable t) {
        error = t;
        signalChange();
    }

    public Throwable error() {
        return error;
    }

    public void wakeReaders() {
        signalChange();
    }

    /**
     * Marks the queue as purging and releases all retained fragments currently
     * held in the queue.
     *
     * <p><strong>Must be called on the Netty I/O event loop.</strong> Because
     * {@link #enqueueRetained} also runs on the event loop, the single-threaded
     * execution model ensures that this method and {@code enqueueRetained} are
     * mutually exclusive: either this drain runs first (after which every
     * subsequent {@code enqueueRetained} sees {@code purging=true} and discards
     * without retaining), or the in-progress {@code enqueueRetained} completes
     * first (its buffer is then visible to the drain loop below). There is no
     * window in which a retained buffer can be stranded after the drain.
     *
     * <p>{@link #signalChange()} is called after setting the flag so that any
     * reader blocked in {@link #awaitChange} wakes up immediately; the
     * {@code !purging} predicate in that method causes the reader to exit the
     * wait without needing a further EOS or fragment signal.
     *
     * <p>Must be called at most once per exchange. A second call is safe (the
     * flag write is idempotent and the drain loop finds an empty queue) but
     * unnecessary.
     */
    public void drainAndRelease() {
        purging = true;

        ByteBuf buf;
        while ((buf = queue.poll()) != null) {
            buffered.addAndGet(-buf.readableBytes());
            buf.release();
        }

        signalChange();
    }

    /**
     * Returns {@code true} if this queue has been put into purge mode via
     * {@link #drainAndRelease()}.
     */
    public boolean isPurging() {
        return purging;
    }
}
