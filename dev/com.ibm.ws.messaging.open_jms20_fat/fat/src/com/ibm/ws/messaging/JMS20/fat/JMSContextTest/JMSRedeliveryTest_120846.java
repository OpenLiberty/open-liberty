/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSRedeliveryTest_120846 {

    private static final LibertyServer engineServer = LibertyServerFactory.getLibertyServer("RedeliveryEngine");

    private static final LibertyServer clientServer = LibertyServerFactory.getLibertyServer("RedeliveryClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String redeliveryAppName = "JMSRedelivery_120846";
    private static final String redeliveryContextRoot = "JMSRedelivery_120846";
    private static final String[] redeliveryPackages = new String[] { "jmsredelivery_120846.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, redeliveryContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
                                                  "lib/features",
                                                  "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("RedeliveryEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
                                                  "lib/features",
                                                  "features/testjmsinternals-1.0.mf");
        TestUtils.addDropinsWebApp(clientServer, redeliveryAppName, redeliveryPackages);
        clientServer.setServerConfigurationFile("RedeliveryClient.xml");

        engineServer.startServer("JMSRedelivery_120846_Engine.log");
        clientServer.startServer("JMSRedelivery_120846_Client.log");
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // When a client receives a message the mandatory JMS-defined
    // message property JMSXDeliveryCount will be set to the number of
    // times the message has been delivered. The first time a message
    // is received it will be set to 1

    @Test
    public void testInitialJMSXDeliveryCount_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testInitialJMSXDeliveryCount_B_SecOff");
        assertTrue("testInitialJMSXDeliveryCount_B_SecOff failed", testResult);
    }

    @Test
    public void testInitialJMSXDeliveryCount_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testInitialJMSXDeliveryCount_TcpIp_SecOff");
        assertTrue("testInitialJMSXDeliveryCount_TcpIp_SecOff failed", testResult);
    }

    // Test with message redelivery : value of 2 or more means the
    // message has been redelivered.

    // If the JMSRedelivered message header value is set then the
    // JMSXDeliveryCount property must always be 2 or more.

    // Test with duplicate delivery of messages.

//TODO
//    @Mode(TestMode.FULL)
//    @Test
    public void testRDC_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testRDC_B");
        assertTrue("testRDC_BindingsAndTcpIp_SecOff.testRDC_B failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMaxRDC_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testMaxRDC_B");
        assertTrue("testRDC_BindingsAndTcpIp_SecOff.testMaxRDC_B failed", testResult);
    }

//TODO
//    @Mode(TestMode.FULL)
//    @Test
    public void testRDC_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testRDC_TCP");
        assertTrue("testRDC_BindingsAndTcpIp_SecOff.testRDC_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMaxRDC_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testMaxRDC_TCP");
        assertTrue("testRDC_BindingsAndTcpIp_SecOff.testRMaxRDC_TCP failed", testResult);
    }

    // Validate targetTransportChains

    @Mode(TestMode.FULL)
    @Test
    public void testTargetTransportChainTcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTargetChain_B");
        assertTrue("testTargetTransportChainTcpIp_SecOff failed", testResult);
    }
}
