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

/**
 * event triggered when a connection gets closed
 * (reset by peer)
 * 
 * @author ran
 */
class ConnectionClosedEvent implements Event
{
	/** connection that was closed */
	private SIPConnection m_connection;

	/**
	 * constructor
	 * @param connection connection that was closed
	 */
	ConnectionClosedEvent(SIPConnection connection) {
		m_connection = connection;
	}
	
	/**
	 * called from the dispatch thread to execute this event
	 */
	public void onExecute() {
		TransportCommLayerMgr.instance().onConnectionClosed(m_connection);
	}
}
