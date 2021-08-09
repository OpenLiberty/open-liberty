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

import java.io.PrintWriter;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 */
@Interceptor
@SendResponseType
@Priority(Interceptor.Priority.APPLICATION)
public class SendResponseInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();
        PrintWriter out = (PrintWriter) params[1];
        params[0] = params[0] + " :SendResponseInterceptor:";
        context.setParameters(params);
        return context.proceed();
    }

}
