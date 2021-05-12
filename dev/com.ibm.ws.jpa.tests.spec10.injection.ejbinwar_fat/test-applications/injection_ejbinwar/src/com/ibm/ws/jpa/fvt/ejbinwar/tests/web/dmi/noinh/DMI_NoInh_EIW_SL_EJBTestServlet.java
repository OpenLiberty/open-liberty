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

package com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.noinh;

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
public class DMI_NoInh_EIW_SL_EJBTestServlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.ejbinwar.testlogic.JPAInjectionTestLogic";
    private final String testMethod = "testInjectionTarget";
    private final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;

    private final static String pubEJBJNDIName = "ejbinwar/dmi/noinh/DMIPubNoInhSLEJB";
    private final static String pkgEJBJNDIName = "ejbinwar/dmi/noinh/DMIPkgNoInhSLEJB";
    private final static String priEJBJNDIName = "ejbinwar/dmi/noinh/DMIPriNoInhSLEJB";
    private final static String proEJBJNDIName = "ejbinwar/dmi/noinh/DMIProNoInhSLEJB";

    @PostConstruct
    private void initFAT() {

    }

//    Verify that proper scoping behavior is being employed by the application server.  Given 2 persistence units
//    that are both named "n", the persistence unit defined by the persistence.xml in the application module
//    should take higher scoping precedence then one defined in a supporting library jar.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRI_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PRO_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PUB_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

//    Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
//    a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
//    annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
//
//    The permutation of this test specifies a PU in the jpa jar in the application's lib directory.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    // Verify that a PU with a name unique to the application module can be injected.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    // Verify that a PU with a name unique to the JPA library jar can be injected.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_AMJTA";
        final String resource = "emf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_AMRL";
        final String resource = "emf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_NOOVD_EJBSL_CMTS";
        final String resource = "em_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

//    Verify that proper scoping behavior is being employed by the application server.  Given 2 persistence units
//    that are both named "n", the persistence unit defined by the persistence.xml in the application module
//    should take higher scoping precedence then one defined in a supporting library jar.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_PKG_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pri_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pro_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUName_Pub_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

//    Given 2 persistence units that are both named "n" in both the application module and a supporting library jar,
//    a specific pathname identifying the PU "n" in the supporting library jar can be specified by the injection
//    annotation/deployment descriptor, which allows the bypass of the default scoping behavior.
//
//    The permutation of this test specifies a PU in the jpa jar in the application's lib directory.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PKG_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRI_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testCommonPUNameSpecifiedPersistencePathLibJar_PUB_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_common_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    // Verify that a PU with a name unique to the application module can be injected.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PKG_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRI_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PRO_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameAppModule_PUB_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_ejb_ejb";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    // Verify that a PU with a name unique to the JPA library jar can be injected.

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_AMJTA";
        final String resource = "ovdemf_amjta_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_AMRL";
        final String resource = "ovdemf_amrl_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_RL;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PKG_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pkgEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRI_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, priEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PRO_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, proEJBJNDIName);
    }

    @Test
    public void jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJBInWAR_DMI_NoInh_testUniquePUNameLibJar_PUB_OVD_EJBSL_CMTS";
        final String resource = "ovdem_cmts_jpalib_earlib";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EARLIB_YESOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, pubEJBJNDIName);
    }

}
