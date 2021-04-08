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

package com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.injection.testlogic.JPAInjectionTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Field
 * Field/Method Protection: Public
 * Inheritance: No
 *
 *
 */
public class DFIPubNoInhTestServlet extends JPATestServlet {
    private static final long serialVersionUID = 1830362102815857889L;

    /*
     * JPA Resource Injection with No Override by Deployment Descriptor
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION)
    public EntityManager em_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA")
    public EntityManagerFactory emf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA")
    public EntityManagerFactory emf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA")
    public EntityManagerFactory emf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA")
    public EntityManagerFactory emf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL")
    public EntityManagerFactory emf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL")
    public EntityManagerFactory emf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL")
    public EntityManagerFactory emf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL")
    public EntityManagerFactory emf_amrl_jpalib_earlib;

    /*
     * JPA Resource Injection with Override by Deployment Descriptor
     *
     * Overridden injection points will refer to a OVRD_<pu name> which contains both the <appmodule>A and B entities.
     */

    // Container Managed Persistence Context

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceContext(unitName = "COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubNoInhTestServlet/ovdem_cmts_common_webapp")
    public EntityManager ovdem_cmts_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceContext(unitName = "WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubNoInhTestServlet/ovdem_cmts_webapp_webapp")
    public EntityManager ovdem_cmts_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "../lib/jpapulib.jar#COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubNoInhTestServlet/ovdem_cmts_common_earlib")
    public EntityManager ovdem_cmts_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceContext(unitName = "JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DFIPubNoInhTestServlet/ovdem_cmts_jpalib_earlib")
    public EntityManager ovdem_cmts_jpalib_earlib;

    // Application Managed Persistence Unit, JTA-Transaction

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @PersistenceUnit(unitName = "COMMON_JTA", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amjta_common_webapp")
    public EntityManagerFactory ovdemf_amjta_common_webapp;

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_JTA", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amjta_webapp_webapp")
    public EntityManagerFactory ovdemf_amjta_webapp_webapp;

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_JTA", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amjta_common_earlib")
    public EntityManagerFactory ovdemf_amjta_common_earlib;

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_JTA", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amjta_jpalib_earlib")
    public EntityManagerFactory ovdemf_amjta_jpalib_earlib;

    // Application Managed Persistence Unit, RL-Transaction

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @PersistenceUnit(unitName = "COMMON_RL", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amrl_common_webapp")
    public EntityManagerFactory ovdemf_amrl_common_webapp;

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @PersistenceUnit(unitName = "WEBAPP_RL", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amrl_webapp_webapp")
    public EntityManagerFactory ovdemf_amrl_webapp_webapp;

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#COMMON_RL", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amrl_common_earlib")
    public EntityManagerFactory ovdemf_amrl_common_earlib;

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @PersistenceUnit(unitName = "JPALIB_RL", name = "jpa/DFIPubNoInhTestServlet/ovdemf_amrl_jpalib_earlib")
    public EntityManagerFactory ovdemf_amrl_jpalib_earlib;

    private final String testLogicClassName = JPAInjectionTestLogic.class.getName();

    private final HashMap<String, com.ibm.ws.testtooling.testinfo.JPAPersistenceContext> jpaPctxMap = new HashMap<String, com.ibm.ws.testtooling.testinfo.JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("cleanup",
                       new com.ibm.ws.testtooling.testinfo.JPAPersistenceContext("cleanup", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));

    }

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     *
     */
    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_NOOVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
     * a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
     * annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
     *
     * The permutation of this test specifies a PU in the jpa jar in the application's lib directory.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that a PU with a name unique to the application module can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_NOOVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that a PU with a name unique to the JPA library jar can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_NOOVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUName_PUB_OVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_common_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
     * a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
     * annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
     *
     * The permutation of this test specifies a PU in the jpa jar in the application's lib directory.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_common_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that a PU with a name unique to the application module can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameAppModule_PUB_OVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_webapp_webapp";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "WEB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that a PU with a name unique to the JPA library jar can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_NoInheritance_testUniquePUNameLibJar_PUB_OVRD_Web_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_jpalib_earlib";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
