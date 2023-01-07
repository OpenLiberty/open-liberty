/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.defaultproperties.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.defaultproperties.testlogic.DefaultPropertiesLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestDefaultProperties_EJB_SL_Servlet")
public class TestDefaultProperties_EJB_SL_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = DefaultPropertiesLogic.class.getName();
        ejbJNDIName = "ejb/DefaultPropertiesSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/DefaultProperties_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/DefaultProperties_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/DefaultProperties_CMTS"));
    }

    // testDefaultProperties Test
    @Test
    public void jpa_jpaconfig_testDefaultProperties_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_jpaconfig_testDefaultProperties_EJB_SL_AMJTA_Web";
        final String testMethod = "testDefaultProperties";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpaconfig_testDefaultProperties_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_jpaconfig_testDefaultProperties_EJB_SL_AMRL_Web";
        final String testMethod = "testDefaultProperties";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpaconfig_testDefaultProperties_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_jpaconfig_testDefaultProperties_EJB_SL_CMTS_Web";
        final String testMethod = "testDefaultProperties";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
