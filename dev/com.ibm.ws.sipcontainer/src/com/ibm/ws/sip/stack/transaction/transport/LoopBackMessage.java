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
package com.ibm.ws.sip.stack.transaction.transport;

import jain.protocol.ip.sip.SipProvider;

import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;

/**
 * @author Amirk
 * support loopback messages to the stack.
 * holds a message that should be sent to the loopback address
 */
public class LoopBackMessage
{
	
	//
	// Members.
	//


	/**
	 * the sip message
	 */
	MessageImpl m_msg;
	
	/**
	 * the sip connection , can be null
	 */
	SIPConnection m_connection;
	
	
	/**
	 * sip provider on which the message was sent
	 */
	SipProvider m_provider;
	
	//
	// Life cycle.
	//
	
	/**
	 * constructor
	 */
	public LoopBackMessage( MessageImpl msg , SipProvider provider ,  SIPConnection con )
	{
		if( msg==null || provider==null ) throw new IllegalArgumentException("Provider and Message cannot be null!!!");
		m_connection = con;
		m_provider = provider;
		m_msg = msg;
	}
	
	/**
	 * @return SIPConnection
	 */
	public SIPConnection getConnection()
	{
		return m_connection;
	}

	/**
	 * @return MessageImpl
	 */
	public MessageImpl getMsg()
	{
		return m_msg;
	}

	/**
	 * @return SipProvider
	 */
	public SipProvider getProvider()
	{
		return m_provider;
	}
}
