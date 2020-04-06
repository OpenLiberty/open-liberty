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
package cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.AsyncEvent;

/**
 *
 */
@Interceptor
@StartAsyncType
@Priority(Interceptor.Priority.APPLICATION)
public class StartAsyncInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();
        AsyncEvent ae = (AsyncEvent) params[0];
        ae.getSuppliedRequest().setAttribute("StartAsyncInterceptor", ":StartAsyncInterceptor:");
        context.setParameters(params);
        return context.proceed();
    }

}
