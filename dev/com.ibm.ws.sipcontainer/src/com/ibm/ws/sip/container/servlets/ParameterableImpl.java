/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.ParametersHeader;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.sip.Parameterable;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericParametersHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;

/**
 * @author Assya Azrieli, Sep 10, 2007
 *
 * Implementation of the Parameterable API. 
 */
public class ParameterableImpl implements Parameterable, Serializable
{

	/** Serialization UID (do not change) */
	static final long serialVersionUID = 1637732213269414559L;

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ParameterableImpl.class);

 
    /**
     * Pointer to the SIP header field value with optional parameters wrapped by this object.
     * The header name is not significant.
     */
    protected ParametersHeader _parametersHeader;

    /**
     * Construct a new Parameterable from the given Jain Sip Name Address Header.
     * @param parametersHeader the Header that the address will be 
     * taken from.
     * @pre parametersHeader != null
     */
    public ParameterableImpl(ParametersHeader parametersHeader)
    {
    	if (parametersHeader == null) {
    		throw new IllegalArgumentException("parametersHeader cannot be null");
    	}
        _parametersHeader = parametersHeader;
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
    {
        ParameterableImpl cloned = null;
        try
        {
            cloned = (ParameterableImpl) super.clone();
            cloned._parametersHeader = (ParametersHeader) _parametersHeader.clone();
        }
        catch (CloneNotSupportedException e)
        {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error(
                    "error.cloning.address",
                    Situation.SITUATION_CREATE,
                    null,
                    e);
            }
        }

        return cloned;

    }

    
    /**
     * @see javax.servlet.sip.Parameterable#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
    	if(name==null){
    		throw new NullPointerException("javax.servlet.sip.Parameterable#getParameter(java.lang.String name): name must not be null" );
    	}
        return _parametersHeader.getParameter(name);
    }

    /**
     * @see javax.servlet.sip.Parameterable#getParameterNames()
     */
    public Iterator<String> getParameterNames()
    {
        Iterator<String> iter = _parametersHeader.getParameters();
        if(null == iter)
        {
            iter = EmptyIterator.getInstance();
        }
        
        return iter; 
    }

    
    /**
     * @see javax.servlet.sip.Parameterable#removeParameter(java.lang.String)
     */
    public void removeParameter(String name)
    {
        if (name == null) {
            if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug("removeParameter:  name is null, throwing exception");
    		}
            throw new NullPointerException("name is null");
        }
        _parametersHeader.removeParameter(name);
    }

   
    /**
     * @see javax.servlet.sip.Parameterable#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
    	if(name==null){
    		throw new NullPointerException("javax.servlet.sip.Parameterable#setParameter(java.lang.String name, java.lang.String value): name must not be null" );
    	}
    	if(value == null){
    		removeParameter(name);
    		return;
    	}
    	
        try
        {
            _parametersHeader.setParameter(name, value);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };
                c_logger.error(
                    "error.set.parameter",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };
                c_logger.error(
                    "error.set.parameter",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * @see javax.servlet.sip.Parameterable#getParameters()
     */
    public Set<Map.Entry<String, String>> getParameters() {

    	Map<String, String> map = new HashMap<String,String>();
    	
    	Iterator localIter = _parametersHeader.getParameters();

        String paramName;
        String localParam;

        while(localIter != null && localIter.hasNext())
        {
            paramName = (String) localIter.next();
            localParam = _parametersHeader.getParameter(paramName);
            map.put(paramName, localParam);
        }
        
        return map.entrySet();
	}

    /**
	 * @see javax.servlet.sip.Parameterable#getValue()
	 */
	public String getValue() {
		ParametersHeader parametersHeader = _parametersHeader;

		if (c_logger.isTraceDebugEnabled()) {
			String headerAsString =
				parametersHeader == null
					? null
					: parametersHeader.getName() + ": " + parametersHeader.getValue();
			c_logger.traceDebug(this, "getValue", headerAsString);
		}
		String value;

		if (parametersHeader == null) {
			value = null;
		}
		else if (parametersHeader instanceof GenericParametersHeaderImpl) {
			// simple case
			GenericParametersHeaderImpl genericParametersHeaderImpl =
				(GenericParametersHeaderImpl)parametersHeader;
			value = genericParametersHeaderImpl.getFieldValue();
		}
		else if (parametersHeader instanceof NameAddressHeader) {
			// a name-address header. return the "name-addr" part as a new string
			NameAddressHeader nameAddressHeader =
				(NameAddressHeader)parametersHeader;
			NameAddress nameAddress = nameAddressHeader.getNameAddress();
			value = nameAddress == null
				? null
				: nameAddress.toString();
		}
		else {
			// standard parameters-header which is not a name-address header.
			// return the "name-addr" part as a new string.
			// the string is everything up to the first semicolon.
			value = parametersHeader.getValue();
			int semi = value.indexOf(';');
			if (semi != -1) {
				value = value.substring(0, semi);
			}
		}
		return value;
	}

	/**
	 * @see javax.servlet.sip.Parameterable#setValue(java.lang.String)
	 */
	public void setValue(String value) throws IllegalStateException {
		if (value == null) {
			// the javadoc doesn't impose this, but the TCK expects us you to
			// throw null here.
			throw new NullPointerException("null parameterable value");
		}
		ParametersHeader parametersHeader = _parametersHeader;

		if (c_logger.isTraceDebugEnabled()) {
			String headerAsString = parametersHeader.toString();
			c_logger.traceDebug(this, "setValue", headerAsString);
		}
		GenericParametersHeaderImpl genericParametersHeaderImpl;

		if (parametersHeader instanceof GenericParametersHeaderImpl)
		{
			// set the value on existing instance of the internal JAIN header
			genericParametersHeaderImpl = (GenericParametersHeaderImpl)parametersHeader;
		}
		else {
			// replace internal JAIN header with a new one, of proper type,
			// and copy parameters by reference.
			genericParametersHeaderImpl = new GenericParametersHeaderImpl("IBM-GenericParametersHeader");
			genericParametersHeaderImpl.assign((ParametersHeaderImpl)parametersHeader);
			_parametersHeader = genericParametersHeaderImpl;
		}
		genericParametersHeaderImpl.setFieldValue(value);
	}
   
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return _parametersHeader.getValue();
    }

   
    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other) {
    	// compare the internal parameters header value, but do not compare 
    	// the header name
    	if (this == other) {
    		return true; // same instance
    	}
    	if (!(other instanceof ParameterableImpl)) {
    		return false;
    	}
    	ParameterableImpl otherParameterable = (ParameterableImpl)other;
    	ParametersHeader thisParameters = _parametersHeader;
    	ParametersHeader otherParameters = otherParameterable._parametersHeader;
    	if (thisParameters == otherParameters) {
    		return true; // same internal instance
    	}
    	if (thisParameters == null || otherParameters == null) {
    		// one is null, the other is not null
    		return false;
    	}

    	// compare parameters
    	if (!compareParameters(other)) {
    		return false;
    	}

    	// compare the value, without parameters
    	if (thisParameters instanceof NameAddressHeader &&
    		otherParameters instanceof NameAddressHeader)
    	{
    		// compare address with address
    		NameAddressHeader thisAddressHeader = (NameAddressHeader)thisParameters;
    		NameAddressHeader otherAddressHeader = (NameAddressHeader)otherParameters;
    		NameAddress thisAddress = thisAddressHeader.getNameAddress();
    		NameAddress otherAddress = otherAddressHeader.getNameAddress();
    		if (thisAddress != otherAddress) {
    			if (thisAddress == null || otherAddress == null) {
    				return false;
    			}
    			if (!thisAddress.equals(otherAddress)) {
    				return false;
    			}
    		}
    	}
    	else {
    		// compare 2 values, at least one of them is not an address
        	String thisValue = getValue();
        	String otherValue = otherParameterable.getValue();
        	if (thisValue != otherValue) {
        		return false;
        	}
        	if (thisValue == null || otherValue == null) {
        		return false;
        	}
    	}
    	return true;
    }

    /**
     * Helper function - compares the local parameters with the given parameter
     * set and determines if they are equal. 
     * Comparing according to the rules specified in RFC 3261 Section 19.1.4
     * @param parameters
     * @return true if both set have a same set of parameters. 
     */
    @SuppressWarnings("unchecked")
	protected boolean compareParameters(Object obj) {
        if (!(obj instanceof ParameterableImpl)) {
        	return false;
        }
        ParameterableImpl other = (ParameterableImpl) obj;
        ParametersHeader parameters = other._parametersHeader;    	
        
        
        if(!_parametersHeader.hasParameters() || !parameters.hasParameters()) {
        	// no parameters on one of the headers, nothing to compare, parameters equals.
            return true;
        }
        
        Iterator<String> localIter = _parametersHeader.getParameters();
        
        boolean rc = true; 
        while(localIter.hasNext()) {
            String paramName = localIter.next();
        
            String localParam = _parametersHeader.getParameter(paramName);
            String otherParam = parameters.getParameter(paramName);
            if( otherParam == null){
            	continue;
            }
            if(!localParam.equals(otherParam))
            {
                rc = false; 
                break;
            }
        }
        return rc;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return _parametersHeader.getValue().hashCode();
    }
    

    /**
     * Get the internal Jain Name Address Header wrapped by this object. 
     * @return
     */
    public ParametersHeader getParametersHeader()
    {
        return _parametersHeader;
    }
}
