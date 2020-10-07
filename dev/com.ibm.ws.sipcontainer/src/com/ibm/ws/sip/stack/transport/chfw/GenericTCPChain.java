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
public class GenericTCPChain extends GenericChain implements ChainEventListener {
    private static final TraceComponent tc = Tr.register(GenericTCPChain.class);

    private final boolean isTLS;

    private String tcpName;
    private String tlsName;


    /**
     * Create the new chain with it's parent endpoint
     * 
     * @param sipEndpointImpl the owning endpoint: used for notifications
     * @param isTls true if this is to be an TLS chain.
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
     * @param cfw Channel framework
     */
    public void init(String endpointId, Object componentId, CHFWBundle cfBundle, String name) {
    	final String root = "TCP" + (isTLS ? "-ssl" : "");
        
    	 String commmonName = root + "_" + name + "_" + endpointId;
    	 
        tcpName = commmonName;
        tlsName = "TLS-" + commmonName;
        
        super.init(endpointId, componentId, cfBundle, name);
    	
    }

   /**
     * @see com.ibm.ws.sip.stack.transport.chfw.GenericChain#createActiveConfiguration()
     */
    protected ActiveConfiguration createActiveConfiguration(){
    	Map<String, Object> tcpOptions = getOwner().getTcpOptions();
        Map<String, Object> sslOptions = (isTLS) ? getOwner().getSslOptions() : null;
        Map<String, Object> endpointOptions = getOwner().getEndpointOptions();
    	return new ActiveConfiguration(isTLS, tcpOptions, sslOptions, endpointOptions,this);
    }
      
    
    /**
     *  @see com.ibm.ws.sip.stack.transport.chfw.GenericChain#createChannels(com.ibm.ws.sip.stack.transport.chfw.ActiveConfiguration)
     */
    protected void createChannels(ActiveConfiguration newConfig){
	    
    	Map<Object, Object> chanProps;
    	Map<String, Object> tcpOptions = getOwner().getTcpOptions();

    	// Endpoint
	    // define is a simple replace of the old value known to the endpointMgr
	    EndPointInfo ep = endpointMgr.getEndPoint(getEndpointName());
	    ep = endpointMgr.defineEndPoint(getEndpointName(), newConfig.configHost, newConfig.configPort);
	
	    // TCP Channel
	    ChannelData tcpChannel = getChannel(tcpName);
	    if (tcpChannel == null) {
	    	chanProps = new HashMap<Object, Object>(tcpOptions);
	        String typeName = (String) tcpOptions.get("type");
	        chanProps.put("endPointName", getEndpointName());
	        chanProps.put("hostname", ep.getHost());
	        chanProps.put("port", String.valueOf(ep.getPort()));
	
	        tcpChannel = addChannel(getName(), typeName, chanProps,newConfig);
	        
	        // SSL Channel
		    if (isTLS) {
		        ChannelData sslChannel = getCfw().getChannel(tlsName);
		        if (sslChannel == null) {
		        	sslChannel = addChannel(tlsName, "SSLChannel", chanProps,newConfig);
		        }
		    }
	    }
	    
	
	    // SIP Channel
	    ChannelData sipChannel = getChannel(sipChannelName);
        if (sipChannel == null) {
            chanProps = new HashMap<Object, Object>();
//            chanProps.put("endPointName", owner.getName());
            chanProps.put("endPointName", newConfig.endpointOptions.get(ID));
            if (isTLS){
                chanProps.put("channelChainProtocolType", "tls");
            }
            else{
                chanProps.put("channelChainProtocolType", "tcp");
            }
            addChannel(sipChannelName, SipInboundChannel.SipInboundChannelName, chanProps,newConfig);
        }
	    ChainData cd = getCfw().getChain(getChainName());
	    if (null == cd) {
	        final String[] chanList;
	        if (isTLS){
	        	chanList = new String[] { getName(), tlsName, sipChannelName};
	        }
	        else{
	        	chanList = new String[] { getName(), sipChannelName};
	        }
	        addChain(chanList,cd,newConfig);
	    }
    }
    
    /**
     * This method is used when configuration of the Endpoint was changed and
     * channels should be rebuilded.
     */
    protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig) {

        // We've been through channel configuration before... 
        // We have to destroy/rebuild the chains because the channels don't
        // really support dynamic updates. *sigh*
       stopChain(oldConfig.toString());

        // Remove any channels that have to be rebuilt.. 
        if (newConfig.tcpChanged(oldConfig))
            removeChannel(tcpName);

        if (newConfig.sslChanged(oldConfig))
            removeChannel(tlsName);
	}

    
    /**
	 * 
	 * @return
	 */
	public String getName() {
		return tcpName;
	}

   
    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
    public void setupEventProps(Map<String, Object> eventProps) {
        
        eventProps.put(GenericServiceConstants.ENDPOINT_IS_TLS, isTLS);
    }

}
