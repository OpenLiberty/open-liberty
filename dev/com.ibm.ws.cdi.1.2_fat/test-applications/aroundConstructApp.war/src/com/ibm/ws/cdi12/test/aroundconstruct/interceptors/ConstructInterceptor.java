/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.aroundconstruct.interceptors;

import static com.ibm.ws.cdi12.test.utils.Utils.id;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger;
import com.ibm.ws.cdi12.test.aroundconstruct.StatelessAroundConstructLogger;
import com.ibm.ws.cdi12.test.aroundconstruct.StatelessEjb;
import com.ibm.ws.cdi12.test.utils.Intercepted;

@Interceptor
@Intercepted
@Priority(Interceptor.Priority.APPLICATION)
public class ConstructInterceptor {

    @Inject
    AroundConstructLogger logger;

    @Inject
    StatelessAroundConstructLogger statelessLogger;

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        //If the stateless bean is being intercepted set this in the stateless logger
        Class<?> declaringClass = context.getConstructor().getDeclaringClass();
        if (id(declaringClass).equals(id(StatelessEjb.class))) {
            statelessLogger.setInterceptedBean(declaringClass);
        }

        logger.setConstructor(context.getConstructor());
        logger.addConstructorInterceptor(this.getClass());
        context.proceed();
        logger.setTarget(context.getTarget());
        return null;
    }
}
