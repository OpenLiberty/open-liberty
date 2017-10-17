/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.cdi.test.basic.injection.jar;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppScopedBean {

    private final String msg = "App Scoped Hello World";

    public String getMsg() {
        return msg;
    }
}
