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

import java.util.AbstractMap;
import java.util.List;

import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.LastStreamSpecificHttpContent;
import io.netty.handler.codec.http2.StreamSpecificHttpContent;
import io.netty.handler.stream.ChunkedInput;

/**
 * A Netty message encoder for encoding message data into HTTP responses.
 * This encoder handles both HTTP/1.1 and HTTP/2, choosing the appropriate encoding method accordingly.
 */
public class BufferEncoder extends MessageToMessageEncoder<AbstractMap.SimpleEntry<Integer, WsByteBuffer>> {

    private long bytesWritten = 0;

    /**
     * Encodes message data into HTTP responses.
     *
     * @param context The channel handler context.
     * @param pair    A key-value pair containing the stream ID and response message.
     * @param out     The list of objects to which the encoded data should be added.
     * @throws Exception If an error occurs during encoding.
     */
    @Override
    public void encode(ChannelHandlerContext context, AbstractMap.SimpleEntry<Integer, WsByteBuffer> pair, List<Object> out) throws Exception {
        Integer streamId = pair.getKey();
        WsByteBuffer message = pair.getValue();

        // Check if "Content-Length" is set for this channel
        boolean hasContentLength = context.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH);

        bytesWritten += message.remaining();

        MSP.log("Encode: bytes written: " + bytesWritten + ", bytes to write: " + message.remaining());
        System.out.println("Encode Got content: " + WsByteBufferUtils.asString(message));

        // If "Content-Length" is set, check if it matches the bytes written
        if (hasContentLength) {
            boolean doLastHttpContent = context.channel().attr(NettyHttpConstants.CONTENT_LENGTH).get() == bytesWritten;

            if (!doLastHttpContent) {
                // Send content with "Content-Length" header
                out.add(new StreamSpecificHttpContent(streamId, Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            } else {
                // Send last HTTP content
                System.out.println("Sending last http content");
                out.add(new LastStreamSpecificHttpContent(streamId, Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            }
        } else {
            // If "Content-Length" is not set, determine whether it's HTTP/2 or HTTP/1.1 with chunked encoding
            if (isHttp2(context)) {
                // Send content as HTTP/2
                System.out.println("Sending HTTP/2 content");
                out.add(new StreamSpecificHttpContent(streamId, Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            } else {
                // Send content with chunked encoding
                System.out.println("Sending chunked input");
                ChunkedInput<ByteBuf> chunkedInput = new WsByteBufferChunkedInput(message);
                MSP.log("Should be writing a chunk of size: " + chunkedInput.length());
                context.writeAndFlush(chunkedInput);
            }
        }
    }

    /**
     * Determines whether HTTP/2 is being utilized based on the context.
     *
     * @param context The channel handler context.
     * @return True if HTTP/2 is detected, false otherwise.
     */

    private boolean isHttp2(ChannelHandlerContext context) {
        HttpToHttp2ConnectionHandler http2Handler = context.pipeline().get(HttpToHttp2ConnectionHandler.class);

        return (http2Handler != null) ? true : false;
    }
}