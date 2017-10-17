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
package com.ibm.ws.cdi12.test.aroundconstruct;

import static com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger.ConstructorType.INJECTED;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectlyIntercepted;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOneBinding;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwoBinding;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.NonCdiInterceptor;
import com.ibm.ws.cdi12.test.utils.Intercepted;

@Stateful
@RequestScoped
@Intercepted
@InterceptorOneBinding
@InterceptorTwoBinding
@Interceptors({ NonCdiInterceptor.class })
public class Ejb {
    public Ejb() {} // necessary to be proxyable

    @DirectlyIntercepted
    @Inject
    public Ejb(AroundConstructLogger logger) {
        logger.setConstructorType(INJECTED);
    }

    public void doSomething() {}
}
