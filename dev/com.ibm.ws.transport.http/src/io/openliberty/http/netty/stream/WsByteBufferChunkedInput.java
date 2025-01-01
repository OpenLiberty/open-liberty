/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.stream;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * A {@code ChunkedInput} implementation that reads data incrementally from a {@link WsByteBuffer}
 * and provides it in fixed-size chunks. Once the end of the buffer is reached, no more
 * data is returned.
 */
public class WsByteBufferChunkedInput implements ChunkedInput<ByteBuf> {

    private final WsByteBuffer buffer;
    private final int chunkSize;
    private boolean endOfInput;
    private long progress;

    /**
     * Creates a new {@code WsByteBufferChunkedInput} with a default chunk size of 8192 bytes.
     *
     * @param buffer the data source from which chunks are read
     */
    public WsByteBufferChunkedInput(WsByteBuffer buffer) {
        this(buffer, 8192);
    }

    /**
     * Creates a new {@code WsByteBufferChunkedInput} with a specified chunk size.
     * A chunk size of 8192 bytes is used if the provided value is less than or equal to zero.
     *
     * @param buffer    the data source from which chunks are read
     * @param chunkSize the maximum number of bytes to include in each chunk
     */
    public WsByteBufferChunkedInput(WsByteBuffer buffer, int chunkSize) {
        this.buffer = buffer;
        this.chunkSize = (chunkSize > 0) ? chunkSize : 8192;
        this.endOfInput = false;
        this.progress = 0;
    }

    /**
     * Reads the next chunk of data from the underlying {@link WsByteBuffer}.
     * Returns {@code null} if the end of the buffer has been reached or if no more data is available.
     *
     * @param allocator the allocator used to create or wrap buffers
     * @return a {@link ByteBuf} containing up to {@code chunkSize} bytes, or {@code null} if none remain
     * @throws Exception if an error occurs while reading data
     */
    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
        if (endOfInput || !buffer.hasRemaining()) {
            endOfInput = true;
            return null;
        }
        int nextChunkSize = Math.min(buffer.remaining(), chunkSize);
        byte[] chunkData = new byte[nextChunkSize];
        buffer.get(chunkData);
        progress += nextChunkSize;
        return Unpooled.wrappedBuffer(chunkData);
    }

    @Override
    @Deprecated
    public ByteBuf readChunk(ChannelHandlerContext context) throws Exception {
        return readChunk(context.alloc());
    }

    /**
     * Closes this chunked input and performs any necessary cleanup. No additional action is taken by default.
     *
     * @throws Exception if closing fails
     */
    @Override
    public void close() throws Exception {

    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return endOfInput;
    }

    /**
     * Returns an unknown length, indicated by -1, for use in Netty's streaming framework.
     *
     * @return -1 to signify an unspecified total length
     */
    @Override
    public long length() {
        return -1;
    }

    /**
     * Returns the total number of bytes read since this input was created.
     *
     * @return the number of bytes that have been successfully read
     */
    @Override
    public long progress() {
        return this.progress;
    }
}
