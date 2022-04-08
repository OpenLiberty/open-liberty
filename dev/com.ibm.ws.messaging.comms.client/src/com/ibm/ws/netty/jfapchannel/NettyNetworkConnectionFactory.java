package com.ibm.ws.netty.jfapchannel;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

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
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyNetworkConnectionFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }

    /** The bootstrap this object wraps */
    Bootstrap bootstrap = new Bootstrap();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private String chainName;

    /**
     * Constructor.
     * 
     * @param vcFactory
     */
    public NettyNetworkConnectionFactory(String chainName)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", chainName);
        this.chainName = chainName;
        bootstrap.group(workerGroup).channel(NioSocketChannel.class);
        bootstrap.attr(JMSClientInboundHandler.CHAIN_ATTR_KEY, chainName);

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, JFapChannelConstants.CONNECT_TIMEOUT_DEFAULT * 1000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", new NettyToWsBufferDecoder());
                pipeline.addLast("encoder", new WsBufferToNettyEncoder());
                pipeline.addLast("handler", new JMSClientInboundHandler());
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
        
        throw new FrameworkException("Not implemented yet");

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
        
        conn = new NettyNetworkConnection(bootstrap, chainName);
        
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConnection", conn);
        
        return conn;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#getOutboundVirtualConFactory()
 */
//    @Override
//    public VirtualConnectionFactory getOutboundVirtualConFactory() {
//    	// TODO: See what to do here. Only called in OutboundConnectionTracker to destroy it. Could have a destroy method only in the interface
//        return null;
//    }
    
    @Override
    public void destroy() throws FrameworkException{
    	// TODO: See what to do here. Only called in OutboundConnectionTracker to destroy it. Could have a destroy method only in the interface
        this.bootstrap = null;
    }

}
