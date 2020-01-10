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
@WebServlet(urlPatterns = "/TestOLGH8294_EJB_SF_Servlet")
public class TestOLGH8294_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8294Logic.class.getName();
        ejbJNDIName = "ejb/OLGH8294SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8294_CMTS"));
    }

    //COALESCE
    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_EJB_SF_AMJTA_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_EJB_SF_AMRL_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCOALESCE_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCOALESCE_EJB_SF_CMTS_Web";
        final String testMethod = "testCOALESCE";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //ABS
    @Test
    public void jpa_spec10_query_olgh8294_testABS_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_EJB_SF_AMJTA_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testABS_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_EJB_SF_AMRL_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testABS_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testABS_EJB_SF_CMTS_Web";
        final String testMethod = "testABS";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //CONCAT
    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_EJB_SF_AMJTA_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_EJB_SF_AMRL_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testCONCAT_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testCONCAT_EJB_SF_CMTS_Web";
        final String testMethod = "testCONCAT";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //EXISTS
    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_EJB_SF_AMJTA_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_EJB_SF_AMRL_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testEXISTS_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testEXISTS_EJB_SF_CMTS_Web";
        final String testMethod = "testEXISTS";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //NUMERICAL
    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_AMJTA_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_AMRL_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testNUMERICALEXPRESSION_EJB_SF_CMTS_Web";
        final String testMethod = "testNUMERICALEXPRESSION";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //IN
    @Test
    public void jpa_spec10_query_olgh8294_testIN_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_EJB_SF_AMJTA_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testIN_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_EJB_SF_AMRL_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testIN_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testIN_EJB_SF_CMTS_Web";
        final String testMethod = "testIN";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //LIKE
    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_EJB_SF_AMJTA_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_EJB_SF_AMRL_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testLIKE_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testLIKE_EJB_SF_CMTS_Web";
        final String testMethod = "testLIKE";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    //SUBSTR
    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_EJB_SF_AMJTA_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_EJB_SF_AMRL_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_olgh8294_testSUBSTR_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh8294_testSUBSTR_EJB_SF_CMTS_Web";
        final String testMethod = "testSUBSTR";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
