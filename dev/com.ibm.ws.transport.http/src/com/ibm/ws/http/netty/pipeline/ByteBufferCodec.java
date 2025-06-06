/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.AbstractMap.SimpleEntry;

import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.StreamSpecificHttpContent;
import io.openliberty.http.netty.stream.WsByteBufferChunkedInput;

/**
 * Stateless duplex handler that bridges Netty's ByteBuf to our WsByteBuffer implementation.
 */
@Sharable
public class ByteBufferCodec extends ChannelDuplexHandler {

    public static final ByteBufferCodec INSTANCE = new ByteBufferCodec();

    private ByteBufferCodec() {}

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception{
        if(!(message instanceof ByteBuf)){
            context.fireChannelRead(message);
            return;
        }

        ByteBuf in = (ByteBuf) message;
        int readable = in.readableBytes();
        if(readable == 0){
            context.fireChannelRead(message);
            return;
            //Should this be instead in.release() and return without firing the read? 

        }
        Object wsBuffer = ChannelFrameworkFactory.getBufferManager().wrap(in.nioBuffer(in.readerIndex(), readable));
        in.skipBytes(readable);
        in.release();

        context.fireChannelRead(wsBuffer);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception{
        if(!(message instanceof SimpleEntry)){
            context.write(message, promise);
            return;
        }

        @SuppressWarnings("unchecked")
        SimpleEntry<Integer, WsByteBuffer> pair = (SimpleEntry<Integer, WsByteBuffer>) message;
        final int streamId = pair.getKey();
        final WsByteBuffer payload = pair.getValue();

        if(isHttp2(context)){
            ByteBuf data;
            if(payload.getWrappedByteBuffer().isDirect()){
                data = Unpooled.wrappedBuffer(payload.getWrappedByteBuffer());
            }else{
                data = context.alloc().directBuffer(payload.remaining(), payload.remaining())
                                .writeBytes(payload.getWrappedByteBuffer());
            }


            context.write(new StreamSpecificHttpContent(streamId, data), promise);

        } else{
            context.write(new WsByteBufferChunkedInput(payload), promise);
        }
    }

    private static boolean isHttp2(ChannelHandlerContext context){
        return context.pipeline().get(HttpToHttp2ConnectionHandler.class) != null;
    }
}
