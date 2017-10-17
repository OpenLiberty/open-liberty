/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Notifies the intercepted beans that they were intercepted
 */
@BasicInterceptorBinding
@Interceptor
public class BasicInterceptor {

    @AroundInvoke
    public Object notifyInterception(InvocationContext invocationContext) throws Exception {
        Object target = invocationContext.getTarget();
        if (target instanceof InterceptedBean) {
            InterceptedBean bean = (InterceptedBean) target;
            bean.setLastInterceptedBy(this.getClass().getSimpleName());
        }
        return invocationContext.proceed();
    }

}
