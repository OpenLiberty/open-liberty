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

import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Feb 17, 2003
 *
 * Implements the Sip Servlet URI Api. 
 */
public class URIImpl implements URI, BaseURI
{
    /**
    * Class Logger. 
    */
    private static final LogMgr c_logger = Log.get(URIImpl.class);

    /**
     * The Jain Sip URI wrapped by this object. 
     */
    protected jain.protocol.ip.sip.address.URI m_jainURI;

    /**
     * Construct a new URI from the given Jain Sip URI. 
     */
    protected URIImpl(jain.protocol.ip.sip.address.URI jainURI)
    {
        m_jainURI = jainURI;
    }

    /**
    * @see javax.servlet.sip.URI#getScheme()
    */
    public String getScheme()
    {
        return m_jainURI.getScheme();
    }

    /**
     * @see javax.servlet.sip.URI#isSipURI()
     */
    public boolean isSipURI()
    {
        return false;
    }

	/**
	 * @see com.ibm.ws.sip.container.servlets.BaseURI#clone(boolean)
	 */
	public Object clone(boolean isProtected) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { isProtected };
			c_logger.traceEntry(URIImpl.class.getName(), "clone", params);
		}
		Object cloned = null;
        try
        {
            cloned = super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = {
                };
                c_logger.error(
                    "error.exception",
                    Situation.SITUATION_CREATE,
                    args,
                    e);
            }
        }

        return cloned;
	}
	
    /**
     * @see java.lang.Object#clone()
     */
    public URI clone()
    {
    	return (URI)clone(false);
    }
    

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return m_jainURI.toString();
    }

    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        boolean rc = false;
        if (obj instanceof URIImpl)
        {
            URIImpl uriObj = (URIImpl) obj;
            if (uriObj.m_jainURI.equals(m_jainURI))
            {
                rc = true;
            }
        }
        return rc;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return "URIImpl".hashCode() ^ m_jainURI.hashCode();
    }

    /**
     * Utility function for getting the host name or ip of the given URI.  
     * @return The host name/ip if available otherwise null
     * 
     * @pre uri != null
     */
    public static String getHost(URI uri)
    {
        String rValue = null;
        if (uri.isSipURI())
        {
            //Sip URIs should be in the following format: 
            //sip:user:password@host:port;uri-parameters?headers

            String data = uri.toString();
            int beginIndex = data.indexOf('@');
            if (beginIndex > 0)
            {
                int endIndex = data.indexOf(':', beginIndex);
                if (endIndex < 0)
                {
                    endIndex = data.indexOf(';', beginIndex);
                    if (endIndex < 0)
                    {
                        //We dont have a match after the host so we will use what
                        //ever is after the calculated begin index. 
                        endIndex = data.length();
                    }
                }

                rValue = data.substring(beginIndex + 1, endIndex);
            }
        }

        return rValue;
    }

    /**
     *  Utility function for getting getting the port number of the given URI.   
     * @return The port if available otherwise null
     * @TODO Amir: Do we need this function. Does it have to be static
     * 
     * @pre uri != null
     */
    public static int getPort(URI uri)
    {
        int rValue = -1;

        if (uri.isSipURI())
        {
            //Sip URIs should be in the following format: 
            //sip:user:password@host:port;uri-parameters?headers

            String data = uri.toString();

            int beginIndex = data.indexOf('@');
            beginIndex = data.indexOf(':', beginIndex);

            if (beginIndex != -1)
            {
                int endIndex = data.indexOf(';', beginIndex) - 1;

                if (endIndex < 0)
                {
                    //We dont have a match after the port so we will use what
                    //ever is after the calculated begin index. 
                    endIndex = data.length();
                }

                data = data.substring(beginIndex + 1, endIndex);
                rValue = Integer.parseInt(data);
            }
        }

        return rValue;
    }
    
    /**
     * Get access to the internal uri wrapped by this object. 
     * @return
     */
    public jain.protocol.ip.sip.address.URI getJainURI()
    {
        return m_jainURI;
    }

	/**
	 *  @see javax.servlet.sip.URI#getParameter(java.lang.String)
	 */
    public String getParameter(String key){
    	if(key==null){
    		throw new NullPointerException("javax.servlet.sip.URI#getParameter(java.lang.String key): key must not be null" );
    	}
    	return null;
	}

	/**
	 *  @see javax.servlet.sip.URI#setParameter(java.lang.String, java.lang.String)
	 */
    public void setParameter(String name, String value){
    	if(name == null || value == null){
    		throw new NullPointerException("javax.servlet.sip.Parameterable#setParameter(java.lang.String name, java.lang.String value): both name and value must not be null" );
    	}
	}

	/**
	 *  @see javax.servlet.sip.URI#removeParameter(java.lang.String)
	 */
    public void removeParameter(String name){
		throw new UnsupportedOperationException("removeParameter: Not implemented in URIImpl");
	}
	
	/**
	 *  @see javax.servlet.sip.URI#getParameterNames()
	 */
    public Iterator<String> getParameterNames(){
		throw new UnsupportedOperationException("getParameterNames: Not implemented in URIImpl");
	}
}
