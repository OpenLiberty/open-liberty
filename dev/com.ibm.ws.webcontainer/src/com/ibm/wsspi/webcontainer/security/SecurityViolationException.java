/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author asisin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SecurityViolationException extends Exception 
{
    private static final long serialVersionUID = 1L;
    
    int statusCode;
    String message;
    String redirectURL;
    Object secObject;
    
    public SecurityViolationException(String msg, int i)
    {
        super (msg);
        this.message = msg;
        this.statusCode = i;
    }
    
    /**
     * @return Returns the message.
     */
    public String getMessage() {
        return message;
    }
    /**
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * @return Returns the statusCode.
     */
    public int getStatusCode() {
        return statusCode;
    }
    /**
     * @param statusCode The statusCode to set.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    /**
     * @return Returns the redirectURL.
     */
    public String getRedirectURL() {
        return redirectURL;
    }
    /**
     * @param redirectURL The redirectURL to set.
     */
    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }
    
    /**
     * Process security violation exception
     * @param req - Http servlet request object
     * @param res - Http servlet response object
     * @throws IOException if error, otherwise redirects to appropriate error or login page
     */
    public void processException(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        if (redirectURL != null)
        {
            res.sendRedirect(redirectURL);
            return;
        }
        
        if (message == null)
        {
            res.sendError(statusCode);
        }
        else
        {
            res.sendError(statusCode, message);
        }
    }

	public Object getWebSecurityContext() {
		return secObject;
	}
	
	public void setWebSecurityContext(Object secObject){
		this.secObject = secObject;
	}
}
