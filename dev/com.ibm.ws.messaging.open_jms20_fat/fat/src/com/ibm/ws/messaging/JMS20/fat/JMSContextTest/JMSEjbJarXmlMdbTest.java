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
package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class JMSEjbJarXmlMdbTest {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSRedelivery_120846?test="
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

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
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

        server.setServerConfigurationFile("EJBMDB_server.xml");
        server.startServer("JMSEjbJarXmlMdb_Client.log");

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
    public void testQueueSendMDB() throws Exception {

        val = runInServlet("testQueueSendMDB");
        assertTrue("testQueueSendMDB failed", val);

        String msg = server.waitForStringInLog("Message received on EJB MDB: testQueueSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    @Test
    public void testAnnotatedMDB() throws Exception {

        val = runInServlet("testAnnotatedMDB");
        assertTrue("testAnnotatedMDB failed", val);

        String msg = server.waitForStringInLog("Message received on Annotated MDB: testAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

// for defect 175486
    @Test
    public void testTopicSendMDB() throws Exception {

        val = runInServlet("testTopicSendMDB");
        assertTrue("testTopicSendMDB failed", val);

        String msg = server.waitForStringInLog("Message received on EJB Topic MDB: testTopicSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    @Test
    public void testTopicAnnotatedMDB() throws Exception {

        val = runInServlet("testTopicAnnotatedMDB");
        assertTrue("testTopicAnnotatedMDB failed", val);

        String msg = server.waitForStringInLog("Message received on Annotated Topic MDB: testTopicAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

}
