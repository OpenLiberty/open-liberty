/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh17369.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh17369.testlogic.JPATestOLGH17369Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH17369Servlet")
public class TestOLGH17369Servlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH17369_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH17369_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH17369_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH17369Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testQueryCaseLiterals1
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals1_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals1_AMJTA_Web";
        final String testMethod = "testQueryCaseLiterals1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals1_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals1_AMRL_Web";
        final String testMethod = "testQueryCaseLiterals1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals1_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals1_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters1
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters1_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters1_AMJTA_Web";
        final String testMethod = "testQueryCaseParameters1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters1_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters1_AMRL_Web";
        final String testMethod = "testQueryCaseParameters1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters1_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters1_CMTS_Web";
        final String testMethod = "testQueryCaseParameters1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals2
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals2_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals2_AMJTA_Web";
        final String testMethod = "testQueryCaseLiterals2";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals2_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals2_AMRL_Web";
        final String testMethod = "testQueryCaseLiterals2";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals2_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals2_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals2";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters2
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters2_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters2_AMJTA_Web";
        final String testMethod = "testQueryCaseParameters2";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters2_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters2_AMRL_Web";
        final String testMethod = "testQueryCaseParameters2";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters2_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters2_CMTS_Web";
        final String testMethod = "testQueryCaseParameters2";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals3
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals3_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals3_AMJTA_Web";
        final String testMethod = "testQueryCaseLiterals3";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals3_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals3_AMRL_Web";
        final String testMethod = "testQueryCaseLiterals3";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals3_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals3_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals3";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters3
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters3_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters3_AMJTA_Web";
        final String testMethod = "testQueryCaseParameters3";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters3_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters3_AMRL_Web";
        final String testMethod = "testQueryCaseParameters3";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters3_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters3_CMTS_Web";
        final String testMethod = "testQueryCaseParameters3";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals4
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals4_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals4_AMJTA_Web";
        final String testMethod = "testQueryCaseLiterals4";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals4_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals4_AMRL_Web";
        final String testMethod = "testQueryCaseLiterals4";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals4_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals4_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals4";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters4
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters4_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters4_AMJTA_Web";
        final String testMethod = "testQueryCaseParameters4";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters4_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters4_AMRL_Web";
        final String testMethod = "testQueryCaseParameters4";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters4_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters4_CMTS_Web";
        final String testMethod = "testQueryCaseParameters4";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
