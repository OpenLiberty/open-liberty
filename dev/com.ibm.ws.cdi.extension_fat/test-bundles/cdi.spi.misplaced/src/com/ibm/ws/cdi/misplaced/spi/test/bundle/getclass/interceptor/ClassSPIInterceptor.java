/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.interceptor;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;

@Interceptor
@Intercept
@Priority(100)
public class ClassSPIInterceptor {

    @AroundInvoke
    public Object logMethodEntry(InvocationContext ctx) throws Exception {
        String interceptedString = (String) ctx.proceed();

        if (interceptedString.equals("Injection of a normal scoped class that was registered via getBeanClasses")) {
            return "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class registered via getBeanClasses " + interceptedString;
        } else if (interceptedString.equals("application bean")) {
            return "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class in the application WAR";
        } else if (interceptedString.contains("A bean created by an annotation defined by the SPI in a different bundle, injected into a bean created by an annotation defined by the spi in the same bundle, intercepted by two interceptors defined by the SPI one from each bundle")) {
            return "MISSPLACED INTERCEPTOR " + interceptedString;
        }

        throw new IllegalArgumentException("unrecogniesd intercepted string");
    }
}
