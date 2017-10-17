/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
