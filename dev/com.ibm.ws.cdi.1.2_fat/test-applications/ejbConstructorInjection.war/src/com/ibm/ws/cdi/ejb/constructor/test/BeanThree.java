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

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

/**
 *
 */
@Dependent
@MyThirdQualifier
public class BeanThree {

    private final String msg = "spam";

    public String getMsg() {
        return msg;
    }

    @PreDestroy
    public void destroy() {
        StaticState.append("destroy called");
    }
}
