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
import jain.protocol.ip.sip.header.CallIdHeader;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Call id header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class CallIdHeaderImpl extends HeaderImpl
    implements CallIdHeader
{

	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 3139528182700772295L;

	//
	// Members.
	//
	private String m_callId;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public CallIdHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public CallIdHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }
 
	/**
     * Gets Call-Id of CallIdHeader
     * @return Call-Id of CallIdHeader
     */
    public String getCallId()
    {
    	return m_callId;
    }

    /**
     * Sets Call-Id of CallIdHeader
     * @param callId String to set
     * @throws IllegalArgumentException if callId is null
     * @throws SipParseException if callId is not accepted by implementation
     */
    public void setCallId(String callId)
        throws IllegalArgumentException, SipParseException
    {
        if (callId == null)
        {
        	throw new IllegalArgumentException("CallIdHeader: null callId"); 
        } 
        
        m_callId = callId;
    }
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        m_callId = parser.toString();
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer buf)
	{
		buf.append(m_callId);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof CallIdHeaderImpl)) {
			return false;
		}
		CallIdHeaderImpl o = (CallIdHeaderImpl)other;
		
		if (m_callId == null || m_callId.length() == 0) {
			if (o.m_callId == null || o.m_callId.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_callId == null || o.m_callId.length() == 0) {
				return false;
			}
			else {
				return m_callId.equals(o.m_callId);
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
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.CALL_ID_SHORT);
		}
		return getName();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactFormSupported()
	 */
	public boolean isCompactFormSupported() {
		return true;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactForm()
	 */
	public boolean isCompactForm() {
		return m_compactForm;
	}
}
