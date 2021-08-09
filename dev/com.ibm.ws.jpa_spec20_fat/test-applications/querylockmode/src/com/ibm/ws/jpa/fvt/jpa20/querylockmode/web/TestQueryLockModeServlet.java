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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.jpa20.querylockmode.testlogic.QueryLockModeTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestQueryLockModeServlet")
public class TestQueryLockModeServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "QueryLockMode_JEE")
    private EntityManager cmtsEM;

    // Application Managed JTA
    @PersistenceUnit(unitName = "QueryLockMode_JEE")
    private EntityManagerFactory amjtaEM;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "QueryLockMode_JEE_RL")
    private EntityManagerFactory amrlEM;

    @PostConstruct
    private void initFAT() {
        testClassName = QueryLockModeTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEM"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEM"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEM"));
    }

    @Test
    public void jpa_spec20_querylockmode_testScenario01_AMJTA_Web() throws Exception {
        final String testName = "jpa20_querylockmode_@LOCKMODE@_testScenario01_AMJTA_Web";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec20_querylockmode_testScenario01_AMRL_Web() throws Exception {
        final String testName = "jpa20_querylockmode_@LOCKMODE@_testScenario01_AMRL_Web";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    /*
     * TODO: Running this test with Container Managed EntityManager requires a transaction on em.refresh().
     * Investigate changing the test to allow cmts.
     *
     * @Test
     * public void jpa_spec20_querylockmode_testScenario01_CMTS_Web() throws Exception {
     * final String testName = "jpa20_querylockmode_@LOCKMODE@_testScenario01_CMTS_Web";
     * final String testMethod = "testScenario01";
     * final String testResource = "test-jpa-resource-cmts";
     * executeTest(testName, testMethod, testResource);
     * }
     */

    @Override
    protected void executeTest(String testName, String testMethod, String testResource) throws Exception {
        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get(testResource));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("dbProductName", getDbProductName());
            properties.put("dbProductVersion", getDbProductVersion());
            properties.put("jdbcDriverVersion", getJdbcDriverVersion());
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx);
        }
    }
}
