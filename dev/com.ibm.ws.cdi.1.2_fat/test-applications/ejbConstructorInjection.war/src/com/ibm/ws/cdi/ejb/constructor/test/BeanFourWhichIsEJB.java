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

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 */
@Stateless
@LocalBean
@MyForthQualifier
public class BeanFourWhichIsEJB implements Iface {

    private final String msg = "eggs";

    @Override
    public String getMsg() {
        return msg;
    }

}
