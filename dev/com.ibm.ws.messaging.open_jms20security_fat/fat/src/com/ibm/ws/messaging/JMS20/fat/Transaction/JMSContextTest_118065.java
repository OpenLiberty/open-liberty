/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.Transaction;

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

import com.ibm.ws.messaging.JMS20security.fat.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JMSContextTest_118065 {

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
        server.startServer("JMSContextTest_118065_Client.log");
        server1.startServer("JMSContextTest_118065_Server.log");

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

    // -----118065 ----------
    // JMSContext: Handle transactional capabilities for JMSContext (commit,
    // rollback, acknowledge, recover)
    // Below test cases are the various APIs for handling transactional
    // capabilities for JMSContext (commit, rollback, recover)

    @Test
    public void testCommitLocalTransaction_B() throws Exception {

        val = runInServlet("testCommitLocalTransaction_B");
        assertTrue("testCommitLocalTransaction_B failed ", val);

    }

    @Test
    public void testCommitLocalTransaction_TCP() throws Exception {

        val = runInServlet("testCommitLocalTransaction_TCP");
        assertTrue("testCommitLocalTransaction_TCP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCommitNonLocalTransaction_B() throws Exception {

        val = runInServlet("testCommitNonLocalTransaction_B");
        assertTrue("testCommitNonLocalTransaction_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCommitNonLocalTransaction_TCP() throws Exception {

        val = runInServlet("testCommitNonLocalTransaction_TCP");
        assertTrue("testCommitNonLocalTransaction_TCP failed ", val);

    }

    @Test
    public void testRollbackLocalTransaction_B() throws Exception {

        val = runInServlet("testRollbackLocalTransaction_B");
        assertTrue("testRollbackLocalTransaction_B failed ", val);

    }

    @Test
    public void testRollbackLocalTransaction_TCP() throws Exception {

        val = runInServlet("testRollbackLocalTransaction_TCP");
        assertTrue("testRollbackLocalTransaction_TCP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testRollbackNonLocalTransaction_B() throws Exception {

        val = runInServlet("testRollbackNonLocalTransaction_B");
        assertTrue("testRollbackNonLocalTransaction_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testRollbackNonLocalTransaction_TCP() throws Exception {

        val = runInServlet("testRollbackNonLocalTransaction_TCP");
        assertTrue("testRollbackNonLocalTransaction_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.ws.LocalTransaction.RolledbackException")
    public void testRecoverNonLocalTransaction_B() throws Exception {

        val = runInServlet("testRecoverNonLocalTransaction_B");
        assertTrue("testRecoverNonLocalTransaction_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.ws.LocalTransaction.RolledbackException")
    public void testRecoverNonLocalTransaction_TCP() throws Exception {

        val = runInServlet("testRecoverNonLocalTransaction_TCP");
        assertTrue("testRecoverNonLocalTransaction_TCP failed ", val);
    }

    // ------------------------------------

    @AfterClass
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
