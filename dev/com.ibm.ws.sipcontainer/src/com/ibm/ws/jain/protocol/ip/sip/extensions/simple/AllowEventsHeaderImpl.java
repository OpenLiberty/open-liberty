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
package com.ibm.ws.jain.protocol.ip.sip.extensions.simple;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The "Allow-Events" header, if present, includes a list of tokens
 * which indicates the event packages supported by the client (if sent
 * in a request) or server (if sent in a response).  In other words, a
 * node sending an "Allow-Events" header is advertising that it can
 * process SUBSCRIBE requests and generate NOTIFY requests for all of
 * the event packages listed in that header.
 *
 * This information is very useful, for example, in allowing user agents
 * to render particular interface elements appropriately according to
 * whether the events required to implement the features they represent
 * are supported by the appropriate nodes.
 * 
 * @author Assaf Azaria, May 2003.
 */
public class AllowEventsHeaderImpl extends HeaderImpl
    implements AllowEventsHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -8183676671342871510L;

	//
	// Members.
	//
 	
	/**
	 * The event type.
	 */
	protected String m_type;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public AllowEventsHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public AllowEventsHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }
	
	//
	// Operations.
	//
	/**
	 * Set the event type.
	 * 
	 * @param type the event type.
	 * @throws IllegalArgumentException in case the type is null. 
	 */
	public void setEventType(String type) throws IllegalArgumentException 
	{
		if (type == null || type.equals(""))
		{
			throw new IllegalArgumentException("Allow Events header: null or empty type");
		}
		
		m_type = type;
	}
	
	/**
	 * Get the event type.
	 */
	public String getEventType()
	{
		return m_type;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_type = parser.toString();
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_type);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof AllowEventsHeaderImpl)) {
			return false;
		}
		AllowEventsHeaderImpl o = (AllowEventsHeaderImpl)other;
		
		if (m_type == null || m_type.length() == 0) {
			if (o.m_type == null || o.m_type.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_type == null || o.m_type.length() == 0) {
				return false;
			}
			else {
				return m_type.equals(o.m_type);
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
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.ALLOW_EVENTS_SHORT);
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
