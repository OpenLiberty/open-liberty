/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transport.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.openliberty.netty.internal.*;
import io.openliberty.netty.internal.exception.NettyException;
import jain.protocol.ip.sip.ListeningPoint;

/**
 * Encapsulation of steps for starting/stopping an http chain in a
 * controlled/predictable manner with a minimum of synchronization.
 */
public class GenericTCPChain extends GenericChain {
	private static final TraceComponent tc = Tr.register(GenericTCPChain.class);
    private static final LogMgr c_logger = Log.get(GenericTCPChain.class);

	private final boolean isTLS;

	private String tcpName;
	private String tlsName;
	SslContext context;

	/**
	 * Create the new chain with it's parent endpoint
	 * 
	 * @param sipEndpointImpl the owning endpoint: used for notifications
	 * @param isTls           true if this is to be an TLS chain.
	 */
	public GenericTCPChain(GenericEndpointImpl owner, boolean isTls) {
		super(owner);
		this.isTLS = isTls;
	}

    /**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as config changes,
     * so come up with names associated with this set of channels/chains that will be reused regardless
     * of start/stop/enable/disable/modify
     * 
     * @param endpointId The id of the sipEndpoint
     * @param componentId The DS component id
     * @param NettyFramework
     * @param name
     */
    public void init(String endpointId, Object componentId, NettyFramework nfBundle, String name) {
        final String root = "TCP" + (isTLS ? "-ssl" : "");

         String commmonName = root + "_" + name + "_" + endpointId;

        tcpName = commmonName;
        tlsName = "TLS-" + commmonName;

        super.init(endpointId, componentId, nfBundle, name);

    }

	/**
	 * @see com.ibm.ws.sip.stack.transport.GenericChainBase#createActiveConfiguration()
	 */
	protected ActiveConfiguration createActiveConfiguration() {
		Map<String, Object> tcpOptions = getOwner().getTcpOptions();
		Map<String, Object> sslOptions = (isTLS) ? getOwner().getSslOptions() : null;
		Map<String, Object> endpointOptions = getOwner().getEndpointOptions();
		return new ActiveConfiguration(isTLS, tcpOptions, sslOptions, endpointOptions, this);
	}

	/**
	 * This method is used when configuration of the Endpoint was changed and
	 * channels should be rebuilded.
	 */
	protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig) {

		// We've been through channel configuration before...
		// We have to destroy/rebuild the chains because the channels don't
		// really support dynamic updates. *sigh*
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return tcpName;
	}

	/**
	 * Publish an event relating to a chain starting/stopping with the given
	 * properties set about the chain.
	 */
	public void setupEventProps(Map<String, Object> eventProps) {

		eventProps.put(GenericServiceConstants.ENDPOINT_IS_TLS, isTLS);
	}

	@Override
	public Type getType() {
		return isTLS ? Type.tls : Type.tcp;
	}

	@Override
	public String getTransport() {
	    return isTLS ? "tls" : ListeningPoint.TRANSPORT_TCP;
	}

    /**
     * Update/start the chain configuration.
     */
    @Override
    public synchronized void update() {
        super.update();

        if (currentConfig.validConfiguration) {
            EndPointMgr em = nettyBundle.getEndpointManager();
            EndPointInfo ep = em.getEndPoint(getEndpointName());
            ep = em.defineEndPoint(getEndpointName(), currentConfig.configHost, currentConfig.configPort);

            ListeningPoint lp = sipInboundChannelFactory.initChannel(this);
            try {
                Map<String, Object> options = new HashMap<String, Object>();
                options.putAll(getCurrentConfig().tcpOptions);
                options.put(ConfigConstants.ExternalName, getName());

                serverBootstrap = nettyBundle.createTCPBootstrap(options);
                serverBootstrap.childHandler(new SipTCPInitializer(serverBootstrap.getBaseInitializer()));
                if (isTLS) {
                    context = GenericEndpointImpl.getTlsProvider().getInboundSSLContext(this.currentConfig.sslOptions, ep.getHost(), Integer.toString(ep.getPort()));
                }
                nettyBundle.start(serverBootstrap, ep.getHost(), ep.getPort(), future -> {
                    if (future.isSuccess()) {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger.traceDebug("SIP " + (isTLS ? "TCP" : "TLS") + " endpoint start success");
                        }
                    } else {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger.traceDebug("SIP " + (isTLS ? "TCP" : "TLS") + " endpoint start failure: " + future.cause());
                        }
                    }
                });
            } catch (NettyException e) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug("Exception creating " + (isTLS ? "TCP" : "TLS") + " bootstrap: " + e);
                }
            }
        }
    }

    /**
     * ChannelInitializer for SIP over TCP, and optionally TLS
     */
    private class SipTCPInitializer extends ChannelInitializerWrapper {

        final ChannelInitializerWrapper parent;

        public SipTCPInitializer(ChannelInitializerWrapper parent) {
            this.parent = parent;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            parent.init(ch);
            ChannelPipeline pipeline = ch.pipeline();
            if (isTLS) {
                SSLEngine engine = context.newEngine(ch.alloc());
                pipeline.addFirst("ssl", new SslHandler(engine, false));
            }
            pipeline.addLast("decoder", new SipMessageBufferStreamDecoder());
            pipeline.addLast("handler", new SipStreamHandler());
        }
    }

}
