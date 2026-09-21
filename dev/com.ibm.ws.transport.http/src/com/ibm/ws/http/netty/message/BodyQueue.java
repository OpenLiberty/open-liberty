package com.ibm.ws.http.netty.message;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private long bytesRead;

    /**
     * Set to {@code true} once the purge lifecycle begins. While purging,
     * {@link #enqueueRetained} releases the buffer immediately instead of
     * queuing it, so arriving fragments are discarded without accumulating.
     * Reads from the application stream are also blocked while purging.
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

    public void enqueueRetained(ByteBuf buf){
        // If a purge is in progress, discard arriving fragments immediately.
        if (purging.get()) {
            // buf is not retained; just release and return.
            ReferenceCountUtil.safeRelease(buf);
            return;
        }
        queue.add(buf.retain());
        bytesRead += buf.readableBytes();
        buffered.addAndGet(buf.readableBytes());
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

    public long awaitChange(long lastToken) throws InterruptedException {
        synchronized (signalLock) {
            while (signal == lastToken && !eos && error == null) {
                signalLock.wait();
            }
            return signal;
        }
    }

    public long bytesRead() {
        return bytesRead;
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
     * Drains and releases all buffered {@link ByteBuf} fragments still held in
     * this queue, and marks the queue as purging so that future calls to
     * {@link #enqueueRetained} discard rather than queue arriving data.
     *
     * <p>Must be called at most once per exchange, after the application has
     * finished with the body and before the connection is offered for reuse.
     * Safe to call from any thread.
     */
    public void drainAndRelease() {
        purging.set(true);
        ByteBuf buf;
        while ((buf = queue.poll()) != null) {
            int readable = buf.readableBytes();
            buffered.addAndGet(-readable);
            ReferenceCountUtil.safeRelease(buf);
        }
        // Wake any blocked readers so they see the purging flag.
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
