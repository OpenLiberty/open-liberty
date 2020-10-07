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
import jain.protocol.ip.sip.header.RetryAfterHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Retry after header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class RetryAfterHeaderImpl extends ExpiresHeaderImpl
    implements RetryAfterHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -961806665217938113L;

	//
	// Constants.
	//
	public final static String DURATION = "duration";
	
	//
	// Memebrs.
	//
	
	/**
	 * The duration.
	 */
	private long m_duration = -1;
	
    /**
     * The optional comment.
     */
	private String m_comment;
    
    /**
     * The map of parameters.
     */
    private ParametersImpl m_params;
    
    /** 
     */
    public RetryAfterHeaderImpl()
    {
        super();
		m_params = null;
    }

	//
    // Methods.
    //
    
    /**
    * Gets comment of RetryAfterHeader
    * (Returns null if comment does not exist)
    * @return comment of RetryAfterHeader
    */
    public String getComment()
    {
        return m_comment;
    }

    /**
    * Gets boolean value to indicate if RetryAfterHeader
    * has comment
    * @return boolean value to indicate if RetryAfterHeader
    * has comment
    */
    public boolean hasComment()
    {
        return (m_comment != null);
    }

    /**
    * Removes comment from RetryAfterHeader (if it exists)
    */
    public void removeComment()
    {
        m_comment = null;
    }
    
	/**
	 * Sets comment of RetryAfterHeader
	 * @param comment String to set
	 * @throws IllegalArgumentException if comment is null
	 * @throws SipParseException if comment is not accepted by implementation
	 */
	public void setComment(String comment)
		throws IllegalArgumentException, SipParseException
	{
		if (comment == null)
		{	
			throw new IllegalArgumentException("RetryAfter: Null comment"); 
		} 
			
		m_comment = comment;
	}

	/**
    * Gets duration of RetryAfterHeader
    * (Returns negative long if duration does not exist)
    * @return duration of RetryAfterHeader
    */
    public long getDuration()
    {
        return m_duration;
    }

    /**
    * Gets boolean value to indicate if RetryAfterHeader
    * has duration
    * @return boolean value to indicate if RetryAfterHeader
    * has duration
    */
    public boolean hasDuration()
    {
    	return (m_duration != -1);
    }

    /**
    * Removes duration from RetryAfterHeader (if it exists)
    */
    public void removeDuration()
    {
        m_duration = -1;
    }

     /**
     * Sets duration of RetryAfterHeader
     * @param duration long to set
     * @throws SipParseException if duration is not accepted by implementation
     */
    public void setDuration(long duration) throws SipParseException
    {
    	if (duration < 0)
    	{
    		throw new SipParseException("Negative duration", "");
    	}
    	
    	m_duration = duration;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        // Delta Seconds.
        setDeltaSeconds(parser.longNumber());

        if (parser.LA(1) == LPAREN) {
        	parser.match(LPAREN);
            setComment(parser.nextToken(RPAREN));
            parser.match(RPAREN);
        }
        
        if (parser.LA(1) == SEMICOLON) {
        	parser.match(SEMICOLON);
        	parser.match(DURATION);
        	parser.match(EQUALS);
            setDuration(parser.longNumber());
        }
        
        // parameters
        if (parser.LA(1) == SEMICOLON) {
        	parser.match(SEMICOLON);
            m_params = parser.parseParametersMap(SEMICOLON, true,false);
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
		super.encodeValue(ret);
		
		if(hasComment()) 
		{
			ret.append(SP);
			ret.append(LPAREN);
			ret.append(m_comment);
			ret.append(RPAREN);
		}
				
		if(hasDuration())
	    {
	    	ret.append(SEMICOLON);
			ret.append(DURATION);
			ret.append(EQUALS);
			ret.append(m_duration);
		}
		
		if (m_params != null) {
			ret.append(SEMICOLON);
			m_params.encode(ret, SEMICOLON, true);
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
		if (!(other instanceof RetryAfterHeaderImpl)) {
			return false;
		}
		RetryAfterHeaderImpl o = (RetryAfterHeaderImpl)other;
		
		if (m_duration != o.m_duration) {
			return false;
		}
		
		if (m_comment == null || m_comment.length() == 0) {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return false;
			}
			else {
				return m_comment.equals(o.m_comment);
			}
		}
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return RetryAfterHeader.name;
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
