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
package com.ibm.ws.cdi12.test.priority.helpers;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi12.test.utils.ChainableList;
import com.ibm.ws.cdi12.test.utils.Utils;

public abstract class AbstractInterceptor {
    private final String name;

    public AbstractInterceptor(final Class<?> subclass) {
        this.name = Utils.id(subclass);
    }

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        return ((ChainableList<String>) context.proceed()).chainAdd(name);
    }
}
