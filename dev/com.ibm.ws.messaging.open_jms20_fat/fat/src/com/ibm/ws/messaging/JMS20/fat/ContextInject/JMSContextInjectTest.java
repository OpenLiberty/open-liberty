/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.ContextInject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

    // This test class is currently failing with WELD deployment exceptions.  The JMSContextInject application
    // fails to start, causing all of the test methods to fail.

    // For example:
    //
    // [10/13/20 22:29:39:539 EDT] 00000060 com.ibm.ws.app.manager.AppMessageHelper
    // E CWWKZ0002E: An exception occurred while starting the application JMSContextInject.
    // The exception message was: com.ibm.ws.container.service.state.StateChangeException:
    // org.jboss.weld.exceptions.DeploymentException: Exception List with 2 exceptions:
    //
    // org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type JMSContext with qualifiers @Default
    //   at injection point [BackedAnnotatedField] @Inject @JMSConnectionFactory private jmscontextinject.web.JMSContextInjectServlet.jmsContextQueueTCP
    //   at jmscontextinject.web.JMSContextInjectServlet.jmsContextQueueTCP(JMSContextInjectServlet.java:0)
    //   at org.jboss.weld.bootstrap.Validator.validateInjectionPointForDeploymentProblems(Validator.java:378)
    //   at org.jboss.weld.bootstrap.Validator.validateInjectionPoint(Validator.java:290)
    //   at org.jboss.weld.bootstrap.Validator.validateGeneralBean(Validator.java:143)
    //   at org.jboss.weld.bootstrap.Validator.validateRIBean(Validator.java:164)
    //   at org.jboss.weld.bootstrap.Validator.validateBean(Validator.java:526)
    //   at org.jboss.weld.bootstrap.ConcurrentValidator$1.doWork(ConcurrentValidator.java:64)
    //   at org.jboss.weld.bootstrap.ConcurrentValidator$1.doWork(ConcurrentValidator.java:62)
    //   at org.jboss.weld.executor.IterativeWorkerTaskFactory$1.call(IterativeWorkerTaskFactory.java:62)
    //   at org.jboss.weld.executor.IterativeWorkerTaskFactory$1.call(IterativeWorkerTaskFactory.java:55)
    //   at java.util.concurrent.FutureTask.run(FutureTask.java:277)
    //   at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1160)
    //   at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
    //   at java.lang.Thread.run(Thread.java:811)
    //
    //   at org.jboss.weld.bootstrap.ConcurrentValidator.validateBeans(ConcurrentValidator.java:72)
    //   at org.jboss.weld.bootstrap.Validator.validateDeployment(Validator.java:487)
    //   at org.jboss.weld.bootstrap.WeldStartup.validateBeans(WeldStartup.java:496)
    //   at org.jboss.weld.bootstrap.WeldBootstrap.validateBeans(WeldBootstrap.java:93)
    //   at com.ibm.ws.cdi.impl.CDIContainerImpl.startInitialization(CDIContainerImpl.java:161)
    //   at com.ibm.ws.cdi.liberty.CDIRuntimeImpl.applicationStarting(CDIRuntimeImpl.java:479)
    //   at com.ibm.ws.container.service.state.internal.ApplicationStateManager.fireStarting(ApplicationStateManager.java:51)
    //   at com.ibm.ws.container.service.state.internal.StateChangeServiceImpl.fireApplicationStarting(StateChangeServiceImpl.java:50)
    //   at com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase.preDeployApp(SimpleDeployedAppInfoBase.java:547)
    //   at com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase.installApp(SimpleDeployedAppInfoBase.java:508)
    //   at com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase.deployApp(DeployedAppInfoBase.java:349)
    //   at com.ibm.ws.app.manager.war.internal.WARApplicationHandlerImpl.install(WARApplicationHandlerImpl.java:65)
    //   at com.ibm.ws.app.manager.internal.statemachine.StartAction.execute(StartAction.java:144)
    //   at com.ibm.ws.app.manager.internal.statemachine.ApplicationStateMachineImpl.enterState(ApplicationStateMachineImpl.java:1317)
    //   at com.ibm.ws.app.manager.internal.statemachine.ApplicationStateMachineImpl.run(ApplicationStateMachineImpl.java:897)
    //   at com.ibm.ws.threading.internal.ExecutorServiceImpl$RunnableWrapper.run(ExecutorServiceImpl.java:239)
    //   at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1160)
    //   at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
    //   at java.lang.Thread.run(Thread.java:811)

@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // Temporarily disabled for Jakarta; (see above).
@Mode(TestMode.FULL)
public class JMSContextInjectTest {

    private static final LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextInjectEngine");
    private static final LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextInjectClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String contextInjectAppName = "JMSContextInject";
    private static final String contextInjectContextRoot = "JMSContextInject";
    private static final String[] contextInjectPackages =
        new String[] { "jmscontextinject.ejb", "jmscontextinject.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, contextInjectContextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextInjectEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextInjectClient.xml");
        TestUtils.addDropinsWebApp(clientServer, contextInjectAppName, contextInjectPackages);

        engineServer.startServer("JMSContectInjectEngine.log");
        clientServer.startServer("JMSContectInjectClient.log");
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //

    @Mode(TestMode.FULL)
    @Test
    public void testP2P_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testP2P_TCP_SecOff");
        assertTrue("testP2P_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSub_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSub_TCP_SecOff");
        assertTrue("testPubSub_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSubDurable_B_SecOff");
        assertTrue("testPubSubDurable_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSubDurable_TCP_SecOff");
        assertTrue("testPubSubDurable_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testNegativeSetters_B_SecOff");
        assertTrue("testNegativeSetters_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testNegativeSetters_TCP_SecOff");
        assertTrue("testNegativeSetters_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessageOrder_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testMessageOrder_B_SecOff");
        assertTrue("testMessageOrder_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test
    public void testMessageOrder_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testMessageOrder_TCP_SecOff");
        assertTrue("testMessageOrder_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetAutoStart_B_SecOff");
        assertTrue("testGetAutoStart_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetAutoStart_TCP_SecOff");
        assertTrue("testGetAutoStart_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBrowser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testBrowser_B_SecOff");
        assertTrue("testBrowser_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test
    public void testBrowser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testBrowser_TCP_SecOff");
        assertTrue("testBrowser_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEJBCallSecOff() throws Exception {
        boolean testResult = runInServlet("testEJBCallSecOff");
        assertTrue("testEJBCallSecOff failed", testResult);
    }
}
