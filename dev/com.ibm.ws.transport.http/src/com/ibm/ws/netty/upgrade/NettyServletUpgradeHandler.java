/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.upgrade;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.channel.VoidChannelPromise;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.util.concurrent.ScheduledFuture;
import io.openliberty.netty.internal.impl.QuiesceState;

import io.netty.util.ReferenceCountUtil;

/**
 *
 */
public class NettyServletUpgradeHandler extends ChannelDuplexHandler {

    private static final TraceComponent tc = Tr.register(NettyServletUpgradeHandler.class);

    public static final String NAME = "NettyServletUpgradeHandler";

    private final Channel channel;
    private ChannelHandlerContext context;
    private final CoalescingBufferQueue queue;

    private final ReentrantLock readLock = new ReentrantLock();
    private final Condition readCondition = readLock.newCondition();

    private volatile long minBytesToRead = 0;
    private volatile boolean isReadingAsync = false;

    private final AtomicInteger waitingThreads = new AtomicInteger(0);
    private final AtomicBoolean peerClosed = new AtomicBoolean(false);
    private final AtomicBoolean immediateTimeout = new AtomicBoolean(false);
    
    private TCPReadCompletedCallback callback;
    private VirtualConnection vc;
    private TCPReadRequestContext readContext;


    public NettyServletUpgradeHandler(Channel channel) {
        this.channel = channel;
        this.queue = new CoalescingBufferQueue(channel);        
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context) throws Exception {
        this.context = context;
        snap("handlerAdded");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        snap("userEventTriggered:" + event);
        // java.io.EOFException: Connection closed: Read failed.  Possible end of stream encountered. local=ip:port remote=ip:port
        if (!peerClosed.get() && (event instanceof ChannelInputShutdownEvent || event instanceof ChannelInputShutdownReadComplete)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler ChannelInputShutdownEvent kicked off for channel " + channel);
            }
            peerClosed.set(true);

            if (isReadingAsync && callback != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "NettyServletUpgradeHandler ChannelInputShutdownEvent reading async found!!");
                }
                isReadingAsync = false;
                HttpDispatcher.getExecutorService().execute(() -> {
                    try{
                        callback.error(vc, readContext, new EOFException("Connection closed: Read failed. Possible end of stream. local=" +
                                channel.localAddress() + " remote=" + channel.remoteAddress()));
                    } catch (Exception ignore){}
                });
                return;
            }
            if(queuedDataSize() > 0){
                signalReadReady();
            }
            super.userEventTriggered(context, event);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        snap("channelRead");
        Tr.debug(this, tc, String.format(
            "UPG: channelRead: +%d bytes, queued=%d, async=%s, need=%d", ((ByteBuf)message).readableBytes(), queuedDataSize(), isReadingAsync, minBytesToRead));


        if (message instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) message;
            queue.add(buf.retain());
            ReferenceCountUtil.release(buf);

            if(isReadingAsync && queuedDataSize() >= minBytesToRead){
                isReadingAsync = false;

                if(callback!=null){
                    HttpDispatcher.getExecutorService().execute(() -> {
                        try {
                            
                            Tr.debug(this, tc, String.format(
                                "UPG: deliveryng async: queuedAfter=%d (need=%d)",
                                queuedDataSize(), minBytesToRead));
                            callback.complete(vc, readContext);
                        } catch (Exception e) {}
                    });
                }
                    
            } else if(queuedDataSize() >= minBytesToRead){
                signalReadReady();
            }
            return;
            
        } 
        context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        snap("channelInactive");
        peerClosed.set(true);
        if (isReadingAsync && callback != null) {
            isReadingAsync = false;
            HttpDispatcher.getExecutorService().execute(() -> {
                try {
                    callback.error(vc, readContext,
                                   new EOFException("Connection closed: Read failed. Possible end of stream. local=" +
                                                    channel.localAddress() + " remote=" + channel.remoteAddress()));
                } catch (Exception ignore) {}
            });
        }
        super.channelInactive(context);
    }

    public void immediateTimeout(){
        if (context != null && context.executor().inEventLoop()) {
            HttpDispatcher.getExecutorService().execute(this::immediateTimeout);
            return;
        }
        snap("immediateTimeout");
        immediateTimeout.set(true);
        signalReadReady();

        if (isReadingAsync && callback != null) {
            isReadingAsync = false;
            HttpDispatcher.getExecutorService().execute(() -> {
                try {
                    callback.error(vc, readContext, new SocketTimeoutException("Immediate timeout requested"));
                } catch (Exception ignore) { }
            });
        }

        if(context != null){
            context.executor().execute(() -> immediateTimeout.set(false));
        } else {
            immediateTimeout.set(false);
        }
    }

    private void signalReadReady() {
        readLock.lock();
        try {
            readCondition.signalAll();
        } finally {
            readLock.unlock();
        }
    }

    public void waitForDataRead(long waitMillis) throws InterruptedException {
        waitingThreads.incrementAndGet();
        try {
            readLock.lock();
            try {
                while (!immediateTimeout.get() && queuedDataSize() == 0 && channel.isActive()) {
                    if (!channel.config().isAutoRead())
                        channel.eventLoop().execute(channel::read);
                    if (!readCondition.await(waitMillis, TimeUnit.MILLISECONDS))
                        break;
                }
            } finally {
                readLock.unlock();
            }
        } finally {
            waitingThreads.decrementAndGet();
        }
    }

    public boolean isAsyncReadArmed(){
        return isReadingAsync;
    }

    public synchronized long setToBuffer() {
        
        if (readContext == null || !containsQueuedData())
            return 0L;
        
        WsByteBuffer[] buffers = readContext.getBuffers();

        if (buffers == null || buffers.length == 0 || buffers[0] == null) 
            return 0L;
            
        int capacity = 0;
        for (WsByteBuffer buffer: buffers){
            if(buffer == null) break;
            capacity += Math.max(0, buffer.remaining());
        }

        final int queued = queuedDataSize();
        if (capacity <= 0 || queued <= 0) 
            return 0L;

        final int toRead = Math.min(capacity, queued);
        Tr.debug(this, tc, "setToBuffer queued=" + queue + " capacity= " + capacity + " toRead=" + toRead);

        ByteBuf chunk = read(toRead, null);
        try{
            byte[] bytes = ByteBufUtil.getBytes(chunk, chunk.readerIndex(), toRead, false);
            int off = 0;
            for (WsByteBuffer buffer: buffers){
                if (buffer == null || off >= bytes.length) break;
                final int can = Math.min(buffer.remaining(), bytes.length - off);
                if(can <= 0) continue;
        
                final java.nio.ByteBuffer j = buffer.getWrappedByteBuffer();
                final int before = j.position();
                j.put(bytes, off, can); 
                final int after = j.position();
                Tr.debug(this, tc, "copy can=" + can + " off=" + off 
                            + " bb.pos(before->after)=" + before+"->"+after+ 
                            " ws.remaining(beforeCopy)="+ buffer.remaining());



                buffer.position(j.position()); 
                off += can;
                

                 
                
            }
            return off;
        } finally{
            chunk.release();
        }
    }

    @Override
    public void close(ChannelHandlerContext context, ChannelPromise promise) throws Exception {
        peerClosed.set(true);
        super.close(context, promise);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        if (!context.channel().config().isAutoRead()
            && !peerClosed.get()
            && (isReadingAsync || waitingThreads.get() > 0)
            && queuedDataSize() < minBytesToRead) {
            context.executor().execute(context.channel()::read);
        }
        super.channelReadComplete(context);
    }

    public boolean containsQueuedData() {
        return !queue.isEmpty();
    }

    public int queuedDataSize() {
        return queue.readableBytes();
    }

    public boolean isImmediateTimeout() {
        return immediateTimeout.get();
    }

    public synchronized ByteBuf read(int size, ChannelPromise promise) {
        if (!containsQueuedData())
            return Unpooled.EMPTY_BUFFER;
        if (promise == null)
            return queue.remove(size, new VoidChannelPromise(channel, true));
        return queue.remove(size, promise);
    }

    public void setReadListener(TCPReadCompletedCallback cb) {
        this.callback = cb;
    }

    public void queueAsyncRead(long minBytesToRead) {
        this.minBytesToRead = (int) Math.max(1L, minBytesToRead);
        this.isReadingAsync = true;
        if (!channel.config().isAutoRead())
            channel.eventLoop().execute(channel::read);

        //If we have enough bytes, just complete now
        if (queuedDataSize() >= this.minBytesToRead && callback != null){
            this.isReadingAsync = false;
            HttpDispatcher.getExecutorService().execute(()->{
                try{
                    callback.complete(vc, readContext);
                } catch (Throwable throwable){
                    try{
                        callback.error(vc, readContext, (throwable instanceof IOException) ? (IOException) throwable:
                                new EOFException(String.valueOf(throwable)));
                    } catch (Throwable ignore){}
                }
            });
        }
    }

    public boolean peerClosedConnection() {
        return peerClosed.get();
    }

    public TCPReadCompletedCallback getReadListener() {
        return callback;
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setTCPReadContext(TCPReadRequestContext tcpReadContext) {
        this.readContext = tcpReadContext;
    }

    private String cid() {
        String ch = (channel != null && channel.id() != null) ? channel.id().asShortText() : "no-ch";
        return "cid=" + ch + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    private void snap(String where) {
        String inEL = (context != null && context.executor() != null) ? String.valueOf(context.executor().inEventLoop()) : "n/a";
        Tr.debug(this, tc,
                 String.format("%s %s where=%s active=%s open=%s autoRead=%s inEL=%s async=%s peerClosed=%s immTO=%s waiters=%d queued=%d need=%d",
                               getClass().getSimpleName(), cid(), where,
                               channel != null && channel.isActive(),
                               channel != null && channel.isOpen(),
                               channel != null && channel.config().isAutoRead(),
                               inEL, isReadingAsync, peerClosed.get(), immediateTimeout.get(),
                               waitingThreads.get(), queuedDataSize(), minBytesToRead));
    }

}