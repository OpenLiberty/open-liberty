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
import jain.protocol.ip.sip.header.ContentLengthHeader;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Content length header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class ContentLengthHeaderImpl extends HeaderImpl
    implements ContentLengthHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 3391206701910414069L;

	/**
	 * The content length;
	 */
	private int m_length = 0;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public ContentLengthHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public ContentLengthHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }
    
	/**
     * Set content-length of ContentLengthHeader
     * @param contentlength int to set
     * @throws SipParseException if contentLength is not accepted 
     * by implementation
     */
    public void setContentLength(int contentlength) throws SipParseException
    {
    	m_length = contentlength;
    }

    /**
     * Gets content-length of ContentLengthHeader
     * @return content-length of ContentLengthHeader
     */
    public int getContentLength()
    {
        return m_length;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_length = parser.number();
		// todo deal with invalid numbers
	}
    
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_length);    
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof ContentLengthHeaderImpl)) {
			return false;
		}
		ContentLengthHeaderImpl o = (ContentLengthHeaderImpl)other;
		return m_length == o.m_length;
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
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.CONTENT_LENGTH_SHORT);
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
