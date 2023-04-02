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

package com.ibm.ws.jpa.jpa31.web;

import org.junit.Test;

import com.ibm.ws.jpa.jpa31.testlogic.TestMathLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestMathServlet")
public class TestMathServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "JPA31_DEFAULT_JTA")
    private EntityManager cmts_defaultEm;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "JPA31_PARTIALBIND_JTA")
    private EntityManager cmts_partialBindEm;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "JPA31_PARTIALBIND_BINDLITERAL_JTA")
    private EntityManager cmts_partialBind_bindLiteralEm;

    @PostConstruct
    private void initFAT() {
        testClassName = TestMathLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-cmts-default",
                       new JPAPersistenceContext("test-jpa-resource-cmts-default", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_defaultEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBindEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-partial-bind-bind-literal",
                       new JPAPersistenceContext("test-jpa-resource-cmts-partial-bind-bind-literal", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmts_partialBind_bindLiteralEm"));
    }

    // testCEILING1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING1_Default_CMTS_Web";
        final String testMethod = "testCEILING1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING1_PartialBind_CMTS_Web";
        final String testMethod = "testCEILING1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCEILING1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCEILING2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING2_Default_CMTS_Web";
        final String testMethod = "testCEILING2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING2_PartialBind_CMTS_Web";
        final String testMethod = "testCEILING2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCEILING2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testCEILING3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING3_Default_CMTS_Web";
        final String testMethod = "testCEILING3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING3_PartialBind_CMTS_Web";
        final String testMethod = "testCEILING3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testCEILING3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testCEILING3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testCEILING3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testFLOOR1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR1_Default_CMTS_Web";
        final String testMethod = "testFLOOR1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR1_PartialBind_CMTS_Web";
        final String testMethod = "testFLOOR1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testFLOOR1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testFLOOR2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR2_Default_CMTS_Web";
        final String testMethod = "testFLOOR2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR2_PartialBind_CMTS_Web";
        final String testMethod = "testFLOOR2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testFLOOR2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testFLOOR3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR3_Default_CMTS_Web";
        final String testMethod = "testFLOOR3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR3_PartialBind_CMTS_Web";
        final String testMethod = "testFLOOR3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testFLOOR3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testFLOOR3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testFLOOR3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testEXP1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP1_Default_CMTS_Web";
        final String testMethod = "testEXP1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP1_PartialBind_CMTS_Web";
        final String testMethod = "testEXP1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testEXP1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testEXP2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP2_Default_CMTS_Web";
        final String testMethod = "testEXP2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP2_PartialBind_CMTS_Web";
        final String testMethod = "testEXP2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testEXP2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testEXP3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP3_Default_CMTS_Web";
        final String testMethod = "testEXP3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP3_PartialBind_CMTS_Web";
        final String testMethod = "testEXP3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testEXP3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testEXP3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testEXP3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLN1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testLN1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN1_Default_CMTS_Web";
        final String testMethod = "testLN1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN1_PartialBind_CMTS_Web";
        final String testMethod = "testLN1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLN1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLN2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testLN2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN2_Default_CMTS_Web";
        final String testMethod = "testLN2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN2_PartialBind_CMTS_Web";
        final String testMethod = "testLN2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLN2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testLN3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testLN3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN3_Default_CMTS_Web";
        final String testMethod = "testLN3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN3_PartialBind_CMTS_Web";
        final String testMethod = "testLN3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testLN3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testLN3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testLN3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testPOWER1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER1_Default_CMTS_Web";
        final String testMethod = "testPOWER1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER1_PartialBind_CMTS_Web";
        final String testMethod = "testPOWER1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testPOWER1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testPOWER2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER2_Default_CMTS_Web";
        final String testMethod = "testPOWER2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER2_PartialBind_CMTS_Web";
        final String testMethod = "testPOWER2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testPOWER2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testPOWER3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER3_Default_CMTS_Web";
        final String testMethod = "testPOWER3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER3_PartialBind_CMTS_Web";
        final String testMethod = "testPOWER3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testPOWER3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testPOWER3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testPOWER3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testROUND1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND1_Default_CMTS_Web";
        final String testMethod = "testROUND1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND1_PartialBind_CMTS_Web";
        final String testMethod = "testROUND1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testROUND1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testROUND2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND2_Default_CMTS_Web";
        final String testMethod = "testROUND2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND2_PartialBind_CMTS_Web";
        final String testMethod = "testROUND2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testROUND2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testROUND3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND3_Default_CMTS_Web";
        final String testMethod = "testROUND3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND3_PartialBind_CMTS_Web";
        final String testMethod = "testROUND3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testROUND3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testROUND3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testROUND3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSIGN1_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN1_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN1_Default_CMTS_Web";
        final String testMethod = "testSIGN1_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN1_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN1_PartialBind_CMTS_Web";
        final String testMethod = "testSIGN1_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN1_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN1_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSIGN1_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSIGN2_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN2_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN2_Default_CMTS_Web";
        final String testMethod = "testSIGN2_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN2_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN2_PartialBind_CMTS_Web";
        final String testMethod = "testSIGN2_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN2_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN2_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSIGN2_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }

    // testSIGN3_Default

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN3_Default_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN3_Default_CMTS_Web";
        final String testMethod = "testSIGN3_Default";
        final String testResource = "test-jpa-resource-cmts-default";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN3_PartialBind_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN3_PartialBind_CMTS_Web";
        final String testMethod = "testSIGN3_PartialBind";
        final String testResource = "test-jpa-resource-cmts-partial-bind";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_query_jpa31_testSIGN3_PartialBind_BindLiteral_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_query_jpa31_testSIGN3_PartialBind_BindLiteral_CMTS_Web";
        final String testMethod = "testSIGN3_PartialBind_BindLiteral";
        final String testResource = "test-jpa-resource-cmts-partial-bind-bind-literal";

        executeTest(testName, testMethod, testResource);
    }
}
