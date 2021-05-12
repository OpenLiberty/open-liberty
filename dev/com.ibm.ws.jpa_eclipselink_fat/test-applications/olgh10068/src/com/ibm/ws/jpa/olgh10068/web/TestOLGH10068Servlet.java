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

package com.ibm.ws.jpa.olgh10068.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh10068.testlogic.JPATestOLGH10068Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH10068Servlet")
public class TestOLGH10068Servlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH10068_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH10068_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH10068_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH10068Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testCriteriaBuilder_IN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_AMJTA_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_AMRL_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testPlatformDetection_DB2ZOS_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_CMTS_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //testCriteriaBuilder_NOTIN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_AMJTA_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_AMRL_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_CMTS_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //testJPQL_IN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_AMJTA_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_AMRL_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_CMTS_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //testJPQL_NOTIN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_AMJTA_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_AMRL_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_CMTS_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
