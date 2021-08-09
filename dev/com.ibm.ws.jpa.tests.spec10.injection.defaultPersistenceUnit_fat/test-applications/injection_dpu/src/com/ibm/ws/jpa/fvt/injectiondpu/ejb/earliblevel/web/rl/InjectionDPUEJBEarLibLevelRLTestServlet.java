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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.rl;

import java.util.HashMap;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

/**
 *
 */
public class InjectionDPUEJBEarLibLevelRLTestServlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injectiondpu.testlogic.InjectionDPUTestLogic";

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_Field_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_Field_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_Field_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_Field_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JNDI_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JNDI_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMRL";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JNDI_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JNDI_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMRL";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_Method_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_Method_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_Method_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_Method_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelRLDPUMethodInjectionSLEJB");
    }
}
