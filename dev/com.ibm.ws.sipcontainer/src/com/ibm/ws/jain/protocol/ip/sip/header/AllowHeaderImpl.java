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
import jain.protocol.ip.sip.header.AllowHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Allow header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class AllowHeaderImpl extends HeaderImpl implements AllowHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -7382757896208679380L;

	//
	// Members.
	//
	/**
	 * The allowed method.
	 */
	protected String m_method;
	
	/**
	 * @throws SipParseException
     */
    public AllowHeaderImpl() {
        super();
    }
    
	//
    // Methods.
    //
    
	/**
     * Gets method of AllowHeader
     * @return method of AllowHeader
     */
    public String getMethod()
    {
        return m_method;
    }

    /**
     * Sets method of AllowHeader
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public void setMethod(String method)
                 throws IllegalArgumentException,SipParseException
    {
        if (method == null)
        {
        	throw new IllegalArgumentException("Allow: null method"); 
        } 
        
        m_method = method;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        m_method = parser.toString();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_method);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof AllowHeaderImpl)) {
			return false;
		}
		AllowHeaderImpl o = (AllowHeaderImpl)other;
		
		if (m_method == null || m_method.length() == 0) {
			if (o.m_method == null || o.m_method.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_method == null || o.m_method.length() == 0) {
				return false;
			}
			else {
				return m_method.equals(o.m_method);
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
