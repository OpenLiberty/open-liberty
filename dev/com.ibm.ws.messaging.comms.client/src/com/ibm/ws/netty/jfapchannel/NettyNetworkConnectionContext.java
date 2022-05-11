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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class NettyNetworkConnectionContext implements NetworkConnectionContext{

	/** Trace */
    private static final TraceComponent tc = SibTr.register(NettyNetworkConnectionContext.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** Log class info on load */
    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/NettyNetworkConnectionContext.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
    }

    /** The underlying connection link */
    private ConversationMetaData metaData;

    /** The connection reference */
    private NettyNetworkConnection conn = null;
    

    /**
     * @param connLink
     */
    public NettyNetworkConnectionContext(NettyNetworkConnection conn)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { conn });
        this.conn = conn;
        // TODO: Check if this is the best way to do this
        this.metaData = new ConversationMetaData() {
			
			@Override
			public boolean isInbound() {
				return conn.isInbound();
			}
			
			@Override
			public SSLSession getSSLSession() {
				return conn.getSSLSession();
			}
			
			@Override
			public int getRemotePort() {
				return ((InetSocketAddress)conn.getVirtualConnection().remoteAddress()).getPort();
			}
			
			@Override
			public InetAddress getRemoteAddress() {
				return ((InetSocketAddress)conn.getVirtualConnection().remoteAddress()).getAddress();
			}
			
			@Override
			public String getChainName() {
				return conn.getChainName();
			}
		};
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#close(com.ibm.ws.sib.jfapchannel.framework.NetworkConnection, java.lang.Throwable)
     */
    @Override
    public void close(NetworkConnection networkConnection, Throwable throwable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", new Object[] { networkConnection, throwable });

        // TODO: Verify this statement
        // If the server is stopping, all connections will be closed/flushed by netty bundle? Verify
        if (FrameworkState.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "close");
            return;
        }
        Exception exception = null;
        if (throwable instanceof Exception)
        {
            exception = (Exception) throwable;
        }
        else
        {
            exception = new Exception(throwable);
        }
        
        if(conn.getVirtualConnection() != null && (conn.getVirtualConnection().isActive() || conn.getVirtualConnection().isOpen())) {
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "close", "Found Netty Channel to close");
        	conn.getVirtualConnection().close();
        }else {
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "close", "Found NULL or non active Netty Channel to close");
        }
        	

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#getIOContextForDevice()
     */
    @Override
    public IOConnectionContext getIOContextForDevice()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getIOContextForDevice");
        
        final IOConnectionContext ioConnCtx = new NettyIOConnectionContext(conn);


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getIOContextForDevice", ioConnCtx);
        return ioConnCtx;
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#getMetaData()
     */
    @Override
    public ConversationMetaData getMetaData()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMetaData", metaData);
        return metaData;
    }
	
	
}
