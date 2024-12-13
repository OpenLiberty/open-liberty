/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.quiesce;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;

/**
 * Defines different strategies for quiescing various protocols.
 * 
 * Each enum constant returns a {@link Callable} that, when invoked, applies
 * a specific quiesce behavior:
 * 
 * HTTP2_GOAWAY: Sends a GOAWAY frame, refusing new streams but allowing existing 
 * streams to complete.
 * 
 * WEBSOCKET_CLOSE: For WebSocket connections, sends a close frame (status
 * code 1001) and then closes the channel.
 * 
 * NO_OP: No operation performed, used as a default.
 * 
 */
public enum QuiesceStrategy {

    /**
     * Sends a GOAWAY frame to the client, indicating that no new HTTP/2 streams should
     * be opened. Existing streams can complete. The server's quiesce logic eventually
     * closes the channel after its configured timeout if streams don't finish in time.
     */
    HTTP2_GOAWAY(() -> {
        Channel ch = QuiesceContextHolder.getChannel();
        if (ch == null || !ch.isActive()) return null;

        HttpToHttp2ConnectionHandler h2Handler = ch.pipeline().get(HttpToHttp2ConnectionHandler.class);
        if (h2Handler != null) {
            ChannelHandlerContext h2Ctx = ch.pipeline().context(h2Handler);
            if (h2Ctx != null) {
                Http2Connection connection = h2Handler.connection();
                int lastStreamId = connection.remote().lastStreamKnownByPeer();
                long errorCode = Http2Error.NO_ERROR.code();
                ByteBuf debugData = h2Ctx.alloc().buffer(0);

                // Send GOAWAY frame
                h2Handler.encoder().writeGoAway(h2Ctx, lastStreamId, errorCode, debugData, h2Ctx.newPromise());
                h2Ctx.flush();
            }
        }

        return null;
    }),

    /**
     * Sends a close frame to WebSocket clients and then closes the channel.
     */
    WEBSOCKET_CLOSE(() -> {
        Channel ch = QuiesceContextHolder.getChannel();
        if (ch == null || !ch.isActive()) return null;
        ch.writeAndFlush(new CloseWebSocketFrame(1001, "Server shutting down"))
          .addListener(ChannelFutureListener.CLOSE);
        return null;
    }),

    /**
     * Does nothing. Used if no special quiesce action is required.
     */
    NO_OP(() -> null);

    private final Callable<Void> task;

    QuiesceStrategy(Callable<Void> task) {
        this.task = task;
    }

    /**
     * Returns the quiesce {@link Callable} associated with this strategy.
     *
     * @return The callable to run when quiesce event triggers.
     */
    public Callable<Void> getTask() {
        return task;
    }
}