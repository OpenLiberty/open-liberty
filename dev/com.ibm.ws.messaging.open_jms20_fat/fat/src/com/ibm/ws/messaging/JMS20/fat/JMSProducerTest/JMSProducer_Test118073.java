/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSProducerTest;

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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

// TODO: What is the relationship of this test and the similarly named
//       "JMSProducerTest_118073"?

@Mode(TestMode.FULL)
public class JMSProducer_Test118073 {
    // Use JMSContextClient instead

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSProducerClient");
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSProducerEngine");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String producerAppName = "JMSProducer_118073";
    private static final String producerContextRoot = "JMSProducer_118073";
    private static final String[] producerPackages = new String[] { "jmsproducer_118073.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, producerContextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        // TODO: Why not for this test?
        // engineServer.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSProducerEngine.xml");
        engineServer.startServer("JMSProducerEngine_118073B.log");

        clientServer.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSProducerClient.xml");
        TestUtils.addDropinsWebApp(clientServer, producerAppName, producerPackages);
        clientServer.startServer("JMSProducerClient_118073B.log");
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
    public void testSetGetJMSReplyTo_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetJMSReplyTo_Topic_B_SecOff");
        assertTrue("testSetGetJMSReplyTo_Topic_B_SecOff", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetJMSReplyTo_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetJMSReplyTo_Topic_TCP_SecOff");
        assertTrue("testSetGetJMSReplyTo_Topic_TCP_SecOff", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNullJMSReplyTo_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testNullJMSReplyTo_B_SecOff");
        assertTrue("testNullJMSReplyTo_B_SecOff", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNullJMSReplyTo_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testNullJMSReplyTo_TCP_SecOff");
        assertTrue("testNullJMSReplyTo_TcpIp_SecOff: Expected output not found", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetAsync_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetAsync_B_SecOff");
        assertTrue("testSetAsync_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetAsync_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetAsync_TCP_SecOff");
        assertTrue("testSetAsync_TCP_SecOff failed", testResult);
    }
}
