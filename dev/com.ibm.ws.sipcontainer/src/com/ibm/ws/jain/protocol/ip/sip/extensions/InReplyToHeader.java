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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.Header;

/**
 * The In-Reply-To header field enumerates the Call-IDs that this call 
 * references or returns. These Call-IDs may have been cached by the client 
 * then included in this header field in a return call. 
 *
 * This allows automatic call distribution systems to route return calls to 
 * the originator of the first call.  This also allows callees to filter calls,
 * so that only return calls for calls they originated will be accepted.  
 * This field is not a substitute for request authentication.
 * 
 * Example: 
 *    In-Reply-To: 70710@saturn.bell-tel.com, 17320@saturn.bell-tel.com
 *
 * @author Assaf Azaria, May 2003.
 */
public interface InReplyToHeader extends Header
{
	/**
	 * Name of InReplyToHeader
	 */
	public final static String name = "In-Reply-To";
	
	/**
	 * Sets Call-Id of InReplyToHeader
	 * 
	 * @param <var>callId</var> Call-Id
	 * @throws IllegalArgumentException if callId is null
	 */
	public void setCallId(String callId) throws IllegalArgumentException;

	/**
	 * Gets Call-Id of InReplyToHeader
	 * @return Call-Id of InReplyToHeader
	 */
	public String getCallId();
}
