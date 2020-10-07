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

import jain.protocol.ip.sip.Parameters;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ParametersHeader;

import java.util.Iterator;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Implemetation of parameters header.
 * 
 * @author Assaf Azaria, Mar 2003/
 */
public abstract class ParametersHeaderImpl extends HeaderImpl
    implements ParametersHeader, Parameters
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 7357153250876659997L;
    
    /**
     * the delegated map of parameters
     */
    private ParametersImpl m_params;
    
    /**
     * constructor
     */
    protected ParametersHeaderImpl() {
        super();
        m_params = null;
    }

    /**
     * @return the separator preceeding the list of parameters
     */
    protected abstract char getListSeparator();
    
    /**
     * @return the separator between parameters
     */
    protected abstract char getParamSeparator();
    
    /**
	 * Gets the value of specified parameter
	 * (Note - zero-length String indicates flag parameter)
	 * (Returns null if parameter does not exist)
	 * @param <var>name</var> name of parameter to retrieve
	 * @return the value of specified parameter
	 * @throws IllegalArgumentException if name is null
	 */
    public String getParameter(String name) throws IllegalArgumentException
    {
    	if(m_params == null)
    	{
    	    return null; 
    	}
    	return m_params.getParameter(name);
    }

    /**
     * Sets value of parameter
     * (Note - zero-length value String indicates flag parameter)
     * @param <var>name</var> name of parameter
     * @param <var>value</var> value of parameter
     * @throws IllegalArgumentException if name or value is null
     * @throws SipParseException if name or value is not accepted by implementation
     */
    public void setParameter(String name, String value)
        throws IllegalArgumentException, SipParseException
    {
    	boolean quoted = false;
    	if (value != null && value.length() > 0 ){
    		ParameterQuoter quoter = ParameterQuoter.instance();
    		quoted = quoter.quote(name, value, false); 
    	}

    	setParameter(name,value,quoted);
    }
    
    /**
     * Sets value of parameter
     * (Note - zero-length value String indicates flag parameter)
     * @param <var>name</var> name of parameter
     * @param <var>value</var> value of parameter
     * @throws IllegalArgumentException if name or value is null
     * @throws SipParseException if name or value is not accepted by implementation
     */
    public void setParameter(String name, String value, boolean qouted)
        throws IllegalArgumentException, SipParseException
    {
		if(m_params == null)
    	{
    	    m_params = new ParametersImpl(); 
    	}
	 	m_params.setParameter(name, value,qouted);
    }

    /**
     * Gets boolean value to indicate if Parameters
     * has any parameters
     * @return boolean value to indicate if Parameters
     * has any parameters
     */
    public boolean hasParameters()
    {
    	return m_params != null && m_params.hasParameters();
    }

    /**
     * Gets boolean value to indicate if Parameters
     * has specified parameter
     * @return boolean value to indicate if Parameters
     * has specified parameter
     * @throws IllegalArgumentException if name is null
     */
    public boolean hasParameter(String name) throws IllegalArgumentException
    {
		return m_params != null && m_params.hasParameter(name);
    }

    /**
     * Removes specified parameter from Parameters (if it exists)
     * @param <var>name</var> name of parameter
     * @throws IllegalArgumentException if parameter is null
     */
    public void removeParameter(String name) throws IllegalArgumentException
    {
		if(m_params != null)
    	{
	    	m_params.removeParameter(name);
    	}
    }

    /**
     * Removes all parameters from Parameters (if any exist)
     */
    public void removeParameters()
    {
        if(m_params != null)
    	{
            m_params.removeParameters(); 
    	}
    }

    /**
     * Gets Iterator of parameter names
     * (Note - objects returned by Iterator are Strings)
     * (Returns null if no parameters exist)
     * @return Iterator of parameter names
     */
    public Iterator getParameters()
    {
        if(m_params == null)
    	{
    	    return null; 
    	}
        return m_params.getParameters();
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		char listSeparator = getListSeparator();
        if (parser.LA(1) == listSeparator) {
        	parser.match(listSeparator);
        	char paramSeparator = getParamSeparator();
        	m_params = parser.parseParametersMap(paramSeparator, false,false);
        }
        else {
        	removeParameters();
        }
    }
    
	/**
	 * encodes the value of this header
	 */
	protected void encodeValue(CharsBuffer buffer) {
		if (m_params != null && m_params.size() > 0) {
			char listSeparator = getListSeparator();
			buffer.append(listSeparator);
        	char paramSeparator = getParamSeparator();
			m_params.encode(buffer, paramSeparator, false);
		}
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof ParametersHeaderImpl)) {
			return false;
		}
		ParametersHeaderImpl o = (ParametersHeaderImpl)other;
		if (m_params == null || m_params.size() == 0) {
			if (o.m_params == null || o.m_params.size() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_params == null || o.m_params.size() == 0) {
				return false;
			}
		}
		return m_params.equals(o.m_params);
	}
	
	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		ParametersHeaderImpl ret = (ParametersHeaderImpl)super.clone(); 
		if (m_params != null)
		{
			ret.m_params = (ParametersImpl)(m_params.clone());
		}
		
		return ret;
	}
	
    /**
     * copies parameters from one header to another.
     * upon return from this call, both headers will have the exact
     * same list of parameters.
     * future modifications to parameters of one header
     * will affect the other.
     * 
     * @param source header to read parameters from
     */
    public void assign(ParametersHeaderImpl source) {
    	m_params = (ParametersImpl)source.m_params;
    }
}
