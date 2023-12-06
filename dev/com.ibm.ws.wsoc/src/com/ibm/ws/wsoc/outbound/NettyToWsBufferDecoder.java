/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 *
 */
public class NettyToWsBufferDecoder extends ByteToMessageDecoder {

    private static final TraceComponent tc = Tr.register(ClientConnector.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "decode", ctx.channel());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "decode", ctx.channel().remoteAddress() + " decoding message [ " + in.toString(StandardCharsets.UTF_8) + " ] from Netty ByteBuf to WSByteBuffer");
        }

        ByteBuf temp = in.readBytes((in.readableBytes()));
        out.add(ChannelFrameworkFactory.getBufferManager().wrap(temp.nioBuffer()).position(in.readerIndex()));
        temp.release();

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "decode", ctx.channel());
    }

    @Override
    public void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "decodeLast", ctx.channel());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "decodeLast", ctx.channel().remoteAddress() + " calling decode ");
        }
        decode(ctx, in, out);
        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "decodeLast", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "exceptionCaught", new Object[] { cause, ctx.channel() });
        super.exceptionCaught(ctx, cause);
        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "exceptionCaught", new Object[] { cause, ctx.channel() });
    }

}
