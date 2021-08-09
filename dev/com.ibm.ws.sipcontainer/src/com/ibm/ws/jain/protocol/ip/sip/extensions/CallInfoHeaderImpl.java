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
 * The Call-Info header field provides additional information about the caller 
 * or callee, depending on whether it is found in a request or response. The 
 * purpose of the URI is described by the "purpose" parameter. The "icon" 
 * parameter designates an image suitable as an iconic representation of the 
 * caller or callee. The "info" parameter describes the caller or callee in 
 * general, for example, through a web page. The "card" parameter provides 
 * a business card, for example, in vCard [36] or LDIF [37] formats. Additional
 * tokens can be registered using IANA and the procedures in Section 27.
 *  
 * Example: 
 *
 *  Call-Info: <http://wwww.example.com/alice/photo.jpg> ;purpose=icon,
 *    <http://www.example.com/alice/> ;purpose=info
 *
 * @author Assaf Azaria
 */
public class CallInfoHeaderImpl extends InfoHeaderImpl
	implements CallInfoHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 1959733034612843938L;

	//
	// Constants.
	//
	public static final String PURPOSE="purpose";                           
	
	
	//
	// Members. 
	// 
	/**
	 * The purpose parameter.
	 */
	protected String m_purpose;
	
	//
	// Constructor.
	//
	
	/**
	 *  Construct a new AlertInfoHeader object.
	 */
	public CallInfoHeaderImpl()
	{
		super();
	}
	
	
	//
	// Operations.
	//
	
	/**
	 * Set the purpose parameter.
	 * 
	 * @throws IllegalArgumentException if purpose is null
	 */
	public void setPurpose(String purpose) throws IllegalArgumentException, 
		SipParseException
	{
		if (purpose == null)
		{
			throw new IllegalArgumentException("CallInfo: null purpose"); 
		}
		
		setParameter(PURPOSE, purpose);
	}

	/**
	 * Get the purpose parameter.
	 * 
	 * @author Assaf Azaria
	 */
	public String getPurpose()
	{
		return getParameter(PURPOSE);
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
