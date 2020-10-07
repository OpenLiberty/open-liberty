/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.WWWAuthenticateHeader;

/**
* WWW authenticate header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class WWWAuthenticateHeaderImpl extends SecurityHeaderImpl
    implements WWWAuthenticateHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 8772326498828278562L;

	/**
	 * @throws SipParseException 
     */
    public WWWAuthenticateHeaderImpl() {
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
		return false;
	}
}
