/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;

/**
 *
 */
public class NettyTCPWriteRequestContext implements TCPWriteRequestContext {

    private final NettyTCPConnectionContext connectionContext;
    private final Channel nettyChannel;

    private WsByteBuffer[] buffers;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;
    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private final ByteBuffer byteBufferArrayOf2[] = null;
    private final ByteBuffer byteBufferArrayOf3[] = null;
    private final ByteBuffer byteBufferArrayOf4[] = null;

    public NettyTCPWriteRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {

        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;

    }

    @Override
    public TCPConnectionContext getInterface() {
        // TODO Auto-generated method stub
        return connectionContext;
    }

    @Override
    public void clearBuffers() {
        if (Objects.nonNull(this.buffers)) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }

    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {
        MSP.log("Setting buffers on, have X buffers: " + bufs.length);
        // Assign the new buffers
        this.buffers = bufs;

        // If buffers are not null, ensure they're compacted to remove any trailing nulls
        if (bufs != null) {
            // Determine the actual number of non-null buffers
            int numBufs = 0;
            for (WsByteBuffer buf : bufs) {
                if (buf == null) {
                    break;
                }
                numBufs++;
            }

            // If there are trailing nulls, create a new array without them
            if (numBufs != bufs.length) {
                this.buffers = new WsByteBuffer[numBufs];
                System.arraycopy(bufs, 0, this.buffers, 0, numBufs);
            }
        }

        // Reset arrays to free memory quicker.
        if (this.byteBufferArray != null) {
            Arrays.fill(this.byteBufferArray, null); // Efficiently set all elements to null
            this.byteBufferArray = null;
        }

        if (this.byteBufferArrayDirect != null) {
            Arrays.fill(this.byteBufferArrayDirect, null); // Efficiently set all elements to null
            this.byteBufferArrayDirect = null;
        }

        // Update byteBufferArray based on the new buffers
        if (this.buffers != null && this.buffers.length > 0) {
            this.byteBufferArray = new ByteBuffer[this.buffers.length];
            for (int i = 0; i < this.buffers.length; i++) {
                this.byteBufferArray[i] = this.buffers[i].getWrappedByteBufferNonSafe();
            }
        } else {
            // If there are no buffers, set byteBufferArray to null
            this.byteBufferArray = null;
        }

        MSP.log("How many buffers do we have: " + this.buffers.length);

    }
//    public void setBuffers(WsByteBuffer[] bufs) {
//        this.buffers = bufs;
//
//        // reset arrays to free memory quicker.
//        if (this.byteBufferArray != null) {
//            // reset references
//            for (int i = 0; i < this.byteBufferArray.length; i++) {
//                this.byteBufferArray[i] = null;
//            }
//        }
//
//        if (this.byteBufferArrayDirect != null) {
//            // reset references
//            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
//                this.byteBufferArrayDirect[i] = null;
//            }
//            this.byteBufferArrayDirect = null;
//        }
//
//        if (bufs != null) {
//            int numBufs;
//            // reuse an existing byteBufferArray if one was already created
//            // kind of hacky, but this allows us to avoid construction of a
//            // new array object unless absolutely necessary
//
//            // following loop will count the number of buffers in
//            // the input array rather than relying on the array length
//            for (numBufs = 0; numBufs < bufs.length; numBufs++) {
//                if (bufs[numBufs] == null) {
//                    break;
//                }
//            }
//
////            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
////                Tr.debug(tc, "setBuffers number of buffers is " + numBufs);
////            }
//
//            if (numBufs == 1) {
//                if (this.byteBufferArrayOf1 == null) {
//                    this.byteBufferArrayOf1 = new ByteBuffer[1];
//                }
//                this.byteBufferArray = this.byteBufferArrayOf1;
//            } else if (numBufs == 2) {
//                if (this.byteBufferArrayOf2 == null) {
//                    this.byteBufferArrayOf2 = new ByteBuffer[2];
//                }
//                this.byteBufferArray = this.byteBufferArrayOf2;
//            } else if (numBufs == 3) {
//                if (this.byteBufferArrayOf3 == null) {
//                    this.byteBufferArrayOf3 = new ByteBuffer[3];
//                }
//                this.byteBufferArray = this.byteBufferArrayOf3;
//            } else if (numBufs == 4) {
//                if (this.byteBufferArrayOf4 == null) {
//                    this.byteBufferArrayOf4 = new ByteBuffer[4];
//                }
//                this.byteBufferArray = this.byteBufferArrayOf4;
//
//            } else {
//                // more than 4 buffers in request, allocate array as needed
//                this.byteBufferArray = new ByteBuffer[numBufs];
//            }
//
//            if (numBufs > 1) {
//                for (int i = 0; i < numBufs; i++) {
//                    this.byteBufferArray[i] = bufs[i].getWrappedByteBufferNonSafe();
//                }
//            } else if (numBufs == 1) {
//                this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
//            }
//
//        } else {
//            // buffers == null, so set byteBufferArray to null also
//            this.byteBufferArray = null;
//        }
//    }

    @Override
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    @Override
    public void setBuffer(WsByteBuffer buf) {

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }
        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }
        this.defaultBuffers[0] = null; // reset reference

        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;

            if (this.byteBufferArrayOf1 == null) {
                this.byteBufferArrayOf1 = new ByteBuffer[1];
            }
            this.byteBufferArray = this.byteBufferArrayOf1;
            this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();

        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }

    }

    @Override
    public long write(long numBytes, int timeout) throws IOException {
    MSP.log("Trying to write synchronously: numBytes->" + numBytes + " timeout->" + timeout);
    AtomicLong writtenBytes = new AtomicLong(0);

    //

    if(!nettyChannel.isWritable()) {
        MSP.log("not writable, wrote 0, not waiting");
        return writtenBytes.get();
    }

    // Use a CountDownLatch for the flush operation
    CountDownLatch latch = new CountDownLatch(1);

    final AtomicReference<Throwable> writeFailure = new AtomicReference<>(null);

    try {
        for (WsByteBuffer buffer : buffers) {
            if (buffer != null) {
                ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                writtenBytes.addAndGet(nettyBuf.readableBytes());
                MSP.log("WRITING -> " + nettyBuf.readableBytes() + " bytes.");

                this.nettyChannel.write(nettyBuf); // Write data to the channel
            }
        }
        
        // Flush all pending writes
        ChannelFuture flushFuture = this.nettyChannel.writeAndFlush(Unpooled.EMPTY_BUFFER);
        
        // Add listener to the flush operation
        flushFuture.addListener(future -> {
            if (!future.isSuccess()) {
                writeFailure.set(future.cause());
            }
            // Countdown latch once flush operation completes
            latch.countDown();
        });

        // Set default timeout to 30 seconds if USE_CHANNEL_TIMEOUT is specified
        if (timeout == USE_CHANNEL_TIMEOUT) {
            timeout = 30000; // 30 seconds in milliseconds
        }

        if (timeout == IMMED_TIMEOUT) { // Check for immediate timeout
            return 0; // Return immediately
        } else if (timeout != NO_TIMEOUT) { // Check if a timeout value is specified
            if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new IOException("Write operation timed out");
            }
        }

        if (writeFailure.get() != null) {
            throw new IOException("Write operation failed", writeFailure.get());
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Thread was interrupted while waiting for write to complete", e);
    }

    return writtenBytes.get(); // Return the total written bytes
}

    @Override
    public VirtualConnection write(long numBytes, TCPWriteCompletedCallback callback, boolean forceQueue, int timeout) {
        MSP.log("NETTY TCP WRITE CONTEXT -> async");
        AtomicInteger pendingWrites = new AtomicInteger(buffers.length);
        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer != null) {
                    ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                    ChannelFuture writeFuture = this.nettyChannel.write(nettyBuf);
                    writeFuture.addListener((ChannelFutureListener) future -> {
                        try {
                            if (future.isSuccess()) {
                                if (pendingWrites.decrementAndGet() == 0) {
                                    callback.complete(null, this);
                                }
                            } else {
                                //callback.error(null, future.cause());
                            }
                        } finally {
                            ReferenceCountUtil.release(nettyBuf);
                        }
                    });

                }
            }
            this.nettyChannel.flush();
        } catch (Exception e) {
            //callback.error(null, this, e);
        }
        return null;
    }

}
