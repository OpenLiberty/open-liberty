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
package com.ibm.ws.cdi12.test.beanDiscoveryModeNone;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class TestBean1 {

    public String test() {
        return "TestBean1";
    }
}
