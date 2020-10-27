/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.MDB;

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

import com.ibm.ws.messaging.JMS20security.fat.TestUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class JMSMDBTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/JMSContextInject?test=" + test);
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

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");
        server.setServerConfigurationFile("EJBMDB_server.xml");
        TestUtils.addDropinsWebApp(server, "JMSContextInject", "web");
        TestUtils.addDropinsWebApp(server, "mdbapp", "mdb");
        server.startServer("JMSConsumerTestClient.log");
        String waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testQueueMDB() throws Exception {

        val = runInServlet("testQueueMDB");
        assertTrue("testQueueMDB failed ", val);

        String msg = server.waitForStringInLog(
                                               "Message received on Annotated MDB: testQueueMDB", 5000);
        assertNotNull("Test testQueueMDB failed", msg);

    }

}
