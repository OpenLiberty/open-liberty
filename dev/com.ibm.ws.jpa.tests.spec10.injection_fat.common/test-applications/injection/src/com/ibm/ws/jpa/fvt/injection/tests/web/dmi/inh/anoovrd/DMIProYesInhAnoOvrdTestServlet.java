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

package com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd;

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

/**
 * JPA Injection Test Servlet
 *
 * Injection Type: Method
 * Field/Method Protection: Protected
 * Inheritance: Yes, Annotation Override of Superclass Injection Methods
 *
 *
 */
public class DMIProYesInhAnoOvrdTestServlet extends DMIProYesInhAnoOvrdTestSuperclass {
    private static final long serialVersionUID = 3510106363675793758L;

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @Override
    public EntityManager getOvdem_cmts_common_webapp() {
        return ovdem_cmts_common_webapp;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_common_webapp")
    protected void setOvdem_cmts_common_webapp(EntityManager ovdemCmtsCommonWebapp) {
        ovdem_cmts_common_webapp = ovdemCmtsCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @Override
    public EntityManager getOvdem_cmts_webapp_webapp() {
        return ovdem_cmts_webapp_webapp;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_WEBAPP_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_webapp_webapp")
    protected void setOvdem_cmts_webapp_webapp(EntityManager ovdemCmtsWebappWebapp) {
        ovdem_cmts_webapp_webapp = ovdemCmtsWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_common_earlib() {
        return ovdem_cmts_common_earlib;
    }

    @Override
    @PersistenceContext(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_common_earlib")
    protected void setOvdem_cmts_common_earlib(EntityManager ovdemCmtsCommonEarlib) {
        ovdem_cmts_common_earlib = ovdemCmtsCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManager getOvdem_cmts_jpalib_earlib() {
        return ovdem_cmts_jpalib_earlib;
    }

    @Override
    @PersistenceContext(unitName = "OVRD_JPALIB_JTA", type = PersistenceContextType.TRANSACTION,
                        name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdem_cmts_jpalib_earlib")
    protected void setOvdem_cmts_jpalib_earlib(EntityManager ovdemCmtsJpalibEarlib) {
        ovdem_cmts_jpalib_earlib = ovdemCmtsJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_JTA in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_webapp() {
        return ovdemf_amjta_common_webapp;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_COMMON_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_common_webapp")
    protected void setOvdemf_amjta_common_webapp(EntityManagerFactory ovdemfAmjtaCommonWebapp) {
        ovdemf_amjta_common_webapp = ovdemfAmjtaCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_JTA in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amjta_webapp_webapp() {
        return ovdemf_amjta_webapp_webapp;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_WEBAPP_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_webapp_webapp")
    protected void setOvdemf_amjta_webapp_webapp(
                                                 EntityManagerFactory ovdemfAmjtaWebappWebapp) {
        ovdemf_amjta_webapp_webapp = ovdemfAmjtaWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_common_earlib() {
        return ovdemf_amjta_common_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_common_earlib")
    protected void setOvdemf_amjta_common_earlib(
                                                 EntityManagerFactory ovdemfAmjtaCommonEarlib) {
        ovdemf_amjta_common_earlib = ovdemfAmjtaCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_JTA in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amjta_jpalib_earlib() {
        return ovdemf_amjta_jpalib_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_JPALIB_JTA", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amjta_jpalib_earlib")
    protected void setOvdemf_amjta_jpalib_earlib(
                                                 EntityManagerFactory ovdemfAmjtaJpalibEarlib) {
        ovdemf_amjta_jpalib_earlib = ovdemfAmjtaJpalibEarlib;
    }

    // This EntityManager should refer to the COMMON_RL in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_webapp() {
        return ovdemf_amrl_common_webapp;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_COMMON_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_common_webapp")
    protected void setOvdemf_amrl_common_webapp(
                                                EntityManagerFactory ovdemfAmrlCommonWebapp) {
        ovdemf_amrl_common_webapp = ovdemfAmrlCommonWebapp;
    }

    // This EntityManager should refer to the WEBAPP_RL in the Web App module
    @Override
    public EntityManagerFactory getOvdemf_amrl_webapp_webapp() {
        return ovdemf_amrl_webapp_webapp;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_WEBAPP_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_webapp_webapp")
    protected void setOvdemf_amrl_webapp_webapp(
                                                EntityManagerFactory ovdemfAmrlWebappWebapp) {
        ovdemf_amrl_webapp_webapp = ovdemfAmrlWebappWebapp;
    }

    // This EntityManager should refer to the COMMON_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_common_earlib() {
        return ovdemf_amrl_common_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "../lib/jpapulib.jar#OVRD_COMMON_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_common_earlib")
    protected void setOvdemf_amrl_common_earlib(
                                                EntityManagerFactory ovdemfAmrlCommonEarlib) {
        ovdemf_amrl_common_earlib = ovdemfAmrlCommonEarlib;
    }

    // This EntityManager should refer to the JPALIB_RL in the jar in the Application's Library directory
    @Override
    public EntityManagerFactory getOvdemf_amrl_jpalib_earlib() {
        return ovdemf_amrl_jpalib_earlib;
    }

    @Override
    @PersistenceUnit(unitName = "OVRD_JPALIB_RL", name = "jpa/DMIProYesInhAnoOvrdTestServlet/ovdemf_amrl_jpalib_earlib")
    protected void setOvdemf_amrl_jpalib_earlib(
                                                EntityManagerFactory ovdemfAmrlJpalibEarlib) {
        ovdemf_amrl_jpalib_earlib = ovdemfAmrlJpalibEarlib;
    }

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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_AMJTA";
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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_AMRL";
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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUName_PRO_NOOVRD_Web_CMTS";
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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_AMJTA";
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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_AMRL";
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
    public void jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testCommonPUNameSpecifiedPersistencePathLibJar_PRO_NOOVRD_Web_CMTS";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_AMJTA";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_AMRL";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameAppModule_PRO_NOOVRD_Web_CMTS";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_AMJTA";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_AMRL() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_AMRL";
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
    public void jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_CMTS() throws Exception {
        final String testName = "jpa10_Injection_DMI_YesInheritance_testUniquePUNameLibJar_PRO_NOOVRD_Web_CMTS";
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

}
