/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.annotatedBeansXML;

import javax.enterprise.context.Dependent;

@Dependent
public class DependentScopedBean {

    private String data;

    public void setData(String data) {
        this.data = data;
    }

}
