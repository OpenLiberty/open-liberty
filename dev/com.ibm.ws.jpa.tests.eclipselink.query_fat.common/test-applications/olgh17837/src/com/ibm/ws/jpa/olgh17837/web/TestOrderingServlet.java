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

import com.ibm.ws.jpa.olgh17837.testlogic.TestOrderingLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOrderingServlet")
public class TestOrderingServlet extends JPADBTestServlet {
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
        testClassName = TestOrderingLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-cmts-default",
                       new JPAPersistenceContext("test-jpa-resource-cmts-default", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_defaultEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBindEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind-bind-literal",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind-bind-literal", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBind_bindLiteralEm"));
    }

    // testAscending1_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending1_Default_CMTS_Web";
        final String testMethod = "testAscending1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending1_PartialBind_CMTS_Web";
        final String testMethod = "testAscending1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAscending1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testAscending2_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending2_Default_CMTS_Web";
        final String testMethod = "testAscending2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending2_PartialBind_CMTS_Web";
        final String testMethod = "testAscending2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAscending2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAscending2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAscending2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testDescending1_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending1_Default_CMTS_Web";
        final String testMethod = "testDescending1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending1_PartialBind_CMTS_Web";
        final String testMethod = "testDescending1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testDescending1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testDescending2_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending2_Default_CMTS_Web";
        final String testMethod = "testDescending2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending2_PartialBind_CMTS_Web";
        final String testMethod = "testDescending2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDescending2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDescending2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testDescending2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testOrderBy1_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy1_Default_CMTS_Web";
        final String testMethod = "testOrderBy1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy1_PartialBind_CMTS_Web";
        final String testMethod = "testOrderBy1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testOrderBy1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testOrderBy2_Default

    // TODO: Enable with delivery of https://github.com/OpenLiberty/open-liberty/issues/20118
    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy2_Default_CMTS_Web";
        final String testMethod = "testOrderBy2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy2_PartialBind_CMTS_Web";
        final String testMethod = "testOrderBy2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testOrderBy2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testOrderBy3_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy3_Default_CMTS_Web";
        final String testMethod = "testOrderBy3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy3_PartialBind_CMTS_Web";
        final String testMethod = "testOrderBy3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testOrderBy3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testOrderBy3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testOrderBy3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }
}
