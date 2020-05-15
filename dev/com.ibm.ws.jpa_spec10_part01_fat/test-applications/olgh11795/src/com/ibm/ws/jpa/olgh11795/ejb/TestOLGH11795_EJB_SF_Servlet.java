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

package com.ibm.ws.jpa.olgh11795.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh11795.testlogic.JPATestOLGH11795Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH11795_EJB_SF_Servlet")
public class TestOLGH11795_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH11795Logic.class.getName();
        ejbJNDIName = "ejb/OLGH11795SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH11795_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH11795_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH11795_CMTS"));
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_AMJTA_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-amjta";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_AMRL_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SF_CMTS_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-cmts";

        executeTest(testName, testMethod, testResource);
    }
}
