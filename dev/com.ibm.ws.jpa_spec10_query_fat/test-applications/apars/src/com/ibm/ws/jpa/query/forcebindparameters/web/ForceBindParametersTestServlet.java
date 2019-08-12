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

package com.ibm.ws.jpa.query.forcebindparameters.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.query.forcebindparameters.testlogic.ForceBindParametersTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ForceBindParametersTestServlet")
public class ForceBindParametersTestServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "AggregateFunctions_JTA")
    private EntityManager cmtsEm;

    @PersistenceContext(unitName = "AggregateFunctions_ForceBindParameters_JTA")
    private EntityManager cmtsEmForceBindParams;

    // Application Managed JTA
    @PersistenceUnit(unitName = "AggregateFunctions_JTA")
    private EntityManagerFactory amjtaEmf;

    @PersistenceUnit(unitName = "AggregateFunctions_ForceBindParameters_JTA")
    private EntityManagerFactory amjtaEmfForceBindParams;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "AggregateFunctions_RL")
    private EntityManagerFactory amrlEmf;

    @PersistenceUnit(unitName = "AggregateFunctions_ForceBindParameters_RL")
    private EntityManagerFactory amrlEmfForceBindParams;

    private final String testLogicClassName = ForceBindParametersTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amjta-forcebindparam",
                       new JPAPersistenceContext("test-jpa-resource-amjta-forcebindparam", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmfForceBindParams"));

        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl-amjta-forcebindparam",
                       new JPAPersistenceContext("test-jpa-resource-amrl-amjta-forcebindparam", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmfForceBindParams"));

        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
        jpaPctxMap.put("test-jpa-resource-cmts-forcebindparam",
                       new JPAPersistenceContext("test-jpa-resource-cmts-forcebindparam", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEmForceBindParams"));

    }

    @Test
    public void apar_forcebindparms_testCOALESCE_ForceBindJPQLParameters_FBOFF_Web() throws Exception {
        final String testName = "apar_forcebindparms_testCOALESCE_ForceBindJPQLParameters_FBOFF_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testCOALESCE_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testCOALESCE_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testCOALESCE_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testABS_ForceBindJPQLParameters_FBOFF_Web() throws Exception {
        final String testName = "apar_forcebindparms_testABS_ForceBindJPQLParameters_FBOFF_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testABS_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testABS_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testABS_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testCONCAT_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testCONCAT_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testCONCAT_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testEXISTS_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testEXISTS_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testEXISTS_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testNUMERICALEXPRESSION_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testNUMERICALEXPRESSION_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testNUMERICALEXPRESSION_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testIN_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testIN_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testIN_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testLIKE_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testLIKE_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testLIKE_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_forcebindparms_testSUBSTR_ForceBindJPQLParameters_FBON_Web() throws Exception {
        final String testName = "apar_forcebindparms_testSUBSTR_ForceBindJPQLParameters_FBON_Web";
        final String testMethod = "testSUBSTR_ForceBindJPQLParameters";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta-forcebindparam"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        properties.put("usingForceBindParameters", "true");

//        executeDDL("apar_forcebindparms_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
