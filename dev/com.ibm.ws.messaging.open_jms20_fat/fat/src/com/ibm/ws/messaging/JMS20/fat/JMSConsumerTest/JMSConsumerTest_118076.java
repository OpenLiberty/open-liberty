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
package com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest;

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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSConsumerTest_118076 {

    private static LibertyServer engineServer = LibertyServerFactory.getLibertyServer("LiteSet2Endine");
    private static LibertyServer clientServer = LibertyServerFactory.getLibertyServer("LiteSet2Client");

    private static final int clientPort = engineServer.getHttpDefaultPort();
    private static final String clientHostName = engineServer.getHostname();

    private static final String CONTEXT_ROOT = "JMSConsumer_118076";

    private boolean runInServlet(String test) throws IOException {
        URL url = new URL(
            "http://" + clientHostName + ":" +
            clientPort + "/" +
            CONTEXT_ROOT + "?test=" + test);

        System.out.println("The Servlet URL is : " + url.toString());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder lines = new StringBuilder();
            String sep = System.lineSeparator();
            for ( String line = br.readLine(); line != null; line = br.readLine() ) {
                lines.append(line).append(sep);
            }
            System.out.println(lines);

            if ( lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0 ) {
                org.junit.Assert.fail("Missing success message in output. " + lines);
                return false;
            } else {
                return true;
            }

        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");

        engineServer.setServerConfigurationFile("JMSContext_Server.xml");
        engineServer.startServer("JMSConsumer_118076_Server.log");
        String changedMessageFromLog = engineServer.waitForStringInLog(
            "CWWKF0011I.*",
            engineServer.getMatchingLogFile("trace.log") );
        assertNotNull(
            "Could not find the upload message in the new file",
            changedMessageFromLog);

        engineServer.setServerConfigurationFile("JMSContext_Client.xml");
        engineServer.startServer("JMSConsumer_118076_Client.log");
        changedMessageFromLog = engineServer.waitForStringInLog(
            "CWWKF0011I.*",
            engineServer.getMatchingLogFile("trace.log") );
        assertNotNull(
            "Could not find the upload message in the new file",
            changedMessageFromLog);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping client server");
            clientServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        try {
            System.out.println("Stopping engine server");
            engineServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

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
