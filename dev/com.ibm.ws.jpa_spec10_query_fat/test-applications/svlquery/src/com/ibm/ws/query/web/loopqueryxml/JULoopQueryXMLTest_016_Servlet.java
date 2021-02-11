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
@WebServlet(urlPatterns = "/JULoopQueryXMLTest_016_Servlet")
public class JULoopQueryXMLTest_016_Servlet extends JPATestServlet {
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
    public void jpa_spec10_query_svlquery_juloopquery_xml_test351_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test351_AMJTA_Web";
        final String testMethod = "testLoop351";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test352_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test352_AMJTA_Web";
        final String testMethod = "testLoop352";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test353_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test353_AMJTA_Web";
        final String testMethod = "testLoop353";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test354_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test354_AMJTA_Web";
        final String testMethod = "testLoop354";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test355_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test355_AMJTA_Web";
        final String testMethod = "testLoop355";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test356_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test356_AMJTA_Web";
        final String testMethod = "testLoop356";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test357_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test357_AMJTA_Web";
        final String testMethod = "testLoop357";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test358_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test358_AMJTA_Web";
        final String testMethod = "testLoop358";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test359_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test359_AMJTA_Web";
        final String testMethod = "testLoop359";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test360_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test360_AMJTA_Web";
        final String testMethod = "testLoop360";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test361_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test361_AMJTA_Web";
        final String testMethod = "testLoop361";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test362_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test362_AMJTA_Web";
        final String testMethod = "testLoop362";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test363_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test363_AMJTA_Web";
        final String testMethod = "testLoop363";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test364_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test364_AMJTA_Web";
        final String testMethod = "testLoop364";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test365_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test365_AMJTA_Web";
        final String testMethod = "testLoop366";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test366_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test366_AMJTA_Web";
        final String testMethod = "testLoop366";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test367_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test367_AMJTA_Web";
        final String testMethod = "testLoop367";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test368_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test368_AMJTA_Web";
        final String testMethod = "testLoop368";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test369_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test369_AMJTA_Web";
        final String testMethod = "testLoop369";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test370_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test370_AMJTA_Web";
        final String testMethod = "testLoop370";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test371_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test371_AMJTA_Web";
        final String testMethod = "testLoop371";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test372_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test372_AMJTA_Web";
        final String testMethod = "testLoop372";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test373_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test373_AMJTA_Web";
        final String testMethod = "testLoop373";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: ordering with eclipselink may be different than openjpa
//    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test374_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test374_AMJTA_Web";
        final String testMethod = "testLoop374";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test375_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test375_AMJTA_Web";
        final String testMethod = "testLoop375";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

}
