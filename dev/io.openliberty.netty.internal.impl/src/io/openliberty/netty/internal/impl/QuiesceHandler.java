/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelPromise;

import io.openliberty.netty.internal.impl.QuiesceState;

/**
 * Channel handler which is added to the pipeline to terminates new connections once the 
 * quiesce period is started.
 *
 */
public class QuiesceHandler extends ChannelDuplexHandler{

	
	
	public static final QuiesceEvent QUIESCE_EVENT = new QuiesceEvent();
	public static final AttributeKey<Boolean> WEBSOCKET_ATTR_KEY = AttributeKey.valueOf("websocket");

	private boolean completed = false;


	private static final TraceComponent tc = Tr.register(QuiesceHandler.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);

	private Callable quiesceTask = null;


	public QuiesceHandler(Callable quiesceTask) {
		this.quiesceTask = quiesceTask;
		System.out.println("Quiesce handler added");
	}

	public void setQuiesceTask(Callable task){
		this.quiesceTask= task;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Added quiesce handler for channel " + ctx.channel() + " with callable: " + quiesceTask);
		}
		super.handlerAdded(ctx);
	}

	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof QuiesceEvent) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received Quiesce Event for " + ctx.channel() + " with callable: " + quiesceTask);
            }

            if (quiesceTask != null) {
                quiesceTask.call();
            }

            handleQuiesce(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (QuiesceState.isQuiesceInProgress() && handleQuiesce(ctx)) {
            // Quiesce handled, do not pass the message further
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (QuiesceState.isQuiesceInProgress() && handleQuiesce(ctx)) {
            // Quiesce handled, do not write the message
            return;
        }
        super.write(ctx, msg, promise);
    }

    private boolean handleQuiesce(ChannelHandlerContext ctx) {
        if (completed) {
            return false; // Already handled
        }
        completed = true;

        Boolean isWebSocket = ctx.channel().attr(WEBSOCKET_ATTR_KEY).get();
        System.out.println("handling quiesce ... isWebSocket: " + isWebSocket);

        if (Boolean.TRUE.equals(isWebSocket)) {

			ctx.executor().schedule(() -> {
        if (Boolean.TRUE.equals(isWebSocket)) {
            ctx.writeAndFlush(new CloseWebSocketFrame(1001, "Server shutting down"))
                .addListener(ChannelFutureListener.CLOSE);
        } 
    }, 2, TimeUnit.SECONDS);
        } else {
            // Close other connections if necessary
            //ctx.close();
        }
        return true;
    }

    static class QuiesceEvent {
    }
}