/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClientFT.retry;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Loggable @Interceptor
public class LoggableInterceptor {

    static Map<String, Integer> invocations = new HashMap<>();

    @AroundInvoke
    public Object logInvocation(InvocationContext ctx) throws Exception {
        Method m = ctx.getMethod();
        String fqMethodName = m.getDeclaringClass().getName() + "." + m.getName();
        invocations.merge(fqMethodName, 1, (k,v) -> { return ++v;});
        System.out.println("Invoking " + fqMethodName);
        try {
            return ctx.proceed();
        } finally {
            System.out.println("Invoked " + fqMethodName);
        }
    }
}
