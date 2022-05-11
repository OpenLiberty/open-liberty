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
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

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
    Bootstrap bootstrap = new Bootstrap();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private String chainName;
    private boolean isInbound;
    
    public static final String HEARTBEAT_HANDLER_KEY = "heartBeatHandler";
    public static final String SSL_HANDLER_KEY = "sslHandler";
    public static final String DECODER_HANDLER_KEY = "decoder";
    public static final String ENCODER_HANDLER_KEY = "encoder";
    public static final String JMS_CLIENT_HANDLER_KEY = "jmsClientHandler";
    public static final String JMS_SERVER_HANDLER_KEY = "jmsServerHandler";

    /**
     * Constructor.
     * 
     * @param chainName
     */
    public NettyNetworkConnectionFactory(String chainName, boolean isInbound)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", chainName);
        this.chainName = chainName;
        this.isInbound = isInbound;
        bootstrap.group(workerGroup).channel(NioSocketChannel.class);
        bootstrap.attr(JMSClientInboundHandler.CHAIN_ATTR_KEY, chainName);
        
        // TODO Check how to link TCP options with bootstrap

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, JFapChannelConstants.CONNECT_TIMEOUT_DEFAULT * 1000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
                pipeline.addLast(ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
                pipeline.addLast(HEARTBEAT_HANDLER_KEY, new JMSHeartbeatHandler(0));
                pipeline.addLast(JMS_CLIENT_HANDLER_KEY, new JMSClientInboundHandler());
            }
        });
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
        
        throw new FrameworkException("Not implemented yet for Netty. Not sure if its used only on tWAS.");

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
        
        conn = new NettyNetworkConnection(bootstrap, chainName, isInbound);
        
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
