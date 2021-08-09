/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.exception;

import javax.servlet.UnavailableException;

/**
 * @author mmulholl
 *
 */
public class WebContainerUnavailableException extends UnavailableException {	
 
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
    static final long serialVersionUID = 7587224098080581230L;
    
    public static WebContainerUnavailableException create(javax.servlet.UnavailableException une)
    {
    	if (une.isPermanent())            		
    		return new WebContainerUnavailableException(une.getMessage(),une);
    	else return new WebContainerUnavailableException(une.getMessage(),une.getUnavailableSeconds(),une);  
	
    }	
    public WebContainerUnavailableException(String msg, int seconds, UnavailableException une)
    { 
        super(msg,seconds);
        initCause(une);
    }	
    	
    public WebContainerUnavailableException(String msg,UnavailableException une)
    { 
        super(msg);
    	initCause(une);
    }	
    	
  
}
