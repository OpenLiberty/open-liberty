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
package com.ibm.ws.sip.stack.transaction.transport.connections.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPStreamConectionAdapter;

/**
 * @author amirk
 * 
 * represents a SIPConnection of tcp. 
 */
public class SIPConnectionImpl 
	extends SIPStreamConectionAdapter 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SIPConnectionImpl.class);
    
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
	
	
	SIPConnectionImpl(  SIPListenningConnection listenningConnection ,
						InetAddress remoteAdress , 
						int remotePort )
	{
		super( listenningConnection , InetAddressCache.getHostAddress(remoteAdress) , remotePort );
	}
			
	public boolean isSecure()
	{
		return false;	
	}
		
	public String getTransport()
	{
		return "tcp";		
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
		return new Socket();			
	}
	
}
