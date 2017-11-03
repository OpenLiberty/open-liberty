/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.cdi;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Traced
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE) //run this interceptor after platform interceptors but before application interceptors
public class TracedInterceptor {

    @AroundInvoke
    public Object executeFT(InvocationContext context) throws Throwable {
        System.out.println("BB- Around Invoke Execution before method run");
        Object result = context.proceed();
        System.out.println("BB - Around Invoke Execution after method run");

        return result;
    }

}