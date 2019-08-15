/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.TestCustomBean;

/**
 * Interceptor checks the 1st parameter is for the TestCustomBean which
 * means this is the test for which this interceptor is being tested.
 * In this case the interceptor adds data to the TestCustimBean to show
 * that it was called.
 */
@Interceptor
@TestCustomResolver
@Priority(Interceptor.Priority.APPLICATION)
public class TestCustomResolverInterceptor {

    @AroundInvoke
    public Object checkParams(InvocationContext context) throws Exception {
        Object[] params = context.getParameters();
        //((MethodBean) params[0]).addData(":SetMethodBeanInterceptor:");
        Object base = params[1];
        if (base != null && base.getClass().getName().equals("com.ibm.ws.jsf22.fat.cdicommon.beans.TestCustomBean")) {
            ((TestCustomBean) base).setData(":TestCustomResolverInterceptor:");
        }
        context.setParameters(params);
        return context.proceed();
    }

}
