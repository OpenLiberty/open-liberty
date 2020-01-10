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
package com.ibm.ws.sip.stack.transaction.transport.connections.tls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPStreamConectionAdapter;

/**
 * @author amirk
 * 
 * represents a SIPConnection.
 * implementation is protocol independent ( TCP,TLS)
 * 
 */
public class SIPConnectionImpl 
	extends SIPStreamConectionAdapter implements HandshakeCompletedListener
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SIPConnectionImpl.class);
	
	/** the socket factory */
	private SSLSocketFactory m_factory;
					
	/**
	 * called when an incoming connection is created to the stack
	 * @param socket
	 * @param remoteHost
	 * @param remotePort
	 * @throws IOException
	 */
	SIPConnectionImpl( SIPListenningConnection listenningConnection ,  
					   Socket socket )
	{
		super( listenningConnection , socket );
	}
	
	
	SIPConnectionImpl( SIPListenningConnection listenningConnection ,
						   SSLSocketFactory factory,   
						   InetAddress remoteAdress , 
						   int remotePort )
	{
		super( listenningConnection , InetAddressCache.getHostAddress(remoteAdress), remotePort );
		m_factory = factory;
	}
			
		
	public boolean isReliable()
	{
		return true;	
	}
	
	public boolean isSecure()
	{
		return true;	
	}
	
	
	public String getTransport()
	{
		return SIPTransactionConstants.TLS;	
	}
	

	public boolean equals( Object obj )
	{
		boolean retVal = false;
		if( obj == this ) retVal = true;
		else if( obj instanceof SIPConnectionImpl  )
		{
			retVal = obj.toString().equals(toString()); 
		}
		return retVal;
	}
	
	public Socket createSocket() throws IOException
	{
		SSLSocket socket = (SSLSocket)m_factory.createSocket();
		//assignin the m_socket memeber is required cause the next step "addHandshakeCompletedListener"
		//will call the hashcode of this connection which is determined by the m_socket memeber
		m_socket = socket;
		socket.addHandshakeCompletedListener(this);
 		return socket;
	}

	public void handshakeCompleted(HandshakeCompletedEvent evt )
	{
		Object[] params ={ getRemoteHost() , new Integer(getRemotePort())  };
		if( c_logger.isInfoEnabled())
		{
		c_logger.info("info.com.ibm.ws.sip.stack.transaction.transport.connections.tls.SIPConnectionImpl.handshakeCompleted",
        				Situation.SITUATION_CONNECT,
        				params);
		}
	}

}
