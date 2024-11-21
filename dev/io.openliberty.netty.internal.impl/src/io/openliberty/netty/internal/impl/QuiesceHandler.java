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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.util.AttributeKey;

/**
 * Channel handler which is added to the pipeline to terminates new connections once the 
 * quiesce period is started.
 *
 */
public class QuiesceHandler extends SimpleUserEventChannelHandler<QuiesceHandler.QuiesceEvent>{

	static class QuiesceEvent{

	}
	
	public static final QuiesceEvent QUIESCE_EVENT = new QuiesceEvent();
	public static final AttributeKey<Boolean> WEBSOCKET_ATTR_KEY = AttributeKey.valueOf("websocket");


	private static final TraceComponent tc = Tr.register(QuiesceHandler.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);

	private Callable quiesceTask = null;


	public QuiesceHandler(Callable quiesceTask) {
		this.quiesceTask = quiesceTask;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Added quiesce handler for channel " + ctx.channel() + " with callable: " + quiesceTask);
		}
		super.handlerAdded(ctx);
	}

	@Override
	protected void eventReceived(ChannelHandlerContext ctx, QuiesceEvent arg1) throws Exception {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Received Quiesce Event for " + ctx.channel() + " with callable: " + quiesceTask);
		}
		if (Boolean.TRUE.equals(ctx.channel().attr(WEBSOCKET_ATTR_KEY).get())) {
			// Send close frame and close the channel
			ctx.writeAndFlush(new CloseWebSocketFrame(1001, "Server shutting down"))
				.addListener(ChannelFutureListener.CLOSE);
		}
		
		if(quiesceTask != null) {
			quiesceTask.call();
		}
	}

}
