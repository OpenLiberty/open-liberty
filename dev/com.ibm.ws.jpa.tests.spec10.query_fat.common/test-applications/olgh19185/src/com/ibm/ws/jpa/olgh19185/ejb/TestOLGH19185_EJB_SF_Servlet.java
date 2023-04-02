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

package com.ibm.ws.jpa.olgh19185.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh19185.testlogic.JPATestOLGH19185Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH19185_EJB_SF_Servlet")
public class TestOLGH19185_EJB_SF_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH19185Logic.class.getName();
        ejbJNDIName = "ejb/OLGH19185SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19185_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19185_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19185_CMTS"));
    }

    // testQueryUpdateLiterals1
    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryUpdateLiterals1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryUpdateLiterals1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateLiterals1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryUpdateLiterals1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryUpdateParameters1
    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_AMJTA_Web";
        final String testMethod = "testQueryUpdateParameters1";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_AMRL_Web";
        final String testMethod = "testQueryUpdateParameters1";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh19185_testQueryUpdateParameters1_EJB_SF_CMTS_Web";
        final String testMethod = "testQueryUpdateParameters1";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
