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
public class SystemSipURIImpl extends ContactSystemSipURIImpl
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SystemSipURIImpl.class);

    /**
     * 
     * @param jainSipUrl
     */
    public SystemSipURIImpl(SipURL jainSipUrl)
    {
        super(jainSipUrl);
    }
    
    /**
     * @see javax.servlet.sip.SipURI#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setParameter", "name: " + name +
				"This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
    
    /**
     * @see javax.servlet.sip.SipURI#setUser(java.lang.String)
     */
    public void setUser(String user)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setUser", "user: " + user +
				"This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }

    /**
     * @see javax.servlet.sip.SipURI#setUserParam(java.lang.String)
     */
    public void setUserParam(String user)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setUserParam", "user: " + user +
				"This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
    
    /**
     * @see javax.servlet.sip.SipURI#setUserPassword(java.lang.String)
     */
    public void setUserPassword(String password)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setUserPassword", "password: " + password +
				"This URI is used in a Contact System " +
				"header context where it cannot be modified ");
		}
		
		return;
    }
}
