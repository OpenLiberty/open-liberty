/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 *
 */
public class WsByteBufferChunkedInput implements ChunkedInput<ByteBuf> {

    private final Queue<ByteBuf> chunks = new LinkedBlockingQueue<>();
    private boolean endOfInput = Boolean.FALSE;

    public WsByteBufferChunkedInput(WsByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            int chunkSize = Math.min(buffer.remaining(), 8192);
            byte[] chunkData = new byte[chunkSize];
            buffer.get(chunkData);
            chunks.add(Unpooled.wrappedBuffer(chunkData));
        }
        endOfInput = Boolean.TRUE;
    }

    @Override
    public void close() throws Exception {
        //TODO: release buffer?
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return endOfInput;
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public long progress() {
        return 0; //No need to track progress
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext arg0) throws Exception {
        return chunks.poll();
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator arg0) throws Exception {
        // TODO Auto-generated method stub
        return chunks.poll();
    }

}
