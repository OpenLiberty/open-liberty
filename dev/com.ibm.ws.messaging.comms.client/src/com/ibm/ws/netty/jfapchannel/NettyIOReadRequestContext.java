/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.netty.jfapchannel;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext. It
 * basically wraps NettyNetworkConnection code making use of the
 * underlying Channel object for running read requests.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext
 *
 */
public class NettyIOReadRequestContext extends NettyIOBaseContext implements IOReadRequestContext{


	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyIOReadRequestContext.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyIOReadRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.4");
	}


	/**
	 * @param conn
	 */
	public NettyIOReadRequestContext(NettyNetworkConnection conn)
	{
		super(conn);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conn});
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	}



	/**
	 *
	 * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#read(int, com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback, boolean, int)
	 */
	public NetworkConnection read(int amountToRead, final IOReadCompletedCallback completionCallback,
			boolean forceQueue, int timeout)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "read",
				new Object[]{Integer.valueOf(amountToRead), completionCallback, Boolean.valueOf(forceQueue), Integer.valueOf(timeout)});
		// Just return null here for now. Eventually could look into disabling autoRead and managing
		// Reads manually but currently all calls are async and original channelfw implementation
		// was expecting null if async so will mimic behavior
		this.conn.getVirtualConnection().read();

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "read", null);
		return null;
	}



}
