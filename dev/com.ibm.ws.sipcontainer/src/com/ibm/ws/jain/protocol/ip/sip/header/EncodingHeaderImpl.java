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
import jain.protocol.ip.sip.header.EncodingHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Encoding header implementation.
 *
 * @see AcceptEncodingHeaderImpl
 * @see ContentEncodingHeaderImpl
 *
 * @author Assaf Azaria, Mar 2003.
 */
public abstract class EncodingHeaderImpl extends HeaderImpl
    implements EncodingHeader
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 9101267871697898922L;

    /**
     * The encoding. 
     */
    private String m_encoding = "";

    /**
	 * initialize a new EncodingHeaderImpl object with no value.
     */
    public EncodingHeaderImpl() {
        super();
    }
    
	//
    // Methods.
    // 
    
    /**
     * Sets the encoding of EncodingHeader
     * @param <var>encoding</var> encoding
     * @throws IllegalArgumentException if encoding is null
     * @throws SipParseException if encoding is not accepted by implementation
     */
    public void setEncoding(String encoding)
                 throws IllegalArgumentException,SipParseException
    {
        if (encoding == null)
        {
            throw new IllegalArgumentException("EncodingHeader:Null encoding");
        }

        m_encoding = encoding;
    }

    /**
     * Gets the encoding of EncodingHeader
     * @return encoding of EncodingHeader
     */
    public String getEncoding()
    {
        return m_encoding;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_encoding = parser.toString();
	}

    /**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer buf)
    {
		buf.append(m_encoding); 
    }
    
	/**
     * compares two parsed header values
     * @param other the other header to compare with
     * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#valueEquals(com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof EncodingHeaderImpl)) {
			return false;
		}
		
		EncodingHeaderImpl o = (EncodingHeaderImpl)other;
		if (m_encoding == null || m_encoding.length() == 0) {
			if (o.m_encoding == null || o.m_encoding.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_encoding == null || o.m_encoding.length() == 0) {
				return false;
			}
			else {
				return m_encoding.equals(o.m_encoding);
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
}
