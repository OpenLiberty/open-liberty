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

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

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
public class InReplyToHeaderImpl extends HeaderImpl 
	implements InReplyToHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -1018705744143567551L;

	//
	// Members.
	//
	
	/**
	 * The call id.
	 */
	private String m_callId;
	
	/** 
	 * Constructor.
	 */
	public InReplyToHeaderImpl()
	{
		super();
	}
	

    /**
	 * Gets Call-Id of InReplyToHeader
	 * @return Call-Id of InReplyToHeader
	 */
	public String getCallId()
	{
		return m_callId;
	}

	/**
	 * Sets Call-Id of InReplyToHeader
	 * @param callId String to set
	 * @throws IllegalArgumentException if callId is null
	 */
	public void setCallId(String callId)
		throws IllegalArgumentException
	{
		if (callId == null)
		{
			throw new IllegalArgumentException("InReplyToHeader: null callId"); 
		} 
        
		m_callId = callId;
	}
    
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_callId);
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_callId = parser.toString();
	}

	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof InReplyToHeaderImpl)) {
			return false;
		}
		InReplyToHeaderImpl o = (InReplyToHeaderImpl)other;
		
		if (m_callId == null || m_callId.length() == 0) {
			if (o.m_callId == null || o.m_callId.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_callId == null || o.m_callId.length() == 0) {
				return false;
			}
			else {
				return m_callId.equals(o.m_callId);
			}
		}
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
