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

package com.ibm.ws.jpa.fvt.util.tests.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.util.testlogic.UtilTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/UtilEJBSFTestServlet")
public class UtilEJBSFTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = UtilTestLogic.class.getName();
        ejbJNDIName = "ejb/UtilTestSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Util_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Util_AMRL"));
    }

    @Test
    public void jpa20_util_basic_001_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa20_util_basic_001_AMJTA_EJB_SF";
        final String testMethod = "testUtilBasic";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa20_util_1x1_001_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa20_util_1x1_001_AMJTA_EJB_SF";
        final String testMethod = "testUtil1x1";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa20_util_1xM_001_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa20_util_1xM_001_AMJTA_EJB_SF";
        final String testMethod = "testUtil1xm";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa20_util_basic_001_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa20_util_basic_001_AMRL_EJB_SF";
        final String testMethod = "testUtilBasic";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa20_util_1x1_001_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa20_util_1x1_001_AMRL_EJB_SF";
        final String testMethod = "testUtil1x1";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa20_util_1xM_001_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa20_util_1xM_001_AMRL_EJB_SF";
        final String testMethod = "testUtil1xm";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA20_UTIL_DELETE_${dbvendor}.ddl");

        executeTest(testName, testMethod, testResource);
    }

}
