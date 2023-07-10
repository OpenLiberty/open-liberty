/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import java.net.SocketTimeoutException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionReadCompletedCallback;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.openliberty.netty.internal.exception.NettyException;
import io.netty.handler.timeout.IdleState;

/**
 * Timeout handler class for managing heartbeat connections for each Netty JMS connection.
 * On each readTimeout, the error method of the read listener inside the Connection
 * is called with a SocketTimeoutException. This is done to closely mimic what is currently
 * done by Channel Framework and the code
 * 
 * @see com.ibm.ws.sib.jfapchannel.impl.OutboundConnection
 */
public class NettyJMSHeartbeatHandler extends IdleStateHandler{


	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyJMSHeartbeatHandler.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (tc.isDebugEnabled())
			SibTr.debug(tc,
					"@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/NettyJMSHeartbeatHandler.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
	}

	public NettyJMSHeartbeatHandler(int heartbeatTimeSeconds) {
		super(heartbeatTimeSeconds, 0, 0);
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "<init>", heartbeatTimeSeconds);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "<init>");
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelActive", ctx.channel());
		super.channelActive(ctx);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelActive", ctx.channel());
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "handlerAdded", ctx.channel());
		super.handlerAdded(ctx);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "handlerAdded", ctx.channel());
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "userEventTriggered", new Object[] {ctx.channel(), evt});

		if(evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;

			if(event.state() != IdleState.READER_IDLE) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) 
					SibTr.warning(tc, "userEventTriggered: Event triggered was not a read timeout. Event will be moved to start of pipeline.", evt);
				// TODO: Run from the start to verify. This should probably be before the inactivity timeout in the pipeline
				ctx.pipeline().firstContext().fireUserEventTriggered(evt);
				return;
			}else {
				Attribute<Connection> attr = ctx.channel().attr(NettyJMSClientHandler.CONNECTION_KEY);
				Connection connection = attr.get();
				if (connection != null) {
					IOReadCompletedCallback callback = connection.getReadCompletedCallback();
					IOReadRequestContext readCtx = connection.getReadRequestContext();
					NetworkConnection networkConnection = connection.getNetworkConnection();
					if(
							callback instanceof NettyConnectionReadCompletedCallback && 
							readCtx instanceof NettyIOReadRequestContext &&
							networkConnection instanceof NettyNetworkConnection
							) {
						// create timeout exception to pass to callback error method
						// Add local and remote address information
						String ioeMessage = "Socket operation timed out before it could be completed";
						ioeMessage = ioeMessage + " local=" + ctx.channel().localAddress() + " remote=" + ctx.channel().remoteAddress();
						((NettyConnectionReadCompletedCallback)callback).error(connection.getNetworkConnection(), readCtx, new SocketTimeoutException(ioeMessage));
						return;
					}else {
						if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
							SibTr.warning(tc, "userEventTriggered: Something's wrong. Callback, network connection, or read context is not netty specific. This shouldn't happen.", new Object[] {connection, callback, readCtx, networkConnection});
						}
						exceptionCaught(ctx, new NettyException("Illegal callback type for channel."));
						return;
					}
				} else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
						SibTr.warning(tc, "userEventTriggered", "could not associate an incoming event with a Connection. Event will be moved through pipeline.");
					}
				}
			}
		}else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
				SibTr.debug(this,tc, "userEventTriggered: Event triggered was not a timeout. Event will be moved through pipeline.", evt);
		}
		super.userEventTriggered(ctx, evt);
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "userEventTriggered", ctx.channel());
	}



}
