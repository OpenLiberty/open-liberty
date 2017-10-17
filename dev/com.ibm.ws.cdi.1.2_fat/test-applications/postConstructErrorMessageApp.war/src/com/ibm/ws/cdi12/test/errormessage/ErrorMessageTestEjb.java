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
package com.ibm.ws.cdi12.test.errormessage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;

import com.ibm.ws.cdi12.test.errormessage.interceptors.ErrorMessageInterceptorBinding;

@Stateless
@ErrorMessageInterceptorBinding
public class ErrorMessageTestEjb {
    public ErrorMessageTestEjb() {} // necessary to be proxyable

    public void doSomething() {}

    @PostConstruct
    public void postConstructMethod() {}
}
