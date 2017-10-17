/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi.services.impl;

import javax.enterprise.context.Dependent;

/**
 * This class only needs to be here to make sure this is a BDA with a BeanManager
 */
@Dependent
public class MyPojoUser {

    public String getUser() {
        String s = "DefaultPojoUser";
        return s;
    }
}
