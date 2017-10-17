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
package cdi12.helloworld.jeeResources.ejb.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * My ejb interceptor
 */
public class MyAnotherEJBInterceptor {
    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {

        return context.proceed();
    }
}
