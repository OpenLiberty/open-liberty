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
import jain.protocol.ip.sip.header.UserAgentHeader;

import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* User agent header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class UserAgentHeaderImpl extends ProductHeaderImpl
    implements UserAgentHeader
{

    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -169525267386912950L;

    /** 
     * Default constructor
     * @throws SipParseException
     */
    public UserAgentHeaderImpl() {
        super();
    }

	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		encodeProducts(buf);
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
