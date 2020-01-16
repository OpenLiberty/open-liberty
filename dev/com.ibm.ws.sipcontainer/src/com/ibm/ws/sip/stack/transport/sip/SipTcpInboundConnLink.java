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

/**
 * represents an inbound tcp connection
 * 
 * @author ran
 */
public class SipTcpInboundConnLink extends SipInboundConnLink
{
	/**
	 * constructor
	 * @param channel channel that created this connection
	 */
	public SipTcpInboundConnLink(SipInboundChannel channel) {
		super(channel);
	}

	// ----------------------------
	// SIPConnection implementation
	// ----------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getTransport()
	 */
	public String getTransport() {
		return "tcp";
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isReliable()
	 */
	public boolean isReliable() {
		return true;
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
		return -1;
	}
	
}
