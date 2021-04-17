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

package com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.web;

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
public class InjectionDPUEJBAppLevelTestServlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injectiondpu.testlogic.InjectionDPUTestLogic";

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMJTA";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SL_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUMethodInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMRL";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SL_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUMethodInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMTS";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUJNDIInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUFieldInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SL_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUMethodInjectionSLEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMJTA";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SF_AMJTA";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amjtaEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_AMRL";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SF_AMRL";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "amrlEMF";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelRLDPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMTS";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUJNDIInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_JNDI_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/InjectionDPU_CMEX";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUJNDIInjectionSFEXEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUFieldInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Field_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Field_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmexEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUFieldInjectionSFEXEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SF_CMTS";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmtsEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUMethodInjectionSFEJB");
    }

    @Test
    public void jpa10_Injection_DPU_AppLevel_Method_EJB_SF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_DPU_AppLevel_Method_EJB_SF_CMEX";
        final String testMethod = "testDefaultPersistenceUnitInjection";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_ES;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "cmexEM";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);

//        executeDDL("JPA_INJECTION_DPU_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejb/AppLevelJTADPUMethodInjectionSFEXEJB");
    }
}
