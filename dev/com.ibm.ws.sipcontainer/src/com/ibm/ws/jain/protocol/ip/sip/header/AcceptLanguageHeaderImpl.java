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
import jain.protocol.ip.sip.header.AcceptLanguageHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Accept language header implementation.
* 
* @Author Assaf Azaria, Mar 2003.
*/
public class AcceptLanguageHeaderImpl extends ParametersHeaderImpl
    implements AcceptLanguageHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -7865142737583561937L;

	//
	// Members. 
	//
	
	/**
	 * The q value.
	 */
	private float m_qValue = -1.0f;
	
	/**
	 * The language range.
	 */
	private String m_langRange;
	
    /**
     * @throws SipParseException
     */
    public AcceptLanguageHeaderImpl() {
        super();
    }
    

    /**
	 * Sets q-value for media-range in AcceptHeader
	 * Q-values allow the user to indicate the relative degree of
	 * preference for that media-range, using the qvalue scale from 0 to 1.
	 * (If no q-value is present, the media-range should be treated as having a q-value of 1.)
	 * @param <var>qValue</var> q-value
	 * @throws SipParseException if qValue is not accepted by implementation
	 */
	public void setQValue(float qValue)
				 throws SipParseException
    {
		if (qValue < 0.0)
		{
			throw new SipParseException("AcceptLangHeader: Q Value < 0", ""); 
		} 
		if (qValue > 1.0)
		{
			throw new SipParseException("AcceptLangHeader: Q value > 1.0", ""); 
		} 
		
		m_qValue = qValue;
    }

	/**
	 * Gets q-value of media-range in AcceptHeader
	 * (Returns negative float if no q-value exists)
	 * @return q-value of media-range
	 */
	public float getQValue()
	{
		return m_qValue;
	}

	/**
	 * Gets boolean value to indicate if AcceptHeader
	 * has q-value
	 * @return boolean value to indicate if AcceptHeader
	 * has q-value
	 */
	public boolean hasQValue()
	{
		return m_qValue != -1.0f;
	}

	/**
	 * Removes q-value of media-range in AcceptHeader (if it exists)
	 */
	public void removeQValue()
	{
		m_qValue = -1.0f;
	}

	/**
     * Sets the language-range of AcceptLanguageHeader
     * @param <var>languageRange</var> language-range of AcceptLanguageHeader
     * @throws IllegalArgumentException if languageRange is null
     * @throws SipParseException if languageRange is not accepted by implementation
     */
    public void setLanguageRange(String languageRange) 
    throws IllegalArgumentException, SipParseException
    {
		if (languageRange == null)
        {
        	throw new IllegalArgumentException("AcceptLang: null arg");
        } 
        
        m_langRange = languageRange;
    }

    /**
     * Gets the language-range of AcceptLanguageHeader
     * @return language-range of AcceptLanguageHeader
     */
    public String getLanguageRange()
    {
    	return m_langRange;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// parse language range
        String language = parser.nextToken(SEMICOLON);
        setLanguageRange(language);

        // parse q-value
        if (parser.LA(1) == SEMICOLON) {
        	parser.match(SEMICOLON);
        	parser.lws();
        	parser.match('q');
        	parser.match(EQUALS);
            String qValue = parser.nextToken(SEMICOLON);
            setQValue(Float.parseFloat(qValue));
        }

        // parse parameters
        super.parseValue(parser);
	}
	
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// encode language range
		if (m_langRange != null) {
			buffer.append(m_langRange);
		}

		// encode q-value
		if (hasQValue()) {
			buffer.append(SEMICOLON);
			buffer.append("q");
			buffer.append(EQUALS);
			buffer.append(m_qValue);
		}
		
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
		if (!(other instanceof AcceptLanguageHeaderImpl)) {
			return false;
		}
		AcceptLanguageHeaderImpl o = (AcceptLanguageHeaderImpl)other;
		
		if (m_qValue != o.m_qValue) {
			return false;
		}
		if (m_langRange == null || m_langRange.length() == 0) {
			if (o.m_langRange == null || o.m_langRange.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_langRange == null || o.m_langRange.length() == 0) {
				return false;
			}
			else {
				return m_langRange.equals(o.m_langRange);
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
