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

import java.text.MessageFormat;

import com.ibm.ejs.ras.TraceNLS;


public class WebAppHostNotFoundException extends WebContainerException
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3834595387381068080L;
	private static TraceNLS nls = TraceNLS.getTraceNLS(WebAppHostNotFoundException.class, "com.ibm.ws.webcontainer.resources.Messages");

    public WebAppHostNotFoundException(String s)
    {
        super(MessageFormat.format(nls.getString("host.has.not.been.defined","The host {0} has not been defined"), new Object[]{s}));
    }

    public WebAppHostNotFoundException(String s, String port)
    {
        super(MessageFormat.format(nls.getString("host.on.port.has.not.been.defined","The host {0} on port {1} has not been defined"), new Object[]{s, port}));
    }
    
    public WebAppHostNotFoundException (Throwable th, String s)
    {
        super(MessageFormat.format(nls.getString("host.has.not.been.defined","The host {0} has not been defined"), new Object[]{s}));
    	super.setStackTrace(th.getStackTrace());
    }
}
