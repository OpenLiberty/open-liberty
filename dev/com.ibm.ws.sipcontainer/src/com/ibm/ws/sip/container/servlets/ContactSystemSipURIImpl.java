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

import jain.protocol.ip.sip.address.SipURL;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Amir Perlman, Mar 23, 2003
 *
 * Implementation for Sip URI Api.  
 */
public class ContactSystemSipURIImpl extends SipURIImpl
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ContactSystemSipURIImpl.class);

    /**
     * 
     * @param jainSipUrl
     */
    public ContactSystemSipURIImpl(SipURL jainSipUrl)
    {
        super(jainSipUrl);
    }

    /**
     * @see javax.servlet.sip.SipURI#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
    	if(name.equalsIgnoreCase(METHOD) ||
    			name.equalsIgnoreCase(TTL) ||
    			name.equalsIgnoreCase(MADDR) ||
    			name.equalsIgnoreCase(LR) ){
    		
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setParameter", "name: " + name + 
					" This URI is used in a Contact System " +
					"header context where it cannot be modified ");
			}
    		
    		return;
    	}
    		
    	super.setParameter(name, value);
    }
    
    /**
     * Remove all headers from this URL
     */
    public void removeHeaders()
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeHeaders",
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#removeParameter(java.lang.String)
     */
    public void removeParameter(String name)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeParameter", "name: " + name + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setHeader", "name: " + name + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setHost(java.lang.String)
     */
    public void setHost(String host)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setHost", "host: " + host + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setLrParam(boolean)
     */
    public void setLrParam(boolean flag)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setLrParam", "flag: " + flag + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setMAddrParam(java.lang.String)
     */
    public void setMAddrParam(String maddr)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setMAddrParam", "maddr: " + maddr + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setMethodParam(java.lang.String)
     */
    public void setMethodParam(String method)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setMethodParam", "method: " + method + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

   

    /**
     * @see javax.servlet.sip.SipURI#setPort(int)
     */
    public void setPort(int port)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setPort", "port: " + port + 
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
    
    /**
     * @see javax.servlet.sip.SipURI#removePort()
     * 
     * Removes port from SIPURL
     */
    public void removePort(){
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removePort",
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
    

    /**
     * @see javax.servlet.sip.SipURI#setSecure(boolean)
     */
    public void setSecure(boolean b)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setSecure", "isSecured: " + b +
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setTransportParam(java.lang.String)
     */
    public void setTransportParam(String transport)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setTransportParam", "transport: " + transport +
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setTTLParam(int)
     */
    public void setTTLParam(int ttl)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setTTLParam", "ttl: " + ttl +
				" This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
}
