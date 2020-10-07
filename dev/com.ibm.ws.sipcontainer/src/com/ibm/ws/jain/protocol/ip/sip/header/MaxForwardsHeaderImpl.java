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

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Max forwards header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class MaxForwardsHeaderImpl extends HeaderImpl
    implements MaxForwardsHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 366272668924552459L;

	/**
	 * The max forwards value.
	 */
	private int m_maxForwards;
    
    /**
     * @throws SipParseException
     */
    public MaxForwardsHeaderImpl() {
        super();
    }

  	 /**
     * Sets max-forwards of MaxForwardsHeader
     * @param maxforwards int to set
     * @throws SipParseException if maxForwards is not accepted by implementation
     */
    public void setMaxForwards(int maxforwards) throws SipParseException
    {
    	if (maxforwards < 0)
    	{
    		throw new SipParseException("negative MaxForwards", "" + maxforwards);
    	}
    	
    	m_maxForwards = maxforwards;
    }
     

    /**
    * Decrements the number of max-forwards by one
    * @throws SipException if implementation cannot decrement max-fowards i.e.
    * max-forwards has reached zero
    */
    public void decrementMaxForwards() throws SipException
    {
        if (m_maxForwards == 0)
        {
       		throw new SipParseException("Max-forwards equals zero"); 
       	} 
        
        m_maxForwards--;
         
    }

    /**
    * Gets max-forwards of MaxForwardsHeader
    * @return max-forwards of MaxForwardsHeader
    */
    public int getMaxForwards()
    {
    	return m_maxForwards;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_maxForwards = parser.number();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_maxForwards);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof MaxForwardsHeaderImpl)) {
			return false;
		}
		MaxForwardsHeaderImpl o = (MaxForwardsHeaderImpl)other;
		return m_maxForwards == o.m_maxForwards;
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
