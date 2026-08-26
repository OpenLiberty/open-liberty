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
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
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
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
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

    private final AtomicInteger waitingThreads = new AtomicInteger(0);
    private final AtomicBoolean peerClosed = new AtomicBoolean(false);
    private final AtomicBoolean immediateTimeout = new AtomicBoolean(false);
    private final AtomicInteger queuedBytes = new AtomicInteger(0);
    
    private final AtomicReference<AsyncReadGeneration> asyncRead = new AtomicReference<>();
    private VirtualConnection vc;
    private TCPReadRequestContext readContext;
    private TCPReadCompletedCallback pendingCallback;

    private final AtomicBoolean readPending = new AtomicBoolean(false);
    private final ArrayDeque<CallbackTask> callbackTasks = new ArrayDeque<>();
    private boolean callbackDispatchScheduled;
    private boolean callbackDrainRunning;

    private static final class CallbackTask implements Runnable {
        private final AtomicBoolean delivered = new AtomicBoolean();
        private final Runnable terminal;

        CallbackTask(Runnable terminal) {
            this.terminal = terminal;
        }

        @Override
        public void run() {
            if (delivered.compareAndSet(false, true)) {
                terminal.run();
            }
        }
    }

    private static final class AsyncReadGeneration {
        final TCPReadCompletedCallback callback;
        final VirtualConnection vc;
        final TCPReadRequestContext callbackContext;
        final WsByteBuffer[] destination;
        final int minBytes;
        final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>();

        AsyncReadGeneration(TCPReadCompletedCallback callback,
                            VirtualConnection vc,
                            TCPReadRequestContext readContext,
                            WsByteBuffer[] destination,
                            long minBytes) {
            this.callback = callback;
            this.vc = vc;
            this.destination = destination;
            this.callbackContext = new GenerationReadContext(readContext, destination);
            this.minBytes = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, minBytes));
        }

        void cancelTimeout() {
            ScheduledFuture<?> future = timeout.getAndSet(null);
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    private static final class GenerationReadContext implements TCPReadRequestContext {
        private final TCPReadRequestContext delegate;
        private WsByteBuffer[] buffers;

        GenerationReadContext(TCPReadRequestContext delegate, WsByteBuffer[] buffers) {
            this.delegate = delegate;
            this.buffers = buffers;
        }

        @Override
        public long read(long numBytes, int timeout) throws IOException {
            applyBuffers();
            return delegate.read(numBytes, timeout);
        }

        @Override
        public VirtualConnection read(long numBytes,
                                      TCPReadCompletedCallback callback,
                                      boolean forceQueue,
                                      int timeout) {
            applyBuffers();
            return delegate.read(numBytes, callback, forceQueue, timeout);
        }

        @Override
        public void setJITAllocateSize(int numBytes) {
            delegate.setJITAllocateSize(numBytes);
        }

        @Override
        public boolean getJITAllocateAction() {
            return delegate.getJITAllocateAction();
        }

        @Override
        public TCPConnectionContext getInterface() {
            return delegate.getInterface();
        }

        @Override
        public void clearBuffers() {
            if (buffers != null) {
                for (WsByteBuffer buffer : buffers) {
                    if (buffer == null) {
                        break;
                    }
                    buffer.clear();
                }
            }
        }

        @Override
        public WsByteBuffer[] getBuffers() {
            return buffers;
        }

        @Override
        public void setBuffers(WsByteBuffer[] buffers) {
            this.buffers = buffers;
        }

        @Override
        public WsByteBuffer getBuffer() {
            return buffers == null ? null : buffers[0];
        }

        @Override
        public void setBuffer(WsByteBuffer buffer) {
            buffers = buffer == null ? null : new WsByteBuffer[] { buffer };
        }

        @Override
        public Socket getSocket() {
            return delegate.getSocket();
        }

        private void applyBuffers() {
            delegate.setBuffers(buffers);
        }
    }


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

            if (asyncRead.get() != null) {
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
            super.userEventTriggered(context, event);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) message;
            final int n = buf.readableBytes();
            queue.add(buf.retain());
            queuedBytes.addAndGet(n);
            ReferenceCountUtil.release(buf);

            AsyncReadGeneration generation = asyncRead.get();
            if (generation != null && queuedBytes.get() >= generation.minBytes) {
                Tr.debug(tc, "[UPGRADE-ASYNC] async threshold met; firing callback. bytes=" + queuedBytes.get() + 
                    " minBytesToRead=" + generation.minBytes);
                fireAsyncReadComplete(generation);
            } else if (generation == null && queuedBytes.get() > 0) {
                signalReadReady();
            }
            return;
        }
        context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        peerClosed.set(true);
        AsyncReadGeneration generation = claimAsyncRead(asyncRead.get());
        try {
            super.channelInactive(context);
        } finally {
            if (generation != null) {
                dispatchError(generation, closedReadFailure());
        }
        }
    }

    public void immediateTimeout() {
        if (context != null && !context.executor().inEventLoop()) {
            context.executor().execute(this::immediateTimeout);
            return;
        }
        immediateTimeout.set(true);
        signalReadReady();

        if (asyncRead.get() != null) {
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
        AsyncReadGeneration generation = asyncRead.get();
        Tr.debug(tc, "[UPGRADE-SYSOUT] NettyServletUpgradeHandler.requestRead autoRead="
            + channel.config().isAutoRead()
            + " active=" + channel.isActive()
            + " peerClosed=" + peerClosed.get()
            + " readPending=" + readPending.get()
            + " waitingThreads=" + waitingThreads.get()
            + " isReadingAsync=" + (generation != null)
            + " minBytesToRead=" + (generation == null ? 0 : generation.minBytes)
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
        return asyncRead.get() != null;
    }

    public long setToBuffer() {
        if (readContext == null || !containsQueuedData())
            return 0L;
        return setToBuffers(readContext.getBuffers());
    }

    private long setToBuffers(WsByteBuffer[] buffers) {
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
            final int removed = chunk.readableBytes();
            try {
                int remaining = removed;
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
                    try {
                    chunk.readBytes(dst);
                    } finally {
                    dst.limit(lim);
                    }

                    b.position(dst.position());
                    remaining -= can;
                    copied += can;
                }

                written.set(copied);
            } finally {
                queuedBytes.addAndGet(-removed);
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
        peerClosed.set(true);
        AsyncReadGeneration generation = claimAsyncRead(asyncRead.get());
        try {
        super.close(context, promise);
        } finally {
            if (generation != null) {
                dispatchError(generation, closedReadFailure());
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        readPending.set(false);
        AsyncReadGeneration generation = asyncRead.get();
        int minimum = generation == null ? 1 : generation.minBytes;
        if (!context.channel().config().isAutoRead()
            && !peerClosed.get()
            && (generation != null || waitingThreads.get() > 0)
            && queuedDataSize() < minimum) {
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

    public void setReadListener(TCPReadCompletedCallback callback) {
        pendingCallback = callback;
    }

    public void queueAsyncRead(long minBytesToRead) {
        TCPReadCompletedCallback callback = pendingCallback;
        pendingCallback = null;
        queueAsyncRead(callback,
                       vc,
                       readContext,
                       minBytesToRead,
                       TCPReadRequestContext.NO_TIMEOUT);
    }

    public void queueAsyncRead(TCPReadCompletedCallback callback,
                               VirtualConnection vc,
                               TCPReadRequestContext readContext,
                               long minBytesToRead,
                               int timeoutMillis) {
        WsByteBuffer[] buffers = readContext == null ? null : readContext.getBuffers();
        if (buffers == null || buffers.length == 0 || buffers[0] == null) {
            throw new IllegalArgumentException("No destination buffers supplied for upgraded asynchronous read");
        }

        AsyncReadGeneration generation = new AsyncReadGeneration(callback,
                                                                 vc,
                                                                 readContext,
                                                                 buffers.clone(),
                                                                 minBytesToRead);
        if (!asyncRead.compareAndSet(null, generation)) {
            throw new IllegalStateException("An upgraded asynchronous read is already armed");
        }

        if (peerClosed.get()) {
            fireAsyncReadError(generation, closedReadFailure());
            return;
        }

        Tr.debug(tc, "[UPGRADE-ASYNC] queueAsyncRead : " + 
            "minBytesToRead = " + minBytesToRead + ", " +
            "queuedBytes = " + queuedBytes.get() + ", " +
            "autoRead = " + channel.config().isAutoRead() + ", " +
            "readPending = " + readPending.get() );

        if (timeoutMillis > 0) {
            ScheduledFuture<?> future = channel.eventLoop().schedule(
                () -> fireAsyncReadError(generation, new SocketTimeoutException("Read operation timed out")),
                timeoutMillis,
                TimeUnit.MILLISECONDS);
            generation.timeout.set(future);
            if (asyncRead.get() != generation) {
                generation.cancelTimeout();
            }
        }

        requestRead();

        if (queuedBytes.get() >= generation.minBytes) {
            if (context != null && !context.executor().inEventLoop()) {
                context.executor().execute(() -> fireAsyncReadComplete(generation));
            } else {
                fireAsyncReadComplete(generation);
            }
        }
    }

    private void fireAsyncReadComplete(AsyncReadGeneration expected) {
        AsyncReadGeneration generation = claimAsyncRead(expected);
        if (generation == null) {
            return;
        }

        final long copied;
        try {
            copied = setToBuffers(generation.destination);
        } catch (Throwable failure) {
            dispatchError(generation, asIOException(failure));
            return;
        }
        if (copied < generation.minBytes) {
            dispatchError(generation,
                          new IOException("Upgraded read completed before minimum bytes were copied: copied="
                                          + copied + " minimum=" + generation.minBytes));
            return;
        }

        executeAsyncReadCallback(() -> {
            try {
                generation.callback.complete(generation.vc, generation.callbackContext);
            } catch (Throwable failure) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Upgraded asynchronous read completion callback failed after terminal success: " + failure);
                }
            }
        });
    }

    private void fireAsyncReadError(IOException error) {
        fireAsyncReadError(asyncRead.get(), error);
    }

    private void fireAsyncReadError(AsyncReadGeneration expected, IOException error) {
        AsyncReadGeneration generation = claimAsyncRead(expected);
        if (generation != null) {
            dispatchError(generation, error);
        }
    }

    private void dispatchError(AsyncReadGeneration generation, IOException error) {
        executeAsyncReadCallback(() -> {
            try {
                generation.callback.error(generation.vc, generation.callbackContext, error);
            } catch (Throwable failure) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Upgraded asynchronous read error callback failed after terminal error: " + failure);
                }
            }
        });
    }

    private AsyncReadGeneration claimAsyncRead(AsyncReadGeneration expected) {
        if (expected == null || !asyncRead.compareAndSet(expected, null)) {
            return null;
        }
        expected.cancelTimeout();
        return expected;
    }

    private static IOException asIOException(Throwable failure) {
        return failure instanceof IOException
               ? (IOException) failure
               : new IOException("Upgraded asynchronous read failed", failure);
        }

    private EOFException closedReadFailure() {
        return new EOFException("Connection closed: Read failed. Possible end of stream. local=" +
                                channel.localAddress() + " remote=" + channel.remoteAddress());
    }

    private void executeAsyncReadCallback(Runnable callbackTask) {
        CallbackTask terminal = new CallbackTask(callbackTask);
        ExecutorService executor = HttpDispatcher.getExecutorService();
        boolean drainInline = false;
        boolean submit = false;
        synchronized (callbackTasks) {
            callbackTasks.add(terminal);
            if (!callbackDrainRunning && !callbackDispatchScheduled) {
                if (executor == null) {
                    callbackDrainRunning = true;
                    drainInline = true;
                } else {
                    callbackDispatchScheduled = true;
                    submit = true;
                }
            }
        }

        if (drainInline) {
            drainOwnedCallbacks();
        } else if (submit) {
            try {
                executor.execute(this::drainAsyncReadCallbacks);
            } catch (RuntimeException failure) {
                boolean recover = false;
                synchronized (callbackTasks) {
                    if (callbackDispatchScheduled && !callbackDrainRunning) {
                        callbackDispatchScheduled = false;
                        callbackDrainRunning = true;
                        recover = true;
                    }
                }
                if (recover) {
                    drainOwnedCallbacks();
                }
            }
        }
    }

    private void drainAsyncReadCallbacks() {
        synchronized (callbackTasks) {
            if (callbackDrainRunning || !callbackDispatchScheduled) {
                return;
            }
            callbackDispatchScheduled = false;
            callbackDrainRunning = true;
        }
        drainOwnedCallbacks();
    }

    private void drainOwnedCallbacks() {
        while (true) {
            CallbackTask task;
            synchronized (callbackTasks) {
                task = callbackTasks.poll();
                if (task == null) {
                    callbackDrainRunning = false;
                    return;
                }
            }
            runCallbackTask(task);
        }
    }

    private void runCallbackTask(CallbackTask task) {
        if (task == null) {
            return;
        }
        try {
            task.run();
        } catch (Throwable failure) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Upgraded asynchronous read terminal callback failed: " + failure);
            }
        }
    }

    public boolean peerClosedConnection() {
        return peerClosed.get();
    }

    public TCPReadCompletedCallback getReadListener() {
        AsyncReadGeneration generation = asyncRead.get();
        return generation == null ? pendingCallback : generation.callback;
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setTCPReadContext(TCPReadRequestContext tcpReadContext) {
        this.readContext = tcpReadContext;
    }

}