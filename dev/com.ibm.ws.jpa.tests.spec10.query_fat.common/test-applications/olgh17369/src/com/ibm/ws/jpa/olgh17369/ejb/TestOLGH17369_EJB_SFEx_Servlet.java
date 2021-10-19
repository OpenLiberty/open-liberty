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

package com.ibm.ws.jpa.olgh17369.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh17369.testlogic.JPATestOLGH17369Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH17369_EJB_SFEx_Servlet")
public class TestOLGH17369_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH17369Logic.class.getName();
        ejbJNDIName = "ejb/OLGH17369SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH17369_CMEX"));
    }

    // testQueryCaseLiterals1
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals1_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals1_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals1";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters1
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters1_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters1_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseParameters1";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals2
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals2_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals2_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals2";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters2
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters2_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters2_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseParameters2";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals3
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals3_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals3_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals3";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters3
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters3_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters3_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseParameters3";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseLiterals4
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseLiterals4_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseLiterals4_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseLiterals4";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryCaseParameters1
    @Test
    public void jpa_spec10_query_olgh17369_testQueryCaseParameters4_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17369_testQueryCaseParameters4_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryCaseParameters4";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
