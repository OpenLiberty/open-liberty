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
package com.ibm.ws.cdi12.test.ejb.scope;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedBean {

    public void doNothing() {
        // Do nothing!
        // Just used to test we can call a request scoped bean
    }

}
