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

package com.ibm.ws.jpa.olgh8014.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh8014.testlogic.JPATestOLGH8014Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH8014Servlet")
public class TestOLGH8014Servlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH8014_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH8014_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH8014_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8014Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testEmptyAggregateFunctionsWithPrimitives
    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithPrimitives_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmptyAggregateFunctionsWithWrappers
    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testEmptyAggregateFunctionsWithWrappers_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testAggregateFunctionsWithPrimitives
    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithPrimitives_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithPrimitives_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithPrimitives_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithPrimitives_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithPrimitives_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithPrimitives_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testAggregateFunctionsWithWrappers
    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithWrappers_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithWrappers_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithWrappers_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithWrappers_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8014_testAggregateFunctionsWithWrappers_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8014_testAggregateFunctionsWithWrappers_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
