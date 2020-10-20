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
package com.ibm.ws.messaging.JMS20security.fat.JMSConsumerTest;

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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Ignore //Porting from closed liberty not completed
@RunWith(FATRunner.class)
public class JMSConsumerTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    // private static final int PORT = 9090;
    private static final String HOST = server.getHostname();

    boolean val;

    private static boolean runInServlet(String test) throws IOException {

        boolean result;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSConsumer?test="
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
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server.startServer("JMSConsumerTestClient.log");
        String waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        server1.startServer("JMSConsumerServer.log");
        waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
    }

    // start 118076
    // @Test
    public void testCloseConsumer_B_SecOn() throws Exception {

        val = runInServlet("testCloseConsumer_B");
        assertTrue("testCloseConsumer_B_SecOn failed", val);

    }

    // TCP and Security on ( with ssl)

    // @Test
    public void testCloseConsumer_TCP_SecOn() throws Exception {

        val = runInServlet("testCloseConsumer_TCP");
        assertTrue("testCloseConsumer_TCP_SecOn failed", val);

    }

    // end 118076

    // start 118077
    @Test
    public void testReceive_B_SecOn() throws Exception {

        val = runInServlet("testReceive_B");
        assertTrue("testReceive_B_SecOn failed", val);
    }

    @Test
    public void testReceive_TCP_SecOn() throws Exception {

        val = runInServlet("testReceive_TCP");
        assertTrue("testReceive_TCP_SecOn failed", val);

    }

    @Test
    public void testReceiveBody_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBody_B");
        assertTrue("testReceiveBody_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBody_TcpIp_SecOn() throws Exception {

        val = val = runInServlet("testReceiveBody_TCP");
        assertTrue("testReceiveBody_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOut_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTimeOut_B");
        assertTrue("testReceiveBodyTimeOut_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOut_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTimeOut_TCP");
        assertTrue("testReceiveBodyTimeOut_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyNoWait_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWait_B");
        assertTrue("testReceiveBodyNoWait_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWait_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWait_TCP");
        assertTrue("testReceiveBodyNoWait_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveWithTimeOut_B_SecOn() throws Exception {

        val = runInServlet("testReceiveWithTimeOut_B_SecOn");
        assertTrue("testReceiveWithTimeOut_B_SecOn failed", val);
    }

    @Test
    public void testReceiveWithTimeOut_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveWithTimeOut_TcpIp_SecOn");
        assertTrue("testReceiveWithTimeOut_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveNoWait_B_SecOn() throws Exception {

        val = runInServlet("testReceiveNoWait_B_SecOn");
        assertTrue("testReceiveNoWait_B_SecOn failed", val);
    }

    @Test
    public void testReceiveNoWait_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveNoWait_TcpIp_SecOn");
        assertTrue("testReceiveNoWait_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyEmptyBody_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBody_B_SecOn");
        assertTrue("testReceiveBodyEmptyBody_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyEmptyBody_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBody_B_SecOn");
        assertTrue("testReceiveBodyEmptyBody_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedType_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedType_B_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveTopic_B");
        assertTrue("testReceiveTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveTopic_TCP_SecOn() throws Exception {

        val = runInServlet("testReceiveTopic_TCP");
        assertTrue("testReceiveTopic_TCP_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTopic_B");
        assertTrue("testReceiveBodyTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTopic_TCP");
        assertTrue("testReceiveBodyTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOutTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTopic_B");
        assertTrue("testReceiveBodyTimeOutTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOutTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTopic_TCP");
        assertTrue("testReceiveBodyTimeOutTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTopic_B");
        assertTrue("testReceiveBodyNoWaitTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTopic_TCP");
        assertTrue("testReceiveBodyNoWaitTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveWithTimeOutTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveWithTimeOutTopic_B_SecOn");
        assertTrue("testReceiveWithTimeOutTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveWithTimeOutTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveWithTimeOutTopic_TcpIp_SecOn");
        assertTrue("testReceiveWithTimeOutTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveNoWaitTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveNoWaitTopic_B_SecOn");
        assertTrue("testReceiveNoWaitTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveNoWaitTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveNoWaitTopic_TcpIp_SecOn");
        assertTrue("testReceiveNoWaitTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyEmptyBodyTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBodyTopic_B_SecOn");
        assertTrue("testReceiveBodyEmptyBodyTopic_B_SecOn failed", val);

    }

    // @Test
    public void testReceiveBodyEmptyBodyTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBodyTopic_B_SecOn");
        assertTrue("testReceiveBodyEmptyBodyTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn failed",
                   val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testRDC_BindingsAndTcpIp_SecOn() throws Exception {

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("120846_Bindings.xml");
        server1.startServer();
        String messageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
                                                           server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      messageFromLog);
        server.startServer();
        messageFromLog = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", messageFromLog);

        val = runInServlet("testRDC_B");

        List<String> strings = null;

        // 19494 - part of the debugging we wait up to 10 seconds for the redelivery to occur (tracing has been enabled via
        //         120846_Bindings.xml for this test so with luck this will give enough detail should this happen again)
        for (int i = 0; 10 > i; ++i) {
            strings = server.findStringsInLogs("Message=2,JMSXDeliveryCount=2,JMSRedelivered=true,text=testRDC_B");
            if (null != strings && 1 == strings.size())
                break;
            Thread.sleep(1000);
        }
        assertTrue("testRDC_B failed", strings != null && strings.size() == 1);

        server1.stopServer();
        server.stopServer();
        System.out.println("Running testRDC_TcpIp_SecOn");
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        messageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
                                                    server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      messageFromLog);
        server.setServerConfigurationFile("120846_Bindings.xml");
        server.startServer();
        messageFromLog = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", messageFromLog);

        val = runInServlet("testRDC_TcpIp");

        // 19494 - part of the debugging we wait up to 10 seconds for the redelivery to occur (tracing has been enabled via
        //         120846_Bindings.xml for this test so with luck this will give enough detail should this happen again)
        for (int i = 0; 10 > i; ++i) {
            strings = server.findStringsInLogs("Message=2,JMSXDeliveryCount=2,JMSRedelivered=true,text=testRDC_TcpIp");
            if (null != strings && 1 == strings.size())
                break;
            Thread.sleep(1000);
        }
        assertTrue("testRDC_TcpIp failed", strings != null && strings.size() == 1);

        server1.stopServer();
        server.stopServer();
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server1.startServer();
        messageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
                                                    server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      messageFromLog);
        server.startServer();
        messageFromLog = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", messageFromLog);

    }

    @Test
    public void testCreateSharedDurable_B_SecOn() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_create");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String messageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
                                                           server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      messageFromLog);
        server.startServer("JMSConsumerTestClient.log");
        messageFromLog = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", messageFromLog);

        val = runInServlet("testCreateSharedDurableConsumer_consume");
        assertTrue("testCreateSharedDurable_B_SecOn failed", val);

    }

    // TCP and Security Off
    @Test
    public void testCreateSharedDurable_TCP_SecOn() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumer_create_TCP");

        server.stopServer();
        server1.stopServer();
        server1.startServer("JMSConsumerTestServer.log");
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer("JMSConsumerTestClient.log");
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedDurableConsumer_consume_TCP");
        assertTrue("testCreateSharedDurable_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableWithMsgSel_B_SecOn() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_create");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_consume");
        assertTrue("testCreateSharedDurableWithMsgSel_B_SecOn failed", val);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableWithMsgSel_TCP_SecOn() throws Exception {

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_create_TCP");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_consume_TCP");
        assertTrue("testCreateSharedDurableWithMsgSel_TCP_SecOn failed", val);

    }

    @Test
    public void testCreateSharedNonDurable_B_SecOn() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumer_create");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedNonDurableConsumer_consume");
        assertTrue("testCreateSharedNonDurable_B_SecOn failed", val);

    }

    // TCP and Security Off
    @Test
    public void testCreateSharedNonDurable_TCP_SecOn() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumer_create_TCP");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedNonDurableConsumer_consume_TCP");

        assertTrue("testCreateSharedNonDurable_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableWithMsgSel_B_SecOn() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_create");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_consume");
        assertTrue("testCreateSharedNonDurableWithMsgSel_B_SecOn failed", val);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableWithMsgSel_TCP_SecOn() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_create_TCP");

        server.stopServer();
        server1.stopServer();
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_consume_TCP");

        assertTrue("testCreateSharedNonDurableWithMsgSel_TCP_SecOn failed", val);

    }

    // @Test
    public void testMultiSharedNonDurableConsumer_SecOn() throws Exception {

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("120846_Bindings.xml");
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull(
                      "Could not find the server start info message in the new file",
                      changedMessageFromLog);

        val = runInServlet("testBasicMDBTopic");
        Thread.sleep(1000);
        int count1 = getCount("Received in MDB1: testBasicMDBTopic:");
        int count2 = getCount("Received in MDB2: testBasicMDBTopic:");

        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        boolean output = false;
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicNonDurable: output value is false", output);

        server1.stopServer();
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        server.stopServer();
        server.setServerConfigurationFile("NonDurSharedMDB_server.xml");
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testBasicMDBTopic_TCP");
        Thread.sleep(1000);
        count1 = getCount("Received in MDB1: testBasicMDBTopic_TCP:");
        count2 = getCount("Received in MDB2: testBasicMDBTopic_TCP:");

        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        output = false;
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicNonDurable: output value is false", output);

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server1.startServer();
        waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

    }

    // @Test
    public void testMultiSharedDurableConsumer_SecOn() throws Exception {

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("DurSharedMDB_Bindings.xml");
        server1.startServer();
        String waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        runInServlet("testBasicMDBTopicDurShared");
        Thread.sleep(1000);
        int count1 = getCount("Received in MDB1: testBasicMDBTopic:");
        int count2 = getCount("Received in MDB2: testBasicMDBTopic:");

        boolean output = false;
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicDurableShared: output value is false",
                   output);

        server1.stopServer();
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        server.stopServer();
        server.setServerConfigurationFile("DurSharedMDB_TCP.xml");
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

        val = runInServlet("testBasicMDBTopicDurShared_TCP");
        Thread.sleep(1000);
        count1 = getCount("Received in MDB1: testBasicMDBTopic_TCP:");
        count2 = getCount("Received in MDB2: testBasicMDBTopic_TCP:");

        output = false;
        if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicDurableShared_TCP: output value is false",
                   output);

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server1.startServer();
        waitFor = server1.waitForStringInLog("CWWKF0011I.*", server1.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);
        server.startServer();
        waitFor = server.waitForStringInLog("CWWKF0011I.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Server ready message not found", waitFor);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageProperty_Bindings_SecOn() throws Exception {

        val = runInServlet("testSetMessageProperty_Bindings_SecOn");

        assertTrue("testSetMessageProperty_Bindings_SecOn failed", val);

    }

    // 118067_9 : Test setting message properties on createProducer using method
    // chaining.
    // Message send options may be specified using one or more of the following
    // methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.
    // TCP/IP and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageProperty_TCP_SecOn() throws Exception {

        val = runInServlet("testSetMessageProperty_TCP_SecOn");
        assertTrue("testSetMessageProperty_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_B_SecOn() throws Exception {

        val = runInServlet("testTopicName_temp_B");
        assertTrue("testTopicName_temp_B_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_TCP_SecOn() throws Exception {

        val = runInServlet("testTopicName_temp_TCP");
        assertTrue("testTopicName_temp_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_Bindings_SecOn() throws Exception {

        val = runInServlet("testQueueNameCaseSensitive_Bindings");
        assertTrue("testQueueNameCaseSensitive_Bindings_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_TCP_SecOn() throws Exception {

        val = runInServlet("testQueueNameCaseSensitive_TCP");

        assertTrue("testQueueNameCaseSensitive_TCP_SecOn failed", val);

    }

    // end 118077
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

    public int getCount(String str) throws Exception {

        String file = server.getLogsRoot() + "trace.log";
        System.out.println("FILE PATH IS : " + file);
        int count1 = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

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
