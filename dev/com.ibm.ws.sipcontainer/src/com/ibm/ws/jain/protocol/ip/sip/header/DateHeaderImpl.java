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
import jain.protocol.ip.sip.header.DateHeader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ibm.ws.sip.parser.SipDate;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Date header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class DateHeaderImpl extends HeaderImpl implements DateHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 3833088348064026414L;

	/**
	 * The SipDate object (for encoding).
	 */
	private SipDate m_sipDate;
    
    /**
     * @throws SipParseException
     */
    public DateHeaderImpl() {
        super();
    }

	/**
     * Gets date of DateHeader
     * (Returns null if date does not exist (this can only apply
     * for ExpiresHeader subinterface - when the expires value is in delta
     * seconds format)
     * @return date of DateHeader
     */
    public Date getDate()
    {
        if (m_sipDate == null)
        {    
        	return null; 
        } 
        else
        { 
           return m_sipDate.getDate(); 
        } 
    }

    /**
     * Sets date of DateHeader
     * @param date Date to set
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public void setDate(Date date)
        throws IllegalArgumentException, SipParseException
    {

        if (date == null)
        {
        	throw new IllegalArgumentException("DateHeader: Null date"); 
        } 
			
		m_sipDate = new SipDate(date);
    }

    /**
     * Sets date of DateHeader
     * @param date String to set
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public void setDate(String date)
        throws IllegalArgumentException, SipParseException
    {

        if (date == null)
        {    
        	throw new IllegalArgumentException("DateHeader: Null date"); 
        } 
        
		// Parse the date string.
        try
        {
			SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
			Date d = formatter.parse(date);
        	
			m_sipDate = new SipDate(d);
        }
        catch (ParseException e)
        {    
        	throw new SipParseException("Wrong date format", date); 
        }
	}
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		String date = parser.toString();
		setDate(date);
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		m_sipDate.writeToCharBuffer(buf);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof DateHeaderImpl)) {
			return false;
		}
		DateHeaderImpl o = (DateHeaderImpl)other;
		
		if (m_sipDate == null) {
			if (o.m_sipDate == null) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_sipDate == null) {
				return false;
			}
			else {
				return m_sipDate.equals(o.m_sipDate);
			}
		}
	}

	/**
	 * For Internal use. TODO: remove.
	 */
	public void setDate(SipDate date)
	{
		m_sipDate = date;
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
