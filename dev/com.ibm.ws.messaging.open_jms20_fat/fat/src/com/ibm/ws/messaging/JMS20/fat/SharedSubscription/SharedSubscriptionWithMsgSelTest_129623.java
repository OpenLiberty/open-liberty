/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.SharedSubscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

import com.ibm.websphere.simplicity.log.Log;

@RunWith(FATRunner.class)
public class SharedSubscriptionWithMsgSelTest_129623 {
    private static final Class<?> c = SharedSubscriptionWithMsgSelTest_129623.class;

    //

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionWithMsgSelClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("SharedSubscriptionEngine");

    private static final String subscriptionAppName = "SharedSubscriptionWithMsgSel";
    private static final String subscriptionContextRoot = "SharedSubscriptionWithMsgSel";
    private static final String[] subscriptionPackages = new String[] {
        "sharedsubscriptionwithmsgsel.web",
        "sharedsubscriptionwithmsgsel.ejb" };

    // Relative to the server 'logs' folder.
    private static final String JMX_LOCAL_ADDRESS_REL_PATH = "state/com.ibm.ws.jmx.local.address";

    private static String readLocalAddress(LibertyServer libertyServer) throws FileNotFoundException, IOException {
        List<String> localAddressLines = TestUtils.readLines(libertyServer, JMX_LOCAL_ADDRESS_REL_PATH);
        // throws FileNotFoundException, IOException

        if ( localAddressLines.isEmpty() ) {
            throw new IOException("Empty JMX local address file" +
                                  " [ " + libertyServer.getLogsRoot() + " ]" +
                                  " [ " + JMX_LOCAL_ADDRESS_REL_PATH + " ]");
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
        return TestUtils.runInServlet(clientHostName, clientPort, subscriptionContextRoot, test, getLocalAddress());
        // throws IOException
    }

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

        String clientXml = "SharedSubscriptionDurClient.xml";
        if ( JakartaEE9Action.isActive() ) {
            Path clientXmlFile = Paths.get("lib/LibertyFATTestFiles", clientXml);
            JakartaEE9Action.transformApp(clientXmlFile);
            Log.info(c, "setUp", "Transformed server " + clientXmlFile);
        }
        clientServer.setServerConfigurationFile(clientXml);

        engineServer.startServer("SharedSubscriptionWithMsgSel_129623_Engine.log");
        setLocalAddress(readLocalAddress(engineServer)); // 'readLocalAddress' throws IOException

        clientServer.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
    }

    private static void restartServers() throws Exception {
        clientServer.stopServer();
        engineServer.stopServer();

        engineServer.startServer("SharedSubscriptionWithMsgSel_129623_Engine.log");
        setLocalAddress(readLocalAddress(engineServer)); // 'readLocalAddress' throws IOException

        clientServer.startServer("SharedSubscriptionWithMsgSel_129623_Client.log");
    }

    @AfterClass
    public static void tearDown() {
        // Remove the try / catch clauses once the following issue is resolved.
        // See https://github.com/OpenLiberty/open-liberty/issues/10931
        try {
            clientServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch (Exception e) {
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

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector() throws Exception {
        boolean testResult = runInServlet("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector");
        assertTrue("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
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

    @ExpectedFFDC( { "com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException",
                     "com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException" } )
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

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
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
    @Test
    public void testMultiSharedDurableConsumer_SecOff() throws Exception {
        clientServer.setMarkToEndOfLog();
        runInServlet("testBasicMDBTopic");
        
        // The servlet has sent 20 distinct messages which are received by either MDB1 or MDB2,
        // the MDB's run as multiple instances so the order the messages are received is unpredictable.
        // We allow up to 120 seconds to receive all of the messages,
        // although normally there should be minimal delay and anything more that 10 seconds means that the test infrastructure is not 
        // providing enough resources.
        long receiveStartMilliseconds = System.currentTimeMillis();
        int count = clientServer.waitForMultipleStringsInLogUsingMark(20, "Received in MDB[1-2]: testBasicMDBTopic:");
        assertEquals("Incorrect number of messages:"+count, count, 20);
        long receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
        
        clientServer.setMarkToEndOfLog();
        runInServlet("testBasicMDBTopic_TCP");
        receiveStartMilliseconds = System.currentTimeMillis();
        count = clientServer.waitForMultipleStringsInLogUsingMark(20, "Received in MDB[1-2]: testBasicMDBTopic_TCP:");
        assertEquals("Incorrect number of messages:"+count, count, 20);
        receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
    }
}
