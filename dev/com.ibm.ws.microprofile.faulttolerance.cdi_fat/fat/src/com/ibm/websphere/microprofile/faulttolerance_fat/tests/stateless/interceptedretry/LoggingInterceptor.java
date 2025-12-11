/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.interceptedretry;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Priority(Integer.MAX_VALUE)
@Interceptor
@LogInterceptorBinding
public class LoggingInterceptor {

    @AroundInvoke
    public Object aroundInvokeMethod(InvocationContext ctx) throws Exception {
        InterceptedRetryOnEJBServlet.log();
        return ctx.proceed();
    }
}
