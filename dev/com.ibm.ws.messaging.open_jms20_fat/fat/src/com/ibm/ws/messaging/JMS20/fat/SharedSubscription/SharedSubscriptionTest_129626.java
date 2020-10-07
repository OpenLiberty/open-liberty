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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

@Mode(TestMode.FULL)
public class SharedSubscriptionTest_129626 {

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionClient");

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionEngine");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String subscriptionAppName = "SharedSubscription";
    private static final String subscriptionContextRoot = "SharedSubscription";
    private static final String[] subscriptionPackages = new String[] {
        "sharedsubscription.web",
        "sharedsubscription.ejb" };

    //

    // Relative to the server 'logs' folder.
    private static final String JMX_LOCAL_ADDRESS_REL_PATH = "state/com.ibm.ws.jmx.local.address";

    private static String readLocalAddress(LibertyServer libertyServer) throws FileNotFoundException, IOException {
        List<String> localAddressLines = TestUtils.readLines(libertyServer, JMX_LOCAL_ADDRESS_REL_PATH);
        // throws FileNotFoundException, IOException

        if ( localAddressLines.isEmpty() ) {
            throw new IOException("Empty JMX local address file [ " + libertyServer.getLogsRoot() + " ] [ " + JMX_LOCAL_ADDRESS_REL_PATH + " ]");
        }

        return localAddressLines.get(0);
    }

    private static String localAddress;

    private static void setLocalAddress(String localAddress) {
        System.out.println("Local address [ " + localAddress + " ]");
        SharedSubscriptionTest_129626.localAddress = localAddress;
    }

    private static String getLocalAddress() {
        return localAddress;
    }

    private static boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet( clientHostName, clientPort, subscriptionContextRoot, test, getLocalAddress() );
        // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("SharedSubscriptionEngine.xml"); // TestServer1.xml

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("SharedSubscriptionClient.xml"); // JMSContext.xml
        TestUtils.addDropinsWebApp(clientServer, subscriptionAppName, subscriptionPackages);

        engineServer.startServer("SharedSubscriptionTestServer_129626.log");
        setLocalAddress( readLocalAddress(engineServer) ); // 'readLocalAddress' throws IOException

        clientServer.startServer("SharedSubscriptionTestClient_129626.log");
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    // 129626_1   JMSConsumer createSharedConsumer(Topic topic,String sharedSubscriptionName)
    // 129626_1_1 Creates a shared non-durable subscription with the specified name on the
    //            specified topic (if one does not already exist) and creates a consumer on
    //            that subscription. This method creates the non-durable subscription without
    //            a message selector.
    // 129626_1_4 Non-durable subscription is not persisted and will be deleted (together with
    //            any undelivered messages associated with it) when there are no consumers on
    //            it. The term "consumer" here means a MessageConsumer or JMSConsumer object
    //            in any client.

    // Bindings and Security Off

    @Test
    public void testCreateSharedNonDurable_B_SecOff() throws Exception {
        boolean testFailed = false;
        if ( !runInServlet("testCreateSharedNonDurableConsumer_create_B_SecOff") ) {
            testFailed = true;
        }
        if ( !runInServlet("testCreateSharedNonDurableConsumer_consume_B_SecOff") ) {
            testFailed = true;
        }

        assertFalse("Test testCreateSharedNonDurable_B_SecOff failed", testFailed);
    }

    // TCP and SecurityOff

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurable_TCP_SecOff() throws Exception {
        boolean testFailed = false;

        if ( !runInServlet("testCreateSharedNonDurableConsumer_create_TCP_SecOff") ) {
            testFailed = true;
        }
        if ( !runInServlet("testCreateSharedNonDurableConsumer_consume_TCP_SecOff") ) {
            testFailed = true;
        }

        assertFalse("Test testCreateSharedNonDurable_TCP_SecOff failed", testFailed);
    }

    // 129626_1_2  If a shared non-durable subscription already exists with the
    //             same name and client identifier (if set), and the same topic
    //             and message selector has been specified, then this method
    //             creates a JMSConsumer on the existing subscription.

    // Bindings and SecOff

    @Test
    public void testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_2Subscribers_TCP failed", testResult);
    }

    /*
     * // 129626_1_3 A non-durable shared subscription is used by a client which
     * //            needs to be able to share the work of receiving messages from
     * //            a topic subscription amongst multiple consumers. A non-durable
     * //            shared subscription may therefore have more than one consumer.
     * //            Each message from the subscription will be delivered to only
     * //            one of the consumers on that subscription
     *
     * // Bindings and Sec Off
     * 
     * // @Test
     * public void testMultiSharedNonDurableConsumer_B_SecOff() throws Exception {
     * clientServer.stopServer();
     * clientServer.setServerConfigurationFile("topicMDBND_server.xml");
     * clientServer.startServer();
     * 
     * runInServlet("testBasicMDBTopicNonDurable");
     * 
     * clientServer.setMarkToEndOfLog();
     * String msg = null;
     * int count1 = 0;
     * int count2 = 0;
     * int i = 0;
     * do {
     * // msg =
     * // clientServer.waitForStringInLog("Received in MDB1: testBasicMDBTopic:",
     * // clientServer.getMatchingLogFile("trace.log"));
     * if (i == 0)
     * msg = clientServer.waitForStringInLogUsingMark(
     * "Received in MDB1: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * else
     * msg = clientServer.waitForStringInLogUsingLastOffset(
     * "Received in MDB1: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * if (msg != null) {
     * count1++;
     * i++;
     * }
     * } while (msg != null);
     * i = 0;
     * do {
     * if (i == 0)
     * msg = clientServer.waitForStringInLogUsingMark(
     * "Received in MDB2: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * else
     * msg = clientServer.waitForStringInLogUsingLastOffset(
     * "Received in MDB2: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * if (msg != null) {
     * count2++;
     * i++;
     * }
     * } while (msg != null);
     * System.out.println("Number of messages received on MDB1 is " + count1
     * + " and number of messages received on MDB2 is " + count2);
     * 
     * boolean output = false;
     * if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
     * output = true;
     * }
     * 
     * clientServer.stopServer();
     * clientServer.setServerConfigurationFile("JMSContext.xml");
     * clientServer.startServer();
     * 
     * assertTrue("output value is false", output);
     * 
     * }
     * 
     * @Mode(TestMode.FULL)
     * // @Test
     * public void testMultiSharedNonDurableConsumer_TCP_SecOff() throws Exception {
     * 
     * engineServer.stopServer();
     * engineServer.setServerConfigurationFile("topicMDBServer.xml");
     * engineServer.startServer();
     * 
     * clientServer.stopServer();
     * clientServer.setServerConfigurationFile("topicMDB_TcpIp_server.xml");
     * clientServer.startServer();
     * 
     * runInServlet("testBasicMDBTopicNonDurable_TCP");
     * clientServer.setMarkToEndOfLog();
     * String msg = null;
     * int count1 = 0;
     * int count2 = 0;
     * int i = 0;
     * do {
     * msg = clientServer.waitForStringInLog(
     * "Received in MDB1: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * if (i == 0)
     * msg = clientServer.waitForStringInLogUsingMark(
     * "Received in MDB1: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * else
     * msg = clientServer.waitForStringInLogUsingLastOffset(
     * "Received in MDB1: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * if (msg != null) {
     * count1++;
     * i++;
     * }
     * } while (msg != null);
     * i = 0;
     * do {
     * if (i == 0)
     * msg = clientServer.waitForStringInLogUsingMark(
     * "Received in MDB2: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * else
     * msg = clientServer.waitForStringInLogUsingLastOffset(
     * "Received in MDB2: testBasicMDBTopic:",
     * clientServer.getMatchingLogFile("trace.log"));
     * if (msg != null) {
     * count2++;
     * i++;
     * }
     * } while (msg != null);
     * System.out.println("Number of messages received on MDB1 is " + count1
     * + " and number of messages received on MDB2 is " + count2);
     * 
     * boolean output = false;
     * if (count1 <= 2 && count2 <= 2 && (count1 + count2 == 3)) {
     * output = true;
     * }
     * 
     * engineServer.stopServer();
     * clientServer.stopServer();
     * clientServer.setServerConfigurationFile("JMSContext.xml");
     * engineServer.setServerConfigurationFile("TestEngineServer.xml");
     * 
     * clientServer.startServer();
     * engineServer.startServer();
     * 
     * assertTrue("output value is false", output);
     * }
     */

    // 129626_1_6 If a shared non-durable subscription already exists with the
    //            same name and client identifier (if set) but a different topic
    //            or message selector value has been specified, and there is a
    //            consumer already active (i.e. not closed) on the subscription,
    //            then a JMSRuntimeException will be thrown.

    // Bindings and Security Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_JRException_B_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_JRException_B_SecOff failed", testResult);
    }

    // TCP and SecurityOff

    @ExpectedFFDC("com.ibm.websphere.sib.exception.SIErrorException , com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    // @Test
    public void testCreateSharedNonDurableConsumer_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_JRException_TCP_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_JRException_TCP_SecOff failed", testResult);
    }

    // 129626_1_7  There is no restriction on durable subscriptions and shared
    //             non-durable subscriptions having the same name and clientId
    //             (which may be unset). Such subscriptions would be completely
    //             separate.

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_coexist_B_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_coexist_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed", testResult);
    }

    //  129626_1_9  InvalidDestinationRuntimeException - if an invalid topic is specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff");
        assertTrue("Test testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff failed", testResult);
    }
}
