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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The base class for all headers containing information in the 
 * format:
 * XXX-Info = "XXX-Info" HCOLON XXX-uri *(COMMA XXX-uri) 
 * XXX-uri = LAQUOT absoluteURI RAQUOT *( SEMI generic-param ) 
 *
 * @see ErrorInfoHeader
 * @see AlertInfoHeader
 * @see CallInfoHeader
 * @author Assaf Azaria, May 2003.
 */
public abstract class InfoHeaderImpl extends ParametersHeaderImpl 
	implements InfoHeader
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -7949035921981846005L;
    
	/** the class logger */
	private static final LogMgr s_logger = Log.get(InfoHeaderImpl.class);

    //
    // Members. 
    // 
    /**
     * The uri.
     */
    protected URI m_uri;

    /**
     * Constructor.
     * @throws SipParseException
     */
    public InfoHeaderImpl() {
        super();
    }

    //
    // Operations.
    //

    /**
    * Gets uri of this header.
    * @return URI
    */
    public URI getURI()
    {
        return m_uri;
    }

    /**
    * Sets uri of this header.
    * @param <var>uri</var> uri
    * @throws IllegalArgumentException if uri is null.
    */
    public void setURI(URI uri) throws IllegalArgumentException
    {
        if (uri == null)
        {
            throw new IllegalArgumentException("AlertInfo: null uri");
        }

        m_uri = uri;
    }

	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        setURI(parser.parseNameAddress().getAddress());

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
		if (m_uri == null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "encodeValue", "null URI",
					new RuntimeException());
			}
		}
		else {
        	ret.append(LESS_THAN);
        	ret.append(m_uri.toString());
        	ret.append(GREATER_THAN);
        }

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
		if (!super.valueEquals(other)) {
			return false;
		}
		if (!(other instanceof InfoHeaderImpl)) {
			return false;
		}
		InfoHeaderImpl o = (InfoHeaderImpl)other;
		
		if (m_uri == null) {
			if (o.m_uri == null) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_uri == null) {
				return false;
			}
			else {
				return m_uri.equals(o.m_uri);
			}
		}
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
}
