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
package com.ibm.ws.cdi12.test.errormessage.interceptors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@ErrorMessageInterceptorBinding
public class ErrorMessageInterceptor {

    @PostConstruct
    public void postConstructMethodInterceptor(InvocationContext ctx) {
        throw new IllegalStateException();
    }
}
