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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The Content language header. 
 * 
 * @author Assaf Azaria, May 2003.
 */
public class ContentLanguageHeaderImpl extends HeaderImpl
    implements ContentLanguageHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 8109638944150595620L;

	//
	// Members.
	//
	
    /**
	 * The language tag.
	 */
    protected String m_langTag;

    /**
     * Constructor.
     * @throws SipParseException
     */
    public ContentLanguageHeaderImpl() {
        super();
    }
	
	//
	// Operations.
	// 
	
    /**
     * Sets language tag of ContentLaguageHeader
     * @param <var>languageTag</var> language tag
     * @throws IllegalArgumentException if languageTag is null
     */
    public void setLanguageTag(String languageTag)
        throws IllegalArgumentException
    {
        if (languageTag == null)
        {
            throw new IllegalArgumentException("Null language tag");
        }

        m_langTag = languageTag;
    }

    /**
     * Gets langauge tag of ContentLaguageHeader
     * @return language tag of ContentLaguageHeader
     */
    public String getLanguageTag()
    {
        return m_langTag;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_langTag = parser.toString();
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_langTag);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof ContentLanguageHeaderImpl)) {
			return false;
		}
		ContentLanguageHeaderImpl o = (ContentLanguageHeaderImpl)other;
		
		if (m_langTag == null || m_langTag.length() == 0) {
			if (o.m_langTag == null || o.m_langTag.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_langTag == null || o.m_langTag.length() == 0) {
				return false;
			}
			else {
				return m_langTag.equals(o.m_langTag);
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
}
