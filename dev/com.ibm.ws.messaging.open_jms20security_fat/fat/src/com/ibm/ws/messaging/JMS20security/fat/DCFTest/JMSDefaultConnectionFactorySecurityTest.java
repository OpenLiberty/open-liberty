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
package com.ibm.ws.messaging.JMS20security.fat.DCFTest;

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

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.messaging.JMS20security.fat.TestUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSDefaultConnectionFactorySecurityTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/JMSDCFSecurity?test=" + test);
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

            if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            } else
                result = true;

            return result;

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

        TestUtils.addDropinsWebApp(server, "JMSDCFSecurity", "web");
        TestUtils.addDropinsWebApp(server, "JMSContextInject", "web");

        startAppservers();
    }

    /**
     * Start both the JMSConsumerClient local and remote messaging engine AppServers.
     *
     * @throws Exception
     */
    private static void startAppservers() throws Exception {
        server.setServerConfigurationFile("DCFResSecurityClient.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server.startServer("DCFTestClient.log");
        server1.startServer("DCFServer.log");

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
        
        ShrinkHelper.cleanAllExportedArchives();
    }

    @Test
    public void testP2P_TCP_SecOn() throws Exception {

        val = runInServlet("testP2P_TCP_SecOn");
        assertTrue("testP2P_TCP_SecOn failed ", val);

    }

    @Test
    public void testPubSub_TCP_SecOn() throws Exception {

        val = runInServlet("testPubSub_TCP_SecOn");
        assertTrue("testPubSub_TCP_SecOn failed ", val);

    }

    @Test
    public void testPubSubDurable_TCP_SecOn() throws Exception {

        val = runInServlet("testPubSubDurable_TCP_SecOn");
        assertTrue("testPubSubDurable_TCP_SecOn failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testP2PMQ_TCP_SecOn() throws Exception {
        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("DCFResSecurityClientMQ.xml");
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        val = runInServlet("testP2PMQ_TCP_SecOn");
        assertTrue("testP2PMQ_TCP_SecOn failed ", val);

        server.stopServer();
        server1.stopServer();
        startAppservers();
    }
}
