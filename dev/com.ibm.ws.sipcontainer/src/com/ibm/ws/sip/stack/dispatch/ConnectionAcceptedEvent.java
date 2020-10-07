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
package com.ibm.ws.sip.stack.dispatch;

import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;

/**
 * event triggered when a new connection is accepted from some peer.
 * 
 * @author ran
 */
class ConnectionAcceptedEvent implements Event
{
	/** listener that accepted the new connection */
	private SIPListenningConnection m_listener;

	/** new connection that is accepted */
	private SIPConnection m_connection;

	/**
	 * constructor
	 * @param listener listener that accepted the new connection
	 * @param connection connection that is accepted
	 */
	ConnectionAcceptedEvent(
		SIPListenningConnection listener,
		SIPConnection connection)
	{
		m_listener = listener;
		m_connection = connection;
	}
	
	/**
	 * called from the dispatch thread to execute this event
	 */
	public void onExecute() {
		TransportCommLayerMgr.instance().onConnectionCreated(m_listener, m_connection);
	}
}
