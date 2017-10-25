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
package com.ibm.ws.cdi12.test.lib3;

@CustomNormalScoped
public class BasicBean3A {

    String data;

    /**
     * @param string
     */
    public void setData(String data) {
        this.data = data;
    }

}
