/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import java.io.IOException;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionWriteCompletedCallback;
import com.ibm.ws.sib.utils.ras.SibTr;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext. It
 * basically wraps NettyNetworkConnection code making use of the
 * underlying Channel object for running write requests.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext
 *
 */
public class NettyIOWriteRequestContext extends NettyIOBaseContext implements IOWriteRequestContext{

	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyIOWriteRequestContext.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	private WsByteBuffer buffer = null;

	/** Log class info on load */
	static
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/NettyIOWriteRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.5");
	}


	/**
	 * @param conn
	 */
	public NettyIOWriteRequestContext(NettyNetworkConnection conn)
	{
		super(conn);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { conn });
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	}


	public NetworkConnection write(WsByteBuffer buffer, final NettyConnectionWriteCompletedCallback completionCallback) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
				new Object[]{buffer, completionCallback});
		NetworkConnection retConn = null;
		final IOWriteRequestContext me = this;

		Channel chan = this.conn.getVirtualConnection();
		
		if(chan.isActive()) {
			
			ChannelFuture future = chan.writeAndFlush(buffer, chan.newPromise().addListener(f -> {
				if (f.isDone() && f.isSuccess()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Succesful write for "+chan);
					completionCallback.complete(getNetworkConnectionInstance(chan), me);
				} else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "Unsuccesful write", new Object[]{chan, f.cause()});
					completionCallback.error(getNetworkConnectionInstance(chan), me, new IOException(f.cause()));
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "Unsuccesful write");
				}
			}));

			if(future.isDone()) {
				retConn = getNetworkConnectionInstance(chan); 
			}
			
		}else {
			completionCallback.error(getNetworkConnectionInstance(chan), me, new IOException("Write was attempted on a channel that is not active!! " + chan));
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "write", retConn);
		return retConn;

	}

	/**
	 *
	 * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#write(int, com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback, boolean, int)
	 */
	public NetworkConnection write(int amountToWrite, final IOWriteCompletedCallback completionCallback,
			boolean queueRequest, int timeout)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
				new Object[]{amountToWrite, completionCallback, queueRequest, timeout});
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "write: Not supported for Netty");
		throw new UnsupportedOperationException("Not currently supported for Netty. Please use other write method.");

	}

	public WsByteBuffer getBuffer(){
		return this.buffer;
	}

	public void setBuffer(WsByteBuffer buffer){
		this.buffer = buffer;
	}

}
