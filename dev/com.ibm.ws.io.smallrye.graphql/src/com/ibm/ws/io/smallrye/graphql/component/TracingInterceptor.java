/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.io.smallrye.graphql.component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.graphql.GraphQLApi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Trivial
@Dependent
@GraphQLApi
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class TracingInterceptor {

    private final static TraceComponent tc = Tr.register(TracingInterceptor.class);

    @AroundInvoke
    @FFDCIgnore({Exception.class})
    public Object logInvocation(InvocationContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Method m = ctx.getMethod();
            String fqMethodName = m.getDeclaringClass().getName() + "." + m.getName();
            Tr.debug(tc, "Invoking: " + fqMethodName, ctx.getParameters());
            Object returnObj = null;
            try {
                returnObj = ctx.proceed();
            } catch (Exception e) {
                Tr.debug(tc, "Caught exception", e);
                throw e;
            } finally {
                Tr.debug(tc, "Invoked: " + fqMethodName, returnObj);
            }
            return returnObj;
        }
        return ctx.proceed();
    }
}