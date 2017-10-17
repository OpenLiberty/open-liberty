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
package com.ibm.ws.cdi12.aftertypediscovery.test;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class InterceptedBean {

    @InterceptedAfterType
    public InterceptedBean() {
        System.out.println("InterceptedBean.InterceptedBean()");
    }

    public void doNothing() {
        System.out.println("InterceptedBean.doNothing()");
    }
}
