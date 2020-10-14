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
import java.util.ArrayList;
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

public class SharedSubscriptionTest_129623 {

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
        SharedSubscriptionTest_129623.localAddress = localAddress;
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
        clientServer.setServerConfigurationFile("SharedSubscriptionClient.xml");
        TestUtils.addDropinsWebApp(clientServer, subscriptionAppName, subscriptionPackages);

        engineServer.startServer("SharedSubscriptionEngine_129623.log");
        clientServer.startServer("SharedSubscriptionClient_129623.log");

        setLocalAddress( readLocalAddress(engineServer) );
        // 'readLocalAddress' throws IOException
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

    //

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
        List<String> testFailures = new ArrayList<String>();

        String testName1 = "testCreateSharedDurableConsumer_create_B_SecOff";
        if ( !runInServlet(testName1) ) {
            testFailures.add(testName1);
        }
        String testName2 = "testCreateSharedDurableConsumer_create_Expiry_B_SecOff";
        if ( !runInServlet(testName2) ) {
            testFailures.add(testName2);
        }

        String testName3 = "testCreateSharedDurableConsumer_consume_B_SecOff";
        if ( !runInServlet(testName3) ) {
             testFailures.add(testName3);
        }
        String testName4 = "testCreateSharedDurableConsumer_consume_Expiry_B_SecOff";
        if ( !runInServlet(testName4) ) {
            testFailures.add(testName4);
        }

        if ( !testFailures.isEmpty() ) {
            assertTrue(
               "testCreateSharedDurableExpiry_B_SecOff failures: [ " + printString(testFailures) + " ]",
                testFailures.isEmpty() );
        }
    }

    private String printString(List<String> storage) {
        int numElements = storage.size();
        if ( numElements == 0 ) {
            return "{ **NONE** }";
        } else {
            StringBuilder printBuffer = new StringBuilder();

            printBuffer.append("{ ");
            printBuffer.append( storage.get(0) );

            for ( int elementNo = 1; elementNo < numElements; elementNo++ ) {
                printBuffer.append(", ");
                printBuffer.append( storage.get(elementNo) );
            }

            printBuffer.append(" }");

            return printBuffer.toString();
        }
    }

    // TCP and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableExpiry_TCP_SecOff() throws Exception {
        List<String> testFailures = new ArrayList<String>();

        String testName1 = "testCreateSharedDurableConsumer_create_TCP_SecOff";
        if ( !runInServlet(testName1) ) {
            testFailures.add(testName1);
        }
        String testName2 = "testCreateSharedDurableConsumer_create_Expiry_TCP_SecOff";
        if ( !runInServlet(testName2) ) {
            testFailures.add(testName2);
        }

        String testName3 = "testCreateSharedDurableConsumer_consume_TCP_SecOff";
        if ( !runInServlet(testName3) ) {
            testFailures.add(testName3);
        }

        String testName4 = "testCreateSharedDurableConsumer_consume_Expiry_TCP_SecOff";
        if ( !runInServlet(testName4) ) {
            testFailures.add(testName4);
        }

        if ( !testFailures.isEmpty() ) {
            assertTrue(
                "testCreateSharedDurableExpiry_TCP_SecOff failures: [ " + printString(testFailures) + " ]",
                testFailures.isEmpty() );
        }
    }

    // 129623_1_4 A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.
    // Bindings and Security Off

    @Test
    public void testCreateSharedDurableConsumer_unsubscribe_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_unsubscribe_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_unsubscribe_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Test
    public void testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff");
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
    // @Test BNDBND
    public void testMultiSharedDurConsumer_B_SecOff() throws Exception {
        runInServlet("testBasicMDBTopic");

        clientServer.setMarkToEndOfLog();

        int count1 = 0;
        String msg1;
        do {
            msg1 = clientServer.waitForStringInLogUsingMark(
                "Received in MDB1: testBasicMDBTopic:",
                clientServer.getMatchingLogFile("trace.log") );
            if ( msg1 != null ) {
                count1++;
            }
        } while ( msg1 != null );
        System.out.println("Messages received on MDB1 [ " + count1 + " ]");

        int count2 = 0;
        String msg2;
        do {
            msg2 = clientServer.waitForStringInLogUsingMark(
                "Received in MDB2: testBasicMDBTopic:",
                clientServer.getMatchingLogFile("trace.log") );
            if ( msg2 != null ) {
                count2++;
            }
        } while ( msg2 != null );
        System.out.println("Messages received on MDB2 [ " + count2 + " ]");

        boolean testFailed = false;
        if ( (count1 > 2) || (count2 > 2) || (count1 + count2 != 3) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            assertFalse(
                "testMultiSharedDurConsumer_B_SecOff failed;" +
                    " MDB1 messages [ " + count1 + " ];" +
                    " MDB2 messages [ " + count2 + " ]",
                testFailed );
        }
    }

    // TCP and SecurityOff
    // @Mode(TestMode.FULL)
    // @Test BNDBND
    public void testMultiSharedDurConsumer_TCP_SecOff() throws Exception {
        runInServlet("testBasicMDBTopic_TCP");

        clientServer.setMarkToEndOfLog();

        int count1 = 0;
        String msg1;
        do {
            String discardedMsg = clientServer.waitForStringInLog(
                "Received in MDB1: testBasicMDBTopic:",
                clientServer.getMatchingLogFile("trace.log"));
            msg1 = clientServer.waitForStringInLogUsingMark(
                 "Received in MDB1: testBasicMDBTopic:",
                 clientServer.getMatchingLogFile("trace.log"));

            if ( msg1 != null ) {
                count1++;
            }
        } while ( msg1 != null );
        System.out.println("Messages received on MDB1 [ " + count1 + " ]");

        int count2 = 0;
        String msg2;
        do {
            msg2 = clientServer.waitForStringInLogUsingMark(
                "Received in MDB2: testBasicMDBTopic:",
                clientServer.getMatchingLogFile("trace.log"));

            if ( msg2 != null ) {
                count2++;
            }
        } while ( msg2 != null );
        System.out.println("Messages received on MDB2 [ " + count2 + " ]");

        boolean testFailed = false;
        if ( (count1 > 2) || (count2 > 2) || (count1 + count2 != 3) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            assertFalse(
                "testMultiSharedDurConsumer_TCP_SecOff" +
                    " MDB1 messages [ " + count1 + " ];" +
                    " MDB2 messages [ " + count2 + " ]",
                testFailed );
        }
    }

    // 129623_1_7 If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.
    // Bindings and SecOff

    @Test
    public void testCreateSharedDurableConsumer_2Subscribers_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_2Subscribers_B_SecOff failed", testResult);
    }

    // TCP and SecOff

    @Test
    public void testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff failed", testResult);
    }

    // 129623_1_8 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    @Test
    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException")
    @Test
    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff");
        assertNotNull("Test testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff failed", testResult);
    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_JRException_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_JRException_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    // @Test
    public void testCreateSharedDurableConsumer_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_JRException_TCP_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_JRException_TCP_SecOff failed", testResult);
    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.
    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableUndurableConsumer_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_B_SecOff");
        assertTrue("Test testCreateSharedDurableUndurableConsumer_JRException_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
    // @Test
    public void testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff");
        assertTrue("Test testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff failed", testResult);
    }

    // 129623_1_12 InvalidDestinationRuntimeException - if an invalid topic is specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff failed", testResult);

    }

    // 129623_1_13 Case where name is null and empty string

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumer_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Null_B_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_Null_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    // @Test
    public void testCreateSharedDurableConsumer_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Null_TCP_SecOff");
        assertTrue("Test testCreateSharedDurableConsumer_Null_TCP_SecOff failed", testResult);
    }

    // Defect 174691

    @Test
    public void testCreateSharedConsumer_Qsession_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedConsumer_Qsession_B_SecOff");
        assertTrue("Test testCreateSharedConsumer_Qsession_B_SecOff failed", testResult);
    }

    // Defect 174713

    @Test
    public void testUnsubscribeInvalidSID_Tsession_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testUnsubscribeInvalidSID_Tsession_B_SecOff");
        assertTrue("Test testUnsubscribeInvalidSID_Tsession_B_SecOff failed", testResult);
    }
}
