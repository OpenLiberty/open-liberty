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
import jain.protocol.ip.sip.header.PriorityHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Priority header implementation.
* 
* @author Assaf Azaria, Mar 2003.  
*/
public class PriorityHeaderImpl extends HeaderImpl
    implements PriorityHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -5171776118010606561L;

	/**
	 * The priotity string.
	 */
	private String m_priority;
	
    /**
     * @throws SipParseException
     */
    public PriorityHeaderImpl()
    {
        super();
    }
    
    /**
    * Gets priority of PriorityHeader
    * @return priority of PriorityHeader
    */
    public String getPriority()
    {
    	return m_priority;
    }

    /**
     * Set priority of PriorityHeader
     * @param prio String to set
     * @throws IllegalArgumentException if priority is null
     * @throws SipParseException if priority is not accepted by implementation
     */
    public void setPriority(String prio)
        throws IllegalArgumentException, SipParseException
    {
        if (prio == null)
        {    
        	throw new IllegalArgumentException("Null priority"); 
        } 
        
        if (!prio.equals(PRIORITY_NORMAL) 	  &&
			!prio.equals(PRIORITY_URGENT) 	  &&
			!prio.equals(PRIORITY_NON_URGENT) &&
			!prio.equals(PRIORITY_EMERGENCY))
		{
			throw new SipParseException("Unknown priority" + prio);	
		}
        
        m_priority = prio;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_priority = parser.toString();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_priority);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof PriorityHeaderImpl)) {
			return false;
		}
		PriorityHeaderImpl o = (PriorityHeaderImpl)other;
		
		if (m_priority == null || m_priority.length() == 0) {
			if (o.m_priority == null || o.m_priority.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_priority == null || o.m_priority.length() == 0) {
				return false;
			}
			else {
				return m_priority.equals(o.m_priority);
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
