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

package com.ibm.ws.jpa.olgh17376.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh17376.testlogic.JPATestOLGH17376Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH17376_EJB_SF_Servlet")
public class TestOLGH17376_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH17376Logic.class.getName();
        ejbJNDIName = "ejb/OLGH17376SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH17376_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH17376_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH17376_CMTS"));
    }

    // testQueryINLiterals1
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINLiterals1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINLiterals1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINLiterals1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINLiterals1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINLiterals1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINLiterals1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINLiterals1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINLiterals1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINLiterals1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryINParameters1
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINParameters1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINParameters1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINParameters1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINParameters1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINParameters1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINParameters1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINParameters1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINParameters1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINParameters1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryINSUBQUERYLiterals1
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryINSUBQUERYParameters1
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINSUBQUERYParameters1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINSUBQUERYParameters1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINSUBQUERYParameters1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryINSUBQUERYLiterals2
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals2";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals2";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYLiterals2_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINSUBQUERYLiterals2";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryINSUBQUERYParameters2
    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryINSUBQUERYParameters2";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryINSUBQUERYParameters2";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17376_testQueryINSUBQUERYParameters2_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryINSUBQUERYParameters2";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
