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

package com.ibm.ws.jpa.olgh16685.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16685.testlogic.JPATestOLGH16685Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16685_EJB_SL_Servlet")
public class TestOLGH16685_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16685Logic.class.getName();
        ejbJNDIName = "ejb/OLGH16685SLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16685_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16685_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16685_CMTS"));
    }

    // testJPQLCriteriaSelectClauseParameters
    @Test
    public void jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_AMJTA_Web";
        final String testMethod = "testJPQLCriteriaSelectClauseParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_AMRL_Web";
        final String testMethod = "testJPQLCriteriaSelectClauseParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SL_CMTS_Web";
        final String testMethod = "testJPQLCriteriaSelectClauseParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
