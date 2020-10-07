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
package com.ibm.ws.sip.stack.transaction.transport.connections.udp;

import java.io.IOException;
import java.util.Objects;

import com.ibm.ws.sip.parser.DatagramMessageParser;
import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionAdapter;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;

/**
 * @author amirk
 * 
 * represents a SIPConnection.
 * implementation is protocol independent ( TCP,TLS)
 * 
 */
public class SIPConnectionImpl 
	extends   SIPConnectionAdapter 
{
	
	/** 
	 * the listening connection the connection was created on
	 **/
	private SIPListenningConnectionImpl m_listenningConnection;
	
	/** 
	 * is the socket closed 
	 **/
	private boolean m_connected;
				
	/** 
	 * is the connection also reading 
	 **/
	private boolean m_OpenForRead;
	
	/**
	 * constructor for established UDP connection
	 * @throws IOException
	 */
	SIPConnectionImpl(
		SIPListenningConnectionImpl listenningConnection,
		String peerHost,
		int peerPort)
	{
		super(peerHost, peerPort);
		m_listenningConnection = listenningConnection;
		m_connected = false;
	}
	
	public void start()
	{
		m_OpenForRead = true;
	}
	
	public synchronized void connect ()
		throws IOException
	{ 
		//do nothing on UDP		
	}
		
	public boolean isReliable()
	{
		return false;	
	}
	
	public boolean isSecure()
	{
		return false;	
	}
	
	public boolean isConnected()
	{
		return m_connected;
	}
	
	public void setConnected() {
		m_connected = true;
	}
	
	/**
	 * gets the message parser associated with this connection.
	 * all datagram connections share the same stateless parser.
	 * 
	 * @return the message parser associated with this connection
	 */
	public MessageParser getMessageParser() {
		return DatagramMessageParser.getGlobalInstance();
	}
	
	public synchronized void close()
	{
		super.close();
	}
	
	public String getTransport()
	{
		return "udp";		
	}
	
	/** the local connection on which the connection was created */
	public SIPListenningConnection getSIPListenningConnection()
	{
		return m_listenningConnection;
	}
	
		
	public boolean equals( Object obj )
	{
		if( obj == this ) return true;
		else if( obj instanceof SIPConnectionImpl  )
		{
			return ((SIPConnectionImpl)obj).getKey().equals(getKey()); 
		}
		return false;
	}
	
	public int hashCode() {
		return Objects.hash(getKey());
	}
		
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#write(jain.protocol.ip.sip.message.Message)
	 */
	public void write(MessageContext messageContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException {
		prepareBuffer(messageContext, considerMtu, useCompactHeaders);
		write(messageContext);
	}

	/**
	 * send the message
	 */
	private void write(MessageContext messageContext) throws IOException
	{
	    m_listenningConnection.write(messageContext, getRemoteHost(), getRemotePort());
	}

	/**
	 * returns the path MTU (maximum transmission unit)
	 * - maximum number of bytes that can be sent in a single packet. 
	 * @return the path MTU in bytes, or 1500 if unknown (see 3261-18.1.1)
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getPathMTU()
	 */
	public int getPathMTU()
	{
		return SIPListenningConnectionImpl.getPathMTU();
	}

}
