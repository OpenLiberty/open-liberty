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
import jain.protocol.ip.sip.header.ExpiresHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Expires header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class ExpiresHeaderImpl extends DateHeaderImpl 
implements ExpiresHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 1733992807032026409L;

	/**
	 * The expiration date in delta seconds.
	 */
	private long m_deltaSeconds = -1;
	
    /**
     * @throws SipParseException 
     */
    public ExpiresHeaderImpl() {
        super();
    }
    
	/**
    * Gets value of ExpiresHeader as delta-seconds
    * (Returns -1 if expires value is not in delta-second format)
    * @return value of ExpiresHeader as delta-seconds
    */
    public long getDeltaSeconds()
    {
    	return m_deltaSeconds;
    }

    /**
    * Gets boolean value to indicate if expiry value of ExpiresHeader
    * is in date format
    * @return boolean value to indicate if expiry value of ExpiresHeader
    * is in date format
    */
    public boolean isDate()
    {
		return (getDate() != null);
    }
    
	/**
    * Sets expires of ExpiresHeader as delta-seconds
    * @param deltaSeconds long to set
    * @throws SipParseException if deltaSeconds is not accepted 
    * by implementation
    */
    public void setDeltaSeconds(long deltaSeconds) throws SipParseException
    {
		m_deltaSeconds = deltaSeconds;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		int mark = parser.mark();
		try {
			// try to parse it as delta (in most cases that's what we get)
			m_deltaSeconds = parser.longNumber();
		}
		catch (SipParseException e) {
			// guess it's an absolute time
			parser.rewind(mark);
			super.parseValue(parser);
		}
	}

    /**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		if (isDate())
		{
			super.encodeValue(buf);
		}
		else
		{
			buf.append(m_deltaSeconds);
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
		if (!(other instanceof ExpiresHeaderImpl)) {
			return false;
		}
		ExpiresHeaderImpl o = (ExpiresHeaderImpl)other;
		return m_deltaSeconds == o.m_deltaSeconds;
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return ExpiresHeader.name;
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
}
