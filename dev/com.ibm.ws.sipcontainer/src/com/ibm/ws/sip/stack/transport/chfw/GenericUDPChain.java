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
package com.ibm.ws.sip.stack.transport.chfw;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transport.sip.SipInboundChannel;
import com.ibm.wsspi.channelfw.ChainEventListener;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class GenericUDPChain extends GenericChain implements ChainEventListener {
    
	
	private static final TraceComponent tc = Tr.register(GenericUDPChain.class);

    /**  Chain name    */
    private String m_myName;

    
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
     * @param cfw Channel framework
     */
    public void init(String endpointId, Object componentId, CHFWBundle cfBundle, String name) {
    
        m_myName = "UDP_" + name + "_" + endpointId;
        
        super.init(endpointId, componentId, cfBundle, name);
    }

    /**
     *  @see com.ibm.ws.sip.stack.transport.chfw.GenericChain#createActiveConfiguration()
     */
    protected ActiveConfiguration createActiveConfiguration(){
    	Map<String, Object> udpOptions = GenericEndpointImpl.getUdpOptions();
        Map<String, Object> endpointOptions = owner.getEndpointOptions();
    	return new ActiveConfiguration(udpOptions, endpointOptions, this);
    }
  
    
    /**
     *  @see com.ibm.ws.sip.stack.transport.chfw.GenericChain#createChannels(com.ibm.ws.sip.stack.transport.chfw.ActiveConfiguration)
     */
    protected void createChannels(ActiveConfiguration newConfig){
	    
    	Map<Object, Object> chanProps;
		Map<String, Object> udpOptions = GenericEndpointImpl.getUdpOptions();

    	// Endpoint
	    // define is a simple replace of the old value known to the endpointMgr
	    EndPointInfo ep = endpointMgr.getEndPoint(getEndpointName());
	    ep = endpointMgr.defineEndPoint(getEndpointName(), newConfig.configHost, newConfig.configPort);
	    
	    	 
	    // UDP Channel
	    ChannelData udpChannel = getChannel(getName());
	    if (udpChannel == null) {
	        String typeName = (String) udpOptions.get("type");
	        chanProps = new HashMap<Object, Object>(udpOptions);
	        chanProps.put("endPointName", getEndpointName());
	        chanProps.put("hostname", ep.getHost());
	        chanProps.put("port", String.valueOf(ep.getPort()));
	
	        udpChannel = addChannel(getName(), typeName, chanProps,newConfig);
	        
	    }
	    
	    // SIP Channel
	    ChannelData sipChannel = getChannel(sipChannelName);
        if (sipChannel == null) {
            chanProps = new HashMap<Object, Object>();
            chanProps.put("endPointName", newConfig.endpointOptions.get(ID));
            chanProps.put("channelChainProtocolType", "udp");
            sipChannel = addChannel(sipChannelName, SipInboundChannel.SipInboundChannelName, chanProps,newConfig);
        }
       
	    ChainData cd = getCfw().getChain(getChainName());
	    if (null == cd) {
	        final String[] chanList;

	        chanList = new String[] { getName(), sipChannelName};
	
	        addChain(chanList,cd,newConfig);
	       
	    }
    }
    
  /**
   * @see com.ibm.ws.sip.stack.transport.chfw.GenericChain#rebuildTheChannel(com.ibm.ws.sip.stack.transport.chfw.ActiveConfiguration, com.ibm.ws.sip.stack.transport.chfw.ActiveConfiguration)
   */
    protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig) {

        // We've been through channel configuration before... 
        // We have to destroy/rebuild the chains because the channels don't
        // really support dynamic updates. *sigh*
       stopChain(oldConfig.toString());

        // Remove any channels that have to be rebuilt.. 
        if (newConfig.udpChanged(oldConfig))
            removeChannel(m_myName);
	}

    
    /**
	 * Returns the name of this chain
	 * @return
	 */
	public String getName() {
		return m_myName;
	}

  
    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
    public void setupEventProps(Map<String, Object> eventProps) {
        //TODO Liberty - do we need to setup properties for this chain ?
    }

 
}
