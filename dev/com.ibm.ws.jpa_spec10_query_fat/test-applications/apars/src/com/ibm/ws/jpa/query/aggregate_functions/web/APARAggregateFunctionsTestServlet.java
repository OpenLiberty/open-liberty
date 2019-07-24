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

package com.ibm.ws.jpa.query.aggregate_functions.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.query.aggregate_functions.testlogic.APARAggregateFunctionsTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/APARAggregateFunctionsTestServlet")
public class APARAggregateFunctionsTestServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "DSS_JTA")
    private EntityManager cmtsEm_dss;

    @PersistenceContext(unitName = "AllowNullMaxmin_JTA")
    private EntityManager cmtsEm_alnmm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "DSS_JTA")
    private EntityManagerFactory amjtaEmf_dss;

    @PersistenceUnit(unitName = "AllowNullMaxmin_JTA")
    private EntityManagerFactory amjtaEmf_alnmm;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "DSS_RL")
    private EntityManagerFactory amrlEmf_dss;

    @PersistenceUnit(unitName = "AllowNullMaxmin_RL")
    private EntityManagerFactory amrlEmf_alnmm;

    private final String testLogicClassName = APARAggregateFunctionsTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta_dss",
                       new JPAPersistenceContext("test-jpa-resource-amjta_dss", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf_dss"));

        jpaPctxMap.put("test-jpa-resource-amrl_dss",
                       new JPAPersistenceContext("test-jpa-resource-amrl_dss", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf_dss"));

        jpaPctxMap.put("test-jpa-resource-cmts_dss",
                       new JPAPersistenceContext("test-jpa-resource-cmts_dss", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm_dss"));

        jpaPctxMap.put("test-jpa-resource-amjta_alnmm",
                       new JPAPersistenceContext("test-jpa-resource-amjta_alnmm", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf_alnmm"));

        jpaPctxMap.put("test-jpa-resource-amrl_alnmm",
                       new JPAPersistenceContext("test-jpa-resource-amrl_alnmm", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf_alnmm"));

        jpaPctxMap.put("test-jpa-resource-cmts_alnmm",
                       new JPAPersistenceContext("test-jpa-resource-cmts_alnmm", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm_alnmm"));

    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_alnmm"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMJTA_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_AMRL_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web() throws Exception {
        final String testName = "apar_aggregateFunctions_testAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts_dss"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_QUERY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

}
