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

@Mode(TestMode.FULL)
public class SharedSubscriptionWithMsgSelTest_129626 {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/SharedSubscriptionWithMsgSel?test=" + test);
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
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("SharedSubscriptionWithMsgSel_129626_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer("SharedSubscriptionWithMsgSel_129626_Client.log");

    }

    //  129626_1    JMSConsumer createSharedConsumer(Topic topic,String sharedSubscriptionName)
    //  129626_1_1      Creates a shared non-durable subscription with the specified name on the specified topic (if one does not already exist) and creates a consumer on that subscription. This method creates the non-durable subscription without a message selector.
    //129626_1_4      Non-durable subscription is not persisted and will be deleted (together with any undelivered messages associated with it) when there are no consumers on it. The term "consumer" here means a MessageConsumer or JMSConsumer object in any client.

    // Bindings and Security Off

    @Test
    public void testCreateSharedNonDurable_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_create");

        server.stopServer();
        server.startServer("SharedSubscriptionWithMsgSel_129626_Client.log");

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_consume");

        assertTrue("testCreateSharedNonDurable_B_SecOff failed", val);
    }

    //TCP and SecurityOff
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurable_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP");

        server.stopServer();
        server1.stopServer();
        server1.startServer("SharedSubscriptionWithMsgSel_129626_Server.log");
        server.startServer("SharedSubscriptionWithMsgSel_129626_Client.log");

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP");

        assertTrue("testCreateSharedNonDurable_TCP_SecOff failed", val);

    }

    // 129626_1_2 If a shared non-durable subscription already exists with the same name and client identifier (if set), and the same topic and message selector has been specified,
    // then this method creates a JMSConsumer on the existing subscription.
    // Bindings and SecOff

    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers");

        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_B_SecOff failed", val);

    }

//mBeans is currently not working for TCP. Below testcase has to be uncommented once mBeans is fixed.
    //@Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP");

        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP_SecOff failed", val);

    }

    //129626_1_6 If a shared non-durable subscription already exists with the same name and client identifier (if set) but a different topic or message selector value has been
    // specified, and there is a consumer already active (i.e. not closed) on the subscription, then a JMSRuntimeException will be thrown.
    // Bindings and Security Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_B_SecOff failed", val);

    }

    //TCP and SecurityOff
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", val);
    }

    //129626_1_7 There is no restriction on durable subscriptions and shared non-durable subscriptions having the same name and clientId (which may be unset). Such subscriptions
    // would be completely separate.

    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_coexist");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_B_SecOff failed", val);
    }

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP");
        assertTrue("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed", val);

    }

    // 129626_1_9 InvalidDestinationRuntimeException - if an invalid topic is specified.
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP() throws Exception {

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testMultiSharedNonDurableConsumer_SecOff() throws Exception {

        server.stopServer();
        server1.stopServer();
        server.setServerConfigurationFile("NonDurSharedMDB_Bindings.xml");
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

        assertTrue("testBasicMDBTopicNonDurable: output value is false", output);

        server1.stopServer();
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();

        server.stopServer();
        server.setServerConfigurationFile("NonDurSharedMDB_TCP.xml");
        server.startServer();

        runInServlet("testBasicMDBTopic_TCP");
        Thread.sleep(1000);
        count1 = getCount("Received in MDB1: testBasicMDBTopic:");
        count2 = getCount("Received in MDB2: testBasicMDBTopic:");

        System.out.println("Number of messages received on MDB1 is " + count1
                           + " and number of messages received on MDB2 is " + count2);

        output = false;
        if (count1 > 1 && count2 > 1 && (count1 + count2 == 20)) {
            output = true;
        }

        assertTrue("testBasicMDBTopicNonDurable: output value is false", output);

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
