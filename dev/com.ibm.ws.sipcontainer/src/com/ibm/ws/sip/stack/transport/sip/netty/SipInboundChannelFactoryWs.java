/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.stack.transport.netty.GenericChain;
import com.ibm.ws.sip.stack.transport.sip.SipChannelFactory;
import com.ibm.ws.sip.stack.util.SipStackUtil;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

import jain.protocol.ip.sip.ListeningPoint;

/**
 * creates inbound channels for any type of transport under websphere. creates
 * an outbound chain for each inbound channel.
 * 
 */
public class SipInboundChannelFactoryWs extends SipChannelFactory {
	/** RAS tracing variable */
	private static final TraceComponent tc = Tr.register(SipInboundChannelFactoryWs.class);

	/** the name identifying this chain group */
	public static final String ACCEPTOR_ID = SipInboundChannelFactoryWs.class.getName();


	/**
	 * chain counter. needed for creating unique chain names of outbound chains, in
	 * case of multiple listening points on the same transport
	 */
	private static int s_chains = 0;

	/**
	 * constructor
	 */
	public SipInboundChannelFactoryWs() {
	}

	public ListeningPoint initChannel(GenericChain chain) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "initChannel", chain);
		}

		String transport = chain.getTransport();

		ListeningPoint lp = createListeningPoint(chain);

		// create inbound channel,

		String chainNumber = String.valueOf(s_chains++);
		SipInboundChannel channel = null;
		String outboundChainName = null;
		
		switch (chain.getType()) {
		case UDP:
			outboundChainName = "UdpOutboundChain_" + chainNumber;
			channel = SipUdpInboundChannel.instance(lp, outboundChainName);
			break;
		case TCP:
			outboundChainName = "TcpOutboundChain_" + chainNumber;
			channel = new SipTcpInboundChannel(lp, outboundChainName);
			break;
		case TLS:
		    outboundChainName = "TcpOutboundChain_" + chainNumber;
		    channel = new SipTlsInboundChannel(lp, outboundChainName);
			break;
		}

		try {
			SIPConnectionFactoryImplWs.instance().addListeningConnection(lp, channel, chain.getChainName());
			return lp;
		} catch (IOException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "failed creating a connection", e);
			}
		}
		return null;
	}

	/**
	 * creates a jain listening point
	 * 
	 * @return a new listening point object, null on error
	 */
	private ListeningPoint createListeningPoint(GenericChain chain) {
		String host = chain.getActiveHost();

		if (host.equals("*")) {
			host = SipStackUtil.INADDR_ANY;
		}

		return new ListeningPointImpl(host, chain.getActivePort(), chain.getTransport(), chain.getChainName());
	}

	@Override
	public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
		return null;
	}

	@Override
	public void updateProperties(Map<Object, Object> properties) throws ChannelFactoryPropertyIgnoredException {
	}

	@Override
	public Map<Object, Object> getProperties() {
		return null;
	}

	@Override
	public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
		return null;
	}

}
