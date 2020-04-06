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
import jain.protocol.ip.sip.header.TimeStampHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Time stamp header implementation.
* 
* @author Assaf Azaria, Mar 2003. 
*/
public class TimeStampHeaderImpl extends HeaderImpl implements TimeStampHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -7788231624812969574L;

	//
	// Members.
	//
	
	/** 
	 * The timeStamp.
	 */
	double m_timeStamp = 0;
    
   	/** 
     * The delay.
 	 */
	double m_delay = -1;
    
    /**
     * @throws SipParseException
     */
    public TimeStampHeaderImpl() {
        super();
    }
    
	//
	// Methods.
	//
	
    /**
    * Gets timestamp of TimeStampHeader
    * @return timestamp of TimeStampHeader
    */
    public float getTimeStamp()
    {
    	return (float)m_timeStamp;
    }

    /**
    * Gets delay of TimeStampHeader
    * (Returns negative float if delay does not exist)
    * @return delay of TimeStampHeader
    */
    public float getDelay()
    {
        return (float)m_delay;
    }

    /**
    * Gets boolean value to indicate if TimeStampHeader
    * has delay
    * @return boolean value to indicate if TimeStampHeader
    * has delay
    */
    public boolean hasDelay()
    {
        return m_delay != -1;
    }

    /**
     * Sets timestamp of TimeStampHeader
     * @param timestamp float to set
     * @throws SipParseException if timeStamp is not accepted by implementation
     */
    public void setTimeStamp(float timestamp) throws SipParseException
    {
		if(timestamp < 0) 
		{
	    	throw new SipParseException("Negative TimeStamp", "" + timestamp);
		}
    	m_timeStamp = (double)timestamp;
    }

    /**
     * Sets delay of TimeStampHeader
     * @param delay float to set
     * @throws SipParseException if delay is not accepted by implementation
     */
    public void setDelay(float delay) throws SipParseException
    {
		if(delay < 0) 
		{
			throw new SipParseException("Negative Delay");
		}
    	m_delay = (double)delay;
    }

    /**
    * Removes delay from TimeStampHeader (if it exists)
    */
    public void removeDelay()
    {
        m_delay = -1;
    }

    /**
     * reads a time value. this is common format to timestamp and delay.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
     * @return the matched value
     * @throws SipParseException
     */
    private double parseTimeValue(SipParser parser) throws SipParseException {
    	StringBuilder buffer = new StringBuilder(64);

    	char digit = parser.LA(1);
        while (Character.isDigit(digit)) {
        	parser.consume();
        	buffer.append(digit);
        	digit = parser.LA();
        }
        if (parser.LA(1) == DOT) {
        	parser.consume();
        	buffer.append(DOT);

        	digit = parser.LA(1);
            while (Character.isDigit(digit)) {
            	parser.consume();
            	buffer.append(digit);
            	digit = parser.LA();
            }
        }
        String s = buffer.toString();
		try {
	        double timeValue = Double.parseDouble(s);
	        if (Double.isInfinite(timeValue)) {
	        	throw new SipParseException("Bad Timestamp header");
	        }
			return timeValue;
		}
		catch (NumberFormatException e) {
			throw new SipParseException(s);
		}
    }

	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        // time stamp
		m_timeStamp = parseTimeValue(parser);

        // delay
        parser.lws();
        if (parser.LA(1) != ENDL) {
        	m_delay = parseTimeValue(parser);
        }
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_timeStamp);
		if(hasDelay()) 
		{
			ret.append(SP);
			ret.append(m_delay);
		}
	} 
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof TimeStampHeaderImpl)) {
			return false;
		}
		TimeStampHeaderImpl o = (TimeStampHeaderImpl)other;
		return m_timeStamp == o.m_timeStamp
			&& m_delay == o.m_delay;
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
}
