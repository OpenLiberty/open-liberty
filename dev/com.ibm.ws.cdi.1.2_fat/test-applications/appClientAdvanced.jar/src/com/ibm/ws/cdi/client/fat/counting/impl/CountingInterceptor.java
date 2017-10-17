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
package com.ibm.ws.cdi.client.fat.counting.impl;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.client.fat.counting.CountBean;
import com.ibm.ws.cdi.client.fat.counting.Counted;

/**
 *
 */
@Interceptor
@Counted
@Priority(APPLICATION)
public class CountingInterceptor {

    @Inject
    private CountBean counter;

    @AroundInvoke
    public Object methodCalled(InvocationContext context) throws Exception {
        Object result;
        try {
            result = context.proceed();
        } finally {
            counter.add(1);
        }
        return result;
    }

}
