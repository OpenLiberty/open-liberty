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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class SharedSubscriptionWithMsgSelTest_129623 {

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionWithMsgSelClient");

    public int occurrencesInLog(String text) throws Exception {
        return TestUtils.occurrencesInLog(clientServer, "trace.log", text);
    }

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

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
        SharedSubscriptionWithMsgSelTest_129623.localAddress = localAddress;
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
        engineServer.setServerConfigurationFile("SharedSubscriptionEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        TestUtils.addDropinsWebApp(clientServer, subscriptionAppName, subscriptionPackages);
        clientServer.setServerConfigurationFile("SharedSubscriptionDurClient.xml");

        engineServer.startServer("SharedSubscriptionWithMsgSel_129623_Engine.log");
        setLocalAddress( readLocalAddress(engineServer) ); // 'readLocalAddress' throws IOException

        clientServer.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
    }

    private static void restartServers() throws Exception {
        clientServer.stopServer();
        engineServer.stopServer();

        engineServer.startServer("SharedSubscriptionWithMsgSel_129623_Engine.log");
        setLocalAddress( readLocalAddress(engineServer) ); // 'readLocalAddress' throws IOException

        clientServer.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
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

    @Test
    public void testSharedDurConsumerWithMsgSelector() throws Exception {
        boolean val1 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create");
        boolean val2 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry");

        // restartServers(); // throws Exception

        boolean val3 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consume");
        boolean val4 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry");

        assertTrue("testSharedDurConsumerWithMsgSelector failed", (val1 && val2 && val3 && val4));
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSharedDurConsumerWithMsgSelector_TcpIp() throws Exception {
        boolean val1 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_TCP");
        boolean val2 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP");

        // restartServers(); // throws Exception

        boolean val3 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP");
        boolean val4 = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_consume_TCP");

        assertTrue("testSharedDurConsumerWithMsgSelector_TcpIp failed", (val1 && val2 && val3 && val4));
    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_SecOff failed", testResult);

    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers failed", testResult);
    }

    // @Test // TODO
    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP failed", testResult);
    }

    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic failed", testResult);
    }

    // @Test // TODO
    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_Null_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_Null");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_Null_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP_SecOff failed", testResult);
    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_JRException_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
    // @Test // TODO
    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", testResult);
    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.
    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException");
        assertTrue("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP");
        assertTrue("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test // TODO MDBMDB
    public void testMultiSharedDurableConsumer_SecOff() throws Exception {
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

        boolean testFailed_TCP = true;
        if ( !((count3 > 1) && (count4 > 1) && (count3 + count4 == 20)) ) {
            testFailed_TCP = true;
        }

        assertFalse("testBasicMDBTopicDurableShared failed testBasicMDBTopic [ " + count1 + " ] [ " + count2 + " ]", testFailed);
        assertFalse("testBasicMDBTopicDurableShared failed testBasicMDBTopic_TCP [ " + count3 + " ] [ " + count4 + " ]", testFailed_TCP);
    }
}
