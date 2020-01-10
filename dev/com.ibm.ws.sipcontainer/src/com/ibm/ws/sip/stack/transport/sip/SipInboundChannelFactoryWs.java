/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip;

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.util.Map;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.stack.transport.chfw.GenericEndpointImpl;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

/**
 * creates inbound channels for any type of transport under websphere.
 * creates an outbound chain for each inbound channel.
 * 
 * @author ran
 */
public class SipInboundChannelFactoryWs extends SipChannelFactory
{
	 /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SipInboundChannelFactoryWs.class);
    
	
	/** the name identifying this chain group */
	public static final String ACCEPTOR_ID = SipInboundChannelFactoryWs.class.getName();
	
	/**
	 * chain counter. needed for creating unique chain names of outbound chains,
	 * in case of multiple listening points on the same transport
	 */ 
	private static int s_chains = 0;

	
	/**
	 * constructor
	 */
	public SipInboundChannelFactoryWs() {
	}
	
	/**
	 * @see com.ibm.wsspi.channelfw.impl.BaseChannelFactory#createChannel(com.ibm.wsspi.channelfw.framework.ChannelData)
	 */
	public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,"findOrCreateChannel", config.getName());
        }
		// check to see if we are running in the control region of Z
		/*TODO Liberty if (AdminHelper.getPlatformHelper().isControlJvm()) {
			// yes - in the control region of Z.
			// it's actually the proxy running here, not the container
			return super.createControlRegionChannel(config);
		}*/
		
		ChainData chain = getChain(config);
		if (chain == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,
					"findOrCreateChannel",
					"Error: no chain data for channel [" + config.getName() + ']');
			}
			return null;
		}
		ChannelData[] channels = getChannels(chain);
		if (channels == null) {
			// getChannels failed and logged some error message
			return null;
		}
		/*
		  get here with a chain of 2 channels (udp/tcp) or 3 channels (tls):
		  1         sip     2
		  0 udp/tcp     tls 1
		       -        tcp 0
		*/
		final int nChannels = channels.length;

		// the way to determine the SIP transport is by fetching this custom
		// attribute from the container channel
		ChannelData containerChannel = (ChannelData)channels[nChannels-1];
		String transport = (String)containerChannel.getPropertyBag().get("channelChainProtocolType");
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"findOrCreateChannel", "transport [" + transport + ']');
		}
		if (transport == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "findOrCreateChannel",
					"Error: no transport property specified "
						+ "in the container channel [" + containerChannel.getName() + ']');
			}
			return null;
		}
		ChannelData transportChannel = (ChannelData)channels[nChannels-2];
		ListeningPoint lp = createListeningPoint(transportChannel, transport);

		if (lp == null) {
			// createListeningPoint failed and logged some error message
			return null;
		}

		// create inbound channel,
		// and use this chance to create the equivalent outbound chain
		
		String chainNumber = String.valueOf(s_chains++);
		SipInboundChannel channel;
		if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_UDP)) {
			String outboundChainName = "UdpOutboundChain_" + chainNumber;
			channel = SipUdpInboundChannel.instance(config, lp, outboundChainName);
			String UDPType = (String)GenericEndpointImpl.getUdpOptions().get("type");
			createOutboundChain(
					//TODO anat: find the appropriate channel Framework
				GenericEndpointImpl.getChannelFramework(),
				outboundChainName,
				new String[] {
					SipUdpOutboundChannel.SipUdpOutboundChannelName + "_" + chainNumber,
					"UdpOutboundChannel_" + chainNumber
				},
				new String[] {
						SipUdpOutboundChannel.SipUdpOutboundChannelName,
						UDPType
				},
				null);
		}
		else if (transport.equalsIgnoreCase(ListeningPoint.TRANSPORT_TCP)) {
			String outboundChainName = "TcpOutboundChain_" + chainNumber;
			channel = new SipTcpInboundChannel(config, lp, outboundChainName);
			String tcpType = (String)GenericEndpointImpl.getTcpOptions().get("type");
			createOutboundChain(
				GenericEndpointImpl.getChannelFramework(),
				outboundChainName,
				new String[] {
					SipTcpOutboundChannel.SipTcpOutboundChannelName + "_" + chainNumber,
					"TcpOutboundChannel_" + chainNumber
				},
				new String[] {
					SipTcpOutboundChannel.SipTcpOutboundChannelName,
					tcpType
				},
				null);
		}
		else if (transport.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS)) {
			String outboundChainName = "TlsOutboundChain_" + chainNumber;
			channel = new SipTlsInboundChannel(config, lp, outboundChainName);
			ChannelData tlsChannel = (ChannelData)channels[nChannels-2];
			String tcpType = (String)GenericEndpointImpl.getTcpOptions().get("type");
			Map props = tlsChannel.getPropertyBag();
			createOutboundChain(
					GenericEndpointImpl.getChannelFramework(),
				outboundChainName,
				new String[] {
					SipTlsOutboundChannel.SipTlsOutboundChannelName + "_" + chainNumber,
					"TlsOutboundChannel_" + chainNumber,
					"TcpSecureOutboundChannel_" + chainNumber
				},
				new String[] {
				SipTlsOutboundChannel.SipTlsOutboundChannelName,
				"SSLChannel",
				tcpType},
				props);
		}
		else {
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc,  "findOrCreateChannel", "unknown transport ["
						+ transport + ']');
	        }			
			return null;
		}
		
		// tell the stack connection factory about this new channel
		String chainName = chain.getName();
		try {
			SIPConnectionFactoryImplWs.instance().addListeningConnection(lp, channel, chainName);
		} catch (IOException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "failed creating a connection", e);
	        }	
			return null; //would cause a throw of an exception from the channel code
		}
		return channel;
	
		
	}
	
	/**
	 * creates a jain listening point
	 * @return a new listening point object, null on error
	 */
	private ListeningPoint createListeningPoint(ChannelData transportChannel, String transport) {
		String host;
		int port;

		Map props = transportChannel.getPropertyBag();
		if (props == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"Error in createListeningPoint: no properties in ["
					+ transportChannel.getName() + ']');
			}
			return null;
		}
		host = (String)props.get(ChannelFrameworkConstants.HOST_NAME);
		if (host == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"Error in createListeningPoint: missing host in channel ["
					+ transportChannel.getName() + ']');
			}
			return null;
		}
		if (host.equals("*")) {
			host = "0.0.0.0";
		}
		String portStr = (String)props.get(ChannelFrameworkConstants.PORT);
		if (portStr == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"Error in createListeningPoint: missing port in channel ["
					+ transportChannel.getName() + ']');
			}
			return null;
		}
		port = Integer.parseInt(portStr);
		ListeningPoint lp = new ListeningPointImpl(host, port, transport, transportChannel.getName());
		return lp;
	}
	
	/**
	 * gets the chain data given a channel that belongs to that chain
	 * @param config given channel
	 * @return the chain data
	 */
	private ChainData getChain(ChannelData config) {
		ChainData chain = (ChainData)config.getPropertyBag().get(ChannelFrameworkConstants.CHAIN_DATA_KEY);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"getChain "+ chain + " config = " + config);
		}
		return chain;
	}
	
	/**
	 * gets the array of channels from config
	 * @param chain chain data from websphere config
	 * @return array of 3 or more channels, null on error
	 */
	private ChannelData[] getChannels(ChainData chain) {
		ChannelData[] channels = chain.getChannelList();

		// expect at least 3 channels: transport+protocol+application
		int nChannels = channels == null ? -1 : channels.length;
		if (nChannels < 2) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"Error in SipInboundChannelFactoryWs.getChannels: only ["
					+ channels.length + "] channels in chain");
			}
			return null;
		}
		return channels;
	}

	/**
	 * creates outbound chain based on given
	 * transport, protocol, and application channel factories
	 */
	private void createOutboundChain(
		ChannelFramework cf,
		String outboundChainName,
		String[] outboundChannelNames,
		String[] outboundChannelFactories,
		Map properties)
	{
		int nChannels = outboundChannelNames.length;
		if (outboundChannelFactories.length != nChannels) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "createOutboundChain", "Error: outbound channel config mismatch ["
					+ nChannels + "] != [" + outboundChannelFactories.length + ']');
			}
			return;
		}
		try {
			for (int i = 0; i < nChannels; i++) {
				String channelName = outboundChannelNames[i];
				String channelFactory = outboundChannelFactories[i];
	            if (cf.getChannel(channelName) == null) {
	            	cf.addChannel(channelName, cf.lookupFactory(channelFactory), properties);
	            }
			}
            if (cf.getChain(outboundChainName) == null) {
                cf.addChain(outboundChainName, FlowType.OUTBOUND, outboundChannelNames);
            }
		}
		catch (ChannelException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "createOutboundChain", "ChannelException", e);
			}
		}
		catch (ChainException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "createOutboundChain", "ChainException", e);
			}
		}
	}


	@Override
	public void updateProperties(Map<Object, Object> properties)
			throws ChannelFactoryPropertyIgnoredException {
		
	}

	@Override
	public Map<Object, Object> getProperties() {
		return null;
	}

	@Override
	public OutboundChannelDefinition getOutboundChannelDefinition(
			Map<Object, Object> props) {
		return null;
	}
	

	/**
	 * @see com.ibm.wsspi.channelfw.WSChannelFactory#getOutboundChannelDefinition(java.util.Map)
	 */
	/*TODO Liberty public OutboundChannelDefinition getOutboundChannelDefinition(Map props) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,this, "getOutboundChannelDefinition", "");
		}
		
        // check to see if we are running in the control region of Z
        if (AdminHelper.getPlatformHelper().isControlJvm()) {
            // yes - in the control region of Z.
            // it's actually the proxy running here, not the container
            return null;
        }
		return new WSSipOutboundDefinition(props);
	}*/

	/**
	 * called by websphere when this factory is loaded
	 * @see com.ibm.wsspi.channelfw.WSChannelFactoryRCS#determineAcceptorID(com.ibm.wsspi.runtime.config.ConfigObject)
	 * @see SIPConnectionsModel constructor
	 */
	/*TODO Liberty public String determineAcceptorID(ConfigObject configObject) throws ConfigurationError {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,this, "determineAcceptorID",
				"object [" + configObject.getString("name", null)
					+ "] group [" + ACCEPTOR_ID + ']');
		}
		
//		// TODO we don't need this code, stack will read the prop directly 
//		boolean implicitWebsphere = ApplicationProperties.getInstance().getBoolean(StackProperties.USE_CHANNEL_FRAMEWORK);
//		if (implicitWebsphere) {
//			// use this chance to tell the stack we are under websphere,
//			// assuming the stack was not started yet
//			Map properties = new HashMap(1);
//			properties.put("javax.sip.channelframework.ws", "true");
//			ApplicationProperties.getInstance().add(properties);
//		}
//		else {
//			// stack configuration is forcing standalone, by explicitly setting
//			// javax.sip.channelframework.ws=false
//		}

		return ACCEPTOR_ID;
	}*/
}
