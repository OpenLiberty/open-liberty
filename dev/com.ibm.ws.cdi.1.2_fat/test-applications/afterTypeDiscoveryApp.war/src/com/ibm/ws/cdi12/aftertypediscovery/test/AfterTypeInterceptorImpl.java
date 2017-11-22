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
package com.ibm.ws.cdi12.aftertypediscovery.test;

import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@InterceptedAfterType
public class AfterTypeInterceptorImpl {

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        System.out.println("interceptor fired");
        GlobalState.addOutput("intercepted");
        return context.proceed();
    }
}
