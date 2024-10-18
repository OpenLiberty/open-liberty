/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jfap.inbound.channel;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jfap.inbound.channel.CommsInboundChain.ChainState;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.server.impl.NettyJMSServerHandler;
import com.ibm.ws.sib.jfapchannel.netty.NettyJMSHeartbeatHandler;
import com.ibm.ws.sib.jfapchannel.netty.NettyNetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.netty.NettyToWsBufferDecoder;
import com.ibm.ws.sib.jfapchannel.netty.WsBufferToNettyEncoder;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.tcp.InactivityTimeoutHandler;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;
import io.openliberty.netty.internal.tcp.TCPUtils;
import io.openliberty.netty.internal.tls.NettyTlsProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;
import io.openliberty.netty.internal.impl.QuiesceHandler;


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

    /** The bootstrap this object wraps */
    private ServerBootstrapExtended bootstrap;
    private Channel serverChan;
    
    NettyInboundChain(CommsServerServiceFacade commsServer, boolean isSecureChain) {
        _commsServerFacade = commsServer;
        _isSecureChain = isSecureChain;
    }

	public InboundChain init(String endpointName, NettyFramework netty) {
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
        return this;
	}

	@Override
	public void enable(boolean enabled) {
		_isEnabled = enabled;
	}
	

	public boolean isEnabled() {
		return _isEnabled;
	}


	public boolean isRunning() {
		return _isEnabled && _isChainStarted;
	}
	
	private void quiesceListener(Channel channel) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "quiesceListener", channel);

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

        // no current connections, notify the final stop can happen now
        _nettyFramework.stop(serverChan);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "quiesceListener");
	}
	
	public void stopChannel(boolean closeGroups) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stopChannel");
		if(!_isChainStarted) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Chain not started. Moving along");
			return;
		}
		if(serverChan == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Netty channel not initialized. Setting chain stop");
			_isChainStarted = false;
			return;
		}else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                SibTr.debug(tc, "stopChannel","Stopping Channel "+ serverChan +" --- "+ serverChan.localAddress());
            }
	        //stopchain() first quiesce's(invokes chainQuiesced) depending on the chainQuiesceTimeOut
	        //Once the chain is quiesced StopChainTask is initiated.Hence we block until the actual stopChain is invoked
	        try {
	        	_nettyFramework.stop(serverChan, -1);
	        } catch (Exception e) {
	            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	                SibTr.debug(tc, "Failed in successfully cleaning(i.e stopping/destorying/removing) chain: ", e);
	        } finally {
	            _isChainStarted = false;
	        }
		}

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stopChannel");
	}

	/**
     * stop will get called only from de-activate.
     */
	@Override
	public void stop() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop");
		
		stopChannel(true);

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
                        (_isSecureChain) ? _commsServerFacade.getSecurePort() : _commsServerFacade.getBasicPort(),
                        _commsServerFacade.getHost(),
                        tcpOptions,
                        sslOptions);

        if ((newConfig.configPort < 0) || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            _currentConfig = newConfig;

            //stop the chain
            stopChannel(false);
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
                stopChannel(false);
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
            options.put(ConfigConstants.EXTERNAL_NAME, _endpointName);
            bootstrap = _nettyFramework.createTCPBootstrap(options);
            if (_isSecureChain) {
                NettyTlsProvider tlsProvider = _commsServerFacade.getNettyTlsProvider();
                String host = ep.getHost();
                String port = Integer.toString(ep.getPort());
                if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Create SSL", new Object[] {tlsProvider, host, port, sslOptions});
                context = tlsProvider.getInboundSSLContext(_currentConfig.sslOptions, host, port);
                if(context == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initChannel","Error adding TLS Support");
                    throw new NettyException("Problems creating SSL context");
                }
            }
            bootstrap.childHandler(new JMSServerInitializer(bootstrap.getBaseInitializer(), this));
            NettyInboundChain parent = this;
            this.serverChan = _nettyFramework.start(bootstrap, ep.getHost(), ep.getPort(), f ->{
                if (f.isCancelled() || !f.isSuccess()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        SibTr.debug(this, tc, "Channel exception during connect: " + f.cause().getMessage());
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "destroy", (Exception) f.cause());
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "destroy");
                }else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "ready", f);
                    Channel chan = f.channel();
                    f.addListener(innerFuture -> {
                        if (innerFuture.isCancelled() || !innerFuture.isSuccess()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                SibTr.debug(this, tc, "Channel exception during connect. Couldn't add quiesce handler: " + f.cause().getMessage());
                            }
                            quiesceListener(chan);
                        }else {
                            if(!_isChainStarted) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    SibTr.debug(this, tc, "Server Channel: " + serverChan + " will be closed because chain was disabled");
                                }
                                quiesceListener(chan);
                            }else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "adding quiesce", f);
                                _nettyFramework.registerEndpointQuiesce(chan, new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            SibTr.debug(this, tc, "Server Channel: " + serverChan + " received quiesce event so running close");
                                        }
                                        quiesceListener(chan);
                                        return null;
                                    }
                                    
                                });
                            }
                        }
                    });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "ready");
                }
            });
            _isChainStarted = true;

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
    private class JMSServerInitializer extends ChannelInitializerWrapper {
    	final ChannelInitializerWrapper parent;
		private final NettyInboundChain chain;

        public JMSServerInitializer(ChannelInitializerWrapper parent, NettyInboundChain chain) {
            this.parent = parent;
            this.chain = chain;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            parent.init(ch);
            ChannelPipeline pipeline = ch.pipeline();
            ch.attr(NettyJMSServerHandler.CHAIN_ATTR_KEY).set(_chainName);
            ch.attr(NettyJMSServerHandler.ATTR_KEY).set(this.chain);
            if(_isSecureChain) {
            	SSLEngine engine = context.newEngine(ch.alloc());
                pipeline.addFirst("ssl", new SslHandler(engine, false));
            }
            pipeline.addLast(NettyNetworkConnectionFactory.DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
            pipeline.addLast(NettyNetworkConnectionFactory.ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
            // Replace the timeout handler to handler the timeouts ourselves
            pipeline.replace(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME, NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, new NettyJMSHeartbeatHandler(0));
			pipeline.addLast(NettyNetworkConnectionFactory.JMS_SERVER_HANDLER_KEY, new NettyJMSServerHandler());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Channel: " + ch + " handler names: " + pipeline.names());
            }
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
