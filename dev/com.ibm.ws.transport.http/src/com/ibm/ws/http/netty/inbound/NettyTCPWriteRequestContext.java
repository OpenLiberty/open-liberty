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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

/**
 *
 */
public class NettyTCPWriteRequestContext implements TCPWriteRequestContext {

    private final NettyTCPConnectionContext connectionContext;
    private final ChannelHandlerContext nettyContext;

    private WsByteBuffer[] buffers;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;
    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private ByteBuffer byteBufferArrayOf2[] = null;
    private ByteBuffer byteBufferArrayOf3[] = null;
    private ByteBuffer byteBufferArrayOf4[] = null;

    public NettyTCPWriteRequestContext(NettyTCPConnectionContext connectionContext, ChannelHandlerContext nettyContext) {

        this.connectionContext = connectionContext;
        this.nettyContext = nettyContext;

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
        this.buffers = bufs;

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

        if (bufs != null) {
            int numBufs;
            // reuse an existing byteBufferArray if one was already created
            // kind of hokey, but this allows us to avoid construction of a
            // new array object unless absolutely neccessary

            // following loop will count the number of buffers in
            // the input array rather than relying on the array length
            for (numBufs = 0; numBufs < bufs.length; numBufs++) {
                if (bufs[numBufs] == null) {
                    break;
                }
            }

//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "setBuffers number of buffers is " + numBufs);
//            }

            if (numBufs == 1) {
                if (this.byteBufferArrayOf1 == null) {
                    this.byteBufferArrayOf1 = new ByteBuffer[1];
                }
                this.byteBufferArray = this.byteBufferArrayOf1;
            } else if (numBufs == 2) {
                if (this.byteBufferArrayOf2 == null) {
                    this.byteBufferArrayOf2 = new ByteBuffer[2];
                }
                this.byteBufferArray = this.byteBufferArrayOf2;
            } else if (numBufs == 3) {
                if (this.byteBufferArrayOf3 == null) {
                    this.byteBufferArrayOf3 = new ByteBuffer[3];
                }
                this.byteBufferArray = this.byteBufferArrayOf3;
            } else if (numBufs == 4) {
                if (this.byteBufferArrayOf4 == null) {
                    this.byteBufferArrayOf4 = new ByteBuffer[4];
                }
                this.byteBufferArray = this.byteBufferArrayOf4;

            } else {
                // more than 4 buffers in request, allocate array as needed
                this.byteBufferArray = new ByteBuffer[numBufs];
            }

            if (numBufs > 1) {
                for (int i = 0; i < numBufs; i++) {
                    this.byteBufferArray[i] = bufs[i].getWrappedByteBufferNonSafe();
                }
            } else if (numBufs == 1) {
                this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
            }

        } else {
            // buffers == null, so set byteBufferArray to null also
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

        long writtenBytes = 0;
        for (WsByteBuffer buffer : buffers) {
            if (buffer != null) {
                ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                writtenBytes += nettyBuf.readableBytes();
                ChannelFuture writeFuture = this.nettyContext.write(nettyBuf);
                writeFuture.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        //TODO "write operation has failed"

                    }
                    //Release buffer
                   // ReferenceCountUtil.release(nettyBuf);
                });
            }
        }

        this.nettyContext.flush();
        return writtenBytes;

        //TODO send only numBytes, for now write everything
//        for (WsByteBuffer buffer : buffers) {
//            if (Objects.nonNull(buffer)) {
//                MSP.log("Writing Netty TCP write Context bytes: [" + buffer.remaining()+"]");
//
//                this.nettyContext.channel().write(Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer)));
//

        //this.nettyContext.pipeline().context(NettyServletUpgraUnpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer)));
        // this.nettyContext.pipeline().writeAndFlush(WsByteBufferUtils.asByteArray(buffer));
        //this.nettyContext.write(buffer);
        //    }
        //}

    }

    @Override
    public VirtualConnection write(long numBytes, TCPWriteCompletedCallback callback, boolean forceQueue, int timeout) {
        AtomicInteger pendingWrites = new AtomicInteger(buffers.length);
        try {
            for (WsByteBuffer buffer : buffers) {
                if (buffer != null) {
                    ByteBuf nettyBuf = Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(buffer));
                    ChannelFuture writeFuture = this.nettyContext.write(nettyBuf);
                    writeFuture.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            if (pendingWrites.decrementAndGet() == 0) {
                                callback.complete(null, this);
                            }
                        } else {
                            //callback.error(null, future.cause());
                        }
                        //ReferenceCountUtil.release(nettyBuf);
                    });

                }
            }
            this.nettyContext.flush();
        } catch (Exception e) {
            //callback.error(null, this, e);
        }
        return null;
    }

}
