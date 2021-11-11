/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEntityManager_EJB_SFEx_Servlet")
public class TestEntityManager_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EntityManagerLogic.class.getName();
        ejbJNDIName = "ejb/EntityManagerSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityManager_CMEX"));
    }

    // Detach001 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach001_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach001_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach001";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach002 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach002_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach002_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach002";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach003 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach003_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach003_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach003";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach004 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach004_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach004_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach004";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach005 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach005_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach005_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach005";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach006 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach006_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach006_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach006";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach007 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach007_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach007_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach007";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach008 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach008_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach008_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach008";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach009 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach009_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach009_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach009";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Detach010 Test
    @Test
    public void jpa_spec20_entitymanager_testDetach010_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testDetach010_EJB_SFEx_CMEX_Web";
        final String testMethod = "testDetach010";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Find001 Test
    @Test
    public void jpa_spec20_entitymanager_testFind001_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testFind001_EJB_SFEx_CMEX_Web";
        final String testMethod = "testFind001";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Find002 Test
    @Test
    public void jpa_spec20_entitymanager_testFind002_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testFind002_EJB_SFEx_CMEX_Web";
        final String testMethod = "testFind002";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Find003 Test
    @Test
    public void jpa_spec20_entitymanager_testFind003_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec20_entitymanager_testFind003_EJB_SFEx_CMEX_Web";
        final String testMethod = "testFind003";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
