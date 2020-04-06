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

import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

public class ReasonHeaderImpl extends ParametersHeaderImpl implements ReasonHeader {

	/** unique serialization identifier */
	private static final long serialVersionUID = -2216327357703903278L;
	
	/**
	 * Protocl that is used in the ReasonHeaser
	 */
	private String _protocol;
	
	/**
	 * constructor
	 */
	public ReasonHeaderImpl() {
		super();
	}
	
	/**
	 * Crot
	 * @param protocol
	 * @param reasonCode
	 * @param reasonText
	 * @throws SipParseException 
	 */
	public ReasonHeaderImpl(String protocol, int reasonCode, String reasonText) throws SipParseException {
		
		_protocol = protocol;
		setCause(Integer.toString(reasonCode));
		setText(reasonText);
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#getCause()
	 */
	public String getCause() {
		return getParameter("cause");
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#getProtocol()
	 */
	public String getProtocol() {
		return _protocol;
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#getText()
	 */
	public String getText() {
		return getParameter("text");
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#setCause(java.lang.String)
	 */
	public void setCause(String cause)  throws SipParseException{
		setParameter("cause", cause);
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#setProtocol(java.lang.String)
	 */
	public void setProtocol(String protocol){
		_protocol = protocol;
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeader#setText(java.lang.String)
	 */
	public void setText(String text) throws SipParseException{
		setParameter("text", text, true);
	}

	/**
	 *  @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName()
	 */
	public String getName() {
		return name;
	}



	/**
	 * parses the value of this header.
	 * Join = "Join" HCOLON callid *(SEMI join-param)
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// protocol
		_protocol = parser.nextToken(SEMICOLON);
		   
		// parameters
		super.parseValue(parser);

		// "A Join header MUST contain exactly one to-tag and exactly one from-
		// tag, as they are required for unique dialog matching"
		String cause = getCause();
		if (cause == null || cause.length() == 0) {
			throw new SipParseException("Missing To tag in Reason header value");
		}
		String text = getText();
		if (text == null || text.length() == 0) {
			throw new SipParseException("Missing From tag in Reason header value");
		}
	}
	
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// call-id
		buffer.append(_protocol);
		
		// parameters
		super.encodeValue(buffer);
	}
	
	/**
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		ReasonHeaderImpl cloned = (ReasonHeaderImpl)super.clone();
		cloned._protocol = _protocol;
		return cloned;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#escapeParameters()
	 */
	protected boolean escapeParameters() {
    	return true;
	}

	/**
	 * from rfc 3911-4:
	 * If more than one Join header field is present in an INVITE [...]
	 * the UAS MUST reject the request with a 400 Bad Request response.
	 */
	public boolean isNested() {
		return false;
	}
}
