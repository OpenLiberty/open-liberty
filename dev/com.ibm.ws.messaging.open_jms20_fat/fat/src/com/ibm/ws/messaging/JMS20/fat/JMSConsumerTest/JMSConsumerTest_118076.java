/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest;

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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.messaging.JMS20.fat.TestUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSConsumerTest_118076 {

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSConsumerEngine");
    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSConsumerClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String consumerAppName = "JMSConsumer_118076";
    private static final String consumerContextRoot = "JMSConsumer_118076";
    private static final String[] consumerPackages = new String[] { "jmsconsumer_118076.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, consumerContextRoot, test);
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSConsumerEngine.xml");
        engineServer.startServer("JMSConsumer_118076_Engine.log");

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSConsumerClient.xml");
        TestUtils.addDropinsWebApp(clientServer, consumerAppName, consumerPackages);
        clientServer.startServer("JMSConsumer_118076_Client.log");
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
    public void testCloseClosedConsumer_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCloseClosedConsumer_B_SecOff");
        assertTrue("testCloseClosedConsumer_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseClosedConsumer_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testCloseClosedConsumer_TcpIp_SecOff");
        assertTrue("testCloseClosedConsumer_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageListener_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetMessageListener_B_SecOff");
        assertTrue("testSetMessageListener_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageListener_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetMessageListener_TcpIp_SecOff");
        assertTrue("testSetMessageListener_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageListener_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageListener_B_SecOff");
        assertTrue("testGetMessageListener_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageListener_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageListener_TcpIp_SecOff");
        assertTrue("testGetMessageListener_TcpIp_SecOff failed ", testResult);
    }

    @Test
    public void testSessionClose_IllegalStateException() throws Exception {
        boolean testResult = runInServlet("testSessionClose_IllegalStateException");
        assertTrue("testSessionClose_IllegalStateException failed ", testResult);
    }

    @Test
    public void testTopicSession_Qrelated_IllegalStateException() throws Exception {
        boolean testResult = runInServlet("testTopicSession_Qrelated_IllegalStateException");
        assertTrue("testTopicSession_Qrelated_IllegalStateException failed ", testResult);
    }
}
