/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.jsf23.fat.spec1433;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A simple RequestScoped bean.
 *
 */
@Named
@RequestScoped
public class TestBean {
    private String valueOne;
    private String valueTwo;

    public void setValueOne(String valueOne) {
        this.valueOne = valueOne;
    }

    public String getValueOne() {
        return this.valueOne;
    }

    public void setValueTwo(String valueTwo) {
        this.valueTwo = valueTwo;
    }

    public String getValueTwo() {
        return this.valueTwo;
    }
}
