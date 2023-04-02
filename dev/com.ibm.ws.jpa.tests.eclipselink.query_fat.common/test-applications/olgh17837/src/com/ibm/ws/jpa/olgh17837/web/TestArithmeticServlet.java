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

package com.ibm.ws.jpa.olgh17837.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh17837.testlogic.TestArithmeticLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestArithmeticServlet")
public class TestArithmeticServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH17837_DEFAULT_JTA")
    private EntityManager cmts_defaultEm;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH17837_PARTIALBIND_JTA")
    private EntityManager cmts_partialBindEm;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH17837_PARTIALBIND_BINDLITERAL_JTA")
    private EntityManager cmts_partialBind_bindLiteralEm;

    @PostConstruct
    private void initFAT() {
        testClassName = TestArithmeticLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-cmts-default",
                       new JPAPersistenceContext("test-jpa-resource-cmts-default", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_defaultEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBindEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind-bind-literal",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind-bind-literal", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBind_bindLiteralEm"));
    }

    // testABS1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testABS1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testABS1_Default_CMTS_Web";
        final String testMethod = "testABS1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testABS1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testABS1_PartialBind_CMTS_Web";
        final String testMethod = "testABS1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testABS1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testABS1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testABS1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testADD1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testADD1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testADD1_Default_CMTS_Web";
        final String testMethod = "testADD1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testADD1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testADD1_PartialBind_CMTS_Web";
        final String testMethod = "testADD1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testADD1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testADD1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testADD1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testMOD1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testMOD1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMOD1_Default_CMTS_Web";
        final String testMethod = "testMOD1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMOD1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMOD1_PartialBind_CMTS_Web";
        final String testMethod = "testMOD1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMOD1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMOD1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testMOD1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSQRT1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSQRT1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSQRT1_Default_CMTS_Web";
        final String testMethod = "testSQRT1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSQRT1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSQRT1_PartialBind_CMTS_Web";
        final String testMethod = "testSQRT1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSQRT1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSQRT1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSQRT1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSUB1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSUB1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSUB1_Default_CMTS_Web";
        final String testMethod = "testSUB1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSUB1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSUB1_PartialBind_CMTS_Web";
        final String testMethod = "testSUB1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSUB1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSUB1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSUB1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }
}
