/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118075 {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSContext_118075";
    private static final String[] appPackages = new String[] { "jmscontext_118075.web" };
    private static final String contextRoot = "JMSContext_118075";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("JMSContext_118075_Engine.log");
        clientServer.startServer("JMSContext_118075_Client.log");
    }

    @AfterClass
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

        ShrinkHelper.cleanAllExportedArchives();
    }

    //

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerInvalidDest_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerInvalidDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerInvalidDest_Topic_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff() throws Exception {

        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff failed", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerNullDest_B_SecOff");
        assertTrue("testCreateConsumerNullDest_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerNullDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerNullDest_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerNullDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_Topic_B_SecOff() throws Exception {
        // reusing existing servlet method which creates consumer
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff() throws Exception {
        // reusing existing servlet method which creates consumer
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff");
        assertTrue("testQueueConsumer_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseConsumerDepth_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCloseConsumerDepth_B_SecOff");
        assertTrue("testCloseConsumerDepth_B_SecOff failed", testResult);
    }

    // @Mode(TestMode.FULL)
    // @Test
    public void testCloseConsumerDepth_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCloseConsumerDepth_TcpIp_SecOff");
        assertTrue("testCloseConsumerDepth_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseConsumerSubscription_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testListSubscriber_B");
        assertTrue("testCloseConsumerSubscription_B_SecOff failed", testResult);
    }

    // @Mode(TestMode.FULL)
    // @Test
    public void testCloseConsumerSubscription_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testListSubscriber_TcpIp");
        assertTrue("testCloseConsumerSubscription_TcpIp_SecOff failed", testResult);
    }

    // @Mode(TestMode.FULL)
    // @Test
    public void testNoLocalTrue_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testNoLocalTrue_TcpIp");
        assertTrue("testNoLocalTrue_TcpIp_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNoLocalTrueQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testNoLocalTrueQueue_B");
        assertTrue("testNoLocalTrueQueue_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNoLocalTrueQueue_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testNoLocalTrueQueue_TcpIp");
        assertTrue("testNoLocalTrueQueue_TcpIp_SecOff failed", testResult);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorTopic_B_SecOff failed", testResult);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocal_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff failed", testResult);
    }

    @Test
    public void testNoLocalTrue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testNoLocalTrue_B");
        assertTrue("testNoLocalTrue_B_SecOff failed", testResult);
    }
}
