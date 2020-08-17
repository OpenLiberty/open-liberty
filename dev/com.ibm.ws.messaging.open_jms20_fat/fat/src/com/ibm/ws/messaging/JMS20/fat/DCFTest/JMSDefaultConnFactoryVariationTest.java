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
@Mode(TestMode.FULL)
public class JMSDefaultConnFactoryVariationTest {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSDefaultCFVariation?test="
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

        server.setServerConfigurationFile("DCFVar_Server.xml");
        // server.setServerConfigurationFile("DCFVarJms20_Server.xml");

        server.startServer();
        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in the new file",
                      changedMessageFromLog);

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
            //   server1.stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testP2PWithoutLookupName() throws Exception {

        val = runInServlet("testP2PWithoutLookupName");
        assertTrue("testP2PWithoutLookupName failed ", val);

    }

    @Test
    public void testP2PWithLookupName() throws Exception {

        val = runInServlet("testP2PWithLookupName");
        assertTrue("testP2PWithLookupName failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testWithJms20() throws Exception {

//            server.stopServer();
        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        server.setServerConfigurationFile("DCFVarJms20_Server.xml");
//            server.startServer();
        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKZ0003I.*JMSDefaultCFVariation.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the app started info message in the new file",
                      changedMessageFromLog);
        val = runInServlet("testWithJms20");
        assertTrue("testWithJms20 failed ", val);

//           server.stopServer();
        server.setServerConfigurationFile("DCFVar_Server.xml");
//           server.startServer();
//        changedMessageFromLog = server.waitForStringInLog(
//                                                          "CWWKF0008I.*", server.getMatchingLogFile("trace.log"));
//        assertNotNull("Could not find the server updated info message in the new file",
//                      changedMessageFromLog);

    }

}
