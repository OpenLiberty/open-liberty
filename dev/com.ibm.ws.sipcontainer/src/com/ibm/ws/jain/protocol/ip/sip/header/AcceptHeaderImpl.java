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
import jain.protocol.ip.sip.header.AcceptHeader;

import com.ibm.ws.sip.parser.SipParser;

/**
* Accept header implementation.
* 
* todo - would be better if this class and ContentTypeHeaderImpl
* would have the same base class,
* rather than this class inherit from ContentTypeHeaderImpl.
* that would solve encodeValue() mess.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class AcceptHeaderImpl extends ContentTypeHeaderImpl
    implements AcceptHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 4541816839432951359L;
	
	//
	// Members. 
	//
	
	/**
	 * The q value.
	 */
	private float m_qValue = -1.0f;
	
    /**
     * @throws SipParseException 
     */
    public AcceptHeaderImpl() {
        super();
    }

	//
	// Methods.
	//
	
	/**
	 * Gets boolean value to indicate if the AcceptHeader
	 * allows all media types (i.e. content type is "*")
	 * @return boolean value to indicate if the AcceptHeader
	 * allows all media types
	 */
	public boolean allowsAllContentTypes()
	{
		return getContentType().equals("" + STAR);
		
	}
	
	/**
	 * Gets boolean value to indicate if the AcceptHeader
	 * allows all media sub-types (i.e. content sub-type is "*")
	 * @return boolean value to indicate if the AcceptHeader
	 * allows all media sub-types
	 */
	public boolean allowsAllContentSubTypes()
	{
		return getContentSubType().equals("" + STAR);
	}
	
	/**
	 * Sets q-value for media-range in AcceptHeader
	 * Q-values allow the user to indicate the relative degree of
	 * preference for that media-range, using the qvalue scale from 0 to 1.
	 * (If no q-value is present, the media-range should be treated as having a q-value of 1.)
	 * @param <var>qValue</var> q-value
	 * @throws SipParseException if qValue is not accepted by implementation
	 */
	public void setQValue(float qValue)
				 throws SipParseException
    {
		if (qValue < 0.0)
		{
			throw new SipParseException("AcceptHeader: Q Value < 0", ""); 
		} 
		if (qValue > 1.0)
		{
			throw new SipParseException("AcceptHeader: Q value > 1.0", ""); 
		} 
		
		m_qValue = qValue;
    }

	/**
	 * Gets q-value of media-range in AcceptHeader
	 * (Returns negative float if no q-value exists)
	 * @return q-value of media-range
	 */
	public float getQValue()
	{
		return m_qValue;
	}

	/**
	 * Gets boolean value to indicate if AcceptHeader
	 * has q-value
	 * @return boolean value to indicate if AcceptHeader
	 * has q-value
	 */
	public boolean hasQValue()
	{
		return m_qValue != -1.0f;
	}

	/**
	 * Removes q-value of media-range in AcceptHeader (if it exists)
	 */
	public void removeQValue()
	{
		m_qValue = -1.0f;
		removeParameter("q");
	}
    
	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ContentTypeHeaderImpl#parseValue(com.ibm.ws.sip.parser.SipParser)
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		super.parseValue(parser);
		String qValue = getParameter("q");
		if (qValue != null) {
			setQValue(Float.parseFloat(qValue));
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
		if (!(other instanceof AcceptHeaderImpl)) {
			return false;
		}
		AcceptHeaderImpl o = (AcceptHeaderImpl)other;
		return m_qValue != o.m_qValue;
	}

	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return AcceptHeader.name;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		return getName();
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
