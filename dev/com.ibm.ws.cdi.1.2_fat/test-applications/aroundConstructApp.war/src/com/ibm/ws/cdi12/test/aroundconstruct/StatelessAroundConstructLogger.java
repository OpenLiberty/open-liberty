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

import static com.ibm.ws.cdi12.test.utils.Utils.id;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StatelessAroundConstructLogger {

    private String interceptedBean;

    public void setInterceptedBean(final Class<?> interceptor) {
        interceptedBean = id(interceptor);
    }

    public String getInterceptedBean() {
        return interceptedBean;
    }
}
