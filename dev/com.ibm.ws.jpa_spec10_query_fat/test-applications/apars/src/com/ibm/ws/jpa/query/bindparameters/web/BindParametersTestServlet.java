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

package com.ibm.ws.jpa.query.bindparameters.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.query.bindparameters.testlogic.BindParametersTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BindParametersTestServlet")
public class BindParametersTestServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "AggregateFunctions_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "AggregateFunctions_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "AggregateFunctions_RL")
    private EntityManagerFactory amrlEmf;

    private final String testLogicClassName = BindParametersTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    //Tests without property set
    @Test
    public void jpa_eclipselink_testCOALESCE_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCOALESCE_Web";
        final String testMethod = "testCOALESCE_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testABS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testABS_Web";
        final String testMethod = "testABS_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testCONCAT_Web() throws Exception {
        final String testName = "jpa_eclipselink_testCONCAT_Web";
        final String testMethod = "testCONCAT_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testEXISTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_testEXISTS_Web";
        final String testMethod = "testEXISTS_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testNUMERICALEXPRESSION_Web() throws Exception {
        final String testName = "jpa_eclipselink_testNUMERICALEXPRESSION_Web";
        final String testMethod = "testNUMERICALEXPRESSION_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testIN_Web() throws Exception {
        final String testName = "jpa_eclipselink_testIN_Web";
        final String testMethod = "testIN_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testLIKE_Web() throws Exception {
        final String testName = "jpa_eclipselink_testLIKE_Web";
        final String testMethod = "testLIKE_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_eclipselink_testSUBSTR_Web() throws Exception {
        final String testName = "jpa_eclipselink_testSUBSTR_Web";
        final String testMethod = "testSUBSTR_BindJPQLParameters";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    private void executeTest(String testName, String testMethod, String testResource) throws Exception {
        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get(testResource));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_OLGH8294_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
