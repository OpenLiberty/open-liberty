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
package com.ibm.ws.cdi12.test.ejbsNoBeansXml;

import javax.annotation.ManagedBean;

/**
 *
 */
@ManagedBean
public class ManagedSimpleBean {
    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
