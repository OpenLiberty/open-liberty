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
@WebServlet(urlPatterns = "/JULoopQueryXMLTest_006_Servlet")
public class JULoopQueryXMLTest_006_Servlet extends JPATestServlet {
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
    public void jpa_spec10_query_svlquery_juloopquery_xml_test126_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test126_AMJTA_Web";
        final String testMethod = "testLoop126";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test127_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test127_AMJTA_Web";
        final String testMethod = "testLoop127";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test128_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test128_AMJTA_Web";
        final String testMethod = "testLoop128";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test129_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test129_AMJTA_Web";
        final String testMethod = "testLoop129";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test130_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test130_AMJTA_Web";
        final String testMethod = "testLoop130";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test131_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test131_AMJTA_Web";
        final String testMethod = "testLoop131";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test132_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test132_AMJTA_Web";
        final String testMethod = "testLoop132";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test133_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test133_AMJTA_Web";
        final String testMethod = "testLoop133";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test134_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test134_AMJTA_Web";
        final String testMethod = "testLoop134";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test135_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test135_AMJTA_Web";
        final String testMethod = "testLoop135";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test136_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test136_AMJTA_Web";
        final String testMethod = "testLoop136";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // Potentially a bug in eclipselink
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test137_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test137_AMJTA_Web";
        final String testMethod = "testLoop137";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // Potentially a bug in eclipselink
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test138_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test138_AMJTA_Web";
        final String testMethod = "testLoop138";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test139_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test139_AMJTA_Web";
        final String testMethod = "testLoop139";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test140_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test140_AMJTA_Web";
        final String testMethod = "testLoop140";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test141_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test141_AMJTA_Web";
        final String testMethod = "testLoop141";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test142_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test142_AMJTA_Web";
        final String testMethod = "testLoop142";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test143_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test143_AMJTA_Web";
        final String testMethod = "testLoop143";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test144_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test144_AMJTA_Web";
        final String testMethod = "testLoop144";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test145_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test145_AMJTA_Web";
        final String testMethod = "testLoop145";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test146_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test146_AMJTA_Web";
        final String testMethod = "testLoop146";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test147_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test147_AMJTA_Web";
        final String testMethod = "testLoop147";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test148_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test148_AMJTA_Web";
        final String testMethod = "testLoop148";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    // Commented out, eclipselink doesn't produce the null row entry
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test149_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test149_AMJTA_Web";
        final String testMethod = "testLoop149";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_xml_test150_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_xml_test150_AMJTA_Web";
        final String testMethod = "testLoop150";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

}
