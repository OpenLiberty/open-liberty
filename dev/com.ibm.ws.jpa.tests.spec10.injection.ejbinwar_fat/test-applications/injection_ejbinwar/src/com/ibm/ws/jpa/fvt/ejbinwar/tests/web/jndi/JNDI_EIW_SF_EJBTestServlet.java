/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ejbinwar.tests.web.jndi;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

/**
 *
 */
public class JNDI_EIW_SF_EJBTestServlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.ejbinwar.testlogic.JPAInjectionTestLogic";
    private final String testMethod = "testInjectionTarget";
    private final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;

    @PostConstruct
    private void initFAT() {

    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/common_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/common_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/common_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_ANO_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/common_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/common_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/common_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/common_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/common_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/ejb_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/ejb_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/ejb_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_ANO_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/ejbinwar/ejb_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/jpalib_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/jpalib_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/jpalib_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_ANO_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/ano/earlib/jpalib_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/AnnotatedJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/common_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/common_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/common_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUName_DD_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/common_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/common_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/common_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/common_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/common_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/ejb_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/ejb_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/ejb_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameAppModule_DD_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/ejbinwar/ejb_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEXEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_AMJTA";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/jpalib_jta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_AMRL";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/jpalib_rl";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_CMTS";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/jpalib_cmts";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEJB");
    }

    @Test
    public void jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_CMEX() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_JNDI_testUniquePUNameLibJar_DD_EJBSF_CMEX";
        final String resource = "java:comp/env/jpa/ejbinwar/jndi/dd/earlib/jpalib_cmex";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, "ejbinwar/jndi/DDJNDISFEXEJB");
    }

}
