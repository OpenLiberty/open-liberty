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
package com.ibm.ws.sip.stack.transaction.transport.connections;

import java.io.IOException;

import com.ibm.ws.sip.parser.MessageParser;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;

/**
 * @author amirk
 * 
 * reresents a SIPConnection.
 * implementation is protocol independent ( TCP,TLS)
 * 
 */
public interface SIPConnection
{

	
	/**
	 * connect to the remote target
	 * @throws IOException - if we cannot connect to the remote target
	 */
	public void	connect () 	throws IOException;
				
	/**
	 * start the connection - this will enable reading and writing to the remote target
	 * @throws IOException
	 */
	public void    start()  throws IOException;

	/**
	 * sends out a message
	 * @param messageContext outbound messageContext
	 * @param useCompactHeaders TODO
	 * @boolean considerMtu true if should be sensitive to the path MTU, per rfc 3261-18.1.1
	 * @throws IOException on failure to serialize or transmit the message
	 */
	public void write(MessageContext messageContext, boolean considerMtu, UseCompactHeaders useCompactHeaders) throws IOException;

	/**
	 * close the connection - this will stop the read and write operations
	 * if stream oriented ( like a stream over a socket for TLS and TCP )
	 * this will close the socket
	 *
	 */
	public void 	close();
	
	/**
	 * this method returns true if the connection was connected and
	 * is now not connected any more.
	 * It does not apply to the case the connection is in the process
	 * of being established 
	 * 
	 */
	public boolean isClosed();

	/**
	 * is the connection reliable.
	 * should return true for TLS,TCP , false for UDP
	 * @return boolean
	 */
	public boolean isReliable();
	
	
	/**
	 * is this socket secure.
	 * should return true on TLS , false on TCP,UDP
	 * @return boolean
	 */
	public boolean isSecure();
	
	/**
	 * is the connection connected (this will return false in 2 occasions:
	 * 1. during connection establishment
	 * 2. after connection closure
	 * @return boolean - true if connnected
	 */
	public boolean isConnected();
	
	/**
	 * get the remote host adress
	 * @return String - the remote host
	 */
	public String getRemoteHost();
	
	/**
	 * get the remote port.
	 * @return int - remote port
	 */
	public int getRemotePort();
	
	/**
	 * set aliace port.
	 * this enables to reuse the connection.
	 * can be used to support the SIP Aliace parameter 
	 * @param port - the aliace port
	 */
	public void setAliacePort( int port );
		
	/**
	 * get the aliace port
	 * @return int 
	 */
	public int getAliacePort();
	
	/**
	 * does connection have alice port
	 * @return boolean - true if has aliace port
	 */
	public boolean hasAliacePort();
	
	/**
	 * returns transport type
	 * @return String - TLS,TCP,UDP
	 */
	public String getTransport();
	
	/**
	 * returns the key for the connection
	 * @return String
	 */
	public Hop getKey();
	
		
	/**
	 * set the key for the connection
	 * @param key
	 */
	public void setKey(Hop key);

	/**
	 * determines if this connection is inbound (accepted) or outbound
	 * (created).
	 * this is only meaningful to stream connections.
	 * @return true if this connection is outbound, false if inbound
	 */
	public boolean isOutbound();

	/**
	 * sets the type of connection - inbound (accepted) or outbound (created)
	 * @param outbound true if this connection is outbound, false if inbound
	 */
	public void setOutbound(boolean outbound);

	/**
	 * gets the message parser associated with this connection.
	 * note that multiple connections can be associated with a single
	 * message parser
	 * 
	 * @return the message parser associated with this connection
	 */
	public MessageParser getMessageParser();
	
	/**
	 * get the listenning connection 
	 * @return SIPListenningConnection
	 */
	public SIPListenningConnection getSIPListenningConnection();
		
	/**
	 * returns the path MTU (maximum transmission unit)
	 * - maximum number of bytes that can be sent in a single datagram packet.
	 * applies only to non-reliable sockets.
	 * caller should check isReliable() before calling this.
	 * 
	 * @return the path MTU in bytes, or -1 if isReliable()
	 */
	public int getPathMTU();
	
	/**
	 * a method that will called if an error ocurrs while reading.
	 *  
	 * @param e - the Exception, if exists
	 *  handle messages that have not been sent yet.
	 */
	public void connectionError(Exception e);
	
	/**
	 * a method that will handle success writing to connection
	 * @param messageContext TODO
	 * 
	 */
	public void writeComplete(MessageContext messageContext);

	/**
	 * a method that will handle success writing to connection
	 * 
	 */
	public void readComplete();
	
	/**
	 * Increments the number of parse errors per the connection.
	 */
	public void incrementNumberOfParseErrors();
	
	/**
	 * Indicates whether the connection should be dropped.
	 * @return true if the connection should be dropped. Otherwise, false.
	 */
	public boolean shouldDropConnection();
	
	/**
	 * Indicates whether at least one parse error is allowed.
	 * @return true if at least one parse error is allowed. Otherwise, false.
	 */
	public boolean isAParseErrorAllowed();

}
