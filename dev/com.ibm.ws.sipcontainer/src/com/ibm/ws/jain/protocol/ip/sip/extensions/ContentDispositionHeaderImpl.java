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

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The Content-Disposition header field describes how the message body or, for
 * multipart messages, a message body part is to be interpreted by the UAC or 
 * UAS. This SIP header field extends the MIME Content- Type (RFC 2183 [18]). 
 * Several new "disposition-types" of the Content-Disposition header are 
 * defined by SIP. The value "session" indicates that the body part describes 
 * a session, for either calls or early (pre-call) media. The value "render" 
 * indicates that the body part should be displayed or otherwise rendered to 
 * the user. Note that the value "render" is used rather than "inline" to 
 * avoid the connotation that the MIME body is displayed as a part of the 
 * rendering of the entire message (since the MIME bodies of SIP messages 
 * oftentimes are not displayed to users). For backward-compatibility, if 
 * the Content-Disposition header field is missing, the server SHOULD assume 
 * bodies of Content-Type application/sdp are the disposition "session", 
 * while other content types are "render". 
 * The disposition type "icon" indicates that the body part contains an image 
 * suitable as an iconic representation of the caller or callee that could be 
 * rendered informationally by a user agent when a message has been received, 
 * or persistently while a dialog takes place. The value "alert" indicates that
 *  the body part contains information, such as an audio clip, that should be 
 * rendered by the user agent in an attempt to alert the user to the receipt of
 *  a request, generally a request that initiates a dialog; this alerting body
 *  could for example be rendered as a ring tone for a phone call after a 180 
 * Ringing provisional response has been sent. 
 *
 * Any MIME body with a "disposition-type" that renders content to the user should only 
 * be processed when a message has been properly authenticated. 
 * 
 * The handling parameter, handling-param, describes how the UAS should react 
 * if it receives a message body whose content type or disposition type it does 
 * not understand. The parameter has defined values of "optional" and 
 * "required". If the handling parameter is missing, the value "required" 
 * SHOULD be assumed. The handling parameter is described in RFC 3204 [19]. 
 *
 * If this header field is missing, the MIME type determines the default 
 * content disposition. If there is none, "render" is assumed. 
 *
 * Example: 
 *
 *    Content-Disposition: session
 *
 * @author Assaf Azaria
 */
public class ContentDispositionHeaderImpl extends ParametersHeaderImpl
	implements ContentDispositionHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 4228648805461504221L;

	//
	// Members.
	//
	/**
	 * The disposition type.
	 */
	protected String m_type;
	
	//
	// Constructor.
	//
	
	/**
	 * Construct a new ContentDispositionHeader object.
	 */
	public ContentDispositionHeaderImpl()
	{
		super();
	}
	
	
	//
	// Operations.
	// 
	
	/**
	 * Set the disposition type.
	 * 
	 * @param type the disposition type.
	 * @throws IllegalArgumentException if type is null or invalid.
	 */
	public void setDispositionType(String type) throws IllegalArgumentException
	{
		if (type == null || type.equals(""))
		{
			throw new IllegalArgumentException("Disp: null or empty type");
		}
		
		m_type = type;
	}

	/**
	 * Get the disposition type.
	 */
	public String getDispositionType()
	{
		return m_type;
	}

	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		String type = parser.nextToken(SEMICOLON);
		setDispositionType(type);
		
		super.parseValue(parser);
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_type);
		
		// Other params (if exist).
		super.encodeValue(ret);
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
		if (!(other instanceof ContentDispositionHeaderImpl)) {
			return false;
		}
		ContentDispositionHeaderImpl o = (ContentDispositionHeaderImpl)other;
		
		if (m_type == null || m_type.length() == 0) {
			if (o.m_type == null || o.m_type.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_type == null || o.m_type.length() == 0) {
				return false;
			}
			else {
				return m_type.equals(o.m_type);
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
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return false;
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}
}
