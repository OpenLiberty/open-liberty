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
 * The Error-Info header field provides a pointer to additional information 
 * about the error status response. 
 * A UAC MAY treat a SIP or SIPS URI in an Error-Info header field as if it 
 * were a Contact in a redirect and generate a new INVITE, resulting in a 
 * recorded announcement session being established. A non-SIP URI MAY be 
 * rendered to the user. 
 * Examples: 
 *
 *    SIP/2.0 404 The number you have dialed is not in service
 *    Error-Info: <sip:not-in-service-recording@atlanta.com>
 * @author Assaf Azaria
 */
public class ErrorInfoHeaderImpl extends InfoHeaderImpl 
	implements ErrorInfoHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 1183748289723100917L;

	//
	// Constructor.
	//
	/**
	 *  Construct a new ErrorInfoHeader object.
	 * @throws SipParseException
	 */
	public ErrorInfoHeaderImpl() {
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
