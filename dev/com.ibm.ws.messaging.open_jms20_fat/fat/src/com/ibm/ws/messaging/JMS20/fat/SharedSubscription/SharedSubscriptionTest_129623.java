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
package com.ibm.ws.messaging.JMS20.fat.SharedSubscription;

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

public class SharedSubscriptionTest_129623 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;
        System.out.println(" Ending : " + test);
        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/SharedSubscription?test=" + test);
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
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");

        server.setServerConfigurationFile("JMSContext.xml");
        server1.setServerConfigurationFile("TestServer1.xml");

        server.startServer("SharedSubscriptionTestClient_129623.log");
        server1.startServer("SharedSubscriptionTestServer_129623.log");
    }

    // 129623_1 JMSConsumer createSharedDurableConsumer(Topic topic,String name)
    // 129623_1_1 Creates a shared durable subscription on the specified topic
    // (if one does not already exist) and creates a consumer on that durable
    // subscription. This method creates the durable subscription without a
    // message selector.

    // 129623_1_3 The JMS provider retains a record of this durable subscription
    // and ensures that all messages from the topic's publishers are retained
    // until they are delivered to, and acknowledged by, a consumer on this
    // durable subscription or until they have expired.
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableExpiry_B_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateSharedDurableConsumer_create_B_SecOff");
        val2 = runInServlet("testCreateSharedDurableConsumer_create_Expiry_B_SecOff");
        server.stopServer();
        Thread.sleep(1000);
        server.startServer("SharedSubscriptionTestClient_129623.log");

        val3 = runInServlet("testCreateSharedDurableConsumer_consume_B_SecOff");
        val4 = runInServlet("testCreateSharedDurableConsumer_consume_Expiry_B_SecOff");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            testResult = true;

        assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableExpiry_TCP_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        boolean val3 = false;
        boolean val4 = false;

        val1 = runInServlet("testCreateSharedDurableConsumer_create_TCP_SecOff");

        val2 = runInServlet("testCreateSharedDurableConsumer_create_Expiry_TCP_SecOff");

        server.stopServer();
        server1.stopServer();
        Thread.sleep(1000);
        server.startServer("SharedSubscriptionTestClient_129623.log");
        server1.startServer("SharedSubscriptionTestServer_129623.log");

        val3 = runInServlet("testCreateSharedDurableConsumer_consume_TCP_SecOff");

        val4 = runInServlet("testCreateSharedDurableConsumer_consume_Expiry_TCP_SecOff");

        if (val1 == true && val2 == true && val3 == true && val4 == true)
            testResult = true;

        assertTrue("testCreateSharedDurableExpiry_TCP_SecOff failed", testResult);
    }

    // 129623_1_4 A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.
    // Bindings and Security Off

    @Test
    public void testCreateSharedDurableConsumer_unsubscribe_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_unsubscribe_B_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_unsubscribe_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    @Test
    public void testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff failed", testResult);
    }

    // 129623_1_5 Any durable subscription created using this method will be
    // shared. This means that multiple active (i.e. not closed) consumers on
    // the subscription may exist at the same time. The term "consumer" here
    // means a JMSConsumer object in any client.
    // 129623_1_6 A shared durable subscription is identified by a name
    // specified by the client and by the client identifier (which may be
    // unset). An application which subsequently wishes to create a consumer on
    // that shared durable subscription must use the same client identifier.
    // Bindings and Sec Off

    // @Mode(TestMode.FULL)
    ////@Test
    public void testMultiSharedDurConsumer_B_SecOff() throws Exception {

        server.stopServer();

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
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
            output = true;
        }

        assertTrue("output value is false", output);

        server.stopServer();
        server.setServerConfigurationFile("JMSContext.xml");
        server.startServer();

    }

    // TCP and SecurityOff
    // @Mode(TestMode.FULL)
    ////@Test
    public void testMultiSharedDurConsumer_TCP_SecOff() throws Exception {
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
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
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

    // 129623_1_7 If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.
    // Bindings and SecOff

    @Test
    public void testCreateSharedDurableConsumer_2Subscribers_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers_B_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_2Subscribers_B_SecOff failed", testResult);

    }

    // TCP and SecOff
    @Test
    public void testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff failed", testResult);

    }

    // 129623_1_8 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    @Test
    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff failed", testResult);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException")
    @Test
    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff");

        assertNotNull("Test testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff failed",
                      testResult);

    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_JRException_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_JRException_B_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_JRException_B_SecOff failed", testResult);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateSharedDurableConsumer_JRException_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedDurableConsumer_JRException_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_JRException_TCP_SecOff failed", testResult);

    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.
    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumer_JRException_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_B_SecOff");

        assertTrue("Test testCreateSharedDurableUndurableConsumer_JRException_B_SecOff failed",
                   testResult);

    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff failed",
                   testResult);

    }

    // 129623_1_12 InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_InvalidDestination_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_B_SecOff");

        assertTrue(
                   "Test testCreateSharedDurableConsumer_InvalidDestination_B_SecOff failed",
                   testResult);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff failed",
                   testResult);

    }

    // 129623_1_13 Case where name is null and empty string

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_Null_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedDurableConsumer_Null_B_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_Null_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    //  @Test
    public void testCreateSharedDurableConsumer_Null_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedDurableConsumer_Null_TCP_SecOff");

        assertTrue("Test testCreateSharedDurableConsumer_Null_TCP_SecOff failed", testResult);

    }

    //Defect 174691
    @Test
    public void testCreateSharedConsumer_Qsession_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedConsumer_Qsession_B_SecOff");

        assertTrue("Test testCreateSharedConsumer_Qsession_B_SecOff failed", testResult);

    }

    //Defect 174713
    @Test
    public void testUnsubscribeInvalidSID_Tsession_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testUnsubscribeInvalidSID_Tsession_B_SecOff");

        assertTrue("Test testUnsubscribeInvalidSID_Tsession_B_SecOff failed", testResult);

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

}
