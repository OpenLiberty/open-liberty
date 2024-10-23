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

import com.ibm.ws.http.netty.MSP;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 *
 */
public class ChunkSizeLoggingHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf chunk = (ByteBuf) msg;
            int chunkSize = chunk.readableBytes();
        }

        super.write(context, msg, promise);
    }

}
