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

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.InetAddress;

public interface SIPListenningConnection 
{
//	public	void	addListener (SIPListenningConnectionListener	connectionServerListener);
//
//	public	void	removeListener (SIPListenningConnectionListener	connectionServerListener);
	
	public SIPConnection createConnection( InetAddress remoteAdress , int remotePort )
		throws IOException;
		
	public void	listen() 
		throws IOException;
	
	public void stopListen();
	
	public void close();
	
	public ListeningPoint getListeningPoint();
	
}
