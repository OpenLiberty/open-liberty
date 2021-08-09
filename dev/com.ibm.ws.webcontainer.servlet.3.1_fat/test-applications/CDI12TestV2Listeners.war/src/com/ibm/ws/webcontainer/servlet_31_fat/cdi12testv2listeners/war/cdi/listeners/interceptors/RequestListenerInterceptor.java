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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;

/**
 *
 */
@Interceptor
@RequestListenerType
@Priority(Interceptor.Priority.APPLICATION)
public class RequestListenerInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();

        ServletRequestEvent sre = (ServletRequestEvent) params[0];
        ServletRequest req = sre.getServletRequest();

        if (req.getAttribute("RLInterceptor") == null)
            req.setAttribute("RLInterceptor", ":Int1");
        else
            req.setAttribute("RLInterceptor", ":Int2");

        return context.proceed();
    }

}
