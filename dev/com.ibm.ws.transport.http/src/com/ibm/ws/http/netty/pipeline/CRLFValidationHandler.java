/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

/**
 * Utility handler to validate that a request is not preceeded by more than two consecutive CRLF characters. 
 */
@Sharable
public class CRLFValidationHandler extends ChannelInboundHandlerAdapter {

    private static final int MAX_CRLF_ALLOWED = 2;
    public static final CRLFValidationHandler INSTANCE = new CRLFValidationHandler();

    private CRLFValidationHandler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(!(msg instanceof ByteBuf)){
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf buffer = (ByteBuf) msg;
        buffer.markReaderIndex();

        int leadingCRLFCount = 0;
        while (buffer.isReadable()) {
            byte b = buffer.readByte();
            if (b != '\r')
                break;
            if (!buffer.isReadable() || buffer.readByte() != '\n')
                break;

            if (++leadingCRLFCount > MAX_CRLF_ALLOWED) {
                ctx.channel().attr(NettyHttpConstants.THROW_FFDC).set(true);
                throw new IllegalArgumentException("Too many leading CRLF characters");
            }
        }

        buffer.resetReaderIndex();
        ctx.fireChannelRead(msg);
    }
}
