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
package com.ibm.ws.webcontainer.webapp;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.servlet.event.ServletInvocationEvent;

/**
 * WebApp implmentation of the WebSphere ServletInvocationEvent.
 * This class provides the ability to set the response time of the servlet
 * via the setResponseTime() method.
 */
public class WebAppServletInvocationEvent extends ServletInvocationEvent
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257290223048995889L;
	long responseTime = -1;
    
    public WebAppServletInvocationEvent(Object source, ServletContext context, String servletName, String servletClassName, ServletRequest req, ServletResponse resp)
    {
        super(source, context, servletName, servletClassName, req, resp);
    }

    public long getResponseTime()
    {
        return responseTime;
    }
    
    /**
     * Set the response time of the request.
     */

// SHS 81242
// Changed from package scope to public to allow the JspServlet to setResponseTime
    public void setResponseTime(long time)
    {
        responseTime = time;
    }
    
}
