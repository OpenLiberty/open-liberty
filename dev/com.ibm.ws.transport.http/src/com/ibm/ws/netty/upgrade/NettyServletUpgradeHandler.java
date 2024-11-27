/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.upgrade;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.VoidChannelPromise;
import io.openliberty.netty.internal.impl.QuiesceState;
import io.netty.channel.CoalescingBufferQueue;

/**
 *
 */
public class NettyServletUpgradeHandler extends ChannelInboundHandlerAdapter {

    private final CoalescingBufferQueue queue;
    private final Channel channel;
    private long totalBytesRead = 0;

    private final ReentrantLock readLock = new ReentrantLock();
    private final Condition readCondition = readLock.newCondition();

    TCPReadCompletedCallback callback;
    private VirtualConnection vc;
    private TCPReadRequestContext readContext;
    private long minBytesToRead = 0;

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
                totalBytesRead += bytesRead;

                if (totalBytesRead >= minBytesToRead) {
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

    private void signalReadReady() {
        readLock.lock();
        try {
            readCondition.signalAll();

        } finally {
            readLock.unlock();
        }
    }

    public void waitForDataRead(long waitTime) throws InterruptedException {

        readLock.lock();
        try {
            while (!containsQueuedData() && channel.isActive()) {
                if(!readCondition.await(waitTime, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public boolean awaitReadReady(long numBytes, int timeout, TimeUnit unit) {
        minBytesToRead = numBytes; // Set the minimum number of bytes to read

        readLock.lock();
        boolean dataReady = false;
        try {

            if(queuedDataSize()>=numBytes) {
                dataReady = true;

            }else {
                long waitTime = timeout == -1 ? Long.MAX_VALUE : unit.toNanos(timeout);
                long endTime = System.nanoTime() + waitTime;
                while (totalBytesRead < minBytesToRead && channel.isActive() && !QuiesceState.isQuiesceInProgress()) {
                    if (timeout != -1) { // If timeout is not -1, calculate the remaining wait time
                        waitTime = endTime - System.nanoTime();
                        if (waitTime <= 0) break; // Exit if the wait time has expired
                    }
                    // If timeout is -1, this will wait indefinitely until signalled
                    if (timeout == -1) {
                        waitTime = TimeUnit.SECONDS.toNanos(1);
                        try{
                        readCondition.awaitNanos(waitTime);
                        }catch(InterruptedException e){
                            Thread.currentThread().interrupt();
                            continue; // loop back again
                        }
                    } else {
                        readCondition.awaitNanos(waitTime);
                    }
                }

                dataReady = totalBytesRead >= minBytesToRead; // Check if the minimum number of bytes was read
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupt status
        } finally {
            readLock.unlock();
        }

        return dataReady;
    }


    public synchronized long setToBuffer() {
        
        if (!containsQueuedData()) {
            return 0;
        }
        
        

        if (queuedDataSize()>=minBytesToRead) { 
            
            int readTotal = queuedDataSize()>=readContext.getBuffer().remaining() ? readContext.getBuffer().remaining(): queuedDataSize();

            byte[] bytes = ByteBufUtil.getBytes(read(readTotal, null));
            readContext.getBuffer().put(bytes);

            // Reset totalBytesRead after fulfilling the read
            totalBytesRead -= bytes.length; // Adjust totalBytesRead
            return bytes.length;
        }
        return 0;
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
