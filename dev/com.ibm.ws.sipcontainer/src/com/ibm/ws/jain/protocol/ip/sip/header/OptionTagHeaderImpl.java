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
import jain.protocol.ip.sip.header.OptionTagHeader;
import jain.protocol.ip.sip.header.ProxyRequireHeader;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.header.UnsupportedHeader;

import com.ibm.ws.sip.parser.SipParser;

/**
 * Option tag header implementation.
 * 
 * @author Assaf Azaria, Mar 2003.
 * 
 * @see ProxyRequireHeader
 * @see RequireHeader
 * @see UnsupportedHeader
 *
 */
public abstract class OptionTagHeaderImpl extends HeaderImpl 
	implements OptionTagHeader
{

    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 5021865955637430322L;

    /**
     * The option tag.
     */
    protected String m_optionTag; 
    
    /**
     * @throws SipParseException
     */
    protected OptionTagHeaderImpl() {
        super();
    }
    
    /**
     * Sets option tag of OptionTagHeader
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public void setOptionTag(String optionTag)
                 throws IllegalArgumentException, SipParseException
	{
		if (optionTag == null)
		{
			throw new IllegalArgumentException("Null option tag");
		}
		
		m_optionTag = optionTag;
	}

	/**
	 * Gets option tag of OptionTagHeader
	 * @return option tag of OptionTagHeader
	 */
	public String getOptionTag()
	{
		return m_optionTag;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_optionTag = parser.toString();
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof OptionTagHeaderImpl)) {
			return false;
		}
		OptionTagHeaderImpl o = (OptionTagHeaderImpl)other;
		if (m_optionTag == null || m_optionTag.length() == 0) {
			if (o.m_optionTag == null || o.m_optionTag.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_optionTag == null || o.m_optionTag.length() == 0) {
				return false;
			}
			else {
				return m_optionTag.equals(o.m_optionTag);
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
