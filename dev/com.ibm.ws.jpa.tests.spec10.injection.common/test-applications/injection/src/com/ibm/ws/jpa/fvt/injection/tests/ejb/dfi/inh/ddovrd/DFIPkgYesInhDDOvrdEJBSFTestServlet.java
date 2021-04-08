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

package com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Field
 * Field/Method Protection: Package
 * Inheritance: Yes, Deployment Descriptor Override of Superclass Injection Fields
 *
 *
 */
public class DFIPkgYesInhDDOvrdEJBSFTestServlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = 4136941150381969903L;

    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injection.testlogic.JPAInjectionTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/dfi/inh/ano/DFIPkgYesInhDDOvrdTestSFEJB";

    @PostConstruct
    private void initFAT() {
//        jpaPctxMap.put("cleanup",
//                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "jpa/cleanup"));

    }

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     *
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_NOOVRD_EJB_SF_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
     * a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
     * annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
     *
     * The permutation of this test specifies a PU in the jpa jar in the application's lib directory.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_AMJTA";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_AMRL";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVRD_EJB_SF_CMTS";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that a PU with a name unique to the application module can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amjta_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "emf_amrl_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_NOOVRD_EJB_SF_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "em_cmts_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that a PU with a name unique to the JPA library jar can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_AMJTA";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_AMRL";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_NOOVRD_EJB_SF_CMTS";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * No Inheritance, Override Tests
     *
     */

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUName_PKG_OVRD_EJB_SF_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_common_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
     * a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
     * annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
     *
     * The permutation of this test specifies a PU in the jpa jar in the application's lib directory.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_AMJTA";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_AMRL";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVRD_EJB_SF_CMTS";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that a PU with a name unique to the application module can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amjta_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdemf_amrl_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameAppModule_PKG_OVRD_EJB_SF_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;
        final String resource = "ovdem_cmts_ejb_ejb";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that a PU with a name unique to the JPA library jar can be injected.
     */

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_AMJTA";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_AMRL";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DFI_YesInheritance_AnoOvrd_testUniquePUNameLibJar_PKG_OVRD_EJB_SF_CMTS";
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
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
