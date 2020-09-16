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
package com.ibm.ws.messaging.JMS20.fat.DCFTest;

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

/**
 *
 */
public class JMSDefaultConnectionFactoryTest {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSDefaultConnectionFactoryTest?test="
                          + test);
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
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            }
            else
                result = true;

            return result;

        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("DCFRes_Client.xml");

        server.startServer();
        changedMessageFromLog = server.waitForStringInLog(
                                                          "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in the new file",
                      changedMessageFromLog);

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
            server1.stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testP2P_B_SecOff() throws Exception {

        val = runInServlet("testP2P_B_SecOff");
        assertTrue("testP2P_B_SecOff failed ", val);

    }

    @Test
    public void testP2P_B_SecOff_inject() throws Exception {

        val = runInServlet("testP2P_B_SecOff_inject");
        assertTrue("testP2P_B_SecOff failed ", val);

    }

    @Test
    public void testPubSub_B_SecOff() throws Exception {

        val = runInServlet("testPubSub_B_SecOff");
        assertTrue("testPubSub_B_SecOff failed ", val);

    }

    @Test
    public void testPubSub_B_SecOff_implicitBinding() throws Exception {

        val = runInServlet("testPubSub_B_SecOff_implicitBinding");
        assertTrue("testPubSub_B_SecOff failed ", val);

    }

    @Test
    public void testPubSubDurable_B_SecOff() throws Exception {

        val = runInServlet("testPubSubDurable_B_SecOff");
        assertTrue("testPubSubDurable_B_SecOff failed ", val);

    }

    @Test
    public void testMessageOrder_B_SecOff() throws Exception {

        val = runInServlet("testMessageOrder_B_SecOff");
        assertTrue("testMessageOrder_B_SecOff failed ", val);

    }

    @Test
    public void testBrowser_B_SecOff() throws Exception {

        val = runInServlet("testBrowser_B_SecOff");
        assertTrue("testBrowser_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testObjects() throws Exception {

        val = runInServlet("testObjects");
        assertTrue("testObjects failed ", val);

    }

}
