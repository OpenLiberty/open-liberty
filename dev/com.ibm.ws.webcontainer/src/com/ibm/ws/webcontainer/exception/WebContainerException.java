/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.exception;

import com.ibm.websphere.servlet.response.ResponseUtils;

public class WebContainerException extends java.lang.Exception
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257002172494199088L;

	public WebContainerException()
    {
        super();
    }
    
    public WebContainerException(Throwable th)
    {
    	super (th);
    }

    public WebContainerException(String s)
    {
        // d147832 - run the string through the encoder to eliminate security hole
        super(ResponseUtils.encodeDataString(s));
    }

    public WebContainerException(String s, Throwable t)
    {
        // d147832 - run the string through the encoder to eliminate security hole
        super(ResponseUtils.encodeDataString(s), t);
    }

    
}
