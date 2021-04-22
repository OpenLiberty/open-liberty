/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injection.tests.mdb.jndi;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.injection.mdb.TestCoordinator;
import com.ibm.ws.jpa.fvt.injection.mdb.TestExecutionResult;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import junit.framework.Assert;

public class AnnotatedJNDIMDBServlet extends JPATestServlet {
    private static final long serialVersionUID = -3611473513604477785L;

    @Resource(lookup = "jms/JPAConnectionFactory")
    private QueueConnectionFactory connectionFactory;

    @Resource(lookup = "jms/AnnotatedJNDIMBean_Queue")
    private Queue queue;

    private javax.jms.QueueConnection conn = null;
    private QueueSession queueSession = null;
    private QueueSender queueSender = null;
    private boolean jmsReady = false;

    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.injection.testlogic.JPAInjectionTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));

        try {
            conn = connectionFactory.createQueueConnection();
            queueSession = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueSender = queueSession.createSender(queue);
            jmsReady = true;
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void executeTestVehicle(TestExecutionContext ctx) {
        if (!jmsReady) {
            Assert.fail("JMS had failed to initialize.  Cannot run the test.");
        }

        TestCoordinator.initalize();
        TestExecutionResult tre = null;

        try {
            ObjectMessage objectMessage = queueSession.createObjectMessage(ctx);
            queueSender.send(objectMessage);
//            String id = objectMessage.getJMSMessageID();
            tre = TestCoordinator.blockForMDBExecutionCompletion();
            if (tre == null) {
                Assert.fail("Did not receive TestExecutionResult.");
            } else if (tre.getException() != null) {
                throw tre.getException();
            }
        } catch (InterruptedException e) {
            // MDB did not respond or complete its work in a timely fashion.
            e.printStackTrace();
            Assert.fail("MDB did not respond or complete its work before timeout.");
        } catch (Throwable t) {
            logException(t, ctx);
        } finally {

        }
    }

    /*
     * JNDI Injection via Annotation
     */

    /*
     * Verify that proper scoping behavior is being employed by the application server. Given 2 persistence units
     * that are both named "n", the persistence unit defined by the persistence.xml in the application module
     * should take higher scoping precedence then one defined in a supporting library jar.
     *
     */
    @Test
    public void jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/common_jta";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_AMRL() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/common_rl";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_CMTS() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUName_ANO_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/common_cmts";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

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
    public void jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/common_jta";
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
    public void jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_AMRL() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/common_rl";
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
    public void jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_CMTS() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testCommonPUNameSpecifiedPersistencePathLibJar_ANO_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/common_cmts";
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
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/ejb_jta";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_AMRL() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/ejb_rl";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_CMTS() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameAppModule_ANO_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/ejb/ejb_cmts";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Verify that a PU with a name unique to the JPA library jar can be injected.
     */
    @Test
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/jpalib_jta";
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
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_AMRL() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/jpalib_rl";
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
    public void jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_CMTS() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_testUniquePUNameLibJar_ANO_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/earlib/jpalib_cmts";
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
     * Template
     */
//    @Test
    public void jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_AMJTA() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_AMJTA";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_JTA;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/webapp/common_jta";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_AMRL() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_AMRL";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.APPLICATION_MANAGED_RL;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/webapp/common_rl";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_CMTS() throws Exception {
        final String testName = "jpa10_Injection_MDB_JNDI_TEMPLATE_ANO_CMTS";
        final String testMethod = "testInjectionTarget";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final PersistenceContextType pcType = PersistenceContextType.CONTAINER_MANAGED_TS;
        final PersistenceInjectionType piType = PersistenceInjectionType.JNDI;
        final String resource = "java:comp/env/jpa/ejb/jndi/ano/webapp/common_cmts";
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("expected.injection.pattern", "EJB_NOOVERRIDE");

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
