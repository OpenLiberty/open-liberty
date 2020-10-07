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
package com.ibm.ws.jsp23.fat.testinjection.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.jsp23.fat.testinjection.beans.Pojo1;

@Interceptor
@InterceptMeBinding
//binding the interceptor here. now any method annotated with @InterceptMeBinding would be intercepted by InterceptMeAroundInvokeMethod
public class InterceptMeInterceptor {
    @AroundInvoke
    public Object InterceptMeAroundInvokeMethod(InvocationContext ctx) throws Exception {
        Pojo1.counter++;
        return ctx.proceed();
    }
}
