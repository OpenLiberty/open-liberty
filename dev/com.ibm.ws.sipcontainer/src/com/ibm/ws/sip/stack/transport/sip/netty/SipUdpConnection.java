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

import com.ibm.ws.sip.parser.DatagramMessageParser;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.util.SipStackUtil;
import io.netty.channel.Channel;

/**
 * a UDP connection object.
 * there is no actual connection in UDP, but this object is used for
 * sending and receiving multiple datagrams with the same UDP endpoint
 * 
 * @author ran
 */
public class SipUdpConnection extends BaseConnection
{
	/** the connection link for sending outbound messages */
	private UdpSender m_connLink;
	
	/** a Netty channel associated with this SIP connection  */
	private Channel m_channel;
	
	/**
	 * constructor for inbound channels
	 * @param lc channel that created this connection
	 * @param connLink the connection link for sending outbound messages
	 * @param channel a Netty channel associated with this SIP connection 
	 */
	public SipUdpConnection(SIPListenningConnection lc, UdpSender connLink, Channel channel) {
		this(null, 0, lc, connLink, channel);
	}
	
	/**
	 * constructor
	 * @param peerHost remote host address in dotted form, null if inbound
	 * @param peerPort remote port number, 0 if inbound
	 * @param channel channel that created this connection
	 * @param connLink the connection link for sending outbound messages
	 * @param channel a Netty channel associated with this SIP connection 
	 */
	public SipUdpConnection(String peerHost, int peerPort, SIPListenningConnection lc, UdpSender connLink, Channel channel) {
		super(peerHost, peerPort, lc);
		m_connLink = connLink;
		m_channel = channel;
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.channelframework.BaseConnection#write(com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer, boolean, UseCompactHeaders)
	 */
	public void write(MessageContext messageSendingContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException {
		prepareBuffer(messageSendingContext,considerMtu,useCompactHeaders);
		
		// send the message, or queue it if cannot send right now
		m_connLink.send(messageSendingContext, useCompactHeaders);
	}

	public Channel getChannel() {
		return m_channel;
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#connect()
	 */
	public void connect() throws IOException {
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getTransport()
	 */
	public String getTransport() {
		return SipStackUtil.UDP;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isReliable()
	 */
	public boolean isReliable() {
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isSecure()
	 */
	public boolean isSecure() {
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getPathMTU()
	 */
	public int getPathMTU() {
		return com.ibm.ws.sip.stack.transaction.transport.connections.
			udp.SIPListenningConnectionImpl.getPathMTU();
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getMessageParser()
	 */
	public MessageParser getMessageParser() {
		// all datagram connections share the same stateless parser
		return DatagramMessageParser.getGlobalInstance();
	}
}
