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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.tests.web;

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

    // Cleanup
    @PersistenceUnit(unitName = "QueryLockMode_JEE_RL")
    private EntityManagerFactory cleanupEmf;

    private final String testLogicClassName = QueryLockModeTestLogic.class.getName();

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
    public void jpa20_QueryLockMode_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa20_QueryLockMode_@LOCKMODE@_001_Ano_AMJTA_Web";
        final String testMethod = "testScenario01";

        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testLogicClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
            jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx);
        }
    }

    @Test
    public void jpa20_QueryLockMode_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa20_QueryLockMode_@LOCKMODE@_001_Ano_AMRL_Web";
        final String testMethod = "testScenario01";

        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testLogicClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
            jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx);
        }
    }

}
