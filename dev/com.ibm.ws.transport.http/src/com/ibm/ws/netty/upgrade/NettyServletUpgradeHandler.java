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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.channel.VoidChannelPromise;
import io.openliberty.netty.internal.impl.QuiesceState;

/**
 *
 */
public class NettyServletUpgradeHandler extends ChannelDuplexHandler {
    
    private static final TraceComponent tc = Tr.register(NettyServletUpgradeHandler.class);

    private final CoalescingBufferQueue queue;
    private final Channel channel;
    private long totalBytesRead = 0;

    private final ReentrantLock readLock = new ReentrantLock();
    private final Condition readCondition = readLock.newCondition();

    TCPReadCompletedCallback callback;
    private VirtualConnection vc;
    private TCPReadRequestContext readContext;
    private long minBytesToRead = 0;
    
    private AtomicBoolean immediateTimeout = new AtomicBoolean(false);

    /**
     * Initialize the queue that will store the data
     */
    public NettyServletUpgradeHandler(Channel channel) {
        this.queue = new CoalescingBufferQueue(channel);
        this.channel = channel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().closeFuture().addListener(future -> {
            signalReadReady();
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;

            try {
                buf.retain();
                queue.add(buf);
                long bytesRead = buf.readableBytes();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "NettyServletUpgradeHandler channelRead called for channel " + channel + " Adding bytes read: " + bytesRead);
                }
                totalBytesRead += bytesRead;

                if (totalBytesRead >= minBytesToRead) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "NettyServletUpgradeHandler channelRead totalBytesRead greater than minimum bytes requested for channel " + channel);
                    }
                    signalReadReady(); // Signal only if minimum bytes are read
                }

            } catch (Exception e) {
                ctx.fireExceptionCaught(e);
            } finally {
                buf.release();
            }

        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        signalReadReady();
        super.channelInactive(ctx);

    }
    
    public void immediateTimeout() {
        System.out.println("In immediate timeout");
        immediateTimeout.getAndSet(true);
        signalReadReady();
        // TODO Loop to make sure no others reads are taking place
        while(readLock.hasQueuedThreads()) { // Queue here until no queued threads are available
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler immediateTimeout waiting to finish immediate timeout on channel: " + channel);
            }
            System.out.println("NettyServletUpgradeHandler immediateTimeout waiting to finish immediate timeout on channel: " + channel);
            // TODO If this is kept and not removed when disabling auto read, switch this logic to provide assertion of not running
            // in the event loop and run a repeatable task with ScheduledExecutorService
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        immediateTimeout.getAndSet(false);
        System.out.println("End immediate timeout");
    }

    private void signalReadReady() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler signalReadReady locking readLock for channel " + channel);
        }
        readLock.lock();
        try {
            readCondition.signalAll();

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler signalReadReady unlocking readLock for channel " + channel);
            }
            readLock.unlock();
        }
    }

    public void waitForDataRead(long waitTime) throws InterruptedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler waitForDataRead locking readLock for channel " + channel);
        }
        readLock.lock();
        try {
            while (!immediateTimeout.get() && !containsQueuedData() && channel.isActive()) {
                if (!readCondition.await(waitTime, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler waitForDataRead unlocking readLock for channel " + channel);
            }
            readLock.unlock();
        }
    }

    public boolean awaitReadReady(long numBytes, int timeout, TimeUnit unit) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler awaitReadReady called for channel " + channel);
        }
        minBytesToRead = numBytes; // Set the minimum number of bytes to read
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler awaitReadReady locking readLock for channel " + channel);
        }
        readLock.lock();
        boolean dataReady = false;
        try {
            if (queuedDataSize() >= numBytes) {
                dataReady = true;
            } else {
                long waitTime = timeout == -1 ? Long.MAX_VALUE : unit.toNanos(timeout);
                long endTime = System.nanoTime() + waitTime;
                while (!immediateTimeout.get() && totalBytesRead < minBytesToRead && channel.isActive() && !QuiesceState.isQuiesceInProgress()) {
                    if (timeout != -1) { // If timeout is not -1, calculate the remaining wait time
                        waitTime = endTime - System.nanoTime();
                        if (waitTime <= 0)
                            break; // Exit if the wait time has expired
                    }
                    // If timeout is -1, this will wait indefinitely until signalled
                    if (timeout == -1) {
                        waitTime = TimeUnit.SECONDS.toNanos(1);
                        try {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "NettyServletUpgradeHandler awaitReadReady waiting " + waitTime + "ns for min bytes to read: " + minBytesToRead + " with total bytes read: " + totalBytesRead + " on channel: " + channel);
                            }
                            readCondition.awaitNanos(waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            continue; // loop back again
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "NettyServletUpgradeHandler awaitReadReady waiting " + waitTime + " for min bytes to read: " + minBytesToRead + " with total bytes read: " + totalBytesRead + " on channel: " + channel);
                        }
                        readCondition.awaitNanos(waitTime);
                    }
                }

                dataReady = totalBytesRead >= minBytesToRead; // Check if the minimum number of bytes was read
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupt status
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler awaitReadReady unlocking readLock for channel: " + channel);
            }
            readLock.unlock();
        }

        return dataReady;
    }

    @FFDCIgnore(RuntimeException.class)
    public synchronized long setToBuffer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler setToBuffer called for channel: " + channel);
        }

        if (!containsQueuedData()) {
            return 0;
        }
        int currentQueuedDataSize = queuedDataSize();
        int remainingBufferSize = readContext.getBuffer().remaining();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyServletUpgradeHandler setToBuffer currentQueuedSize:" + currentQueuedDataSize + ", remainingBufferSize" + remainingBufferSize);
        }
        if (currentQueuedDataSize >= minBytesToRead) {
            int readTotal = currentQueuedDataSize >= remainingBufferSize ? remainingBufferSize : currentQueuedDataSize;
            byte[] bytes = ByteBufUtil.getBytes(read(readTotal, null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler setToBuffer queue bigger than min bytes. Reading total of: " + readTotal);
            }
            try {
                readContext.getBuffer().put(bytes);
            } catch (RuntimeException e) {
                // TODO: See how best to handle this exception
                // Assume this is async and if we get a runtime exception we can
                // assume the buffer was already release therefore no need to continue here
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "NettyServletUpgradeHandler setToBuffer hit RuntimeException when adding bytes. We assume the buffer was already released so stop thread run.");
                }
                Thread.currentThread().interrupt();
            }
            

            // Reset totalBytesRead after fulfilling the read
            totalBytesRead -= bytes.length; // Adjust totalBytesRead
            return bytes.length;
        }
        return 0;
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // If we close the channel ourselves we need to destroy the connlink used because
        // in legacy, the upgrade handler calls destroy on it while closing the connection
        if (vc != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "NettyServletUpgradeHandler close verifying Virtual Connection maps.");
            }
            String upgraded = (String) (vc.getStateMap().get(TransportConstants.UPGRADED_CONNECTION));
            if ("true".equalsIgnoreCase(upgraded)) {
                Object webConnectionObject = vc.getStateMap().get(TransportConstants.UPGRADED_WEB_CONNECTION_OBJECT);
                if (webConnectionObject != null) {
                    if (webConnectionObject instanceof TransportConnectionAccess) {
                        TransportConnectionAccess tWebConn = (TransportConnectionAccess) webConnectionObject;
                        try {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "NettyServletUpgradeHandler close attempting to close TransportConnectionAccess.");
                            }
                            tWebConn.close();
                        } catch (Exception webConnectionCloseException) {
                            //continue closing other resources
                            //I don't believe the close operation should fail - but record trace if it does
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Failed to close WebConnection {0}", webConnectionCloseException);
                            }
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "call application destroy if not done yet");
                        }
                    }
                }
            }
        }
        super.close(ctx, promise);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    /**
     * Helper method to establish if there is data on the queue to be read
     */
    public boolean containsQueuedData() {
        return !queue.isEmpty();
    }

    /**
     * Helper method to establish if there is data on the queue to be read
     */
    public int queuedDataSize() {
        return queue.readableBytes();
    }
    
    public boolean isImmediateTimeout() {
        return immediateTimeout.get();
    }

    /**
     * Helper method to read from Queue
     */
    public synchronized ByteBuf read(int size, ChannelPromise promise) {
        if (!containsQueuedData()) {
            // No data to send, do we need to wait until we get some? Or do we call back? Or just send error or empty buffer?
            return Unpooled.EMPTY_BUFFER;
        }
        if (promise == null)
            return queue.remove(size, new VoidChannelPromise(channel, true));
        return queue.remove(size, promise);
    }

    /**
     * Helper method to set read listener
     */
    public void setReadListener(TCPReadCompletedCallback callback) {
        this.callback = callback;
    }

    public TCPReadCompletedCallback getReadListener() {
        return this.callback;
    }

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setTCPReadContext(TCPReadRequestContext context) {
        this.readContext = context;
    }
}
