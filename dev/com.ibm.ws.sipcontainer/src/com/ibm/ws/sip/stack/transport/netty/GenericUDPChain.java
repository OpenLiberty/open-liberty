/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.util.HashMap;
import java.util.Map;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.ws.sip.stack.transport.ActiveConfiguration;
import com.ibm.ws.sip.stack.transport.GenericEndpointImpl;

import io.netty.channel.*;
import io.openliberty.netty.internal.*;
import io.openliberty.netty.internal.exception.NettyException;
import jain.protocol.ip.sip.ListeningPoint;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class GenericUDPChain extends GenericChain  {
    
    private static final LogMgr c_logger = Log.get(GenericUDPChain.class);

    private String chainName;

    
    /**
     * Create the new chain with it's parent endpoint
     * 
     * @param sipEndpointImpl the owning endpoint: used for notifications
     * @param isTls true if this is to be an TLS chain.
     */
    public GenericUDPChain(GenericEndpointImpl owner) {
    	super(owner);
    	
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
        chainName = "UDP_" + name + "_" + endpointId;        
        super.init(endpointId, componentId, nfBundle, name);
    }

    /**
     *  @see com.ibm.ws.sip.stack.transport.GenericChainBase#createActiveConfiguration()
     */
    protected ActiveConfiguration createActiveConfiguration(){
    	Map<String, Object> udpOptions = GenericEndpointImpl.getUdpOptions();
        Map<String, Object> endpointOptions = owner.getEndpointOptions();
    	return new ActiveConfiguration(udpOptions, endpointOptions, this);
    }

  /**
   * @see com.ibm.ws.sip.stack.transport.GenericChainBase#rebuildTheChannel(com.ibm.ws.sip.stack.transport.ActiveConfiguration, com.ibm.ws.sip.stack.transport.ActiveConfiguration)
   */
    protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig) {

        // We've been through channel configuration before... 
        // We have to destroy/rebuild the chains because the channels don't
        // really support dynamic updates. *sigh*

	}

    
    /**
	 * Returns the name of this chain
	 * @return
	 */
	public String getName() {
		return chainName;
	}

  
    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
    public void setupEventProps(Map<String, Object> eventProps) {
        // No properties for this chain at this time
    }

	@Override
	public Type getType() {
		return Type.UDP;
	}

	@Override
	public String getTransport() {
		return ListeningPoint.TRANSPORT_UDP;
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
                    options.putAll(getCurrentConfig().udpOptions);
                    options.put(ConfigConstants.EXTERNAL_NAME, getName());
                    bootstrap = nettyBundle.createUDPBootstrapInbound(options);
                    bootstrap.handler(new SipUDPInitializer(null));

                    nettyBundle.startInbound(bootstrap, ep.getHost(), ep.getPort(), future -> {
                        if (future.isSuccess()) {
                            if (c_logger.isTraceDebugEnabled()) {
                                c_logger.traceDebug("SIP UDP endpoint start success");
                            }
                        } else {
                            if (c_logger.isTraceDebugEnabled()) {
                                c_logger.traceDebug("SIP UDP endpoint start failure: " + future.cause());
                            }
                        }
                    });
                } catch (NettyException e) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug("Exception creating UDP bootstrap: " + e);
                    }
                }
            }
    }

    /**
     * ChannelInitializer for SIP over UDP
     */
    private class SipUDPInitializer extends ChannelInitializerWrapper {

        ChannelInitializerWrapper parent;

        public SipUDPInitializer(ChannelInitializerWrapper parent) {
            this.parent = parent;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            if (parent != null) {
                parent.init(ch);
            }
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new SipMessageBufferDatagramDecoder());
            pipeline.addLast("handler", new SipDatagramHandler());
        }
    }

}
