/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.messaging.JMS20.fat.TemporaryQueue;

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
import org.junit.runner.RunWith;

import com.ibm.ws.messaging.JMS20contextSecurity2.fat.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JMSContextTest_118068 {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean val = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/TemporaryQueue?test=" + test);
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
            String sep = System.lineSeparator();
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);

                result = false;
            } else
                result = true;

            return result;

            // return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/cert.der");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/ltpa.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/ltpaFIPS.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/mykey.jks");

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");

        TestUtils.addDropinsWebApp(server, "TemporaryQueue", "web");

        startAppservers();
    }

    /**
     * Start both the JMSConsumerClient local and remote messaging engine AppServers.
     *
     * @throws Exception
     */
    private static void startAppservers() throws Exception {
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server.startServer("JMSContextTest_118068_Client.log");
        server1.startServer("JMSContextTest_118068_Server.log");

        // CWWKF0011I: The TestServer1 server is ready to run a smarter planet. The TestServer1 server started in 6.435 seconds.
        // CWSID0108I: JMS server has started.
        // CWWKS4105I: LTPA configuration is ready after 4.028 seconds.
        for (String messageId : new String[] { "CWWKF0011I.*", "CWSID0108I.*", "CWWKS4105I.*" }) {
            String waitFor = server.waitForStringInLog(messageId, server.getMatchingLogFile("messages.log"));
            assertNotNull("Server message " + messageId + " not found", waitFor);
            waitFor = server1.waitForStringInLog(messageId, server1.getMatchingLogFile("messages.log"));
            assertNotNull("Server1 message " + messageId + " not found", waitFor);
        }
    }

    // ---- 118068
    // Below test cases are the APIs to implement the funtionality to support
    // the creation of temporary queues and topics for JMSContext

    @Test
    public void testCreateTemporaryQueueSecOnBinding() throws Exception {

        val = runInServlet("testCreateTemporaryQueueSecOnBinding");
        assertTrue("testCreateTemporaryQueueSecOnBinding failed ", val);

    }

    @Test
    public void testCreateTemporaryQueueSecOnTCPIP() throws Exception {

        val = runInServlet("testCreateTemporaryQueueSecOnTCPIP");
        assertTrue("testCreateTemporaryQueueSecOnTCPIP failed ", val);

    }

    @Test
    public void testTemporaryQueueLifetimeSecOn_B() throws Exception {

        val = runInServlet("testTemporaryQueueLifetimeSecOn_B");
        assertTrue("testTemporaryQueueLifetimeSecOn_B failed ", val);

    }

    @Test
    public void testTemporaryQueueLifetimeSecOn_TCPIP() throws Exception {

        val = runInServlet("testTemporaryQueueLifetimeSecOn_TCPIP");
        assertTrue("testTemporaryQueueLifetimeSecOn_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testgetTemporaryQueueNameSecOnBinding() throws Exception {

        val = runInServlet("testgetTemporaryQueueNameSecOnBinding");
        assertTrue("testgetTemporaryQueueNameSecOnBinding failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testgetTemporaryQueueNameSecOnTCPIP() throws Exception {

        val = runInServlet("testgetTemporaryQueueNameSecOnTCPIP");
        assertTrue("testgetTemporaryQueueNameSecOnTCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryQueueNameSecOnBinding() throws Exception {

        val = runInServlet("testToStringTemporaryQueueNameSecOnBinding");
        assertTrue("testToStringTemporaryQueueNameSecOnBinding failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryQueueNameSecOnTCPIP() throws Exception {

        val = runInServlet("testToStringTemporaryQueueNameSecOnTCPIP");
        assertTrue("testToStringTemporaryQueueNameSecOnTCPIP failed ", val);
    }

    @Test
    public void testDeleteTemporaryQueueNameSecOnBinding() throws Exception {

        val = runInServlet("testDeleteTemporaryQueueNameSecOnBinding");
        assertTrue("testDeleteTemporaryQueueNameSecOnBinding failed ", val);
    }

    @Test
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException")
    public void testDeleteTemporaryQueueNameSecOnTCPIP() throws Exception {

        val = runInServlet("testDeleteTemporaryQueueNameSecOnTCPIP");
        assertTrue("testDeleteTemporaryQueueNameSecOnTCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryQueueNameSecOn_B() throws Exception {

        val = runInServlet("testDeleteExceptionTemporaryQueueNameSecOn_B");
        assertTrue("testDeleteExceptionTemporaryQueueNameSecOn_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException")
    public void testDeleteExceptionTemporaryQueueNameSecOn_TCPIP() throws Exception {

        val = runInServlet("testDeleteExceptionTemporaryQueueNameSecOn_TCPIP");
        assertTrue("testDeleteExceptionTemporaryQueueNameSecOn_TCPIP failed ",
                   val);

    }

    @Test
    public void testPTPTemporaryQueue_Binding() throws Exception {

        val = runInServlet("testPTPTemporaryQueue_Binding");
        assertTrue("testPTPTemporaryQueue_Binding failed ", val);

    }

    @Test
    public void testPTPTemporaryQueue_TCP() throws Exception {

        val = runInServlet("testPTPTemporaryQueue_TCP");
        assertTrue("testPTPTemporaryQueue_TCP failed ", val);
    }

    @Test
    public void testCreateTemporaryTopicSecOnBinding() throws Exception {

        val = runInServlet("testCreateTemporaryTopicSecOnBinding");

        assertTrue("testCreateTemporaryTopicSecOnBinding failed ", val);
    }

    @Test
    public void testCreateTemporaryTopicSecOnTCPIP() throws Exception {

        val = runInServlet("testCreateTemporaryTopicSecOnTCPIP");

        assertTrue("testCreateTemporaryTopicSecOnTCPIP failed ", val);
    }

    @Test
    public void testTemporaryTopicLifetimeSecOn_B() throws Exception {

        val = runInServlet("testTemporaryTopicLifetimeSecOn_B");

        assertTrue("testTemporaryTopicLifetimeSecOn_B failed ", val);
    }

    @Test
    public void testTemporaryTopicLifetimeSecOn_TCPIP() throws Exception {

        val = runInServlet("testTemporaryTopicLifetimeSecOn_TCPIP");
        assertTrue("testTemporaryTopicLifetimeSecOn_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetTemporaryTopicSecOnBinding() throws Exception {

        val = runInServlet("testGetTemporaryTopicSecOnBinding");
        assertTrue("testGetTemporaryTopicSecOnBinding failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetTemporaryTopicSecOnTCPIP() throws Exception {

        val = runInServlet("testGetTemporaryTopicSecOnTCPIP");
        assertTrue("testGetTemporaryTopicSecOnTCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryTopicSecOnBinding() throws Exception {

        val = runInServlet("testToStringTemporaryTopicSecOnBinding");
        assertTrue("testToStringTemporaryTopicSecOnBinding failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringeTemporaryTopicSecOnTCPIP() throws Exception {

        val = runInServlet("testToStringeTemporaryTopicSecOnTCPIP");
        assertTrue("testToStringeTemporaryTopicSecOnTCPIP failed ", val);
    }

    @Test
    public void testDeleteTemporaryTopicSecOnBinding() throws Exception {

        val = runInServlet("testDeleteTemporaryTopicSecOnBinding");
        assertTrue("testDeleteTemporaryTopicSecOnBinding failed ", val);
    }

    @Test
    public void testDeleteTemporaryTopicSecOnTCPIP() throws Exception {

        val = runInServlet("testDeleteTemporaryTopicSecOnTCPIP");
        assertTrue("testDeleteTemporaryTopicSecOnTCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryTopicSecOn_B() throws Exception {

        val = runInServlet("testDeleteExceptionTemporaryTopicSecOn_B");
        assertTrue("testDeleteExceptionTemporaryTopicSecOn_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    public void testDeleteExceptionTemporaryTopicSecOn_TCPIP() throws Exception {

        val = runInServlet("testDeleteExceptionTemporaryTopicSecOn_TCPIP");
        assertTrue("testDeleteExceptionTemporaryTopicSecOn_TCPIP failed ", val);

    }

    @Test
    public void testTemporaryTopicPubSubSecOn_B() throws Exception {

        val = runInServlet("testTemporaryTopicPubSubSecOn_B");
        assertTrue("testTemporaryTopicPubSubSecOn_B failed ", val);
    }

    @Test
    public void testTemporaryTopicPubSubSecOn_TCPIP() throws Exception {

        val = runInServlet("testTemporaryTopicPubSubSecOn_TCPIP");
        assertTrue("testTemporaryTopicPubSubSecOn_TCPIP failed ", val);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping client server");
            server.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Stopping engine server");
            server1.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
