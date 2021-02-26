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

package com.ibm.ws.jpa.olgh10068.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh10068.testlogic.JPATestOLGH10068Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH10068_EJB_SF_Servlet")
public class TestOLGH10068_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH10068Logic.class.getName();
        ejbJNDIName = "ejb/OLGH10068SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH10068_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH10068_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH10068_CMTS"));
    }

    // testCriteriaBuilder_IN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_AMJTA_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_AMRL_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_IN_ClauseLimit_EJB_SF_CMTS_Web";
        final String testMethod = "testCriteriaBuilder_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //testCriteriaBuilder_NOTIN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_AMJTA_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_AMRL_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testCriteriaBuilder_NOTIN_ClauseLimit_EJB_SF_CMTS_Web";
        final String testMethod = "testCriteriaBuilder_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQL_IN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_AMJTA_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_AMRL_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_IN_ClauseLimit_EJB_SF_CMTS_Web";
        final String testMethod = "testJPQL_IN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQL_NOTIN_ClauseLimit tests
    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_AMJTA_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_AMRL_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh10068_testJPQL_NOTIN_ClauseLimit_EJB_SF_CMTS_Web";
        final String testMethod = "testJPQL_NOTIN_ClauseLimit";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
