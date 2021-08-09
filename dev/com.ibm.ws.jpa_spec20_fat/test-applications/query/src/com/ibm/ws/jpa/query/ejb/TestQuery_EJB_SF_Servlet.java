/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.query.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.query.testlogic.QueryLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestQuery_EJB_SF_Servlet")
public class TestQuery_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = QueryLogic.class.getName();
        ejbJNDIName = "ejb/QuerySFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Query_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Query_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Query_CMTS"));
    }

    // Detach001 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_EJB_SF_AMJTA_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_EJB_SF_AMRL_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap001_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap001_EJB_SF_CMTS_Web";
        final String testMethod = "testUnwrap001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Detach002 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_EJB_SF_AMJTA_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_EJB_SF_AMRL_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap002_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap002_EJB_SF_CMTS_Web";
        final String testMethod = "testUnwrap002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Detach003 Test
    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_EJB_SF_AMJTA_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_EJB_SF_AMRL_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.persistence.PersistenceException")
    public void jpa_spec20_query_testUnwrap003_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec20_query_testUnwrap003_EJB_SF_CMTS_Web";
        final String testMethod = "testUnwrap003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
