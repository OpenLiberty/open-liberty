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

    //COALESCE
    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_AMJTA_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_AMRL_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_CMTS_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //ABS
    @Test
    public void jpa_spec10_query_olgh8294_testABS_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_AMJTA_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testABS_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_AMRL_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testABS_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_CMTS_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //CONCAT
    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_AMJTA_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_AMRL_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_CMTS_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //EXISTS
    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_AMJTA_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_AMRL_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_CMTS_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //NUMERICAL
    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_AMJTA_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_AMRL_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_CMTS_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //IN
    @Test
    public void jpa_spec10_query_olgh8294_testIN_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_AMJTA_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testIN_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_AMRL_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testIN_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_CMTS_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //LIKE
    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_AMJTA_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_AMRL_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_CMTS_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //SUBSTR
    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_AMJTA_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_AMRL_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_CMTS_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
