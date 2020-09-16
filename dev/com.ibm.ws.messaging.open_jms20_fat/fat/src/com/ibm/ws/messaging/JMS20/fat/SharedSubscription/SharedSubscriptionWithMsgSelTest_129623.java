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
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class SharedSubscriptionWithMsgSelTest_129623 {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer"); //client

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1"); //server

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/SharedSubscriptionWithMsgSel?test="
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

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("SharedSubscriptionWithMsgSel_129623_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
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
    public void testSharedDurConsumerWithMsgSelector_B() throws Exception {

        boolean val1 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create");
        boolean val2 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry");
        server.stopServer();
        server1.stopServer();
        server1.startServer("SharedSubscriptionWithMsgSel_129623_Server.log");
        server.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
        boolean val3 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consume");
        boolean val4 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry");

        if (val1 && val2 && val3 && val4)
            val = true;
        assertTrue("testSharedDurConsumerWithMsgSelector_B failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testSharedDurConsumerWithMsgSelector_TcpIp() throws Exception {

        boolean val1 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_TCP");
        boolean val2 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP");
        server1.stopServer();
        server.stopServer();
        Thread.sleep(500);
        server1.startServer("SharedSubscriptionWithMsgSel_129623_Server.log");
        server.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");

        boolean val3 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP");
        boolean val4 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consume_TCP");

        if (val1 && val2 && val3 && val4)
            val = true;

        assertTrue("testSharedDurConsumerWithMsgSelector_TcpIp failed", val);

    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_B_SecOff failed", val);

    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP_SecOff failed", val);

    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers failed", val);

    }

//commented
    //@Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP failed", val);

    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic failed", val);

    }

//commented
    //@Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_Null_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_Null");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_Null_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_Null_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP_SecOff failed", val);

    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_JRException_B_SecOff failed", val);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", val);

    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_B_SecOff failed", val);
    }

    // TCP and Sec Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testMultiSharedDurableConsumer_SecOff() throws Exception {

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("DurSharedMDB_Bindings.xml");
        server1.startServer();
        server.startServer();

        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull(
                      "Could not find the server start info message in the new file",
                      changedMessageFromLog);

        runInServlet("testBasicMDBTopic");
        Thread.sleep(1000);
        int count1 = getCount("Received in MDB1: testBasicMDBTopic:");
        int count2 = getCount("Received in MDB2: testBasicMDBTopic:");

        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        boolean output = false;
        if (count1 > 1 && count2 > 1 && (count1 + count2 == 20)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicDurableShared: output value is false",
                   output);

        server1.stopServer();
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();

        server.stopServer();
        server.setServerConfigurationFile("DurSharedMDB_TCP.xml");
        server.startServer();

        runInServlet("testBasicMDBTopic_TCP");
        Thread.sleep(1000);
        count1 = getCount("Received in MDB1: testBasicMDBTopic:");
        count2 = getCount("Received in MDB2: testBasicMDBTopic:");

        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        output = false;
        // if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
        if (count1 > 1 && count2 > 1 && (count1 + count2 == 20)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicDurableShared_TCP: output value is false",
                   output);

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("JMSContext_Client.xml");
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        server.startServer();

    }

    public int getCount(String str) throws Exception {

        String file = server.getLogsRoot() + "trace.log";
        System.out.println("FILE PATH IS : " + file);
        int count1 = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file));) {

            String sCurrentLine;

            // read lines until reaching the end of the file
            while ((sCurrentLine = br.readLine()) != null) {

                if (sCurrentLine.length() != 0) {
                    // extract the words from the current line in the file
                    if (sCurrentLine.contains(str))
                        count1++;
                }
            }
        } catch (FileNotFoundException exception) {

            exception.printStackTrace();
        } catch (IOException exception) {

            exception.printStackTrace();
        }
        return count1;

    }
}
