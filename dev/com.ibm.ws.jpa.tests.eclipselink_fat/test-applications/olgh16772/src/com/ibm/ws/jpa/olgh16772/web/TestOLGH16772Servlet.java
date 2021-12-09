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

package com.ibm.ws.jpa.olgh16772.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16772.testlogic.JPATestOLGH16772Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16772Servlet")
public class TestOLGH16772Servlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH16772_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH16772_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH16772_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16772Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testWhereTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereTrim_AMJTA_Web";
        final String testMethod = "testWhereTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereTrim_AMRL_Web";
        final String testMethod = "testWhereTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereTrim_CMTS_Web";
        final String testMethod = "testWhereTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testWhereLeftTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereLeftTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereLeftTrim_AMJTA_Web";
        final String testMethod = "testWhereLeftTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereLeftTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereLeftTrim_AMRL_Web";
        final String testMethod = "testWhereLeftTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereLeftTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereLeftTrim_CMTS_Web";
        final String testMethod = "testWhereLeftTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testWhereRightTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereRightTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereRightTrim_AMJTA_Web";
        final String testMethod = "testWhereRightTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereRightTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereRightTrim_AMRL_Web";
        final String testMethod = "testWhereRightTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testWhereRightTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereRightTrim_CMTS_Web";
        final String testMethod = "testWhereRightTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectTrim_AMJTA_Web";
        final String testMethod = "testSelectTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectTrim_AMRL_Web";
        final String testMethod = "testSelectTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectTrim_CMTS_Web";
        final String testMethod = "testSelectTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectLeftTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectLeftTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectLeftTrim_AMJTA_Web";
        final String testMethod = "testSelectLeftTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectLeftTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectLeftTrim_AMRL_Web";
        final String testMethod = "testSelectLeftTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectLeftTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectLeftTrim_CMTS_Web";
        final String testMethod = "testSelectLeftTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectRightTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectRightTrim_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectRightTrim_AMJTA_Web";
        final String testMethod = "testSelectRightTrim";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectRightTrim_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectRightTrim_AMRL_Web";
        final String testMethod = "testSelectRightTrim";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16772_testSelectRightTrim_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectRightTrim_CMTS_Web";
        final String testMethod = "testSelectRightTrim";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

}
