/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh16970.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16970.testlogic.JPATestOLGH16970Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16970Servlet")
public class TestOLGH16970Servlet extends JPADBTestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH16970_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH16970_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH16970_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16970Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testSimpleQuery1
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery1_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery1_AMJTA_Web";
        final String testMethod = "testSimpleQuery1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery1_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery1_AMRL_Web";
        final String testMethod = "testSimpleQuery1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery1_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery1_CMTS_Web";
        final String testMethod = "testSimpleQuery1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testSimpleQuery2
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery2_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery2_AMJTA_Web";
        final String testMethod = "testSimpleQuery2";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery2_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery2_AMRL_Web";
        final String testMethod = "testSimpleQuery2";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery2_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery2_CMTS_Web";
        final String testMethod = "testSimpleQuery2";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testSimpleQuery3
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery3_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery3_AMJTA_Web";
        final String testMethod = "testSimpleQuery3";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery3_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery3_AMRL_Web";
        final String testMethod = "testSimpleQuery3";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery3_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery3_CMTS_Web";
        final String testMethod = "testSimpleQuery3";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
