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

import jain.protocol.ip.sip.header.ContentEncodingHeader;

import com.ibm.ws.sip.parser.SipConstants;

/**
* Content encoding header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class ContentEncodingHeaderImpl extends EncodingHeaderImpl
    implements ContentEncodingHeader
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -7968507956928989516L;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public ContentEncodingHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public ContentEncodingHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
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
			return String.valueOf(SipConstants.CONTENT_ENCODING_SHORT);
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
