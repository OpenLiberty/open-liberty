package com.ibm.ws.http.netty.message;

import java.util.ArrayDeque;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

final public class BodyQueue {

    private static final int DEFAULT_HIGH = 256 * 1024;
    private static final int DEFAULT_LOW = 64 * 1024;

    private final ArrayDeque<ByteBuf> queue = new ArrayDeque<>();
    private final int lowWater, highWater;
    private int buffered;
    private volatile boolean eos;
    private volatile Throwable error;
    private final ByteBufAllocator allocator;

    public BodyQueue(ByteBufAllocator allocator){
        this(allocator, DEFAULT_HIGH, DEFAULT_LOW);
    }

    public BodyQueue(ByteBufAllocator allocator, int high, int low){
        this.allocator = allocator;
        this.highWater = Math.max(high, low);
        this.lowWater = low;
    }

    public void enqueueRetained(ByteBuf buf){
        queue.addLast(buf.retain());
        buffered += buf.readableBytes();
    }

    public ByteBuf poll(){
        ByteBuf b = queue.pollFirst();
        if(b!=null){
            buffered -= b.readableBytes();
        }
        return b;
    }

    public boolean wantsInput(){
        return error == null && !eos && buffered < lowWater;
    }

    public boolean isEos(){
        return eos && queue.isEmpty();
    }

    public void signalEos(){
        eos = true;
    }

    public void signalError(Throwable t){
        error = t;
    }

    public Throwable error(){
        return error;
    }


}
