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
import jain.protocol.ip.sip.header.OrganizationHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Organization header implementation.
*/
public class OrganizationHeaderImpl extends HeaderImpl
    implements OrganizationHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 6242499669808936827L;

	/**
	 * The organization string.
	 */
	String m_organization;
	
    /**
     * @throws SipParseException
     */
    public OrganizationHeaderImpl() {
        super();
    }
		
	/**
    * Gets organization of OrganizationHeader
    * @return organization of OrganizationHeader
    */
    public String getOrganization()
    {
    	return m_organization;
    }

    /**
     * Sets organization of OrganizationHeader
     * @param org String to set
     * @throws IllegalArgumentException if organization is null
     * @throws SipParseException if organization is not accepted 
     * by implementation
     */
    public void setOrganization(String org)
        throws IllegalArgumentException, SipParseException
    {
        if (org == null)
        {    
        	throw new IllegalArgumentException("Organization is null"); 
        } 
        
        m_organization = org;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_organization = parser.toString();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_organization);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof OrganizationHeaderImpl)) {
			return false;
		}
		OrganizationHeaderImpl o = (OrganizationHeaderImpl)other;
		if (m_organization == null || m_organization.length() == 0) {
			if (o.m_organization == null || o.m_organization.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_organization == null || o.m_organization.length() == 0) {
				return false;
			}
			else {
				return m_organization.equals(o.m_organization);
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
