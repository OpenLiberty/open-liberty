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

import com.ibm.ws.jpa.olgh17837.testlogic.TestFunctionLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestFunctionServlet")
public class TestFunctionServlet extends JPADBTestServlet {
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
        testClassName = TestFunctionLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-cmts-default",
                       new JPAPersistenceContext("test-jpa-resource-cmts-default", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_defaultEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBindEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind-bind-literal",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind-bind-literal", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBind_bindLiteralEm"));
    }

    // testAll1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAll1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAll1_Default_CMTS_Web";
        final String testMethod = "testAll1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAll1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAll1_PartialBind_CMTS_Web";
        final String testMethod = "testAll1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAll1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAll1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAll1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testAny1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testAny1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAny1_Default_CMTS_Web";
        final String testMethod = "testAny1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAny1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAny1_PartialBind_CMTS_Web";
        final String testMethod = "testAny1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testAny1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testAny1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testAny1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCast1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testCast1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCast1_Default_CMTS_Web";
        final String testMethod = "testCast1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCast1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCast1_PartialBind_CMTS_Web";
        final String testMethod = "testCast1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testCast1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testCast1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCast1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testConcat1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat1_Default_CMTS_Web";
        final String testMethod = "testConcat1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat1_PartialBind_CMTS_Web";
        final String testMethod = "testConcat1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testConcat1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testConcat2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat2_Default_CMTS_Web";
        final String testMethod = "testConcat2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat2_PartialBind_CMTS_Web";
        final String testMethod = "testConcat2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testConcat2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testConcat2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testConcat2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLeftTrim1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim1_Default_CMTS_Web";
        final String testMethod = "testLeftTrim1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim1_PartialBind_CMTS_Web";
        final String testMethod = "testLeftTrim1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLeftTrim1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLeftTrim2_1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim2_1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim2_1_Default_CMTS_Web";
        final String testMethod = "testLeftTrim2_1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim2_1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim2_1_PartialBind_CMTS_Web";
        final String testMethod = "testLeftTrim2_1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLeftTrim2_1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLeftTrim2_1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLeftTrim2_1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLength1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength1_Default_CMTS_Web";
        final String testMethod = "testLength1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength1_PartialBind_CMTS_Web";
        final String testMethod = "testLength1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLength1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLength2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength2_Default_CMTS_Web";
        final String testMethod = "testLength2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength2_PartialBind_CMTS_Web";
        final String testMethod = "testLength2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLength2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLength2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLength2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLocate1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate1_Default_CMTS_Web";
        final String testMethod = "testLocate1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate1_PartialBind_CMTS_Web";
        final String testMethod = "testLocate1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLocate1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLocate2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate2_Default_CMTS_Web";
        final String testMethod = "testLocate2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate2_PartialBind_CMTS_Web";
        final String testMethod = "testLocate2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLocate2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLocate2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLocate2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLower1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testLower1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLower1_Default_CMTS_Web";
        final String testMethod = "testLower1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLower1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLower1_PartialBind_CMTS_Web";
        final String testMethod = "testLower1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testLower1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testLower1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLower1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testRightTrim1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim1_Default_CMTS_Web";
        final String testMethod = "testRightTrim1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim1_PartialBind_CMTS_Web";
        final String testMethod = "testRightTrim1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testRightTrim1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testRightTrim2_1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim2_1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim2_1_Default_CMTS_Web";
        final String testMethod = "testRightTrim2_1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim2_1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim2_1_PartialBind_CMTS_Web";
        final String testMethod = "testRightTrim2_1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testRightTrim2_1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testRightTrim2_1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testRightTrim2_1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSize1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSize1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSize1_Default_CMTS_Web";
        final String testMethod = "testSize1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSize1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSize1_PartialBind_CMTS_Web";
        final String testMethod = "testSize1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSize1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSize1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSize1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSome1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSome1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSome1_Default_CMTS_Web";
        final String testMethod = "testSome1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSome1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSome1_PartialBind_CMTS_Web";
        final String testMethod = "testSome1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSome1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSome1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSome1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSubstring1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring1_Default_CMTS_Web";
        final String testMethod = "testSubstring1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring1_PartialBind_CMTS_Web";
        final String testMethod = "testSubstring1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSubstring1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSubstring2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring2_Default_CMTS_Web";
        final String testMethod = "testSubstring2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring2_PartialBind_CMTS_Web";
        final String testMethod = "testSubstring2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testSubstring2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testSubstring2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSubstring2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testTrim1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim1_Default_CMTS_Web";
        final String testMethod = "testTrim1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim1_PartialBind_CMTS_Web";
        final String testMethod = "testTrim1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testTrim1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testTrim2_1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_1_Default_CMTS_Web";
        final String testMethod = "testTrim2_1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_1_PartialBind_CMTS_Web";
        final String testMethod = "testTrim2_1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testTrim2_1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testTrim2_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_Default_CMTS_Web";
        final String testMethod = "testTrim2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_PartialBind_CMTS_Web";
        final String testMethod = "testTrim2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testTrim2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testTrim2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testTrim2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testUpper1_Default

    @Test
    public void jpa_eclipselink_query_olgh17837_testUpper1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testUpper1_Default_CMTS_Web";
        final String testMethod = "testUpper1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testUpper1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testUpper1_PartialBind_CMTS_Web";
        final String testMethod = "testUpper1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_olgh17837_testUpper1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_olgh17837_testUpper1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testUpper1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }
}
