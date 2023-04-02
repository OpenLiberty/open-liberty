/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.header.AcceptEncodingHeader;

public class AcceptEncodingHeaderImpl extends EncodingHeaderImpl
    implements AcceptEncodingHeader
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 5111990055120825242L;

    /**
	 * initialize a new AcceptEncodingHeaderImpl object with no value.
     */
    public AcceptEncodingHeaderImpl() {
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
