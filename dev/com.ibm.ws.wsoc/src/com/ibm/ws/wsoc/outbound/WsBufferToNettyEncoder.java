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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder for transforming outgoing WsByteBuffer objects to ByteBuf objects
 * that the Netty framework uses to send data.
 *
 */
public class WsBufferToNettyEncoder extends MessageToByteEncoder<WsByteBuffer> {

    private static final TraceComponent tc = Tr.register(ClientConnector.class);

    @Override
    public void encode(ChannelHandlerContext ctx, WsByteBuffer msg, ByteBuf out) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "encode", ctx.channel());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "encode", ctx.channel().remoteAddress() + " encoding message [ " + msg.toString() + " ] from WSByteBuffer to Netty ByteBuf");
        }
        //WsByteBuffer wrappedBuffer = (WsByteBuffer) msg.getUnderlyingBuffer();
        WsByteBuffer wrappedBuffer = msg;
        out.writeBytes(wrappedBuffer.getWrappedByteBuffer());
        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "encode", ctx.channel());
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
