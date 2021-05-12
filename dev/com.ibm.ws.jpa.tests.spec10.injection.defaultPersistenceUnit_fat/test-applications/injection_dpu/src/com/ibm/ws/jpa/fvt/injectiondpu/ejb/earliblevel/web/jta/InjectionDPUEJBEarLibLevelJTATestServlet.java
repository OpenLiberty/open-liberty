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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.jta;

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
public class InjectionDPUEJBEarLibLevelJTATestServlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injectiondpu.testlogic.InjectionDPUTestLogic";

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMJTA";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUMethodInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMTS";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUMethodInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMJTA";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMTS";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_JNDI_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMEX";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUJNDIInjectionSFEXEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Field_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmexEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUFieldInjectionSFEXEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_EarLibLevel_JTA_Method_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmexEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/EarLibLevelJTADPUMethodInjectionSFEXEJB");
    }
}
