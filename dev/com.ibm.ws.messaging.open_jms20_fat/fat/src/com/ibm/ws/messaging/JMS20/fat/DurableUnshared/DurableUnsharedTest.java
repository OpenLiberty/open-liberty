/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.DurableUnshared;

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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class DurableUnsharedTest {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean val = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/DurableUnshared?test=" + test);
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
            } else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server.setServerConfigurationFile("JMSContext.xml");
        server1.setServerConfigurationFile("TestServer1.xml");
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");
        server.startServer("DurableUnShared_Client.log");
        server1.startServer("DurableUnShared_Server.log");

    }

    // ========================================================

    @Test
    public void testCreateUnSharedDurableExpiry_B_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateUnSharedDurableConsumer_create");
        val2 = runInServlet("testCreateUnSharedDurableConsumer_create_Expiry");
        server.stopServer();
        Thread.sleep(1000);
        server.startServer("DurableUnShared_create_Expiry_B_Client.log");

        val3 = runInServlet("testCreateUnSharedDurableConsumer_consume");
        val4 = runInServlet("testCreateUnSharedDurableConsumer_consume_Expiry");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            val = true;

        assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableExpiry_TCP_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateUnSharedDurableConsumer_create_TCP");

        val2 = runInServlet("testCreateUnSharedDurableConsumer_create_Expiry_TCP");

        server.stopServer();
        server1.stopServer();
        Thread.sleep(1000);
        server.startServer("DurableUnShared_Client.log");
        server1.startServer("DurableUnShared_Server.log");

        val3 = runInServlet("testCreateUnSharedDurableConsumer_consume_TCP");

        val4 = runInServlet("testCreateUnSharedDurableConsumer_consume_Expiry_TCP");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            val = true;

        assertTrue("testCreateSharedDurableExpiry_TCP_SecOff failed", val);
    }

    // 3rd metod and 6th method

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_JRException_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_JRException");

        assertTrue("testCreateUnSharedDurableConsumer_JRException_B_SecOff failed", val);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_JRException_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_JRException_TCP");

        assertTrue("testCreateUnSharedDurableConsumer_JRException_TCP_SecOff failed", val);

    }

    // 4th method

    // // @Test
    public void testMultiUnSharedDurConsumer_B_SecOff() throws Exception {

        server.setServerConfigurationFile("topicMDB_server.xml");

        server.startServer();

        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull(
                      "Could not find the server start info message in the new file",
                      changedMessageFromLog);

        runInServlet("testBasicMDBTopic");

        server.setMarkToEndOfLog();
        String msg = null;
        int count1 = 0;
        int count2 = 0;
        int i = 0;
        do {
            // msg =
            // server.waitForStringInLog("Received in MDB1: testBasicMDBTopic:",
            // server.getMatchingLogFile("trace.log"));
            if (i == 0)
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            else
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count1++;
                i++;
            }
        } while (msg != null);
        i = 0;
        do {
            if (i == 0)
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            else
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count2++;
                i++;
            }
        } while (msg != null);
        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        boolean output = false;
        if (count1 <= 9 && count2 <= 9 && (count1 + count2 == 10)) {
            output = true;
        }

        assertTrue("output value is false", output);

        server.stopServer();

    }

    // TCP and SecurityOff
    // ////  @Test
    public void testMultiUnSharedDurConsumer_TCP_SecOff() throws Exception {
        server1.stopServer();
        server1.setServerConfigurationFile("topicMDBServer.xml");
        server1.startServer();

        server.stopServer();
        server.setServerConfigurationFile("topicMDB_TcpIp_server.xml");
        server.startServer();

        runInServlet("testBasicMDBTopic_TCP");
        server.setMarkToEndOfLog();
        String msg = null;
        int count1 = 0;
        int count2 = 0;
        int i = 0;
        do {
            msg = server.waitForStringInLog(
                                            "Received in MDB1: testBasicMDBTopic:",
                                            server.getMatchingLogFile("trace.log"));
            if (i == 0)
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            else
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count1++;
                i++;
            }
        } while (msg != null);
        i = 0;
        do {
            if (i == 0)
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            else
                msg = server.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         server.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count2++;
                i++;
            }
        } while (msg != null);
        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        boolean output = false;
        if (count1 <= 9 && count2 <= 9 && (count1 + count2 == 10)) {
            output = true;
        }

        assertTrue("output value is false", output);

        server1.stopServer();
        server.stopServer();
        server.setServerConfigurationFile("JMSContext.xml");
        server1.setServerConfigurationFile("TestServer1.xml");

        server.startServer();
        server1.startServer();
    }

    // 5th method

    // ////  @Test
    public void testCreateUnSharedDurableConsumer_2Subscribers_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_2Subscribers");
        assertTrue("Test testCreateUnSharedDurableConsumer_2Subscribers_SecOff failed", val);

    }

    // TCP and SecOff

    //////  @Test
    public void testCreateUnSharedDurableConsumer_2Subscribers_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_2Subscribers_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_2Subscribers_TCP_SecOff failed", val);

    }

    // 7th method

    // ////  @Test
    public void testCreateUnSharedDurableConsumer_2SubscribersDiffTopic()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic");
        assertTrue("Test testCreateUnSharedDurableConsumer_2SubscribersDiffTopic failed", val);

    }

    //////  @Test
    public void testCreateUnSharedDurableConsumer_2SubscribersDiffTopic_TCP()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_2SubscribersDiffTopic_TCP failed", val);

    }

    //8th method

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableUndurableConsumer_JRException_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumer_JRException");
        assertTrue("Test testCreateUnSharedDurableUndurableConsumer_JRException_B_SecOff failed", val);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateUnSharedDurableUndurableConsumer_JRException_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_TCP");
        assertTrue("Test testCreateUnSharedDurableUndurableConsumer_JRException_TCP_SecOff failed", val);

    }

    // 10th method

    // 129623_1_12 InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_InvalidDestination_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_InvalidDestination");
        assertTrue("Test testCreateUnSharedDurableConsumer_InvalidDestination_B_SecOff failed", val);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_InvalidDestination_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_InvalidDestination_TCP_SecOff failed", val);

    }

    // 13th method

    // Case where name is null and empty string

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Null_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Null");
        assertTrue("Test testCreateUnSharedDurableConsumer_Null_B_SecOff failed", val);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Null_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Null_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_Null_TCP_SecOff failed", val);

    }

    // ========================================================

    @Test
    public void testCreateUnSharedDurableExpiry_Sel_B_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create");
        val2 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_Expiry");
        server.stopServer();
        Thread.sleep(1000);
        server.startServer("DurableUnShared_create_Expiry_B_Client.log");

        val3 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume");
        val4 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_Expiry");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            val = true;

        assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableExpiry_Sel_TCP_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_TCP");

        val2 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_Expiry_TCP");

        server.stopServer();
        server1.stopServer();
        Thread.sleep(1000);
        server.startServer("DurableUnShared_Client.log");
        server1.startServer("DurableUnShared_Server.log");

        val3 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_TCP");

        val4 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            val = true;

        assertTrue("testCreateSharedDurableExpiry_TCP_SecOff failed", val);
    }

    // 3rd metod and 6th method

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_JRException_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_JRException");

        assertTrue("testCreateUnSharedDurableConsumer_Sel_JRException_B_SecOff failed", val);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_JRException_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_JRException_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_JRException_TCP_SecOff failed", val);

    }

    // 5th method

    //////  @Test
    public void testCreateUnSharedDurableConsumer_Sel_2Subscribers_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_2Subscribers");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2Subscribers_SecOff failed", val);

    }

    // TCP and SecOff

    //////  @Test
    public void testCreateUnSharedDurableConsumer_Sel_2Subscribers_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_2Subscribers_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2Subscribers_TCP_SecOff failed", val);

    }

    // 7th method

    // ////  @Test
    public void testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic failed", val);

    }

    // ////  @Test
    public void testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP failed", val);

    }

    //8th method

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableUndurableConsumer_Sel_JRException_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumer_Sel_JRException");
        assertTrue("testCreateUnSharedDurableUndurableConsumer_Sel_JRException_B_SecOff failed", val);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateUnSharedDurableUndurableConsumer_Sel_JRException_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumer_Sel_JRException_TCP");
        assertTrue("testCreateUnSharedDurableUndurableConsumer_Sel_JRException_TCP_SecOff failed", val);

    }

    // 10th method

    //  InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_InvalidDestination_B_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_InvalidDestination");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_InvalidDestination_B_SecOff failed", val);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_InvalidDestination_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP");

        assertTrue("testCreateUnSharedDurableConsumer_Sel_InvalidDestination_TCP_SecOff failed", val);

    }

    // 13th method

    // Case where name is null and empty string

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_Null_B_SecOff()
                    throws Exception {
        val = runInServlet("testCreateSharedDurableConsumer_Sel_Null");

        assertTrue("testCreateUnSharedDurableConsumer_Sel_Null_B_SecOff failed", val);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_Null_TCP_SecOff()
                    throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_Sel_Null_TCP");

        assertTrue("testCreateUnSharedDurableConsumer_Sel_Null_TCP_SecOff failed", val);

    }

    // -----------------------------------------=================================================================

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

}
