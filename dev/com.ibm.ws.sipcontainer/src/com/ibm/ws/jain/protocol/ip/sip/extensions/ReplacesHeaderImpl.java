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
import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Implementation of the Replaces header
 * 
 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader
 * @author ran
 */
public class ReplacesHeaderImpl extends ParametersHeaderImpl implements ReplacesHeader
{
	/** unique serialization identifier */
	private static final long serialVersionUID = 5469509062223911453L;

	/**
	 * constructor
	 */
	public ReplacesHeaderImpl() {
		super();
	}

	/** the Call-ID part of the Replaces header */
	private String m_callId;

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#setCallId(java.lang.String)
	 */
	public void setCallId(String callId) {
		m_callId = callId;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#getCallId()
	 */
	public String getCallId() {
		return m_callId;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#setToTag(java.lang.String)
	 */
	public void setToTag(String toTag) throws SipParseException {
		setParameter("to-tag", toTag);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#getToTag()
	 */
	public String getToTag() {
		return getParameter("to-tag");
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#setFromTag(java.lang.String)
	 */
	public void setFromTag(String fromTag) throws SipParseException {
		setParameter("from-tag", fromTag);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#getFromTag()
	 */
	public String getFromTag() {
		return getParameter("from-tag");
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#setEarly(boolean)
	 */
	public void setEarly(boolean early) throws SipParseException {
		setParameter("early-only", "");
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader#isEarly()
	 */
	public boolean isEarly() {
		return getParameter("early-only") != null;
	}

	/**
	 * parses the value of this header.
	 * Replaces = "Replaces" HCOLON callid *(SEMI replaces-param)
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// call-id
		m_callId = parser.nextToken(SEMICOLON);

		// call-id validation
		if (!Coder.isCallId(m_callId)) {
			throw new SipParseException("Bad Call-ID in Replaces header value", m_callId);
		}

        // parameters
        super.parseValue(parser);

		// A Replaces header field MUST contain exactly one to-tag and exactly
        // one from-tag, as they are required for unique dialog matching
		String toTag = getToTag();
		if (toTag == null || toTag.length() == 0) {
			throw new SipParseException("Missing To tag in Replaces header value");
		}
		String fromTag = getFromTag();
		if (fromTag == null || fromTag.length() == 0) {
			throw new SipParseException("Missing From tag in Replaces header value");
		}
	}
    
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// call-id
		buffer.append(m_callId);
		
		// parameters
		super.encodeValue(buffer);
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		ReplacesHeaderImpl cloned = (ReplacesHeaderImpl)super.clone();
		cloned.m_callId = m_callId;
		return cloned;
	}

	/**
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
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
	 * from rfc 3891-3:
	 * If more than one Replaces header field is present in an INVITE [...]
	 * the UAS MUST reject the request with a 400 Bad Request response.
	 */
	public boolean isNested() {
		return false;
	}
}
