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

package com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

public class DeploymentDescriptorJNDIEJBSFServlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = -7642267902342523971L;

    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injection.testlogic.JPAInjectionTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/jndi/DDJNDISFEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));
    }

    /*
     * JNDI Injection via Deployment Descriptor
     */

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     *
     */
    @Test
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/common_jta";
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
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/common_rl";
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
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUName_DD_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/common_cmts";
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
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/common_jta";
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
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/common_rl";
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
    public void jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_DD_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/common_cmts";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/ejb_jta";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/ejb_rl";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameAppModule_DD_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/ejb/ejb_cmts";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/jpalib_jta";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/jpalib_rl";
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
    public void jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_testUniquePUNameLibJar_DD_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/dd/earlib/jpalib_cmts";
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
     * Template
     */
//    @Test
    public void jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/web/jndi/dd/webapp/common_jta";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_AMRL() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/web/jndi/dd/webapp/common_rl";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_CMTS() throws Exception {
        final String testName = "jpa10_Injection_EJB_SF_JNDI_TEMPLATE_DD_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/web/jndi/dd/webapp/common_cmts";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
