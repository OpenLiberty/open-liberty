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

package com.ibm.ws.query.web.loopqueryxml;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.query.testlogic.JULoopQueryAnoTest;
import com.ibm.ws.query.utils.SetupQueryTestCase;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JULoopQueryXMLTest_002_Servlet")
public class JULoopQueryXMLTest_002_Servlet extends JPATestServlet {
    // Application Managed JTA
    @PersistenceUnit(unitName = "QUERY_JTA")
    private EntityManagerFactory amjtaEmf;

    private SetupQueryTestCase setup = null;

    @PostConstruct
    private void initFAT() {
        testClassName = JULoopQueryAnoTest.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));

    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test026_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test026_AMJTA_Web";
        final String testMethod = "testLoop026";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test027_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test027_AMJTA_Web";
        final String testMethod = "testLoop027";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test028_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test028_AMJTA_Web";
        final String testMethod = "testLoop028";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test029_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test029_AMJTA_Web";
        final String testMethod = "testLoop029";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test030_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test030_AMJTA_Web";
        final String testMethod = "testLoop030";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test031_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test031_AMJTA_Web";
        final String testMethod = "testLoop031";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test032_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test032_AMJTA_Web";
        final String testMethod = "testLoop032";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test033_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test033_AMJTA_Web";
        final String testMethod = "testLoop033";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test034_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test034_AMJTA_Web";
        final String testMethod = "testLoop034";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test035_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test035_AMJTA_Web";
        final String testMethod = "testLoop035";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test036_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test036_AMJTA_Web";
        final String testMethod = "testLoop036";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test037_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test037_AMJTA_Web";
        final String testMethod = "testLoop037";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test038_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test038_AMJTA_Web";
        final String testMethod = "testLoop038";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: Follow up on test fail
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test039_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test039_AMJTA_Web";
        final String testMethod = "testLoop039";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test040_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test040_AMJTA_Web";
        final String testMethod = "testLoop040";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test041_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test041_AMJTA_Web";
        final String testMethod = "testLoop041";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test042_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test042_AMJTA_Web";
        final String testMethod = "testLoop042";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test043_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test043_AMJTA_Web";
        final String testMethod = "testLoop043";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test044_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test044_AMJTA_Web";
        final String testMethod = "testLoop044";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test045_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test045_AMJTA_Web";
        final String testMethod = "testLoop045";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test046_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test046_AMJTA_Web";
        final String testMethod = "testLoop046";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test047_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test047_AMJTA_Web";
        final String testMethod = "testLoop047";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: Fails on SQL Server
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test048_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test048_AMJTA_Web";
        final String testMethod = "testLoop048";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test049_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test049_AMJTA_Web";
        final String testMethod = "testLoop049";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test050_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test050_AMJTA_Web";
        final String testMethod = "testLoop050";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

}
