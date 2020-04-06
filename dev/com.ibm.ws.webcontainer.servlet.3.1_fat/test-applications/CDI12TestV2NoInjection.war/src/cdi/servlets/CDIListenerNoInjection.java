/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class CDIListenerNoInjection implements ServletRequestListener {

    public static final String LISTENER_DATA = "CDIListenerData";

    @Override
    public void requestInitialized(ServletRequestEvent requestEvent) {
        ServletRequest servletRequest = requestEvent.getServletRequest();
        ServletContext requestContext = servletRequest.getServletContext();
        requestContext.setAttribute(LISTENER_DATA, "Listener Hello! No Injection.");
    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletrequestevent) {
        // EMPTY
    }
}
