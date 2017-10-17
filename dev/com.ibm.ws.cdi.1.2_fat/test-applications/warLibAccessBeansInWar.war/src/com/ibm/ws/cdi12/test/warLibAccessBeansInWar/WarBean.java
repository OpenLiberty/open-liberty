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
package com.ibm.ws.cdi12.test.warLibAccessBeansInWar;

import javax.ejb.LocalBean;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.WarBeanInterface;
import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.WarBeanInterface2;

/**
 *
 */
@LocalBean
@ApplicationScoped
public class WarBean implements WarBeanInterface, WarBeanInterface2 {

    @Override
    public String getBeanMessage() {
        return "WarBean";
    }

}
