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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http2.LastStreamSpecificHttpContent;
//import io.netty.handler.codec.http.DefaultHttpContent;
//import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http2.StreamSpecificHttpContent;

/**
 *
 */
public class BufferEncoder extends MessageToMessageEncoder<AbstractMap.SimpleEntry<Integer, WsByteBuffer>> {

    private long bytesWritten = 0;

    @Override
    public void encode(ChannelHandlerContext context, AbstractMap.SimpleEntry<Integer, WsByteBuffer> pair, List<Object> out) throws Exception {
        //out.writeBytes(message.getWrappedByteBuffer());
        Integer streamId = pair.getKey();
        WsByteBuffer message = pair.getValue();

        boolean doLastHttpContent = Boolean.FALSE;

        MSP.log("Encode: bytes written: " + bytesWritten + ", bytes to write: " + message.remaining());
        System.out.println("Encode Got content: " + WsByteBufferUtils.asString(message));

        if (context.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH)) {

            bytesWritten += message.remaining();
            // TODO Should this be <= or >=?
            // doLastHttpContent = context.channel().attr(NettyHttpConstants.CONTENT_LENGTH).get() <= bytesWritten;
            doLastHttpContent = context.channel().attr(NettyHttpConstants.CONTENT_LENGTH).get() == bytesWritten;
            System.out.println("Has content lenght! Bytest Written: " + bytesWritten + " contentLength: " + context.channel().attr(NettyHttpConstants.CONTENT_LENGTH).get());

        }

        //if (context.channel().hasAttr(NettyHttpConstants.CHUNCKED_ENCODING) && context.channel().attr(NettyHttpConstants.CHUNCKED_ENCODING).get()
        //              || !doLastHttpContent) {
        // Do Chunked Input
        // Checked adding counter for bytes written when in Chunked encoding
//            out.add(new HttpChunkedInput(message.getWrappedByteBuffer()));
        if (!doLastHttpContent) {
            System.out.println("Sending chunked input");
//            out.add(new DefaultHttpContent(Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            out.add(new StreamSpecificHttpContent(streamId, Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
        } else {
            // Do Content Length
            // TODO Check if should be full http message
//            out.add(new DefaultHttpContent(Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            System.out.println("Sending last http content");
//            out.add(new DefaultLastHttpContent(Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
            out.add(new LastStreamSpecificHttpContent(streamId, Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
        }
    }

}
