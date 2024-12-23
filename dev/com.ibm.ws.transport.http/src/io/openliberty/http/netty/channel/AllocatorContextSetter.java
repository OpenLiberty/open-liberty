/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.RecvByteBufAllocator;

/**
 * A handler that, when added to the pipeline, checks if the current channel's
 * {@link RecvByteBufAllocator.Handle} is a {@link LoggingRecvByteBufAllocator.LoggingHandle}
 * and sets the {@link ChannelHandlerContext} so the allocator can log
 * read requests with contextual information.
 */
public class AllocatorContextSetter extends ChannelInboundHandlerAdapter {

    /**
     * Constructs a new context setter for the specified logging allocator.
     *
     */
    public AllocatorContextSetter() {}

    /**
     * Called when the handler is added to the pipeline. Checks the current
     * {@link RecvByteBufAllocator.Handle} and sets the context if it's a logging handle.
     *
     * @param ctx the context for this handler
     * @throws Exception if any error occurs
     */
    @Override
    @SuppressWarnings("deprecation")
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);

        // Netty doesn't have an non-deprecated approach, so for now
        // suppress the warnings.
        RecvByteBufAllocator.Handle handle = ctx.channel().unsafe().recvBufAllocHandle();

        // If the handle is one of our logging handles, set the context to enable logging
        if (handle instanceof LoggingRecvByteBufAllocator.LoggingHandle) {
            ((LoggingRecvByteBufAllocator.LoggingHandle) handle).setChannelHandlerContext(ctx);
        }
    }
}
