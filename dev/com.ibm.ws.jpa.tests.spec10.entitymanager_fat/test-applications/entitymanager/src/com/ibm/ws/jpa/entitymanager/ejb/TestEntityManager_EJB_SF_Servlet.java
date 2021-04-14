/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.entitymanager.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.entitymanager.testlogic.EntityManagerLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEntityManager_EJB_SF_Servlet")
public class TestEntityManager_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EntityManagerLogic.class.getName();
        ejbJNDIName = "ejb/EntityManagerSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityManager_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityManager_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityManager_CMTS"));
    }

    // Remove001 Test
    @Test
    public void jpa_spec10_entitymanager_testRemove001_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_EJB_SF_AMJTA_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_entitymanager_testRemove001_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_EJB_SF_AMRL_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_entitymanager_testRemove001_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_EJB_SF_CMTS_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Remove002 Test
    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_EJB_SF_AMJTA_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_EJB_SF_AMRL_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_EJB_SF_CMTS_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
