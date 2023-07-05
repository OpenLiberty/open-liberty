/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.netty;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Netty decoder for transforming incoming ByteBuf objects to WsByteBuffer objects
 * we work with in the Wsoc bundle.
 *
 */
public class NettyToWsBufferDecoder extends ByteToMessageDecoder {

    /** Trace */
    private static final TraceComponent tc = Tr.register(NettyToWsBufferDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (tc.isEntryEnabled())
            TR.entry(this, tc, "decode", ctx.channel());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            TR.debug(this, tc, "decode", ctx.channel().remoteAddress() + " decoding message [ " + in.toString(StandardCharsets.UTF_8) + " ] from Netty ByteBuf to WSByteBuffer");
        }

        // TODO: Verify if this is the most effective way to do this. See https://github.com/OpenLiberty/open-liberty/issues/24816

        ByteBuf temp = in.readBytes(in.readableBytes());

        byte[] bytes;
        int offset;
        int length = temp.readableBytes();

        if (temp.hasArray()) {
            bytes = temp.array();
            offset = temp.arrayOffset();
        } else {
            bytes = new byte[length];
            temp.getBytes(temp.readerIndex(), bytes);
            offset = 0;
        }

        out.add(WsByteBufferPool.getInstance().wrap(bytes).position(in.readerIndex()));
        temp.release();

        if (tc.isEntryEnabled())
            TR.exit(this, tc, "decode", ctx.channel());
    }

    @Override
    public void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (tc.isEntryEnabled())
            TR.entry(this, tc, "decodeLast", ctx.channel());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            TR.debug(this, tc, "decodeLast", ctx.channel().remoteAddress() + " calling decode ");
        }
        decode(ctx, in, out);
        if (tc.isEntryEnabled())
            TR.exit(this, tc, "decodeLast", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (tc.isEntryEnabled())
            TR.entry(this, tc, "exceptionCaught", new Object[] { cause, ctx.channel() });
        super.exceptionCaught(ctx, cause);
        if (tc.isEntryEnabled())
            TR.exit(this, tc, "exceptionCaught", new Object[] { cause, ctx.channel() });
    }

}
