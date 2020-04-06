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
import jain.protocol.ip.sip.header.AcceptHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Content type header implementation.
* 
* @see AcceptHeader
* @author Assaf Azaria, Mar 2003.
*/
public class ContentTypeHeaderImpl extends ParametersHeaderImpl
    implements ContentTypeHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 354547676897011474L;

	//
	// Members. 
	//
	
	/**
	 * Content type.
	 */
	private String m_type;
	
	/**
	 * Content sub type;
	 */
	private String m_subType;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public ContentTypeHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public ContentTypeHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }
    
    //
    // Methods.
    //
    
    /**
     * Gets media type of ContentTypeHeader
     * @return media type of ContentTypeHeader
     */
    public String getContentType()
    {
    	return m_type;
    }

    /**
     * Gets media sub-type of ContentTypeHeader
     * @return media sub-type of ContentTypeHeader
     */
    public String getContentSubType()
    {
    	return m_subType;
    }

    /**
     * Sets value of media subtype in ContentTypeHeader
     * @param contentSubType String to set
     * @throws IllegalArgumentException if sub-type is null
     * @throws SipParseException if contentSubType is not 
     * accepted by implementation
     */
    public void setContentSubType(String contentSubType)
        throws IllegalArgumentException, SipParseException
    {
        if (contentSubType == null)
        {
        	throw new IllegalArgumentException("ConTypeHeader: null arg"); 
    	} 
        
        m_subType = contentSubType;
    }

    /**
     * Sets value of media type in ContentTypeHeader
     * @param ContentType String to set
     * @throws IllegalArgumentException if type is null
     * @throws SipParseException if contentType is not accepted by implementation
     */
    public void setContentType(String contentType)
        throws IllegalArgumentException, SipParseException
    {
		if (contentType == null)
		{
			throw new IllegalArgumentException("ConTypeHeader: null arg"); 
		} 
        
		m_type = contentType;
	}
    
    /**
     * @return true if type is set, false if both type and sub-type are missing
     */
    public boolean hasType() {
    	return m_type != null;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// parse type and sub-type
		// sub classes might parse additional properties here
		parseType(parser);
		
		// parse parameters
		super.parseValue(parser);
	}
	
	/**
	 * parses the content type and sub-type
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseType(SipParser parser) throws SipParseException {
        String type = parser.nextToken(SLASH);
        setContentType(type);
        parser.match(SLASH);

        String subType = parser.nextToken(SEMICOLON);
        setContentSubType(subType);
	}
    
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// encode type and sub-type.
		// sub classes might encode additional properties here
		encodeType(buffer);

		// encode parameters
		super.encodeValue(buffer);
	}
	
	/**
	 * encodes the content type and sub-type
	 */
	protected void encodeType(CharsBuffer buffer) {
		if (hasType()) {
			buffer.append(m_type);
			buffer.append(SLASH);
			buffer.append(m_subType);
		}
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
		if (!(other instanceof ContentTypeHeaderImpl)) {
			return false;
		}
		ContentTypeHeaderImpl o = (ContentTypeHeaderImpl)other;

		if (m_type == null || m_type.length() == 0) {
			if (o.m_type != null && o.m_type.length() > 0) {
				return false;
			}
		}
		else {
			if (o.m_type == null || o.m_type.length() == 0) {
				return false;
			}
		}
		
		if (m_subType == null || m_subType.length() == 0) {
			if (o.m_subType == null || o.m_subType.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_subType == null || o.m_subType.length() == 0) {
				return false;
			}
			else {
				return m_subType.equals(o.m_subType);
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
    
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.CONTENT_TYPE_SHORT);
		}
		return getName();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactFormSupported()
	 */
	public boolean isCompactFormSupported() {
		return true;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactForm()
	 */
	public boolean isCompactForm() {
		return m_compactForm;
	}
}
