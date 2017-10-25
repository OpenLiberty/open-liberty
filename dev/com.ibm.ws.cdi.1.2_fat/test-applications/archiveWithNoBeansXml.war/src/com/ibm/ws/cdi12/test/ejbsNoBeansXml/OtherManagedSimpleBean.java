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
import javax.enterprise.context.Dependent;

/**
 *
 */
@ManagedBean
@Dependent
public class OtherManagedSimpleBean {

    private String value;

    public void setOtherValue(String value) {
        this.value = value;
    }

    public String getOtherValue() {
        return this.value;
    }
}
