/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionReadCompletedCallback;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * Main handler class for managing connections for each Netty JMS client.
 * There is a 1-1 mapping between each object of this class and  single OutboundConnection.
 * On each read the readCompleted method of the read listener inside the OutboundConnection
 * is called.
 * 
 * @see com.ibm.ws.sib.jfapchannel.impl.OutboundConnection
 */
public class NettyJMSClientHandler extends SimpleChannelInboundHandler<WsByteBuffer>{

	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyJMSClientHandler.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (tc.isDebugEnabled())
			SibTr.debug(tc,
					"@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/NettyJMSClientHandler.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
	}


	protected final static AttributeKey<Connection> CONNECTION_KEY = AttributeKey.valueOf("OutboundConnection");
	protected final static AttributeKey<String> CHAIN_ATTR_KEY = AttributeKey.valueOf("CHAIN_NAME");

	/** Called when a new connection is established */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelActive: connected for chain " + ctx.channel().attr(CHAIN_ATTR_KEY).get(), ctx.channel());

		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelActive", ctx.channel());

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WsByteBuffer msg) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelRead0", ctx.channel());

		Attribute<Connection> attr = ctx.channel().attr(CONNECTION_KEY);
		Connection connection = attr.get();

		if (connection != null) {
			IOReadCompletedCallback callback = connection.getReadCompletedCallback();
			IOReadRequestContext readCtx = connection.getReadRequestContext();
			NetworkConnection networkConnection = connection.getNetworkConnection();
			if(
					callback instanceof NettyConnectionReadCompletedCallback && 
					readCtx instanceof NettyIOReadRequestContext && 
					networkConnection instanceof NettyNetworkConnection) {
				((NettyConnectionReadCompletedCallback)callback).readCompleted(msg, readCtx, (NettyNetworkConnection)networkConnection);
			}else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
					SibTr.warning(tc, "channelRead0: Something's wrong. Callback, network connection, or read context is not netty specific. This shouldn't happen.", new Object[] {connection, callback, readCtx, networkConnection});
				}
				exceptionCaught(ctx, new NettyException("Illegal callback type for channel."));
				return;
			}

		}else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				SibTr.warning(tc, "channelRead0: could not associate an incoming message with a Connection. Message will be ignored and channel will be closed.", new Object[] {ctx.channel()});
				ctx.close();
			}
		}
		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelRead0", ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "channelInactive", ctx.channel());

		// TODO: Check how to manage inactive channels appropriately
		Connection connection = ctx.channel().attr(CONNECTION_KEY).get();

		if (tc.isEntryEnabled() && tc.isDebugEnabled()) {
			SibTr.debug(this, tc, "Fired for connection which is closed?: " + connection.isClosed() + " closedDeffered?: " + connection.isCloseDeferred(), new Object[] {ctx.channel(), connection});
		}
		ctx.channel().attr(CONNECTION_KEY).set(null);
		ctx.close();

		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "channelInactive", ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (tc.isEntryEnabled())
			SibTr.entry(this, tc, "exceptionCaught", new Object[] {ctx.channel(), cause});

		Attribute<Connection> attr = ctx.channel().attr(CONNECTION_KEY);
		Connection connection = attr.get();

		if (connection == null) {
			if (tc.isEntryEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "Found null connection for JMS Channel. Closing channel and moving cause through pipeline.", ctx.channel());
			ctx.close();
		}else {
			connection.invalidate(false, cause, "Connection closed due to exception");
			ctx.channel().attr(CONNECTION_KEY).set(null);
			ctx.close();
		}

		super.exceptionCaught(ctx, cause);

		if (tc.isEntryEnabled())
			SibTr.exit(this, tc, "exceptionCaught", ctx.channel());
	}

}
