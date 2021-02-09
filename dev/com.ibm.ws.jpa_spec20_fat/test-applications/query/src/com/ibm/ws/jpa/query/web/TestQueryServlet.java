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

package com.ibm.ws.jpa.query.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.query.testlogic.QueryLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestQueryServlet")
public class TestQueryServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "QUERY_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "QUERY_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "QUERY_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = QueryLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // Detach001 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_AMJTA_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_AMRL_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_CMTS_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Detach002 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_AMJTA_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_AMRL_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_CMTS_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Detach003 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_AMJTA_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_AMRL_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_CMTS_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
