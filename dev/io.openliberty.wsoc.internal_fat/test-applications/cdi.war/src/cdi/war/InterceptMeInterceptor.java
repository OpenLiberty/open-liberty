/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.war;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@InterceptMeBinding
//binding the interceptor here. now any method annotated with @InterceptMeBinding would be intercepted by InterceptMeAroundInvokeMethod
public class InterceptMeInterceptor {
    @AroundInvoke
    public Object InterceptMeAroundInvokeMethod(InvocationContext ctx) throws Exception {
        AnnotatedCDI1ServerEP.interceptorMessage = "Intercepted:" + ctx.getMethod().getName();
        return ctx.proceed();
    }
}
