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

package com.ibm.ws.jpa.fvt.criteriaquery.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.criteriaquery.testlogic.CriteriaQueryTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CriteriaQueryTestServlet")
public class CriteriaQueryTestServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "CriteriaQuery_JEE")
    private EntityManager cmtsEM;

    // Application Managed JTA
    @PersistenceUnit(unitName = "CriteriaQuery_JEE")
    private EntityManagerFactory amjtaEM;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "CriteriaQuery_RL")
    private EntityManagerFactory amrlEM;

    private final String testLogicClassName = CriteriaQueryTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEM"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEM"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEM"));
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "cleanupEmf"));

    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_byte_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_byte_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_byte_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_byte_AMRL_Web";
        final String testMethod = "testCriteriaQuery_byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_byte_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_byte_CMTS_Web";
        final String testMethod = "testCriteriaQuery_byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Byte_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Byte_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Byte_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Byte_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Byte_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Byte_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Byte";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_char_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_char_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_char";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_char_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_char_AMRL_Web";
        final String testMethod = "testCriteriaQuery_char";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_char_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_char_CMTS_Web";
        final String testMethod = "testCriteriaQuery_char";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Character_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Character_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Character";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Character_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Character_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Character";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Character_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Character_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Character";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_String_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_String_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_String";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_String_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_String_AMRL_Web";
        final String testMethod = "testCriteriaQuery_String";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_String_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_String_CMTS_Web";
        final String testMethod = "testCriteriaQuery_String";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_double_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_double_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_double_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_double_AMRL_Web";
        final String testMethod = "testCriteriaQuery_double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_double_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_double_CMTS_Web";
        final String testMethod = "testCriteriaQuery_double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Double_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Double_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Double_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Double_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Double_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Double_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Double";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_float_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_float_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_float_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_float_AMRL_Web";
        final String testMethod = "testCriteriaQuery_float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_float_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_float_CMTS_Web";
        final String testMethod = "testCriteriaQuery_float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Float_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Float_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Float_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Float_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Float_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Float_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Float";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_int_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_int_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_int";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_int_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_int_AMRL_Web";
        final String testMethod = "testCriteriaQuery_int";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_int_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_int_CMTS_Web";
        final String testMethod = "testCriteriaQuery_int";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Integer_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Integer_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Integer";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Integer_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Integer_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Integer";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Integer_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Integer_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Integer";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_long_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_long_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_long_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_long_AMRL_Web";
        final String testMethod = "testCriteriaQuery_long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_long_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_long_CMTS_Web";
        final String testMethod = "testCriteriaQuery_long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Long_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Long_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Long_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Long_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Long_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Long_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Long";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_short_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_short_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_short_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_short_AMRL_Web";
        final String testMethod = "testCriteriaQuery_short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_short_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_short_CMTS_Web";
        final String testMethod = "testCriteriaQuery_short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Short_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Short_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_Short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Short_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Short_AMRL_Web";
        final String testMethod = "testCriteriaQuery_Short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_Short_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_Short_CMTS_Web";
        final String testMethod = "testCriteriaQuery_Short";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_BigDecimal";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_AMRL_Web";
        final String testMethod = "testCriteriaQuery_BigDecimal";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigDecimal_CMTS_Web";
        final String testMethod = "testCriteriaQuery_BigDecimal";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigInteger_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigInteger_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_BigInteger";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigInteger_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigInteger_AMRL_Web";
        final String testMethod = "testCriteriaQuery_BigInteger";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_BigInteger_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_BigInteger_CMTS_Web";
        final String testMethod = "testCriteriaQuery_BigInteger";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_JavaUtilDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_AMRL_Web";
        final String testMethod = "testCriteriaQuery_JavaUtilDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaUtilDate_CMTS_Web";
        final String testMethod = "testCriteriaQuery_JavaUtilDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_AMJTA_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_AMJTA_Web";
        final String testMethod = "testCriteriaQuery_JavaSqlDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_AMRL_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_AMRL_Web";
        final String testMethod = "testCriteriaQuery_JavaSqlDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_CMTS_Web() throws Exception {
        final String testName = "jpa20_CriteriaApi_testCriteriaQuery_JavaSqlDate_CMTS_Web";
        final String testMethod = "testCriteriaQuery_JavaSqlDate";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        executeDDL("JPA_CRITERIAAPI_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

}
