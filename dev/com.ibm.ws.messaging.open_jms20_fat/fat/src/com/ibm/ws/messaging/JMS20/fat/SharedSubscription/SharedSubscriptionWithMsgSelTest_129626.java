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
public class SharedSubscriptionWithMsgSelTest_129626 {

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionWithMsgSelClient");

    public int occurrencesInLog(String text) throws Exception {
        return TestUtils.occurrencesInLog(clientServer, "trace.log", text);
    }

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    // SharedSubscriptionWithMsgSel\src\
    // sharedsubscriptionwithmsgsel\web\SharedSubscriptionWithMsgSelServlet.java

    private static final String subscriptionAppName = "SharedSubscriptionWithMsgSel";
    private static final String subscriptionContextRoot = "SharedSubscriptionWithMsgSel";
    private static final String[] subscriptionPackages = new String[] {
        "sharedsubscriptionwithmsgsel.web",
        "sharedsubscriptionwithmsgsel.ejb" };

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
        SharedSubscriptionWithMsgSelTest_129626.localAddress = localAddress;
    }

    private static String getLocalAddress() {
        return localAddress;
    }

    private static boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet( clientHostName, clientPort, subscriptionContextRoot, test, getLocalAddress() );
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("SharedSubscriptionEngine.xml"); // JMSContext_Server.xml

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        TestUtils.addDropinsWebApp(clientServer, subscriptionAppName, subscriptionPackages);
        clientServer.setServerConfigurationFile("SharedSubscriptionNonDurClient.xml"); // JMSContext_Client.xml

        engineServer.startServer("SharedSubscriptionWithMsgSel_129626_Engine.log");
        setLocalAddress( readLocalAddress(engineServer) ); // 'readLocalAddress' throws IOException
        clientServer.startServer("SharedSubscriptionWithMsgSel_129626_Client.log");
    }

    private static void restartServers() throws Exception {
        clientServer.stopServer();
        engineServer.stopServer();

        engineServer.startServer("SharedSubscriptionWithMsgSel_129626_Engine.log");
        setLocalAddress( readLocalAddress(engineServer) ); // 'readLocalAddress' throws IOException
        clientServer.startServer("SharedSubscriptionWithMsgSel_129626_Client.log");
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

    // 129626_1    JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName)
    // 129626_1_1  Creates a shared non-durable subscription with the specified
    //             name on the specified topic (if one does not already exist) and creates
    //             a consumer on that subscription. This method creates the non-durable
    //             subscription without a message selector.
    // 129626_1_4  Non-durable subscription is not persisted and will be deleted
    //             (together with any undelivered messages associated with it) when there
    //             are no consumers on it. The term "consumer" here means a MessageConsumer
    //             or JMSConsumer object in any client.

    // Bindings and Security Off

    @Test
    public void testCreateSharedNonDurable_B_SecOff() throws Exception {
        boolean testFailed = false;

        if ( !runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_create") ) {
            testFailed = true;
        }

        // restartServers();

        if ( !runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_consume") ) {
            testFailed = true;
        }

        assertFalse("testCreateSharedNonDurable_B_SecOff failed", testFailed);
    }

    // TCP and SecurityOff

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurable_TCP_SecOff() throws Exception {
        boolean testFailed = false;

        if ( !runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP") ) {
            testFailed = true;
        }

        // restartServers(); // throws Exception

        if ( !runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP") ) {
            testFailed = true;
        }

        assertFalse("testCreateSharedNonDurable_TCP_SecOff failed", testFailed);
    }

    // 129626_1_2 If a shared non-durable subscription already exists with the same
    //            name and client identifier (if set), and the same topic and message
    //            selector has been specified, then this method creates a JMSConsumer
    //            on the existing subscription.

    // Bindings and SecOff

    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_B_SecOff failed", testResult);
    }

    // mBeans is currently not working for TCP. Below testcase has
    // to be uncommented once mBeans is fixed.

    // @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP_SecOff failed", testResult);
    }

    // 129626_1_6 If a shared non-durable subscription already exists with the same name
    //            and client identifier (if set) but a different topic or message selector
    //            value has been specified, and there is a consumer already active (i.e. not
    //            closed) on the subscription, then a JMSRuntimeException will be thrown.

    // Bindings and Security Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_B_SecOff failed", testResult);
    }

    // TCP and SecurityOff

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", testResult);
    }

    // 129626_1_7 There is no restriction on durable subscriptions and shared non-durable
    //            subscriptions having the same name and clientId (which may be unset). Such
    //            subscriptions would be completely separate.

    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_coexist");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP");
        assertTrue("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed", testResult);
    }

    // 129626_1_9 InvalidDestinationRuntimeException - if an invalid topic is specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
        assertTrue("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test MDBMDB
    public void testMultiSharedNonDurableConsumer_SecOff() throws Exception {
        runInServlet("testBasicMDBTopic");
        Thread.sleep(1000);
        int count1 = occurrencesInLog("Received in MDB1: testBasicMDBTopic:");
        int count2 = occurrencesInLog("Received in MDB2: testBasicMDBTopic:");

        boolean testFailed = false;
        if ( !((count1 > 1) && (count2 > 1) && (count1 + count2 == 20)) ) {
            testFailed = true;
        }

        runInServlet("testBasicMDBTopic_TCP");
        Thread.sleep(1000);
        int count3 = occurrencesInLog("Received in MDB1: testBasicMDBTopic_TCP:");
        int count4 = occurrencesInLog("Received in MDB2: testBasicMDBTopic_TCP:");

        boolean testFailed_TCP = false;
        if ( !((count3 > 1) && (count4 > 1) && (count3 + count4 == 20)) ) {
            testFailed_TCP = true;
        }

        assertFalse("testBasicMDBTopicNonDurable failed [ " + count1 + " ] [ " + count2 + " ]", testFailed);
        assertFalse("testBasicMDBTopicNonDurable_TCP failed [ " + count3 + " ] [ " + count4 + " ]", testFailed_TCP);
    }
}
