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
package com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class SimpleEjbBean2 {

    private String message2;

    public void setMessage2(String message) {
        this.message2 = message;
    }

    public String getMessage2() {
        return this.message2;
    }

}
