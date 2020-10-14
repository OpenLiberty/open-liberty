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

public class DurableUnsharedTest {

    private static final LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("DurableUnsharedEngine");
    private static final LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("DurableUnsharedClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String durableUnsharedAppName = "DurableUnshared";
    private static final String durableUnsharedContextRoot = "DurableUnshared";
    private static final String[] durableUnsharedPackages =
        new String[] { "durableunshared.web" };

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
        DurableUnsharedTest.localAddress = localAddress;
    }

    private static String getLocalAddress() {
        return localAddress;
    }

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet( clientHostName, clientPort, durableUnsharedContextRoot, test, getLocalAddress() );
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("DurableUnsharedEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        TestUtils.addDropinsWebApp(clientServer, durableUnsharedAppName, durableUnsharedPackages);
        clientServer.setServerConfigurationFile("DurableUnsharedClient.xml");

        engineServer.startServer("DurableUnshared_Engine.log");
        clientServer.startServer("DurableUnshared_Client.log");

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

    @Test
    public void testCreateUnSharedDurableExpiry_B_SecOff() throws Exception {
        boolean result1 = runInServlet("testCreateUnSharedDurableConsumer_create");
        boolean result2 = runInServlet("testCreateUnSharedDurableConsumer_create_Expiry");

        boolean result3 = runInServlet("testCreateUnSharedDurableConsumer_consume");
        boolean result4 = runInServlet("testCreateUnSharedDurableConsumer_consume_Expiry");

        assertTrue( "testCreateSharedDurableExpiry_B_SecOff failed",
                    (result1 && result2 && result3 && result4) );
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableExpiry_TCP_SecOff() throws Exception {
        boolean result1 = runInServlet("testCreateUnSharedDurableConsumer_create_TCP");
        boolean result2 = runInServlet("testCreateUnSharedDurableConsumer_create_Expiry_TCP");

        boolean result3 = runInServlet("testCreateUnSharedDurableConsumer_consume_TCP");
        boolean result4 = runInServlet("testCreateUnSharedDurableConsumer_consume_Expiry_TCP");

        assertTrue( "testCreateSharedDurableExpiry_TCP_SecOff failed",
                    (result1 && result2 && result3 && result4) );
    }

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_JRException");
        assertTrue("testCreateUnSharedDurableConsumer_JRException_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_JRException_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_JRException_TCP_SecOff failed", testResult);
    }

    // @Test
    public void testMultiUnSharedDurConsumer_B_SecOff() throws Exception {
        clientServer.setServerConfigurationFile("topicMDB_server.xml");
        clientServer.startServer();

        runInServlet("testBasicMDBTopic");

        clientServer.setMarkToEndOfLog();
        String msg = null;
        int count1 = 0;
        int count2 = 0;
        int i = 0;
        do {
            // msg =
            // clientServer.waitForStringInLog("Received in MDB1: testBasicMDBTopic:",
            // clientServer.getMatchingLogFile("trace.log"));
            if (i == 0)
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            else
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count1++;
                i++;
            }
        } while (msg != null);
        i = 0;
        do {
            if (i == 0)
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            else
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
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

        clientServer.stopServer();

    }

    // TCP and SecurityOff
    // @Test
    public void testMultiUnSharedDurConsumer_TCP_SecOff() throws Exception {
        engineServer.stopServer();
        engineServer.setServerConfigurationFile("topicMDBServer.xml");
        engineServer.startServer();

        clientServer.stopServer();
        clientServer.setServerConfigurationFile("topicMDB_TcpIp_server.xml");
        clientServer.startServer();

        runInServlet("testBasicMDBTopic_TCP");
        clientServer.setMarkToEndOfLog();
        String msg = null;
        int count1 = 0;
        int count2 = 0;
        int i = 0;
        do {
            msg = clientServer.waitForStringInLog(
                                            "Received in MDB1: testBasicMDBTopic:",
                                            clientServer.getMatchingLogFile("trace.log"));
            if (i == 0)
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            else
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB1: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            if (msg != null) {
                count1++;
                i++;
            }
        } while (msg != null);
        i = 0;
        do {
            if (i == 0)
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
            else
                msg = clientServer.waitForStringInLogUsingMark(
                                                         "Received in MDB2: testBasicMDBTopic:",
                                                         clientServer.getMatchingLogFile("trace.log"));
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

        engineServer.stopServer();
        clientServer.stopServer();
        clientServer.setServerConfigurationFile("JMSContext.xml");
        engineServer.setServerConfigurationFile("TestServer1.xml");

        clientServer.startServer();
        engineServer.startServer();
    }

    // @Test
    public void testCreateUnSharedDurableConsumer_2Subscribers_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers");
        assertTrue("Test testCreateUnSharedDurableConsumer_2Subscribers_SecOff failed", testResult);
    }

    // TCP and SecOff

    // @Test
    public void testCreateUnSharedDurableConsumer_2Subscribers_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2Subscribers_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_2Subscribers_TCP_SecOff failed", testResult);
    }

    // @Test
    public void testCreateUnSharedDurableConsumer_2SubscribersDiffTopic() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic");
        assertTrue("Test testCreateUnSharedDurableConsumer_2SubscribersDiffTopic failed", testResult);
    }

    // @Test
    public void testCreateUnSharedDurableConsumer_2SubscribersDiffTopic_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_2SubscribersDiffTopic_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableUndurableConsumer_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException");
        assertTrue("Test testCreateUnSharedDurableUndurableConsumer_JRException_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
    // @Test
    public void testCreateUnSharedDurableUndurableConsumer_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_JRException_TCP");
        assertTrue("Test testCreateUnSharedDurableUndurableConsumer_JRException_TCP_SecOff failed", testResult);
    }

    // 129623_1_12 InvalidDestinationRuntimeException if an invalid topic is specified.

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination");
        assertTrue("Test testCreateUnSharedDurableConsumer_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_InvalidDestination_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // Case where name is null and empty string

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Null");
        assertTrue("Test testCreateUnSharedDurableConsumer_Null_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Null_TCP");
        assertTrue("Test testCreateUnSharedDurableConsumer_Null_TCP_SecOff failed", testResult);
    }

    //

    @Test
    public void testCreateUnSharedDurableExpiry_Sel_B_SecOff() throws Exception {
        boolean result1 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create");
        boolean result2 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_Expiry");
        boolean result3 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume");
        boolean result4 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_Expiry");

        assertTrue( "testCreateSharedDurableExpiry_B_SecOff failed",
                    (result1 && result2 && result3 && result4) );
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableExpiry_Sel_TCP_SecOff() throws Exception {
        boolean result1 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_TCP");
        boolean result2 = runInServlet("testCreateUnSharedDurableConsumer_Sel_create_Expiry_TCP");
        boolean result3 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_TCP");
        boolean result4 = runInServlet("testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP");

        assertTrue( "testCreateSharedDurableExpiry_TCP_SecOff failed",
                    (result1 && result2 && result3 && result4) );
    }

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_JRException");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_JRException_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_JRException_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_JRException_TCP_SecOff failed", testResult);
    }


    // @Test
    public void testCreateUnSharedDurableConsumer_Sel_2Subscribers_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_2Subscribers");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2Subscribers_SecOff failed", testResult);
    }

    // TCP and SecOff

    // @Test
    public void testCreateUnSharedDurableConsumer_Sel_2Subscribers_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_2Subscribers_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2Subscribers_TCP_SecOff failed", testResult);
    }

    // @Test
    public void testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic failed", testResult);
    }

    // @Test
    public void testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableUndurableConsumer_Sel_JRException_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_Sel_JRException");
        assertTrue("testCreateUnSharedDurableUndurableConsumer_Sel_JRException_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException")
    @Mode(TestMode.FULL)
    // @Test
    public void testCreateUnSharedDurableUndurableConsumer_Sel_JRException_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableUndurableConsumer_Sel_JRException_TCP");
        assertTrue("testCreateUnSharedDurableUndurableConsumer_Sel_JRException_TCP_SecOff failed", testResult);
    }

    // InvalidDestinationRuntimeException if an invalid topic is specified.

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_InvalidDestination");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // Case where name is null and empty string

    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_Null");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_Null_B_SecOff failed", testResult);
    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateUnSharedDurableConsumer_Sel_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumer_Sel_Null_TCP");
        assertTrue("testCreateUnSharedDurableConsumer_Sel_Null_TCP_SecOff failed", testResult);
    }
}
