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
package com.ibm.ws.clientcontainer.fat;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class HelloBean {

    public String getHello() {
        return "Bean hello!";
    }
}
