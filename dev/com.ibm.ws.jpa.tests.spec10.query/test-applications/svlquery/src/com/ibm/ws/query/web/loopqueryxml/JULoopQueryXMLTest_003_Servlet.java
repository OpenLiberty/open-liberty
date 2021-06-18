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
@WebServlet(urlPatterns = "/JULoopQueryXMLTest_003_Servlet")
public class JULoopQueryXMLTest_003_Servlet extends JPATestServlet {
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
    public void jpa_spec10_query_svlquery_juloopquery_xml_test051_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test051_AMJTA_Web";
        final String testMethod = "testLoop051";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test052_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test052_AMJTA_Web";
        final String testMethod = "testLoop052";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test053_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test053_AMJTA_Web";
        final String testMethod = "testLoop053";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test054_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test054_AMJTA_Web";
        final String testMethod = "testLoop054";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test055_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test055_AMJTA_Web";
        final String testMethod = "testLoop055";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test056_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test056_AMJTA_Web";
        final String testMethod = "testLoop056";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test057_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test057_AMJTA_Web";
        final String testMethod = "testLoop057";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test058_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test058_AMJTA_Web";
        final String testMethod = "testLoop058";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test059_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test059_AMJTA_Web";
        final String testMethod = "testLoop059";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test060_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test060_AMJTA_Web";
        final String testMethod = "testLoop060";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test061_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test061_AMJTA_Web";
        final String testMethod = "testLoop061";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test062_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test062_AMJTA_Web";
        final String testMethod = "testLoop062";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test063_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test063_AMJTA_Web";
        final String testMethod = "testLoop063";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test064_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test064_AMJTA_Web";
        final String testMethod = "testLoop064";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test065_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test065_AMJTA_Web";
        final String testMethod = "testLoop065";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test066_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test066_AMJTA_Web";
        final String testMethod = "testLoop066";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test067_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test067_AMJTA_Web";
        final String testMethod = "testLoop067";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test068_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test068_AMJTA_Web";
        final String testMethod = "testLoop068";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test069_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test069_AMJTA_Web";
        final String testMethod = "testLoop069";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test070_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test070_AMJTA_Web";
        final String testMethod = "testLoop070";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test071_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test071_AMJTA_Web";
        final String testMethod = "testLoop071";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test072_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test072_AMJTA_Web";
        final String testMethod = "testLoop072";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test073_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test073_AMJTA_Web";
        final String testMethod = "testLoop073";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test074_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test074_AMJTA_Web";
        final String testMethod = "testLoop074";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test075_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test075_AMJTA_Web";
        final String testMethod = "testLoop075";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

}
