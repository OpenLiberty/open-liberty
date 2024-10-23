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

import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 *
 */
public class BufferDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf temp = in.readBytes((in.readableBytes()));
        out.add(ChannelFrameworkFactory.getBufferManager().wrap(temp.nioBuffer()).position(in.readerIndex()));
        temp.release();
    }

    @Override
    public void decodeLast(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
        decode(context, in, out);
    }

}
