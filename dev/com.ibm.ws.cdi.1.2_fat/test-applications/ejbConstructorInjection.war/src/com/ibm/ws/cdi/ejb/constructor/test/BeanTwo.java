package com.ibm.ws.cdi.ejb.constructor.test;

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

import javax.enterprise.context.RequestScoped;

/**
 *
 */
@RequestScoped
@MySecondQualifier
public class BeanTwo implements Iface {

    private final String msg = "bar";

    @Override
    public String getMsg() {
        return msg;
    }

}
