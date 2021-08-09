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

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;

import java.util.Iterator;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Mar 23, 2003
 *
 * Implementation for Sip URI Api.  
 */
public class SipURIImpl extends URIImpl implements SipURI
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipURIImpl.class);

    /**
     * Defintion for the "lr" parameter
     */
    public static final String LR = "lr";

    /**
     * Defintion for the "maddr" parameter
     */
    public static final String MADDR = "maddr";

    /**
     * Defintion for the "method" parameter
     */
    public static final String METHOD = "method";

    /**
     * Defintion for the "transport" parameter
     */
    public static final String TRANSPORT = "transport";

    /**
     * Defintion for the "transport" parameter
     */
    public static final String TTL = "ttl";

    /**
     * Defintion for the "transport" parameter
     */
    private static final String USER = "user";

    //
    //String constants for supported schemes
    //
    private final static String SIP = "sip";
    private final static String SIPS = "sips";

    /**
     * 
     * @param jainSipUrl
     */
    public SipURIImpl(SipURL jainSipUrl)
    {
        super(jainSipUrl);
    }

    /**
     * @see javax.servlet.sip.SipURI#getHeader(java.lang.String)
     */
    public String getHeader(String name)
    {
    	if(name==null){
    		throw new NullPointerException("javax.servlet.sip.SipURI#getHeader(java.lang.String name): name must not be null" );
    	}
        return getJainURL().getHeader(name);
    }
    
    /**
     * Remove all headers from this URL
     */
    public void removeHeaders()
    {
       getJainURL().removeHeaders();
    }

    /**
     * Helper function. Casts the internal object to a SIP URL
     * @return
     */
    private final SipURL getJainURL() {
        return (SipURL) m_jainURI;
    }

    /**
     * @see javax.servlet.sip.SipURI#getHeaderNames()
     */
    public Iterator getHeaderNames()
    {
        Iterator iter = getJainURL().getHeaders();
        if(null == iter)
        {
            iter = EmptyIterator.getInstance(); 
        }
        
        return iter;
    }

    /**
     * @see javax.servlet.sip.SipURI#getHost()
     */
    public String getHost()
    {
        return getJainURL().getHost();
    }

    /**
     * @see javax.servlet.sip.SipURI#getLrParam()
     */
    public boolean getLrParam()
    {
        return getParameter(LR)!=null;
    }

    /**
     * @see javax.servlet.sip.SipURI#getMAddrParam()
     */
    public String getMAddrParam()
    {
        return getParameter(MADDR);
    }

    /**
     * @see javax.servlet.sip.SipURI#getMethodParam()
     */
    public String getMethodParam()
    {
        return getParameter(METHOD);
    }

    /** 
     * @see javax.servlet.sip.URI#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
    	super.getParameter(name);
        return getJainURL().getParameter(name);
    }

    /**
     * @see javax.servlet.sip.SipURI#getParameterNames()
     */
    public Iterator getParameterNames()
    {
        Iterator iter = getJainURL().getParameters();
        if(null == iter)
        {
            iter = EmptyIterator.getInstance(); 
        }
        
        return iter; 
    }

    /**
     * @see javax.servlet.sip.SipURI#getPort()
     */
    public int getPort()
    {
        return getJainURL().getPort();
    }

    /**
     * @see javax.servlet.sip.SipURI#getTransportParam()
     */
    public String getTransportParam()
    {
        return getParameter(TRANSPORT);
    }

    /**
     * @see javax.servlet.sip.SipURI#getTTLParam()
     */
    public int getTTLParam()
    {
        int rValue = -1; 
    	String s = getParameter(TTL);
        if (null != s)
        {
            try
            {
            	rValue = Integer.parseInt(s);
            }
            catch(NumberFormatException e)
            {
            	if(c_logger.isTraceDebugEnabled()) 
            	{
            		c_logger.traceDebug(this, "getTTLParam", 
            				"Failed to parse: " + s + " , returning -1 ");
            	}
            }
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipURI#getUser()
     */
    public String getUser()
    {
        return getJainURL().getUserName();
    }

    /**
     * @see javax.servlet.sip.SipURI#getUserParam()
     */
    public String getUserParam()
    {
        return getParameter(USER);
    }

    /**
     * @see javax.servlet.sip.SipURI#getUserPassword()
     */
    public String getUserPassword()
    {
        return getJainURL().getUserPassword();
    }

    /**
     * @see javax.servlet.sip.SipURI#isSecure()
     */
    public boolean isSecure()
    {
        return getJainURL().getScheme().equals("sips");
    }

    /**
     * @see javax.servlet.sip.SipURI#removeParameter(java.lang.String)
     */
    public void removeParameter(String name)
    {
        getJainURL().removeParameter(name);
    }

    /**
     * @see javax.servlet.sip.SipURI#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value)
    {
        try
        {
            getJainURL().setHeader(name, value);
        }
        catch (SipParseException e)
        {
            // TODO Amir
            // Assaf says that he never throws this exception
            // the other one is a run time so i can let the app
            // handles it. 
            // Do u see a better way
            // Valid also for most of the other set in the class
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };

                c_logger.error(
                    "error.set.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }

        }
    }

    /**
     * @see javax.servlet.sip.SipURI#setHost(java.lang.String)
     */
    public void setHost(String host)
    {
        try
        {
            getJainURL().setHost(host);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { host };

                c_logger.error(
                    "error.set.host",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

    }

    /**
     * @see javax.servlet.sip.SipURI#setLrParam(boolean)
     */
    public void setLrParam(boolean flag)
    {
        if (flag)
        {
            setParameter(LR, "");
        }
        else
        {
            removeParameter(LR);
        }
    }

    /**
     * @see javax.servlet.sip.SipURI#setMAddrParam(java.lang.String)
     */
    public void setMAddrParam(String maddr)
    {
        setParameter(MADDR, maddr);

    }

    /**
     * @see javax.servlet.sip.SipURI#setMethodParam(java.lang.String)
     */
    public void setMethodParam(String method)
    {
        setParameter(METHOD, method);

    }

    /**
     * @see javax.servlet.sip.SipURI#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
    	super.setParameter(name, value);
        try
        {
            getJainURL().setParameter(name, value);
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
     * @see javax.servlet.sip.SipURI#setPort(int)
     */
    public void setPort(int port)
    {
        try
        {
            if (port > 0)
            {
                getJainURL().setPort(port);
            }
            else
            {
                getJainURL().removePort();
            }

        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { new Integer(port)};
                c_logger.error(
                    "error.set.port",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

    }
    
    /**
     * Removes port from SIPURL
     */
    public void removePort(){
        getJainURL().removePort();
    }
    

    /**
     * @see javax.servlet.sip.SipURI#setSecure(boolean)
     */
    public void setSecure(boolean b)
    {
        String scheme = b ? "sips" : "sip";

        try
        {
            getJainURL().setScheme(scheme);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { new Boolean(b)};
                c_logger.error(
                    "error.set.secure",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

    }

    /**
     * @see javax.servlet.sip.SipURI#setTransportParam(java.lang.String)
     */
    public void setTransportParam(String transport)
    {
        try
        {
            getJainURL().setTransport(transport);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { TRANSPORT, transport };
                c_logger.error(
                    "error.set.parameter",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * @see javax.servlet.sip.SipURI#setTTLParam(int)
     */
    public void setTTLParam(int ttl)
    {
        setParameter(TTL, Integer.toString(ttl));
    }

    /**
     * @see javax.servlet.sip.SipURI#setUser(java.lang.String)
     */
    public void setUser(String user)
    {
        try
        {
            getJainURL().setUserName(user);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { user };
                c_logger.error(
                    "error.set.user",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }

        }
    }

    /**
     * @see javax.servlet.sip.SipURI#setUserParam(java.lang.String)
     */
    public void setUserParam(String user)
    {
        setParameter(USER, user);
    }
    
    /**
     * @see javax.servlet.sip.SipURI#setUserPassword(java.lang.String)
     */
    public void setUserPassword(String password)
    {
        try
        {
            getJainURL().setUserPassword(password);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { password };
                c_logger.error(
                    "error.set.user.password",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }

        }
        catch (SipException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { password };
                c_logger.error(
                    "error.set.user.password",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.sip.URI#getScheme()
     */
    public String getScheme()
    {
        return getJainURL().getScheme();
    }

    /* (non-Javadoc)
     * @see javax.servlet.sip.URI#isSipURI()
     */
    public boolean isSipURI()
    {
        return true;
    }

    /**
	 * @see com.ibm.ws.sip.container.servlets.BaseURI#clone(boolean)
	 */
    @Override
	public Object clone(boolean isProtected) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { isProtected };
			c_logger.traceEntry(SipURIImpl.class.getName(), "clone", params);
		}
		
		SipURIImpl cloned = null;
		if(!isProtected){
			cloned = new SipURIImpl((SipURL) getJainURL().clone());
		} else {
	        cloned = new SipURIImpl((SipURL) getJainURL()); 
		}
		
        return (URI)cloned;
	}

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getJainURL().toString();
    }

    /**
     * Get the Jain Sip URL wrapped by this object. 
     * @return
     */
    public jain.protocol.ip.sip.address.SipURL getJainSipUrl()
    {
        return getJainURL();
    }

    /** 
    * Removed from javax - hide 289 API
       * @see java.lang.Object#equals(java.lang.Object)
       */
    public boolean equals(Object obj)
    {
        boolean rc = false;
        if (obj instanceof SipURIImpl)
        {
            SipURIImpl urlObj = (SipURIImpl) obj;
            if (urlObj.getJainURL().equals(getJainURL()))
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
        return "SipURIImpl".hashCode() ^ getJainURL().hashCode();
    }

    /**
     * Helper function to determine whether this scheme is supported by this
     * class
     */
    public static boolean isSchemeSupported(String scheme)
    {
        boolean rc = false;
        if (scheme.equalsIgnoreCase(SIP) || scheme.equalsIgnoreCase(SIPS))
        {
            rc = true;
        }

        return rc;
    }

	/**
	 *  @see javax.servlet.sip.SipURI#removeHeader(java.lang.String)
	 */
    public void removeHeader(String name) {
    	getJainURL().removeHeader(name);	
	}

}
