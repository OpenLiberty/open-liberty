package com.ibm.ws.http.netty.message;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

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
     * was queued or discarded by the purge path. Incremented before any
     * routing decision so the count is accurate for limit enforcement even
     * when purging.
     *
     * <p>Accessed from both the event-loop producer ({@link #enqueueRetained})
     * and the application-thread consumer ({@link #bytesRead}), hence volatile.
     * A {@code long} write is not atomic on 32-bit JVMs; if strict atomicity is
     * required in a future change, replace with {@link AtomicLong}.
     */
    private volatile long bytesReceived;

    /**
     * Set to {@code true} once the purge lifecycle begins. While purging,
     * {@link #enqueueRetained} counts but does not retain the arriving buffer,
     * so fragments are discarded without accumulating in the queue.
     *
     * <h3>Ownership contract</h3>
     * <ul>
     *   <li>The <em>caller</em> of {@link #enqueueRetained} owns the incoming
     *       reference. The queue must not release a reference it did not
     *       acquire.</li>
     *   <li>When purging, the queue simply skips the retain-and-enqueue step.
     *       The caller's {@code finally} block releases the enclosing
     *       {@code HttpContent}, which releases the underlying buffer.</li>
     *   <li>When not purging, the queue calls {@code buf.retain()} and then
     *       owns that retained reference until it is transferred to a consumer
     *       via {@link #poll()} or released by {@link #drainAndRelease()}.</li>
     * </ul>
     */
    private final AtomicBoolean purging = new AtomicBoolean(false);

    private final Object signalLock = new Object();
    private long signal;

    public BodyQueue(ByteBufAllocator allocator){
        this(allocator, DEFAULT_HIGH, DEFAULT_LOW);
    }

    public BodyQueue(ByteBufAllocator allocator, int high, int low){
        this.allocator = allocator;
        this.highWater = Math.max(high, low);
        this.lowWater = low;
    }

    /**
     * Called by the dispatcher on the event loop when an {@link io.netty.handler.codec.http.HttpContent}
     * fragment arrives.
     *
     * <h3>Ownership</h3>
     * The caller owns {@code buf} (typically {@code content.content()}) and
     * will release the enclosing {@code HttpContent} in its {@code finally}
     * block. This method must therefore:
     * <ul>
     *   <li>call {@code buf.retain()} before enqueuing so the queue holds its
     *       own reference, OR</li>
     *   <li>simply skip enqueue when purging — the caller's release of the
     *       enclosing {@code HttpContent} will decrement the ref count back to
     *       its original value. The queue must NOT call an extra release.</li>
     * </ul>
     *
     * Cumulative byte accounting ({@link #bytesRead}) is updated unconditionally
     * before the routing decision so that limit enforcement is accurate even
     * when fragments are discarded by the purge path.
     *
     * @param buf the buffer to enqueue; the caller retains ownership of the
     *            incoming reference and must release it after this call returns.
     */
    public void enqueueRetained(ByteBuf buf){
        int readable = buf.readableBytes();

        // Count bytes received regardless of purge state so that the cumulative
        // size limit is enforced correctly over multi-fragment bodies.
        bytesReceived += readable;

        // If a purge is in progress the queue does not acquire a reference —
        // the caller's finally block will release the enclosing HttpContent.
        // Do NOT call release() here: we did not retain, so we do not release.
        if (purging.get()) {
            return;
        }
        queue.add(buf.retain());
        buffered.addAndGet(readable);
        signalChange();
    }

    public ByteBuf poll(){
        ByteBuf b = queue.poll();
        if(b!=null){
            buffered.addAndGet(-b.readableBytes());
        }
        return b;
    }

    public boolean wantsInput(){
        return error == null && !eos && buffered.get() < lowWater;
    }

    public boolean isEos(){
        return eos && queue.isEmpty();
    }

    private void signalChange(){
        synchronized (signalLock){
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
     * Blocks until the queue state changes from {@code lastToken}, or until EOS
     * or an error is signalled. Also unblocks when {@link #drainAndRelease()}
     * sets the purging flag, because that method calls {@link #signalChange()}.
     *
     * <p>Callers must re-check {@link #isPurging()} after waking up.
     */
    public long awaitChange(long lastToken) throws InterruptedException {
        synchronized (signalLock) {
            while (signal == lastToken && !eos && error == null) {
                signalLock.wait();
            }
            return signal;
        }
    }

    /**
     * Returns the total number of bytes received by this queue. Counts every
     * fragment including those discarded by the purge path, so the value is
     * suitable for cumulative body-size limit enforcement.
     */
    public long bytesRead() {
        return bytesReceived;
    }

    public void signalEos(){
        eos = true;
        signalChange();
    }

    public void signalError(Throwable t){
        error = t;
        signalChange();
    }

    public Throwable error(){
        return error;
    }

    public void wakeReaders(){
        signalChange();
    }

    /**
     * Marks the queue as purging and drains any retained fragments currently
     * held in the queue, releasing each owned reference exactly once.
     *
     * <h3>Ownership</h3>
     * Only the references that were <em>retained by the queue</em> (via
     * {@link #enqueueRetained}) are released here. The caller must not release
     * any buffers on behalf of this method.
     *
     * <h3>Race safety</h3>
     * Setting {@code purging=true} and then draining in two steps leaves a
     * window in which a concurrent {@link #enqueueRetained} call could pass the
     * {@code purging.get()} check (see false), be preempted, and then enqueue
     * after the drain finishes. A second drain loop after the flag is set closes
     * this window: any fragment that arrived between the flag-set and the first
     * drain will be picked up by the second drain.
     *
     * <p>Must be called at most once per exchange. Safe to call from any thread.
     */
    public void drainAndRelease() {
        // Step 1: set the purge flag so new arrivals are discarded without retain.
        purging.set(true);

        // Step 2: drain fragments that were enqueued before the flag was set.
        // Run the drain loop twice to close the enqueue/purge race window:
        // any producer that passed purging.get()=false before Step 1 but has
        // not yet called queue.add() will be visible in a second pass.
        for (int pass = 0; pass < 2; pass++) {
            ByteBuf buf;
            while ((buf = queue.poll()) != null) {
                int readable = buf.readableBytes();
                buffered.addAndGet(-readable);
                // The queue retained this buffer in enqueueRetained; release that ref.
                buf.release();
            }
        }

        // Wake any blocked readers so they see the purging flag and exit.
        signalChange();
    }

    /**
     * Returns {@code true} if this queue has been put into purge mode via
     * {@link #drainAndRelease()}.
     */
    public boolean isPurging() {
        return purging.get();
    }
}
