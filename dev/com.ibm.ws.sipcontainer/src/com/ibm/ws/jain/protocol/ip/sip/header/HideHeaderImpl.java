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
import jain.protocol.ip.sip.header.HideHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Hide header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class HideHeaderImpl extends HeaderImpl implements HideHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 399510361655211858L;

	/**
	 * The hide value.
	 */
	private String m_hide;
	
    /**
     * @throws SipParseException
     */
    public HideHeaderImpl() {
        super();
    }

	/**
    * Returns hide value of HideHeader
    * @return hide value of HideHeader
    */
    public String getHide()
    {
    	return m_hide;
    }

    /**
     * Sets hide value of HideHeader
     * @param <var>hide</var> hide value of HideHeader
     * @throws IllegalArgumentException if hide is null
     * @throws SipParseException if hide is not accepted by implementation
     */
    public void setHide(String hide)
                 throws IllegalArgumentException,SipParseException
    {
        if (hide == null)
        {
       		throw new IllegalArgumentException("Hide: null arg"); 
       	} 
        
        m_hide = hide;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_hide = parser.toString();
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_hide);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof HideHeaderImpl)) {
			return false;
		}
		HideHeaderImpl o = (HideHeaderImpl)other;
		
		if (m_hide == null || m_hide.length() == 0) {
			if (o.m_hide == null || o.m_hide.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_hide == null || o.m_hide.length() == 0) {
				return false;
			}
			else {
				return m_hide.equals(o.m_hide);
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
		return false;
	}
}
