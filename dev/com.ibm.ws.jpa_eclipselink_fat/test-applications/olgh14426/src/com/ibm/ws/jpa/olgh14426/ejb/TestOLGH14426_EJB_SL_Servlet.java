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

package com.ibm.ws.jpa.olgh14426.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh14426.testlogic.JPATestOLGH14426Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH14426_EJB_SL_Servlet")
public class TestOLGH14426_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH14426Logic.class.getName();
        ejbJNDIName = "ejb/OLGH14426SLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH14426_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH14426_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH14426_CMTS"));
    }

    @Test
    public void jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_AMJTA_Web";
        final String testMethod = "testEclipseLinkCopy";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA_OLGH14426_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_AMRL_Web";
        final String testMethod = "testEclipseLinkCopy";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA_OLGH14426_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh14426_testEclipseLinkCopy_EJB_SL_CMTS_Web";
        final String testMethod = "testEclipseLinkCopy";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA_OLGH14426_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
