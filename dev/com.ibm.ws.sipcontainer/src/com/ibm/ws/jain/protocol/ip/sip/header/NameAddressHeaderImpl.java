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
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.EndPointHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;

import com.ibm.ws.jain.protocol.ip.sip.address.NameAddressImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.AlertInfoHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.CallInfoHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ErrorInfoHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeader;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * NameAddress header implementation.
 * 
 * @see ContactHeader
 * @see EndPointHeader
 * @see RouteHeader
 * @see RecordRouteHeader
 * @author Assaf Azaria, Mar 2003.
 */
public abstract class NameAddressHeaderImpl extends ParametersHeaderImpl
    implements NameAddressHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 7650958641914669533L;

	/**
	 * The address object.
	 */
	private NameAddressImpl m_nameAddress;
	
    /**
     * constructor
     */
    public NameAddressHeaderImpl() {
        super();
    }

	/**
     * Sets NameAddress of NameAddressHeader
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from the
     * same JAIN SIP implementation
     */
    public void setNameAddress(NameAddress nameAddress)
        throws IllegalArgumentException
    {
        if (nameAddress == null)
        {
        	throw new IllegalArgumentException("NameAddressHeader: null arg");
       	}
        if (!(nameAddress instanceof NameAddressImpl)) {
        	throw new IllegalArgumentException(
        		"NameAddressHeader: expected same JAIN SIP implementation");
        }

		m_nameAddress = (NameAddressImpl)nameAddress;
    }

    /**
     * Gets NameAddress of NameAddressHeader
     * (Returns null if NameAddress does not exist - i.e. wildcard ContactHeader)
     * @return NameAddress of NameAddressHeader
     */
    public NameAddress getNameAddress()
    {
        return m_nameAddress;
    }

    /**
     * determines whether the RFC allows the "addr-spec" format for this header.
     * 
     * name-addr = [ display-name ] LAQUOT addr-spec RAQUOT
     * addr-spec = SIP-URI / SIPS-URI / absoluteURI
     * 
     * some header types do not allow addr-spec, regardless of the value.
     * return false only for those.
     * 
     * @return true if addr-spec is allowed, false if name-addr is forced.
     */
    private boolean isAddrSpecFormatAllowed() {
    	String n = getName();
    	if (n == null ||
    		headerNamesEqual(n, RouteHeader.name) ||       // 3261: route-param = name-addr *( SEMI rr-param )
    		headerNamesEqual(n, RecordRouteHeader.name) || // 3261: rec-route = name-addr *( SEMI rr-param )
    		headerNamesEqual(n, PathHeader.name) ||        // 3327: path-value = name-addr *( SEMI rr-param )
    		headerNamesEqual(n, CallInfoHeader.name) ||    // 3261: info = LAQUOT absoluteURI RAQUOT *( SEMI info-param)
    		headerNamesEqual(n, AlertInfoHeader.name) ||   // 3261: alert-param = LAQUOT absoluteURI RAQUOT *( SEMI generic-param )
    		headerNamesEqual(n, ErrorInfoHeader.name))     // 3261: error-uri = LAQUOT absoluteURI RAQUOT *( SEMI generic-param )
    	{
    		return false;
    	}
    	return true;
    }

	/**
	 * encodes the value of this header
	 */
    protected void encodeValue(CharsBuffer buffer) {
    	// encode the name-address
    	boolean addrSpecFormatAllowed = isAddrSpecFormatAllowed();
    	m_nameAddress.writeToCharBuffer(buffer, addrSpecFormatAllowed);
    	
    	// encode parameters
    	super.encodeValue(buffer);
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// parse the name-address
        NameAddress address = parser.parseNameAddress();
        setNameAddress(address);

        // parse the parameters
       	super.parseValue(parser);
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
		if (!(other instanceof NameAddressHeaderImpl)) {
			return false;
		}
		NameAddressHeaderImpl o = (NameAddressHeaderImpl)other;
		if (m_nameAddress == null) {
			if (o.m_nameAddress == null) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_nameAddress == null) {
				return false;
			}
			else {
				return m_nameAddress.equals(o.m_nameAddress);
			}
		}
	}
	
	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		NameAddressHeaderImpl ret = (NameAddressHeaderImpl)super.clone(); 
		if (m_nameAddress != null)
		{
			ret.m_nameAddress = (NameAddressImpl)m_nameAddress.clone();
		}
		
		return ret;
	}

    /**
     * copies all address properties from one header to another.
     * upon return from this call, both headers will have the exact
     * same list of parameters.
     * future modifications to properties of one header
     * will affect the other.
     * 
     * @param source header to read properties from
     */
	public void assign(NameAddressHeader source) {
		NameAddressHeaderImpl o = (NameAddressHeaderImpl)source;
		m_nameAddress = o.m_nameAddress;
		super.assign(o);
	}
}
