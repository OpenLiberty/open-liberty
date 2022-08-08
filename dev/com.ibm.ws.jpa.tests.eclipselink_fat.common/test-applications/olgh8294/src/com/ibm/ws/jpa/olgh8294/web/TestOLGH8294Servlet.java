/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh8294.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh8294.testlogic.JPATestOLGH8294Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH8294Servlet")
public class TestOLGH8294Servlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH8294_PROPERTY_JTA")
    private EntityManager cmtsEmProperty;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH8294_PROPERTY_JTA")
    private EntityManagerFactory amjtaEmfProperty;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH8294_PROPERTY_RL")
    private EntityManagerFactory amrlEmfProperty;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8294Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmfProperty"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmfProperty"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEmProperty"));
    }

    @Test
    public void jpa_eclipselink_testCOALESCE_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_AMJTA_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCOALESCE_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_AMRL_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCOALESCE_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_CMTS_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_AMJTA_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_AMRL_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_CMTS_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_AMJTA_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_AMRL_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_CMTS_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_AMJTA_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_AMRL_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_CMTS_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_AMJTA_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_AMRL_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_CMTS_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_AMJTA_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_AMRL_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_CMTS_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_AMJTA_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_AMRL_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_CMTS_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_AMJTA_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_AMRL_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_CMTS_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
