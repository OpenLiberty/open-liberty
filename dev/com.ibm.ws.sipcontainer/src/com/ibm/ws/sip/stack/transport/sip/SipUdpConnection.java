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

import java.io.IOException;

import com.ibm.ws.sip.parser.DatagramMessageParser;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
//TODO Liberty import com.ibm.ws.management.AdminHelper;

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
	
	/**
	 * constructor for inbound channels
	 * @param channel channel that created this connection
	 * @param connLink the connection link for sending outbound messages
	 */
	public SipUdpConnection(SIPListenningConnection channel, UdpSender connLink) {
		this(null, 0, channel, connLink);
	}
	
	/**
	 * constructor
	 * @param peerHost remote host address in dotted form, null if inbound
	 * @param peerPort remote port number, 0 if inbound
	 * @param channel channel that created this connection
	 * @param connLink the connection link for sending outbound messages
	 */
	public SipUdpConnection(String peerHost, int peerPort, SIPListenningConnection channel, UdpSender connLink) {
		super(peerHost, peerPort, channel);
		m_connLink = connLink;
	}
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.channelframework.BaseConnection#write(com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer, boolean, UseCompactHeaders)
	 */
	public void write(MessageContext messageSendingContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException {
		/*TODO Liberty if (AdminHelper.getPlatformHelper().isZOS()) {
			// don't prepareBuffer() on Z. the conn-link is a SipConnLink
			// (and not a SipUdpConnLink) who's about to call prepareBuffer()
		}
		else {
			prepareBuffer(messageSendingContext,considerMtu,useCompactHeaders);
		}*/

		prepareBuffer(messageSendingContext,considerMtu,useCompactHeaders);
		
		// send the message, or queue it if cannot send right now
		m_connLink.send(messageSendingContext, useCompactHeaders);
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
		return "udp";
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
