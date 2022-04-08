package com.ibm.ws.netty.jfapchannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.MetaDataProvider;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOConnectionContext;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnectionContext;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

import io.netty.channel.Channel;

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
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyNetworkConnectionContext.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
    }

    /** The underlying connection link */
    private ConversationMetaData metaData;

    /** The connection reference */
    private NettyNetworkConnection conn = null;
    

    /**
     * @param connLink
     */ // Could use either concreate jfap class or similar analogous placeholder
    public NettyNetworkConnectionContext(NettyNetworkConnection conn)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { conn });
        this.conn = conn;
        // TODO: Check if this is the best way to do this
        this.metaData = new ConversationMetaData() {
			
			@Override
			public boolean isInbound() {
				return false;
			}
			
			@Override
			public SSLSession getSSLSession() {
				// TODO Check what to do with SSL here
				return null;
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
                SibTr.debug(this, tc, "close", "Found NULL Netty Channel to close");
        }
        	

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#getIOContextForDevice()
     */
    @Override // Connlink instance of JMS class could add API to get netty channel and use netty channel instead of connlink
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
