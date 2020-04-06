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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Dec 14 2003
 *
 * Implementation for the specific case where the name address represents 
 * a wildcard address. 
 */
public class WildcardNameAddress implements Address
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(WildcardNameAddress.class);

	/**
	 * Constant for constructing exceptions
	 */
	private static final String WILDCARD_ADDRESS_EXCEPTION =
										"Wildcard address can not be modified";
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone()
    {
        Object cloned = null;
        try
        {
            cloned = super.clone();
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
     * @see javax.servlet.sip.Address#getDisplayName()
     */
    public String getDisplayName()
    {
        return null;
    }

    /**
     * @see javax.servlet.sip.Address#getExpires()
     */
    public int getExpires()
    {
        return -1;
    }

    /**
     * @see javax.servlet.sip.Address#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
        return null;
    }

    /**
     * @see javax.servlet.sip.Address#getParameterNames()
     */
    public Iterator getParameterNames()
    {
        return null;
    }

    /**
     * @see javax.servlet.sip.Address#getQ()
     */
    public float getQ()
    {
        return -1;
    }

    /**
     * @see javax.servlet.sip.Address#getURI()
     */
    public URI getURI()
    {
        return null;
    }

    /**
     * @see javax.servlet.sip.Address#isWildcard()
     */
    public boolean isWildcard()
    {
        return true;
    }

    /**
     * @see javax.servlet.sip.Address#removeParameter(java.lang.String)
     */
    public void removeParameter(String name)
    {
        //do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
    }

    /**
     * @see javax.servlet.sip.Address#setDisplayName(java.lang.String)
     */
    public void setDisplayName(String name)
    {
        //do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
    }

    /**
     * @see javax.servlet.sip.Address#setExpires(int)
     */
    public void setExpires(int seconds)
    {
        //do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
    }

    /**
     * @see javax.servlet.sip.Address#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
        //do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
    }

    /**
     * @see javax.servlet.sip.Address#setQ(float)
     */
    public void setQ(float q)
    {
        //do nothing it is wildcard address - not applicable
    }

    /**
     * @see javax.servlet.sip.Address#setURI(javax.servlet.sip.URI)
     */
    public void setURI(URI uri)
    {
        //do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        boolean rc = false;
        if (obj instanceof WildcardNameAddress)
        {
            rc = true;
        }

        return rc;
    }

    /** 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return "com.ibm.ws.sip.container.servlets.WildcardNameAddress1"
            .hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "*";
    }

    /**
     * @see com.ibm.ws.sip.container.jain289API.Parameterable#getParameters()
     */
	public Set<Entry<String, String>> getParameters() {
//		do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
	}

	/**
     * @see com.ibm.ws.sip.container.jain289API.Parameterable#getValue()
     */
	public String getValue() {
//		do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
	}

	/**
     * @see com.ibm.ws.sip.container.jain289API.Parameterable#setValue(String)
     */
	public void setValue(String value) throws IllegalStateException {
//		do nothing it is wildcard address - not applicable
        throw new java.lang.IllegalStateException(WILDCARD_ADDRESS_EXCEPTION);
		
	}
}
