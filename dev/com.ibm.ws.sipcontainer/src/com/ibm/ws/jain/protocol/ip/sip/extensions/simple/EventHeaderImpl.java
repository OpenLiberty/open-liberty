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
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * A header for the purposes of matching responses and NOTIFY messages with
 * SUBSCRIBE messages, the event-type portion of the "Event" header is
 * compared byte-by-byte, and the "id" parameter token (if present) is
 * compared byte-by-byte.  An "Event" header containing an "id"
 * parameter never matches an "Event" header without an "id" parameter.
 * No other parameters are considered when performing a comparison.
 *
 * Note that the forgoing text means that "Event: foo; id=1234" would
 * match "Event: foo; param=abcd; id=1234", but not "Event: foo" (id
 * does not match) or "Event: Foo; id=1234" (event portion does not
 * match).
 *
 * @author Assaf Azaria, May 2003.
 */
public class EventHeaderImpl extends ParametersHeaderImpl 
	implements EventHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -5077878265416815300L;

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
    public EventHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public EventHeaderImpl(boolean compactForm) {
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
			throw new IllegalArgumentException("Event header: null or empty type");
		}
		
		m_type = type;
	}
	
	/**
	 * Set the event type.
	 * 
	 * @param id the event id.
	 * @throws IllegalArgumentException in case the id is null.
	 * @throws SipParseException if type is not accepted by implementation 
	 */
	public void setEventId(String id) throws IllegalArgumentException,
		SipParseException
	{
		if (id == null || id.equals(""))
		{
			throw new IllegalArgumentException("Event header: null or empty id");
		}
		
		setParameter(ID, id);
		
	}
	
	/**
	 * Get the event type.
	 */
	public String getEventType()
	{
		return m_type;
	}
	
	/**
	 * Get the event id, if it exists.
	 */
	public String getEventId()
	{
		return getParameter(ID);
	}
	
	/**
	 * Check whether this header contains an Id parameter.
	 */
	public boolean hasId()
	{
		return (getParameter(ID) != null);
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        String eventType = parser.nextToken(SEMICOLON);
		setEventType(eventType);
		
		// parameters
		super.parseValue(parser);
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
		
		// Other params (if exist).
		super.encodeValue(ret);
	}

	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!super.valueEquals(other)){
			return false;
		}
		if (!(other instanceof EventHeaderImpl)) {
			return false;
		}
		EventHeaderImpl o = (EventHeaderImpl)other;
		
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
		return false;
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

    /**
     * @return true if parameters should be escaped
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#escapeParameters()
     */
    protected boolean escapeParameters() {
    	return true;
    }
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.EVENT_SHORT);
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
