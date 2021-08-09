/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh9018.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.jpa.olgh9018.testlogic.JPATestOLGH9018Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH9018_EJB_SL_Servlet")
public class TestOLGH9018_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH9018Logic.class.getName();
        ejbJNDIName = "ejb/OLGH9018SLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH9018_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH9018_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH9018_CMTS"));
    }

    /*
     * TODO: This test fails on first assert. Need to investigate this behavior.
     */
//    @Test
//    public void jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_AMJTA_Web() throws Exception {
//        final String testName = "jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_AMJTA_Web";
//        final String testMethod = "testUnwrapExclusiveConnection";
//        final String testResource = "test-jpa-resource-amjta";
//        executeTest(testName, testMethod, testResource);
//    }

    /*
     * TODO: This test fails on first assert. Need to investigate this behavior.
     */
//    @Test
//    public void jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_AMRL_Web() throws Exception {
//        final String testName = "jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_AMRL_Web";
//        final String testMethod = "testUnwrapExclusiveConnection";
//        final String testResource = "test-jpa-resource-amrl";
//        executeTest(testName, testMethod, testResource);
//    }

    /*
     * TODO: This test fails on first assert. Need to investigate this behavior.
     */
//    @Test
//    public void jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_CMTS_Web() throws Exception {
//        final String testName = "jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SL_CMTS_Web";
//        final String testMethod = "testUnwrapExclusiveConnection";
//        final String testResource = "test-jpa-resource-cmts";
//        executeTest(testName, testMethod, testResource);
//    }
}
