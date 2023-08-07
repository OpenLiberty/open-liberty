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

import java.util.List;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultHttpContent;

/**
 *
 */
//public class BufferEncoder extends MessageToByteEncoder<WsByteBuffer> {
public class BufferEncoder extends MessageToMessageEncoder<WsByteBuffer> {

//    @Override
//    public void encode(ChannelHandlerContext context, WsByteBuffer message, ByteBuf out) throws Exception {
//        out.writeBytes(message.getWrappedByteBuffer());
//    }

    @Override
    protected void encode(ChannelHandlerContext context, WsByteBuffer message, List<Object> out) throws Exception {
        // TODO Auto-generated method stub
        out.add(new DefaultHttpContent(Unpooled.wrappedBuffer(message.getWrappedByteBuffer())));
    }

}
