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
package com.ibm.ws.cdi12.test.aroundconstruct.interceptors;

import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger;

@Interceptor
@InterceptorOneBinding
public class InterceptorOne {

    @Inject
    AroundConstructLogger logger;

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        logger.addConstructorInterceptor(this.getClass());
        return context.proceed();
    }
}
