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
package com.ibm.ws.cdi12.test.common.web;

import javax.enterprise.context.Dependent;

/**
 *
 */
@Dependent
public class SimpleBean {

    public String test() {
        return "bean exists";
    }

}
