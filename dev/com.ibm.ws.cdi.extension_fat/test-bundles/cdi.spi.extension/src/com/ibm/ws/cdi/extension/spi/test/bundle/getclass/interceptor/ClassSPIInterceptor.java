/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;

@Interceptor
@Intercept
@Priority(100)
public class ClassSPIInterceptor {

    @AroundInvoke
    public Object logMethodEntry(InvocationContext ctx) throws Exception {
        return "Intercepted " + ctx.proceed();
    }
}
