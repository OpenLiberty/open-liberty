/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicInteger queuedBytes = new AtomicInteger(0);
    
    private final AtomicReference<TCPReadCompletedCallback> callback = new AtomicReference<>();
    private VirtualConnection vc;
    private TCPReadRequestContext readContext;

    private final AtomicBoolean readPending = new AtomicBoolean(false);


    public NettyServletUpgradeHandler(Channel channel) {
        this.channel = channel;
        this.queue = new CoalescingBufferQueue(channel);        
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context) throws Exception {
        this.context = context;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        // java.io.EOFException: Connection closed: Read failed.  Possible end of stream encountered. local=ip:port remote=ip:port
        if (!peerClosed.get() && (event instanceof ChannelInputShutdownEvent || event instanceof ChannelInputShutdownReadComplete)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler ChannelInputShutdownEvent kicked off for channel " + channel);
            }
            peerClosed.set(true);

            if (isReadingAsync && callback.get() != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "NettyServletUpgradeHandler ChannelInputShutdownEvent reading async found!!");
                }
                fireAsyncReadError(new EOFException("Connection closed: Read failed. Possible end of stream. local=" +
                                                    channel.localAddress() + " remote=" + channel.remoteAddress()));
                return;
            }
            if(queuedDataSize() > 0){
                signalReadReady();
            }
        }
        super.userEventTriggered(context, event);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) message;
            final int n = buf.readableBytes();
            queue.add(buf.retain());
            queuedBytes.addAndGet(n);
            ReferenceCountUtil.release(buf);

            if (isReadingAsync && queuedBytes.get() >= minBytesToRead) {
                Tr.debug(tc, "[UPGRADE-ASYNC] async threshold met; firing callback. bytes=" + queuedBytes.get() + 
                    " minBytesToRead=" + minBytesToRead);
                fireAsyncReadComplete();
            } else if (queuedBytes.get() >= minBytesToRead) {
                signalReadReady();
            }
            return;
        }
        context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        peerClosed.set(true);
        if (isReadingAsync && callback.get() != null) {
            ExecutorService executor = HttpDispatcher.getExecutorService();
            if (executor == null) {
                // Dispatcher is already deactivated - nothing to schedule.
                return;
            }

            fireAsyncReadError(new EOFException("Connection closed: Read failed. Possible end of stream. local=" +
                                                channel.localAddress() + " remote=" + channel.remoteAddress()));
        }
        super.channelInactive(context);
    }

    public void immediateTimeout() {
        if (context != null && !context.executor().inEventLoop()) {
            context.executor().execute(this::immediateTimeout);
            return;
        }
        immediateTimeout.set(true);
        signalReadReady();

        if (isReadingAsync && callback.get() != null) {
            fireAsyncReadError(new SocketTimeoutException("Immediate timeout requested"));
        }
        if (context != null) {
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

    public void requestReadIfNeeded(){
        requestRead();
    }

    public boolean isReadPending(){
        return readPending.get();
    }

    private void requestRead(){
        Tr.debug(tc, "[UPGRADE-SYSOUT] NettyServletUpgradeHandler.requestRead autoRead="
            + channel.config().isAutoRead()
            + " active=" + channel.isActive()
            + " peerClosed=" + peerClosed.get()
            + " readPending=" + readPending.get()
            + " waitingThreads=" + waitingThreads.get()
            + " isReadingAsync=" + isReadingAsync
            + " minBytesToRead=" + minBytesToRead
            + " queuedBytes=" + queuedBytes.get());
        if(peerClosed.get()){
            return;
        }
        if(channel == null || !channel.isActive()){
            return;
        }
        if(channel.config().isAutoRead()){
            return;
        }
        if(!readPending.compareAndSet(false, true)){
            return;
        }
        channel.eventLoop().execute(channel::read);

    }

    public void waitForDataRead(long waitMillis) throws InterruptedException {
        waitingThreads.incrementAndGet();
        try {
            readLock.lock();
            try {
                while (!immediateTimeout.get() && queuedDataSize() == 0 && channel.isActive()) {
                    requestRead();
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

    public long setToBuffer() {
        
        if (readContext == null || !containsQueuedData())
            return 0L;

        final WsByteBuffer[] buffers = readContext.getBuffers();
        if (buffers == null || buffers.length == 0 || buffers[0] == null)
            return 0L;

        final AtomicLong written = new AtomicLong(0L);
        final Runnable task = () -> {
            int capacity = 0;
            for (WsByteBuffer b : buffers) {
                if (b == null)
                    break;
                capacity += Math.max(0, b.remaining());
            }
            final int available = queuedBytes.get();
            final int toRead = Math.min(capacity, available);
            if (toRead <= 0) {
                written.set(0L);
                return;
            }

            ByteBuf chunk = read(toRead, null);
            try {
                int remaining = chunk.readableBytes();
                int copied = 0;

                for (WsByteBuffer b : buffers) {
                    if (b == null || remaining == 0)
                        break;
                    final java.nio.ByteBuffer dst = b.getWrappedByteBuffer();
                    final int can = Math.min(dst.remaining(), remaining);
                    if (can <= 0)
                        continue;

                    final int lim = dst.limit();
                    final int pos = dst.position();
                    dst.limit(pos + can);
                    chunk.readBytes(dst);
                    dst.limit(lim);

                    b.position(dst.position());
                    remaining -= can;
                    copied += can;
                }

            
                if (copied > 0) {
                    queuedBytes.addAndGet(-copied);
                }
                written.set(copied);
            } finally {
                chunk.release();
            }
        };

        if (context != null && !context.executor().inEventLoop()) {
            final CountDownLatch latch = new CountDownLatch(1);
            context.executor().execute(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await(250, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } else {
            task.run();
        }

        return written.get();
    }

    @Override
    public void close(ChannelHandlerContext context, ChannelPromise promise) throws Exception {
        if (vc != null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(this, tc, "NettyServletUpgradeHandler close verifying Virtual Connection maps");
            }

            String upgraded = (String) (vc.getStateMap().get(TransportConstants.UPGRADED_CONNECTION));
            if ("true".equalsIgnoreCase(upgraded)) {
                Object webConnectionObject = vc.getStateMap().get(TransportConstants.UPGRADED_WEB_CONNECTION_OBJECT);
                if (webConnectionObject != null) {
                    if (webConnectionObject instanceof TransportConnectionAccess) {
                        TransportConnectionAccess tWebConn = (TransportConnectionAccess) webConnectionObject;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "NettyServletUpgradeHandler close attempting to close TransportConnectionAccess.");
                        }

                        // close() needs to run on a managed thread rather than the Netty I/O event loop thread.
                        // This is necessary for environments such as CICS where the I/O eventloop thread is not a CICS-enabled thread.
                        HttpDispatcher.getExecutorService().execute(() -> {
                            try {
                                tWebConn.close();
                            } catch (Exception webConnectionCloseException) {
                                //continue closing other resources
                                //I don't believe the close operation should fail - but record trace if it does
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "NettyServletUpgradeHandler Failed to close WebConnection {0}", webConnectionCloseException);
                                }
                            }
                        });
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "call application destroy if not done yet");
                        }
                    }
                } 
            }
        }

        peerClosed.set(true);
        super.close(context, promise);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        readPending.set(false);
        if (!context.channel().config().isAutoRead()
            && !peerClosed.get()
            && (isReadingAsync || waitingThreads.get() > 0)
            && queuedDataSize() < minBytesToRead) {
            requestRead();
        }
        super.channelReadComplete(context);
    }

    public boolean containsQueuedData() {
        return queuedBytes.get() > 0;
    }

    public int queuedDataSize() {
        return queuedBytes.get();
    }

    public boolean isImmediateTimeout() {
        return immediateTimeout.get();
    }

    public synchronized ByteBuf read(int size, ChannelPromise promise) {
        if (context != null && !context.executor().inEventLoop()) {
            throw new IllegalStateException("Upgrade queue read must run on the channel EventLoop");
        }
        if (!containsQueuedData())
            return Unpooled.EMPTY_BUFFER;

        ByteBuf out = (promise == null) ? queue.remove(size, new VoidChannelPromise(channel, true)) : queue.remove(size, promise);


        return out;
    }

    public void setReadListener(TCPReadCompletedCallback cb) {
        callback.set(cb);
    }

    public void queueAsyncRead(long minBytesToRead) {
        this.minBytesToRead = (int) Math.max(1L, minBytesToRead);
        this.isReadingAsync = true;

        Tr.debug(tc, "[UPGRADE-ASYNC] queueAsyncRead : " + 
            "minBytesToRead = " + minBytesToRead + ", " +
            "queuedBytes = " + queuedBytes.get() + ", " +
            "autoRead = " + channel.config().isAutoRead() + ", " +
            "readPending = " + readPending.get() );

        requestRead();

        final int q = queuedBytes.get();
        if (q >= this.minBytesToRead && callback.get() != null) {
            fireAsyncReadComplete();
        }
    }

    private void fireAsyncReadComplete() {
        TCPReadCompletedCallback cb = claimAsyncReadCallback();
        if (cb == null) {
            return;
        }
        executeAsyncReadCallback(() -> {
            try {
                cb.complete(vc, readContext);
            } catch (Throwable t) {
                try {
                    cb.error(vc, readContext, (t instanceof IOException) ? (IOException) t : new EOFException(String.valueOf(t)));
                } catch (Throwable ignore) {
                }
            }
        });
    }

    private void fireAsyncReadError(IOException error) {
        TCPReadCompletedCallback cb = claimAsyncReadCallback();
        if (cb == null) {
            return;
        }
        executeAsyncReadCallback(() -> {
            try {
                cb.error(vc, readContext, error);
            } catch (Exception ignore) {
            }
        });
    }

    private TCPReadCompletedCallback claimAsyncReadCallback() {
        TCPReadCompletedCallback cb = callback.getAndSet(null);
        if (cb != null) {
            isReadingAsync = false;
        }
        return cb;
    }

    private void executeAsyncReadCallback(Runnable callbackTask) {
        ExecutorService executor = HttpDispatcher.getExecutorService();
        if (executor != null) {
            executor.execute(callbackTask);
        }
    }

    public boolean peerClosedConnection() {
        return peerClosed.get();
    }

    public TCPReadCompletedCallback getReadListener() {
        return callback.get();
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setTCPReadContext(TCPReadRequestContext tcpReadContext) {
        this.readContext = tcpReadContext;
    }

}