/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class LiteBucketSet2Test {

    private static LibertyServer clientServer = LibertyServerFactory.getLibertyServer("LiteSet2Client");
    private static LibertyServer engineServer = LibertyServerFactory.getLibertyServer("LiteSet2Engine");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private boolean runInServlet(String contextRoot, String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, contextRoot, test); // throws IOException
    }

    private static final String CONSUMER_118076_CONTEXT_ROOT = "JMSConsumer_118076";
    private static final String CONSUMER_118076_APPNAME = "JMSConsumer_118076";
    private static final String[] CONSUMER_118076_PACKAGES = new String[] { "jmsconsumer_118076.web" };

    private static final String CONSUMER_118077_CONTEXT_ROOT = "JMSConsumer_118077";
    private static final String CONSUMER_118077_APPNAME = "JMSConsumer_118077";
    private static final String[] CONSUMER_118077_PACKAGES = new String[] { "jmsconsumer_118077.web" };

    private static final String CONTEXT_INJECT_CONTEXT_ROOT = "JMSContextInject";
    private static final String CONTEXT_INJECT_APPNAME = "JMSContextInject";
    private static final String[] CONTEXT_INJECT_PACKAGES = new String[] { "jmscontextinject.web", "jmscontextinject.ejb" };

    private static final String PRODUCER_118073_CONTEXT_ROOT = "JMSProducer_118073";
    private static final String PRODUCER_118073_APPNAME = "JMSProducer_118073";
    private static final String[] PRODUCER_118073_PACKAGES = new String[] { "jmsproducer_118073.web" };

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        // Prepare the server which runs the messaging engine.

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("Lite2Engine.xml");

        // Prepare the server which runs the messaging client and which
        // runs the test application.

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("Lite2Client.xml");

        TestUtils.addDropinsWebApp(clientServer, CONSUMER_118077_APPNAME, CONSUMER_118077_PACKAGES);
        TestUtils.addDropinsWebApp(clientServer, CONSUMER_118076_APPNAME, CONSUMER_118076_PACKAGES);
        TestUtils.addDropinsWebApp(clientServer, CONTEXT_INJECT_APPNAME, CONTEXT_INJECT_PACKAGES);
        TestUtils.addDropinsWebApp(clientServer, PRODUCER_118073_APPNAME, PRODUCER_118073_PACKAGES);

        // Start both servers.  Start the engine first, so that its resources
        // are available when the client starts.

        engineServer.startServer("LiteBucketSet2_Engine.log");
        clientServer.startServer("LiteBucketSet2_Client.log");
    }

    @AfterClass
    public static void tearDown() {
        // Stop the messaging client ...
        try {
            clientServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        // ... then stop the messaging engine.
        try {
            engineServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    // start of tests JMSConsumerTest_118077

    @Test
    public void testReceive_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceive_B_SecOff");
        assertTrue("testReceive_B_SecOff failed ", result);
    }

    @Test
    public void testReceive_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceive_TcpIp_SecOff");
        assertTrue("testReceive_TcpIp_SecOff failed ", result);
    }

    @Test
    public void testReceiveBody_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveBody_B_SecOff");
        assertTrue("testReceiveBody_B_SecOff failed ", result);
    }

    @Test
    public void testReceiveBody_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveBody_TcpIp_SecOff");
        assertTrue("testReceiveBody_TcpIp_SecOff failed ", result);
    }

    @Test
    public void testReceiveWithTimeOut_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveWithTimeOut_B_SecOff");
        assertTrue("testReceiveWithTimeOut_B_SecOff failed ", result);
    }

    @Test
    public void testReceiveWithTimeOut_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveWithTimeOut_TcpIp_SecOff");
        assertTrue("testReceiveWithTimeOut_TcpIp_SecOff failed ", result);
    }

    @Test
    public void testReceiveNoWait_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveNoWait_B_SecOff");
        assertTrue("testReceiveNoWait_B_SecOff failed ", result);
    }

    @Test
    public void testReceiveNoWait_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveNoWait_TcpIp_SecOff");
        assertTrue("testReceiveNoWait_TcpIp_SecOff failed ", result);
    }

    @Test
    public void testReceiveBodyNoWait_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveBodyNoWait_B_SecOff");
        assertTrue("testReceiveBodyNoWait_B_SecOff failed ", result);
    }

    @Test
    public void testReceiveBodyNoWait_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118077_CONTEXT_ROOT, "testReceiveBodyNoWait_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWait_TcpIp_SecOff failed ", result);
    }

    // end of tests of 118077

    // start of tests from JMSConsumerTest_118076

    @Test
    public void testCloseConsumer_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118076_CONTEXT_ROOT, "testCloseConsumer_B_SecOff");
        assertTrue("testCloseConsumer_B_SecOff failed ", result);
    }

    @Test
    public void testCloseConsumer_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118076_CONTEXT_ROOT, "testCloseConsumer_TcpIp_SecOff");
        assertTrue("testCloseConsumer_TcpIp_SecOff failed ", result);
    }

    @Test
    public void testGetMessageSelector_B_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118076_CONTEXT_ROOT, "testGetMessageSelector_B_SecOff");
        assertTrue("testGetMessageSelector_B_SecOff failed ", result);
    }

    @Test
    public void testGetMessageSelector_TcpIp_SecOff() throws Exception {
        boolean result = runInServlet(CONSUMER_118076_CONTEXT_ROOT, "testGetMessageSelector_TcpIp_SecOff");
        assertTrue("testGetMessageSelector_TcpIp_SecOff failed ", result);
    }

    //end of tests from JMSConsumerTest_118076

    //start of JMSProducer_Test118073

    @Test
    public void testSetGetJMSReplyTo_B_SecOff() throws Exception {
        boolean result = runInServlet(PRODUCER_118073_CONTEXT_ROOT, "testSetGetJMSReplyTo_B_SecOff");
        assertTrue("testSetGetJMSReplyTo_B_SecOff failed", result);
    }

    @Test
    public void testSetGetJMSReplyTo_TCP_SecOff() throws Exception {
        boolean result = runInServlet(PRODUCER_118073_CONTEXT_ROOT, "testSetGetJMSReplyTo_TCP_SecOff");
        assertNotNull("testSetGetJMSReplyTo_TCP_SecOff", result);
    }

    @Test
    public void testGetAsync_B_SecOff() throws Exception {
        boolean result = runInServlet(PRODUCER_118073_CONTEXT_ROOT, "testGetAsync_B_SecOff");
        assertTrue("testGetAsync_B_SecOff", result);
    }

    @Test
    public void testGetAsync_TCP_SecOff() throws Exception {
        boolean result = runInServlet(PRODUCER_118073_CONTEXT_ROOT, "testGetAsync_TCP_SecOff");
        assertTrue("testGetAsync_TCP_SecOff", result);
    }

    // end of tests from JMSProducer_test118073

    // start of tests from JMSContextInjectTest

    @Test
    public void testP2P_B_SecOff() throws Exception {
        boolean result = runInServlet(CONTEXT_INJECT_CONTEXT_ROOT, "testP2P_B_SecOff");
        assertTrue("testP2P_B_SecOff failed ", result);
    }

    @Test
    public void testPubSub_B_SecOff() throws Exception {
        boolean result = runInServlet(CONTEXT_INJECT_CONTEXT_ROOT, "testPubSub_B_SecOff");
        assertTrue("testPubSub_B_SecOff failed ", result);
    }

    // end of tests from JMSContextInjectTest
}
