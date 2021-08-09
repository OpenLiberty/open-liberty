/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.jta;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.injectiondpu.testlogic.InjectionDPUTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
public class AppLevelJTADPUMethodInjectionServlet extends JPATestServlet {
    private EntityManager cmtsEM;
    private EntityManagerFactory amjtaEMF;

    private final String testLogicClassName = InjectionDPUTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEMF"));

        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEM"));
    }

    public EntityManager getCmtsEM() {
        return cmtsEM;
    }

    @PersistenceContext
    public void setCmtsEM(EntityManager cmtsEM) {
        this.cmtsEM = cmtsEM;
    }

    public EntityManagerFactory getAmjtaEMF() {
        return amjtaEMF;
    }

    @PersistenceUnit
    public void setAmjtaEMF(EntityManagerFactory amjtaEMF) {
        this.amjtaEMF = amjtaEMF;
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_Web_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_Web_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
