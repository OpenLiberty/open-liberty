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
package com.ibm.ws.sib.netty.jfapchannel;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.utils.ras.SibTr;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory. Based
 * on the CFWNetworkConnectionFactory class. Basically wraps the Bootstrap code in the Netty
 * framework mimicking the Channel Framework implementation as close as possible.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory
 * 
 */
public class NettyNetworkConnectionFactory implements NetworkConnectionFactory{
	
	/** Trace */
    private static final TraceComponent tc = SibTr.register(NettyNetworkConnectionFactory.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/NettyNetworkConnectionFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }

    /** The bootstrap this object wraps */
    private BootstrapExtended bootstrap;
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private String chainName;
	private NettyFramework nettyBundle;
	private Map<String, Object> sslOptions;
	private NettyTlsProvider tlsProvider;
    
    public static final String HEARTBEAT_HANDLER_KEY = "heartBeatHandler";
    public static final String SSL_HANDLER_KEY = "sslHandler";
    public static final String DECODER_HANDLER_KEY = "decoder";
    public static final String ENCODER_HANDLER_KEY = "encoder";
    public static final String JMS_CLIENT_HANDLER_KEY = "jmsClientHandler";
    public static final String JMS_SERVER_HANDLER_KEY = "jmsServerHandler";
    
    //TODO: Temporary internal to choose weather to use the Netty bundle or not to workaround errors on quiesce
  	public static final boolean USE_BUNDLE = false;

    /**
     * Constructor.
     * 
     * @param chainName
     */
    public NettyNetworkConnectionFactory(String chainName, Map<String, Object> tcpOptions, Map<String, Object> sslOptions, NettyTlsProvider tlsProvider)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", chainName);
        this.chainName = chainName;
        this.sslOptions = sslOptions;
        nettyBundle = CommsClientServiceFacade.getNettyBundle();
        this.tlsProvider = tlsProvider;
        Map<String, Object> options = new HashMap<String, Object>(tcpOptions);
        options.put(ConfigConstants.ExternalName, chainName);
        try {
        	if(USE_BUNDLE) {
        		bootstrap = nettyBundle.createTCPBootstrapOutbound(options);
        		bootstrap.attr(NettyJMSClientHandler.CHAIN_ATTR_KEY, chainName);
        	}else {
        		BootstrapExtended bundleBootstrap = nettyBundle.createTCPBootstrapOutbound(options);
        		bootstrap = new BootstrapExtended();
        		bootstrap.attr(NettyJMSClientHandler.CHAIN_ATTR_KEY, chainName);
        		bootstrap.group(workerGroup);
        		bootstrap.channel(NioSocketChannel.class);
                bootstrap.applyConfiguration(bundleBootstrap.getConfiguration());
                bootstrap.setBaseInitializer(bundleBootstrap.getBaseInitializer());
        	}
			// TODO: Check this for timeouts
//	        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, JFapChannelConstants.CONNECT_TIMEOUT_DEFAULT * 1000);
	        
		} catch (NettyException e) {
			SibTr.error(tc, "<init>: Failure initializing Netty Bootstrap", e);
		}
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#createConnection(java.lang.Object)
     */
    @Override
    public NetworkConnection createConnection(Object endpoint) throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection", endpoint);
        // TODO: Verify if this is used
        throw new FrameworkException("Not implemented yet for Netty. Currently only used on tWAS not Liberty.");

    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#createConnection()
     */
    @Override
    public NetworkConnection createConnection() throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection");

        NetworkConnection conn = null;
        
        conn = new NettyNetworkConnection(bootstrap, chainName, nettyBundle, sslOptions, tlsProvider, false);
        
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConnection", conn);
        
        return conn;
    }

    
    @Override
    public void destroy() throws FrameworkException{
    	// TODO: See how to destroy here
        this.bootstrap = null;
    }

}
