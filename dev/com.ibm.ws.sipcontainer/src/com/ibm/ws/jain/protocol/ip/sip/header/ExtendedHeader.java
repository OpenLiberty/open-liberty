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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.sip.parser.CharArray;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * a header that's not part of the standard
 * 
 * @author ran
 */
public class ExtendedHeader extends HeaderImpl
{
    /**
     * the proprietary header name
     */
    private final String m_name;
    
    /**
     * the header value, which is never parsed
     */
    private String m_value;

    /**
     * constructor
     * 
     * @param name The header's name.
     */
	public ExtendedHeader(String name)
		throws IllegalArgumentException
	{
		this(name, null);
	}
	
    /**
     * constructor
     * 
     * @param name The header's name.
     * @param value char array containing header's value.
     */
    public ExtendedHeader(String name, CharArray value)
        throws IllegalArgumentException
    {
		super(value);
        if (name == null) {
            throw new IllegalArgumentException("ExtendedHeader: null name");
        }
        m_name = name;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_value = parser.toString();
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf) {
   		buf.append(m_value);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof ExtendedHeader)) {
			return false;
		}
		ExtendedHeader o = (ExtendedHeader)other;
		
		if (m_value == null || m_value.length() == 0) {
			if (o.m_value == null || o.m_value.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_value == null || o.m_value.length() == 0) {
				return false;
			}
			else {
				return m_value.equals(o.m_value);
			}
		}
	}
	
	/**
	 * @return the name of this header 
     * @see jain.protocol.ip.sip.header.Header#getName()
     */
    public String getName() {
		return m_name;
	}

	/**
	 * determines whether or not this header can have nested values according to RFC
	 */
	public boolean isNested() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders && getName().equals(SipConstants.REFER_TO)){
			return String.valueOf(SipConstants.REFER_TO_SHORT);
		}
		return getName();
	}
	
	/**
	 * a unique method to "ExtendedHeader" only.
	 * Return true if the header is defined to be parsed as containing "comma.separated" values
	 */
	public boolean isInCommaSeparated() {
		HeaderSeparator hs = HeaderSeparator.instance();
		boolean nested = hs.isCommaSeparated(m_name, true);
		return nested;
	}
	
	
}
