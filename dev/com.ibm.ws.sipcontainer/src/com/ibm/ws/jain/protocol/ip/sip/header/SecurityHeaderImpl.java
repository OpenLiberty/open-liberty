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
import jain.protocol.ip.sip.header.AuthorizationHeader;
import jain.protocol.ip.sip.header.EncryptionHeader;
import jain.protocol.ip.sip.header.ProxyAuthenticateHeader;
import jain.protocol.ip.sip.header.ProxyAuthorizationHeader;
import jain.protocol.ip.sip.header.ResponseKeyHeader;
import jain.protocol.ip.sip.header.SecurityHeader;
import jain.protocol.ip.sip.header.WWWAuthenticateHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Security header implementation.
 * 
 * @see AuthorizationHeader
 * @see EncryptionHeader
 * @see ProxyAuthenticateHeader
 * @see ProxyAuthorizationHeader
 * @see ResponseKeyHeader
 * @see WWWAuthenticateHeader
 * @author Assaf Azaria, Mar 2003.
 */
public abstract class SecurityHeaderImpl extends ParametersHeaderImpl 
	implements SecurityHeader

{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -7167993611197627052L;

	/**
	 * The scheme string.
	 */
	private String m_scheme;
	
	/**
	 * @throws SipParseException
     */
    SecurityHeaderImpl() {
        super();
    }
    
	/**
	 * Method used to get the scheme
	 * @return the scheme
	 */
	public String getScheme()
	{
		return m_scheme;
	}

	/**
	 * Method used to set the scheme
	 * @param String the scheme
	 * @throws IllegalArgumentException if scheme is null
	 * @throws SipParseException if scheme is not accepted by implementation
	 */
	public void setScheme(String scheme)
				 throws IllegalArgumentException,SipParseException
	{
		if (scheme == null)
		{
			throw new IllegalArgumentException("SecurityHeader: null scheme");
		}
		
		m_scheme = scheme;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// parse scheme
		String scheme = parser.sipToken();
		setScheme(scheme.trim());

		// parse parameters
		super.parseValue(parser);
	}
	
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// encode scheme
		buffer.append(m_scheme); 

		// encode parameters
		super.encodeValue(buffer);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!super.valueEquals(other)) {
			return false;
		}
		if (!(other instanceof SecurityHeaderImpl)) {
			return false;
		}
		SecurityHeaderImpl o = (SecurityHeaderImpl)other;
		
		if (m_scheme == null || m_scheme.length() == 0) {
			if (o.m_scheme == null || o.m_scheme.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_scheme == null || o.m_scheme.length() == 0) {
				return false;
			}
			else {
				return m_scheme.equals(o.m_scheme);
			}
		}
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
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SP;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return COMMA;
	}
	
    /**
     * @return true if parameters should be escaped
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#escapeParameters()
     */
    protected boolean escapeParameters() {
    	return false;
    }
}
