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

package com.ibm.ws.jpa.olgh16588.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16588.testlogic.JPATestOLGH16588Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16588Servlet")
public class TestOLGH16588Servlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH16588_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH16588_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH16588_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16588Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testJPQLAggregateCollection
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLAggregateCollection_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLAggregateCollection_AMJTA_Web";
        final String testMethod = "testJPQLAggregateCollection";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLAggregateCollection_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLAggregateCollection_AMRL_Web";
        final String testMethod = "testJPQLAggregateCollection";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLAggregateCollection_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLAggregateCollection_CMTS_Web";
        final String testMethod = "testJPQLAggregateCollection";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQLJoin
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLJoin_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLJoin_AMJTA_Web";
        final String testMethod = "testJPQLJoin";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLJoin_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLJoin_AMRL_Web";
        final String testMethod = "testJPQLJoin";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLJoin_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLJoin_CMTS_Web";
        final String testMethod = "testJPQLJoin";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQLNestedEmbeddable
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_AMJTA_Web";
        final String testMethod = "testJPQLNestedEmbeddable";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_AMRL_Web";
        final String testMethod = "testJPQLNestedEmbeddable";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_CMTS_Web";
        final String testMethod = "testJPQLNestedEmbeddable";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
