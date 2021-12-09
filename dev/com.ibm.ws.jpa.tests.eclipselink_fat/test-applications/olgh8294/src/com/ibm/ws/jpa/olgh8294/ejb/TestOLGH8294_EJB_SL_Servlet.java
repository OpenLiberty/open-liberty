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

package com.ibm.ws.jpa.olgh8294.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh8294.testlogic.JPATestOLGH8294Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH8294_EJB_SL_Servlet")
public class TestOLGH8294_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8294Logic.class.getName();
        ejbJNDIName = "ejb/OLGH8294SLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_CMTS"));
    }

    //COALESCE
    @Test
    public void jpa_eclipselink_testCOALESCE_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_EJB_SL_AMJTA_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCOALESCE_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_EJB_SL_AMRL_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCOALESCE_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_EJB_SL_CMTS_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //ABS
    @Test
    public void jpa_eclipselink_testABS_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_EJB_SL_AMJTA_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_EJB_SL_AMRL_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_EJB_SL_CMTS_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //CONCAT
    @Test
    public void jpa_eclipselink_testCONCAT_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_EJB_SL_AMJTA_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_EJB_SL_AMRL_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_EJB_SL_CMTS_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //EXISTS
    @Test
    public void jpa_eclipselink_testEXISTS_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_EJB_SL_AMJTA_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_EJB_SL_AMRL_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_EJB_SL_CMTS_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //NUMERICAL
    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_AMJTA_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_AMRL_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_EJB_SL_CMTS_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //IN
    @Test
    public void jpa_eclipselink_testIN_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_EJB_SL_AMJTA_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_EJB_SL_AMRL_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_EJB_SL_CMTS_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //LIKE
    @Test
    public void jpa_eclipselink_testLIKE_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_EJB_SL_AMJTA_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_EJB_SL_AMRL_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_EJB_SL_CMTS_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //SUBSTR
    @Test
    public void jpa_eclipselink_testSUBSTR_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_EJB_SL_AMJTA_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_EJB_SL_AMRL_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_EJB_SL_CMTS_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
