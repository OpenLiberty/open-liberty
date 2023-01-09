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

import com.ibm.ws.jpa.olgh17837.testlogic.TestAggregateLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestAggregateServlet")
public class TestAggregateServlet extends JPADBTestServlet {
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
        testClassName = TestAggregateLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-cmts-default",
                       new JPAPersistenceContext("test-jpa-resource-cmts-default", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_defaultEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBindEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind-bind-literal",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind-bind-literal", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBind_bindLiteralEm"));
    }

    // testAvg1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg1_Default_CMTS_Web";
        final String testMethod = "testAvg1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg1_PartialBind_CMTS_Web";
        final String testMethod = "testAvg1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAvg1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testAvg2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg2_Default_CMTS_Web";
        final String testMethod = "testAvg2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg2_PartialBind_CMTS_Web";
        final String testMethod = "testAvg2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvg2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvg2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAvg2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testAvgDistinct1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct1_Default_CMTS_Web";
        final String testMethod = "testAvgDistinct1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct1_PartialBind_CMTS_Web";
        final String testMethod = "testAvgDistinct1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAvgDistinct1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testAvgDistinct2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct2_Default_CMTS_Web";
        final String testMethod = "testAvgDistinct2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct2_PartialBind_CMTS_Web";
        final String testMethod = "testAvgDistinct2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAvgDistinct2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAvgDistinct2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAvgDistinct2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCount1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount1_Default_CMTS_Web";
        final String testMethod = "testCount1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount1_PartialBind_CMTS_Web";
        final String testMethod = "testCount1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCount1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCount2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount2_Default_CMTS_Web";
        final String testMethod = "testCount2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount2_PartialBind_CMTS_Web";
        final String testMethod = "testCount2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCount2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCount2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCount2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCountDistinct1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct1_Default_CMTS_Web";
        final String testMethod = "testCountDistinct1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct1_PartialBind_CMTS_Web";
        final String testMethod = "testCountDistinct1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCountDistinct1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCountDistinct2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct2_Default_CMTS_Web";
        final String testMethod = "testCountDistinct2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct2_PartialBind_CMTS_Web";
        final String testMethod = "testCountDistinct2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCountDistinct2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCountDistinct2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCountDistinct2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testDistinct1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testDistinct1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDistinct1_Default_CMTS_Web";
        final String testMethod = "testDistinct1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDistinct1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDistinct1_PartialBind_CMTS_Web";
        final String testMethod = "testDistinct1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testDistinct1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testDistinct1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testDistinct1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testMax1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax1_Default_CMTS_Web";
        final String testMethod = "testMax1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax1_PartialBind_CMTS_Web";
        final String testMethod = "testMax1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testMax1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testMax2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax2_Default_CMTS_Web";
        final String testMethod = "testMax2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax2_PartialBind_CMTS_Web";
        final String testMethod = "testMax2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMax2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMax2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testMax2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testMin1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin1_Default_CMTS_Web";
        final String testMethod = "testMin1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin1_PartialBind_CMTS_Web";
        final String testMethod = "testMin1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testMin1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testMin2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin2_Default_CMTS_Web";
        final String testMethod = "testMin2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin2_PartialBind_CMTS_Web";
        final String testMethod = "testMin2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testMin2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testMin2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testMin2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSum1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum1_Default_CMTS_Web";
        final String testMethod = "testSum1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum1_PartialBind_CMTS_Web";
        final String testMethod = "testSum1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSum1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSum2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum2_Default_CMTS_Web";
        final String testMethod = "testSum2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum2_PartialBind_CMTS_Web";
        final String testMethod = "testSum2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSum2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSum2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSum2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }
}
