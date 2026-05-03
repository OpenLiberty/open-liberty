package com.ibm.ws.http.netty.message;

import java.util.ArrayDeque;
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
    private long bytesRead;

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

}
