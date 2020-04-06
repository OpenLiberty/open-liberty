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
import jain.protocol.ip.sip.header.EndPointHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.ToHeader;

/**
 * End point header implementation.
 * 
 * @author Assaf Azaria, Mar 2003.
 * 
 * @see NameAddressHeader
 * @see FromHeader
 * @see ToHeader
 */
public abstract class EndPointHeaderImpl extends NameAddressHeaderImpl
	implements EndPointHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -6649922672090643170L;

	/**
	 * The tag constant.
	 */
	public static final String TAG = "tag";
	
	/**
	 * @throws SipParseException
     */
    public EndPointHeaderImpl() {
        super();
    }
    
	/**
     * Gets boolean value to indicate if EndPointHeader
     * has tag
     * @return boolean value to indicate if EndPointHeader
     * has tag
     */
    public boolean hasTag()
    {
    	return hasParameter(TAG);
    }

    /**
     * Sets tag of EndPointHeader
     * @param <var>tag</var> tag
     * @throws IllegalArgumentException if tag is null
     * @throws SipParseException if tag is not accepted by implementation
     */
    public void setTag(String tag)
        throws IllegalArgumentException, SipParseException
    {
        if (tag == null)
        {
        	throw new IllegalArgumentException("EndPointHeader: null tag");
        }
        
        setParameter(TAG, tag);
    }

	/**
	 * Removes tag from EndPointHeader (if it exists)
	 */
    public void removeTag()
    {
    	removeParameter(TAG);
    }

    /**
     * Gets tag of EndPointHeader
     * (Returns null if tag does not exist)
     * @return tag of EndPointHeader
     */
    public String getTag()
    {
    	return getParameter(TAG);
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
	
	protected boolean escapeParameters() {
    	return false;
    }
}
