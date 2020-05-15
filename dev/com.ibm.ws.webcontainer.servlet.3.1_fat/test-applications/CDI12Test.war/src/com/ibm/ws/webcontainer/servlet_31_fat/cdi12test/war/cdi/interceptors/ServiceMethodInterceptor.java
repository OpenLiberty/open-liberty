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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Interceptor
@ServiceMethodType
@Priority(Interceptor.Priority.APPLICATION)
public class ServiceMethodInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();

        HttpServletRequest req = (HttpServletRequest) params[0];
        if (req.getAttribute("Interceptor") == null)
            req.setAttribute("Interceptor", "ServiceMethodInterceptor1");
        else
            req.setAttribute("Interceptor", "ServiceMethodInterceptor2");

        return context.proceed();
    }

}
