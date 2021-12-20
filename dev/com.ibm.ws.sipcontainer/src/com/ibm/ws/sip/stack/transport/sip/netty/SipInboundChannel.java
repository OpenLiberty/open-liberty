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
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;

import jain.protocol.ip.sip.ListeningPoint;

/**
 * base class for inbound channels of any transport
 * 
 * @author ran
 */
public abstract class SipInboundChannel implements SIPListenningConnection {
	/** class logger */

	protected static final TraceComponent tc = Tr.register(SipInboundChannel.class);

	public static final String SipInboundChannelName = "SipInboundChannel";

	/** host/port/transport this channel is listening to */
	private ListeningPoint m_listeningPoint;

	/** name of outbound chain, for creating outbound connections */
	private String m_outboundChainName;

	/**
	 * constructor
	 */
	public SipInboundChannel(ListeningPoint lp, String outboundChainName) {
		m_listeningPoint = lp;
		m_outboundChainName = outboundChainName;
	}

// -------------------------------------
// InboundProtocolChannel implementation
// -------------------------------------

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#start()
	 */
	public void start() throws ChannelException {
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#stop(long)
	 */
	public void stop(long millisec) throws ChannelException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Stop channel: " + this + " time=" + millisec);
		}

		signalNoConnections();

		// TODO Anat: We need to make an intelligent decision about this Channel and not

	}

	/**
	 * Send an event to the channel framework that there are no more active
	 * connections on this quiesced channel instance. This will allow an early final
	 * chain stop instead of waiting the full quiesce timeout length.
	 */
	private void signalNoConnections() {
		// ANNA
		/*
		 * EventAdmin engine = GenericEndpointImpl.getEventAdmin();
		 * 
		 * Map<String, Object> eventProps = new HashMap<String, Object>(1);
		 * eventProps.put(ChannelFramework.EVENT_CHANNELNAME,
		 * m_config.getExternalName());
		 * 
		 * if (engine != null) { Event event = new
		 * Event(ChannelFramework.EVENT_STOPCHAIN.toString(), eventProps);
		 * engine.postEvent(event); }
		 */
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#init()
	 */
	public void init() throws ChannelException {
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#destroy()
	 */
	public void destroy() throws ChannelException {
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#update(com.ibm.wsspi.channelfw.framework.ChannelData)
	 */
	public void update(ChannelData cc) {
	}

	/**
	 * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
	 */
	public Class getDiscriminatoryType() {
		// there is no data type
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "getDiscriminatoryType", "");
		}

		return null;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
	 */
	public Class getApplicationInterface() {
		// there is no application interface

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "getApplicationInterface", "");
		}

		return null;
	}

// --------------------------------------
// SIPListenningConnection implementation
// --------------------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#listen()
	 */
	public void listen() throws IOException {
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#stopListen()
	 */
	public void stopListen() {
		try {
			stop(0);
		} catch (ChannelException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "stopListen", "ChannelException", e);
			}
		}
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#close()
	 */
	public void close() {
		try {
			destroy();
		} catch (ChannelException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "stopListen", "ChannelException", e);
			}
		}
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#getListeningPoint()
	 */
	public ListeningPoint getListeningPoint() {
		return m_listeningPoint;
	}

	/**
	 * called when creating an outbound connection
	 * 
	 * @return name of outbound chain
	 */
	public String getOutboundChainName() {
		return m_outboundChainName;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return m_listeningPoint == null ? "null" : m_listeningPoint.toString();
	}

}
