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
public interface CallInfoHeader extends InfoHeader
{
	/**
	 * Name of Call Info Header.
	 */
	public static final String name = "Call-Info";
	
	/**
	 * Icon value of purpose parameter.
	 */
	public static final String ICON="icon";
	
	/**
	 * Card value of purpose parameter.
	 */
	public static final String CARD="card";                           
	
	/**
	 * Info value of purpose parameter.
	 */
	public static final String INFO="info"; 
	
	//
	// Operations.
	//
	
	/**
	 * Set the purpose parameter.
	 * @throws IllegalArgumentException if purpose is null or invalid.
	 * (Purpose can be one of CARD, INFO, ICON).
	 */
	public void setPurpose(String purpose) throws IllegalArgumentException, 
		SipParseException;
	
	/**
	 * Get the purpose parameter.
	 * 
	 * @author Assaf Azaria
	 */
	public String getPurpose();
}
