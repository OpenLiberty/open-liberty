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

import com.ibm.websphere.ras.TraceComponent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder for transforming outgoing WsByteBuffer objects to ByteBuf objects
 * that the Netty framework uses to send data.
 *
 */
public class WsBufferToNettyEncoder extends MessageToByteEncoder<WsByteBuffer> {

    /** Trace */
    private static final TraceComponent tc = TR.register(WsBufferToNettyEncoder.class);

    @Override
    public void encode(ChannelHandlerContext ctx, WsByteBuffer msg, ByteBuf out) throws Exception {
        if (tc.isEntryEnabled())
            TR.entry(this, tc, "encode", ctx.channel());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            TR.debug(this, tc, "encode", ctx.channel().remoteAddress() + " encoding message [ " + msg.toString() + " ] from WSByteBuffer to Netty ByteBuf");
        }
        com.ibm.wsspi.bytebuffer.WsByteBuffer wrappedBuffer = (com.ibm.wsspi.bytebuffer.WsByteBuffer) ((RichByteBufferImpl) msg).getUnderlyingBuffer();
        out.writeBytes(wrappedBuffer.getWrappedByteBuffer());

        if (tc.isEntryEnabled())
            TR.exit(this, tc, "encode", ctx.channel());
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
