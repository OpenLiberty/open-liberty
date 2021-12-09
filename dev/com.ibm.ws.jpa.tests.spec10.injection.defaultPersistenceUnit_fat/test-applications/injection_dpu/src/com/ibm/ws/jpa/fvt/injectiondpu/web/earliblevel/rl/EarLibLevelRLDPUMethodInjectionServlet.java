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

package com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.rl;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.injectiondpu.testlogic.InjectionDPUTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
public class EarLibLevelRLDPUMethodInjectionServlet extends JPATestServlet {
    private EntityManagerFactory amrlEMF;

    public EntityManagerFactory getAmrlEMF() {
        return amrlEMF;
    }

    @PersistenceUnit
    public void setAmrlEMF(EntityManagerFactory amrlEMF) {
        this.amrlEMF = amrlEMF;
    }

    private final String testLogicClassName = InjectionDPUTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEMF"));
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_Method_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_Method_Web_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
