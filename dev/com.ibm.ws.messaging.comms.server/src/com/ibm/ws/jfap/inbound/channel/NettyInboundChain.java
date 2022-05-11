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
package com.ibm.ws.jfap.inbound.channel;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.netty.jfapchannel.JMSClientInboundHandler;
import com.ibm.ws.netty.jfapchannel.JMSHeartbeatHandler;
import com.ibm.ws.netty.jfapchannel.NettyNetworkConnectionFactory;
import com.ibm.ws.netty.jfapchannel.NettyToWsBufferDecoder;
import com.ibm.ws.netty.jfapchannel.WsBufferToNettyEncoder;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.server.impl.JMSServerHandler;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;
import io.openliberty.netty.internal.tcp.TCPMessageConstants;
import io.openliberty.netty.internal.impl.TCPUtils;
import io.openliberty.netty.internal.tls.NettyTlsProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

public class NettyInboundChain implements InboundChain{
    private static final TraceComponent tc = Tr.register(NettyInboundChain.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

	private boolean _isSecureChain = false;
	private boolean _isEnabled = false;
    private String _chainName;
    
    private SslContext context;

    private final CommsServerServiceFacade _commsServerFacade;
    
    private String _endpointName;
    private EndPointMgr _endpointMgr;
    private NettyFramework _nettyFramework;

    //channel names
    private String _tcpName;
    private String _sslName;
    private String _jfapName;
    
    private volatile boolean _isChainStarted = false;

    private ChainConfiguration _currentConfig;
	
	/**
     * The TCP based bootstrap.
     */
//    private ServerBootstrapExtended serverBootstrap;
    /** The bootstrap this object wraps */
    ServerBootstrapExtended bootstrap = new ServerBootstrapExtended();
    private EventLoopGroup parentGroup = new NioEventLoopGroup();
    private EventLoopGroup childGroup = new NioEventLoopGroup();
    private Channel serverChan;
    
    NettyInboundChain(CommsServerServiceFacade commsServer, boolean isSecureChain) {
        _commsServerFacade = commsServer;
        _isSecureChain = isSecureChain;
        bootstrap.group(parentGroup,childGroup);
    }
	
	public void init(String endpointName, NettyFramework netty) {
		_nettyFramework = netty;
        _endpointMgr = netty.getEndpointManager();
        _endpointName = endpointName;

        if (_isSecureChain) {
            _chainName = "InboundSecureMessaging";
            _tcpName = _endpointName;
            _sslName = "SSL-" + _endpointName;
            _jfapName = "JFAP-" + _endpointName;
        } else {
            _chainName = "InboundBasicMessaging";
            _tcpName = _endpointName;
            _jfapName = "JFAP-" + _endpointName;
        }
	}

	@Override
	public void enable(boolean enabled) {
		_isEnabled = enabled;
	}
	
	
	public boolean isRunning() {
		return _isEnabled && _isChainStarted;
	}

	/**
     * stop will get called only from de-activate.
     */
	@Override
	public void stop() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop");
		if(serverChan == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Netty channel not initialized");
			return;
		}else {
			SibTr.debug(tc, "stop","Stopping Channel "+ serverChan +" --- "+ serverChan.localAddress());
			SibTr.debug(tc, "stopTesting: "+serverChan.isActive()+" "+serverChan.isOpen());
	        //stopchain() first quiesce's(invokes chainQuiesced) depending on the chainQuiesceTimeOut
	        //Once the chain is quiesced StopChainTask is initiated.Hence we block until the actual stopChain is invoked
	        try {
	        	//First stop any MP connections which are established through COMMS 
	            //stopping connections is Non-blocking
	            try {
	                if (this._isSecureChain)
	                    _commsServerFacade.closeViaCommsMPConnections(JsConstants.ME_STOP_COMMS_SSL_CONNECTIONS);
	                else
	                    _commsServerFacade.closeViaCommsMPConnections(JsConstants.ME_STOP_COMMS_CONNECTIONS);
	            } catch (Exception e) {
	                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	                    SibTr.debug(tc, "Failed in stopping MP connections which are establised through COMMS: ", e);
	            }
	            ChannelFuture future = serverChan.close().sync();
	            if(!future.isSuccess()) {
	            	SibTr.debug(tc, "Failed stopping server channel: "+serverChan);
	            }else {
	            	SibTr.debug(tc, "Succesfully stopped server channel: "+serverChan);
	            }
	            Future<?> parentFuture = parentGroup.shutdownGracefully().sync();
	            if(!parentFuture.isSuccess()) {
	            	SibTr.debug(tc, "Error stopping parent channel for: "+serverChan);
	            }else {
	            	SibTr.debug(tc, "Succesfully stopped parent channel for: "+serverChan);
	            }
	            Future<?> childFuture = childGroup.shutdownGracefully().sync();
	            if(!childFuture.isSuccess()) {
	            	SibTr.debug(tc, "Error stopping active channels for: "+serverChan);
	            }else {
	            	SibTr.debug(tc, "Succesfully stopped active channels for: "+serverChan);
	            }
	            TCPUtils.logChannelStopped(serverChan);
//	    		_nettyFramework.stop(channel); //BLOCK till stopChain actually completes from StopChainTask
	    		
	        } catch (Exception e) {
	            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	                SibTr.debug(tc, "Failed in successfully cleaning(i.e stopping/destorying/removing) chain: ", e);
	        } finally {
	            _isChainStarted = false;
	        }
		}

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
	}
	

	@Override
	public void update() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "update");

        if (!_isEnabled || FrameworkState.isStopping()) //dont do any thing.. just return
            return;

        final ChainConfiguration oldConfig = _currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.isValidConfig;

        Map<String, Object> tcpOptions = _commsServerFacade.getTcpOptions();
        Map<String, Object> sslOptions = (_isSecureChain) ? _commsServerFacade.getSslOptions() : null;

        final ChainConfiguration newConfig = new ChainConfiguration(
                        (_isSecureChain) ? _commsServerFacade.getConfigured_wasJmsSSLPort() : _commsServerFacade.getConfigured_wasJmsPort(),
                        _commsServerFacade.getConfigured_Host(),
                        tcpOptions,
                        sslOptions);

        if ((newConfig.configPort < 0) || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            _currentConfig = newConfig;

            //stop the chain
            stop();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "update");
            return;
        }

        //newConfig is valid one.. then compare to old one and take actions.
        if (validOldConfig && newConfig.unchanged(oldConfig)) {
            //new and old config are identical... then check whether listening ports also same .. 
            //in that case chain is already started with the same configuration.. so just exit
            if (newConfig.getActivePort() == oldConfig.getActivePort() && newConfig.getActivePort() != -1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(this, tc, "Chain is already started " + oldConfig);
                }
                //exiting here as nothing to be done.. no need to save config as both old and new are identical
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Existing config must be started " + newConfig);
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "New/changed chain configuration " + newConfig);
        }

        try {
            if (validOldConfig) {
                //we have good old config means.. chain was started successfully..
                //first stop the chain.. it should be harmless if chain is already stopped
                stop();
            }

            //as of now, JFAP options are not exposed.. so no need to touch it.

            // save the new/changed configuration before we start setting up the new chain
            _currentConfig = newConfig;

            // define is a simple replace of the old value known to the endpointMgr
            //by defining with Endpoint Manager, Jfap endpoint can be queried from Mbeans
            _endpointMgr.defineEndPoint(_endpointName, newConfig.configHost, newConfig.configPort);
            
            // Start Netty channel
            EndPointInfo ep = _endpointMgr.getEndPoint(_endpointName);
            ep = _endpointMgr.defineEndPoint(_endpointName, _currentConfig.configHost, _currentConfig.configPort);
            
            Map<String, Object> options = new HashMap<String, Object>();
            options.putAll(_currentConfig.tcpOptions);
            options.put(ConfigConstants.ExternalName, _endpointName);
            // TODO: Hackish way of re-setting group so need to address this later on
            ServerBootstrapExtended serverBootstrap = _nettyFramework.createTCPBootstrap(options);
            
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.applyConfiguration(serverBootstrap.getConfiguration());
            bootstrap.setBaseInitializer(serverBootstrap.getBaseInitializer());
            if (_isSecureChain) {
            	NettyTlsProvider tlsProvider = _commsServerFacade.getTlsProviderService();
	          	String host = ep.getHost();
	          	String port = Integer.toString(ep.getPort());
	          	if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Create SSL", new Object[] {tlsProvider, host, port, sslOptions});
	          	context = tlsProvider.getInboundSSLContext(_currentConfig.sslOptions, host, port);
	          	if(context == null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initChannel","Error adding TLS Support");
		            throw new NettyException("Problems creating SSL context");
	          	}
//                context = _commsServerFacade.getTlsProviderService().getInboundSSLContext(_currentConfig.sslOptions, ep.getHost(), Integer.toString(ep.getPort()));
            }
            bootstrap.childHandler(new JMSTCPInitializer(bootstrap.getBaseInitializer(), this));
            String inetHost = ep.getHost();
            if (inetHost.equals("*")) {
                inetHost = "0.0.0.0";
            }
            serverChan = bootstrap.bind(inetHost, ep.getPort()).sync().channel();
            TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
            // set common channel attrs
            serverChan.attr(ConfigConstants.NameKey).set(config.getExternalName());
            serverChan.attr(ConfigConstants.HostKey).set(inetHost);
            serverChan.attr(ConfigConstants.PortKey).set(ep.getPort());
            serverChan.attr(ConfigConstants.IsInboundKey).set(config.isInbound());
        	_isChainStarted = true;
        	newConfig.isValidConfig = true;

            // set up the a helpful log message
            String hostLogString = inetHost == "0.0.0.0" ? "*" : inetHost;
            SocketAddress addr = serverChan.localAddress();
            InetSocketAddress inetAddr = (InetSocketAddress)addr;
            String IPvType = "IPv4";
            if (inetAddr.getAddress() instanceof Inet6Address) {
                IPvType = "IPv6";
            }
            if (inetHost == "0.0.0.0") {
                hostLogString = "*  (" + IPvType + ")";
            } else {
                hostLogString = config.getHostname() + "  (" + IPvType + ": "
                           + inetAddr.getAddress().getHostAddress() + ")";
            }
            
            TCPUtils.logChannelStarted(serverChan);
//
//            if (config.isInbound()) {
//                Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
//                        new Object[] { config.getExternalName(), hostLogString, String.valueOf(ep.getPort()) });
//            } else {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
//                            new Object[] { config.getExternalName(), hostLogString, String.valueOf(ep.getPort()) });
//                }
//            }
//            _nettyFramework.start(serverBootstrap, ep.getHost(), ep.getPort(), future -> {
//                if (future.isSuccess()) {
//                	//Chain successfully started and bound to port. Channel Framework logs to System.out. So just add to trace
//                	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                        SibTr.debug(tc, "JFAP "+ (_isSecureChain ? "TLS" : "TCP") + " chain InboundBasicMessaging successfully started and bound to: " + future.channel().localAddress());
//                	_isChainStarted = true;
//                    newConfig.isValidConfig = true;
//                } else {
//                	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                		SibTr.debug(tc, "JFAP "+ (_isSecureChain ? "TLS" : "TCP") + " chain InboundBasicMessaging failed to start", future.cause() );
//                }
//            }).sync();


        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Problem in starting the chain  " + newConfig,e);
            }
        }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "update");
	}
	
	/**
     * ChannelInitializer for SIP over TCP, and optionally TLS
     */
    private class JMSTCPInitializer extends ChannelInitializerWrapper {
    	final ChannelInitializerWrapper parent;
		private final NettyInboundChain chain;

        public JMSTCPInitializer(ChannelInitializerWrapper parent, NettyInboundChain chain) {
            this.parent = parent;
            this.chain = chain;
        }
        
        @Override
        protected void initChannel(Channel ch) throws Exception {
            parent.init(ch);
            ChannelPipeline pipeline = ch.pipeline();
            ch.attr(JMSServerHandler.CHAIN_ATTR_KEY).set(_endpointName);
            ch.attr(JMSServerHandler.ATTR_KEY).set(this.chain);
            if(_isSecureChain) {
            	SSLEngine engine = context.newEngine(ch.alloc());
                pipeline.addFirst("ssl", new SslHandler(engine, false));
            }
            pipeline.addLast(NettyNetworkConnectionFactory.DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
            pipeline.addLast(NettyNetworkConnectionFactory.ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
            pipeline.addLast(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, new JMSHeartbeatHandler(0));
            pipeline.addLast(NettyNetworkConnectionFactory.JMS_SERVER_HANDLER_KEY, new JMSServerHandler());
        }
    }
	
	private final class ChainConfiguration {
        final int configPort;
        final String configHost;

        final Map<String, Object> tcpOptions;
        final Map<String, Object> sslOptions;

        volatile int activePort = -1;

        boolean isValidConfig = false;

        ChainConfiguration(int port, String host,
                           Map<String, Object> tcp,
                           Map<String, Object> ssl) {

            tcpOptions = tcp;
            sslOptions = ssl;
            configPort = port;
            configHost = host;
        }

        /**
         * @return returns listening port for the chain. if chain has some problem, returns -1
         */
        public int getActivePort() {
            if (configPort < 0)
                return -1;
            //TODO Check this
            return configPort;
        }

        /**
         * @return true if the ActiveConfiguration contains the required
         *         configuration to start the jfap chain. Basic chain needs only tcpOptions,
         *         secure chain needs both tcpOptions and sslOptions
         */
        @Trivial
        public boolean complete() {
            if (tcpOptions == null)
                return false;

            if (_isSecureChain && sslOptions == null)
                return false;

            return true;
        }

        /**
         * @return true if config is unchanged (host,port,tcpOtions and sslOptions are unchanged) </br>
         *         otherwise returns false
         */
        protected boolean unchanged(ChainConfiguration other) {
            if (other == null)
                return false;

            // Only look at ssl options if this is an secure chain
            if (_isSecureChain) {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       sslOptions == other.sslOptions;
            } else {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions;
            }
        }

        protected boolean tcpChanged(ChainConfiguration other) {
            if (other == null)
                return false;

            return !configHost.equals(other.configHost) ||
                   configPort != other.configPort ||
                   tcpOptions != other.tcpOptions;
        }

        protected boolean sslChanged(ChainConfiguration other) {
            if (other == null)
                return false;

            return sslOptions != other.sslOptions;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                   + "[host=" + configHost
                   + ",port=" + configPort
                   + ",listening=" + activePort
                   + ",complete=" + complete()
                   + "]";
        }
    }

}
