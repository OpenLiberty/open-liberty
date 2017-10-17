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

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A bean that will be intercepted
 */
@RequestScoped
@Named("interceptedBean")
@BasicInterceptorBinding
public class InterceptedBean {

    private String lastInterceptedBy = null;

    public void setLastInterceptedBy(String interceptorClassName) {
        this.lastInterceptedBy = interceptorClassName;
    }

    public String getMessage() {
        if (this.lastInterceptedBy == null) {
            return "Not Intercepted";
        }
        return "Last Intercepted by: " + this.lastInterceptedBy;
    }

}
