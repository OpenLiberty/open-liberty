/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh19342.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh19342.testlogic.JPATestOLGH19342Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

import componenttest.annotation.SkipIfSysProp;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH19342_EJB_SF_Servlet")
public class TestOLGH19342_EJB_SF_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH19342Logic.class.getName();
        ejbJNDIName = "ejb/OLGH19342SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19342_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19342_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19342_CMTS"));
    }

    // testCaseExpressionOperatorConcurrency
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_ZOS)
    public void jpa_spec10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_AMJTA_Web";
        final String testMethod = "testCaseExpressionOperatorConcurrency";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_ZOS)
    public void jpa_spec10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_AMRL_Web";
        final String testMethod = "testCaseExpressionOperatorConcurrency";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    // @Test // Not a valid scenario
    public void jpa_spec10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SF_CMTS_Web";
        final String testMethod = "testCaseExpressionOperatorConcurrency";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
