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
package com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 *
 */
@Dependent
public class TestInjectionClass {

    @Inject
    WarBeanInterface bean;

    public String getMessage() {

        return ("TestInjectionClass: " + bean.getBeanMessage());
    }

}
