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
package com.ibm.ws.cdi.client.security.fat;

import javax.enterprise.context.ApplicationScoped;

/**
 * A bean with hard-coded test credentials.
 */
@ApplicationScoped
public class TestCredentialBean {

    public String getUsername() {
        return "testUser";
    }

    public char[] getPassword() {
        return "testPass".toCharArray();
    }
}
