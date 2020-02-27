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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.OptionTagHeaderImpl;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The Supported header field enumerates all the extensions supported by the 
 * UAC or UAS. The Supported header field contains a list of option tags, 
 * described in Section 19.2, that are understood by the UAC or UAS. A UA 
 * compliant to this specification MUST only include option tags corresponding
 * to standards-track RFCs. If empty, it means no extensions are supported. 
 * The compact form of the Supported header field is k. 
 * Example: 
 *
 *    Supported: 100rel
 *
 * @author Assaf Azaria
 */
public class SupportedHeaderImpl extends OptionTagHeaderImpl 
	implements SupportedHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 8702610679846804676L;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public SupportedHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public SupportedHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_optionTag);
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
			return String.valueOf(SipConstants.SUPPORTED_SHORT);
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
