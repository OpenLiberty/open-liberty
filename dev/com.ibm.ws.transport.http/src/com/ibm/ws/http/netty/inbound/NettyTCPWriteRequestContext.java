/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.NettyHttpConstants;
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
import io.netty.handler.stream.ChunkedInput;
import io.openliberty.http.netty.stream.WsByteBufferChunkedInput;

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

    private VirtualConnection vc;
    private String streamID = "-1";

    public NettyTCPWriteRequestContext(NettyTCPConnectionContext connectionContext, Channel nettyChannel) {

        this.connectionContext = connectionContext;
        this.nettyChannel = nettyChannel;
    }

    @Override
    public TCPConnectionContext getInterface() {
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

    public void setVC(VirtualConnection vc) {
        this.vc = vc;
    }

    public void setStreamId(String streamId) {
        this.streamID = streamId;
    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {

        if (Objects.isNull(bufs)) {
            clearBuffers();
            return;
        }
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

    }

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
        AtomicLong writtenBytes = new AtomicLong(0);
        // Check if "Content-Length" is set for this channel
        boolean hasContentLength = nettyChannel.hasAttr(NettyHttpConstants.CONTENT_LENGTH) && Objects.nonNull(nettyChannel.attr(NettyHttpConstants.CONTENT_LENGTH).get());

        //check if wsoc
        final String protocol = nettyChannel.attr(NettyHttpConstants.PROTOCOL).get();

        final boolean isHttp10 = "HTTP10".equals(protocol);

        final boolean isWsoc = "WebSocket".equals(protocol);

        final boolean isH2 = "HTTP2".equals(protocol);
        if (!nettyChannel.isWritable()) {
            return writtenBytes.get();
        }

        // Use a CountDownLatch for the flush operation
        CountDownLatch latch = new CountDownLatch(1);

        final AtomicReference<Throwable> writeFailure = new AtomicReference<>(null);

        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer != null && buffer.remaining() != 0) {

                    if (isH2) {
                        writtenBytes.addAndGet(buffer.remaining());
                        AbstractMap.SimpleEntry<Integer, WsByteBuffer> entry = new AbstractMap.SimpleEntry<Integer, WsByteBuffer>(Integer.valueOf(this.streamID), HttpDispatcher.getBufferManager().wrap(WsByteBufferUtils.asByteArray(buffer)));
                        this.nettyChannel.write(entry);
                    }

                    else if (hasContentLength || isWsoc || isHttp10) {
                        ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                        this.nettyChannel.write(nettyBuf); // Write data to the channel
                        writtenBytes.addAndGet(nettyBuf.readableBytes());
                    }

                    else {
                        ChunkedInput<ByteBuf> chunkedInput = new WsByteBufferChunkedInput(buffer);
                        ChannelFuture chunkFuture = nettyChannel.writeAndFlush(chunkedInput);
                        chunkFuture.awaitUninterruptibly();
                        writtenBytes.addAndGet(chunkedInput.length());
                    }
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
                timeout = 60000; // 30 seconds in milliseconds
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
        boolean wasWritable = nettyChannel.isWritable();
        long totalWrittenBytes = 0;
        ChannelFuture lastWriteFuture = null;
        boolean hasContentLength = nettyChannel.hasAttr(NettyHttpConstants.CONTENT_LENGTH) && Objects.nonNull(nettyChannel.attr(NettyHttpConstants.CONTENT_LENGTH).get());
        //check if wsoc
        final String protocol = nettyChannel.attr(NettyHttpConstants.PROTOCOL).get();

        final boolean isHttp10 = "HTTP10".equals(protocol);

        final boolean isWsoc = "WebSocket".equals(protocol);

        final boolean isH2 = "HTTP2".equals(protocol);

        if (Objects.isNull(buffers)) {
            return null;
        }

        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer != null && buffer.hasRemaining()) { // Check if buffer is not null and has data
                    byte[] byteArray = WsByteBufferUtils.asByteArray(buffer);
                    if (byteArray != null) {

                        if (isH2) {
                            totalWrittenBytes += buffer.remaining();
                            AbstractMap.SimpleEntry<Integer, WsByteBuffer> entry = new AbstractMap.SimpleEntry<Integer, WsByteBuffer>(Integer.valueOf(this.streamID), HttpDispatcher.getBufferManager().wrap(WsByteBufferUtils.asByteArray(buffer)));
                            this.nettyChannel.writeAndFlush(entry);

                        }

                        else if (hasContentLength || isWsoc || isHttp10) {
                            ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                            lastWriteFuture = this.nettyChannel.writeAndFlush(nettyBuf); // Write data to the channel
                            totalWrittenBytes += nettyBuf.readableBytes();
                        }

                        else {
                            ChunkedInput<ByteBuf> chunkedInput = new WsByteBufferChunkedInput(buffer);
                            lastWriteFuture = nettyChannel.writeAndFlush(chunkedInput);
                            totalWrittenBytes += chunkedInput.length();
                        }

//                        ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
//                        lastWriteFuture = nettyChannel.write(nettyBuf);
//                        totalWrittenBytes += nettyBuf.readableBytes();
                    }
                }
            }

            boolean stillWritable = nettyChannel.isWritable();
            //nettyChannel.flush();

            if (lastWriteFuture == null && wasWritable && stillWritable && totalWrittenBytes >= numBytes) {
                return vc;

            } else {

                if (lastWriteFuture != null) {
                    // We don't have to do the callback if everything wrote properly
                    if (lastWriteFuture.isDone()) {
                        return vc;
                    }
                    lastWriteFuture.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            callback.complete(vc, this);
                        } else {
                            callback.error(vc, this, new IOException(future.cause()));
                        }
                    });
                }
            }

        } catch (Exception e) {
            callback.error(vc, null, new IOException(e));
        }
        return null; // Return null as the write operation is queued or forced to queue
    }
}