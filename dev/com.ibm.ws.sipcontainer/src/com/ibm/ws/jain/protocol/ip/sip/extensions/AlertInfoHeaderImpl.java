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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.SipParseException;

/**
 * This was not included in Jain. 
 * 
 * When present in an INVITE request, the Alert-Info header field specifies an 
 * alternative ring tone to the UAS. When present in a 180 (Ringing) response, 
 * the Alert-Info header field specifies an alternative ringback tone to the 
 * UAC. A typical usage is for a proxy to insert this header field to provide 
 * a distinctive ring feature. 
 * 
 * Example: 
 *
 *    Alert-Info: <http://www.example.com/sounds/moo.wav>
 *
 * @author Assaf Azaria
 */
public class AlertInfoHeaderImpl extends InfoHeaderImpl
	implements AlertInfoHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -5666028043591008362L;

	//
	// Constructor.
	//
	
	/**
	 *  Construct a new AlertInfoHeader object.
	 * @throws SipParseException
	 */
	public AlertInfoHeaderImpl() {
		super();
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		// This is required in case someone will inherit 
		// from this class.
		return super.clone(); 
	}

	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return true;
	}
}
