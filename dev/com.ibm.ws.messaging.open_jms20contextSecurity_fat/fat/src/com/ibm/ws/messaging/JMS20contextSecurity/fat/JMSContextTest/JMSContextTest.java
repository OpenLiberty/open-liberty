/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20contextSecurity.fat.JMSContextTest;

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
import org.junit.runner.RunWith;

import com.ibm.ws.messaging.JMS20contextSecurity.fat.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JMSContextTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean sVal = false;
    boolean testResult;

    private static boolean runInServlet(String test) throws IOException {

        boolean result;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext?test="
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
                                            "serverLTPAKeys/ltpaFIPS.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/mykey.jks");

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");

        TestUtils.addDropinsWebApp(server, "JMSContext", "web");

        startAppservers();
    }

    /**
     * Start both the JMSConsumerClient local and remote messaging engine AppServers.
     *
     * @throws Exception
     */
    private static void startAppservers() throws Exception {
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server.startServer("JMSConsumerTestClient.log");
        server1.startServer("JMSContextServer.log");

        // CWWKF0011I: The TestServer1 server is ready to run a smarter planet. The TestServer1 server started in 6.435 seconds.
        // CWSID0108I: JMS server has started.
        // CWWKS4105I: LTPA configuration is ready after 4.028 seconds.
        for (String messageId : new String[] { "CWWKF0011I.*", "CWSID0108I.*", "CWWKS4105I.*" }) {
            String waitFor = server.waitForStringInLog(messageId, server.getMatchingLogFile("messages.log"));
            assertNotNull("Server message " + messageId + " not found", waitFor);
            waitFor = server1.waitForStringInLog(messageId, server1.getMatchingLogFile("messages.log"));
            assertNotNull("Server1 message " + messageId + " not found", waitFor);
        }
    }

    // Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext()

    // 118058_1_1 :Creation of JMSContext from Connection factory.
    // Bindings and Security on

    @Test
    public void testCreateContext_B_SecOn() throws Exception {

        testResult = runInServlet("testCreateContext_B_SecOn");
        assertTrue("Test testCreateContext_B_SecOn failed", testResult);

    }

    // TCP and Security on ( with ssl)

    @Test
    public void testCreateContext_TCP_SecOn() throws Exception {
        testResult = runInServlet("testCreateContext_TCP_SecOn");
        assertTrue("Test testCreateContext_TCP_SecOn failed", testResult);

    }

    // 118058_5: JMSSecurityRuntimeException - if client authentication fails
    // due to an invalid user name or password.

    // Bindings and Security On

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC(value = { "javax.resource.ResourceException",
                            "javax.resource.spi.ResourceAllocationException",
                            "com.ibm.wsspi.sib.core.exception.SIAuthenticationException" })
    public void testFailAuth_B_SecOn() throws Exception {
        testResult = runInServlet("testFailAuth_B_SecOn");
        assertTrue("Test testFailAuth_B_SecOn failed", testResult);

    }

    // TCP and Security On (ssl)

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC(value = { "javax.resource.ResourceException",
                            "javax.resource.spi.ResourceAllocationException",
                            "com.ibm.wsspi.sib.core.exception.SIAuthenticationException" })
    public void testFailAuth_TCP_SecOn() throws Exception {

        testResult = runInServlet("testFailAuth_TCP_SecOn");
        assertTrue("testFailAuth_TCP_SecOn", testResult);

    }

    // 118058_2_1 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName, String password)

    // Bindings and Security On

    @Test
    public void testcreateContextwithUser_B_SecOn() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_B_SecOn");

        assertTrue("Test testcreateContextwithUser_B_SecOn failed", testResult);

    }

    // TCP and Security On (ssl)

    @Test
    public void testcreateContextwithUser_TCP_SecOn() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_TCP_SecOn");
        assertTrue("Test testcreateContextwithUser_TCP_SecOn failed",
                   testResult);

    }

    // 118058_4 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName,String password, int

    // Bindings and Sec on
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_B_SecOn() throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_B_SecOn");

        assertTrue("Test testcreateContextwithUserSessionMode_B_SecOn failed",
                   testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_TCP_SecOn() throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_TCP_SecOn");

        assertTrue(
                   "Test testcreateContextwithUserSessionMode_TCP_SecOn failed",
                   testResult);

    }

    // 118061_2 Verify creation of Object message from JMSContext.
    // createObjectMessage() . Perform a getObject,setObject and getBody. Send
    // and Receive
    // Bindings and Security On

    @Test
    public void testCreateObjectMessage_B_SecOn() throws Exception {

        testResult = runInServlet("testCreateObjectMessage_B_SecOn");
        assertTrue("Test testCreateObjectMessage_B_SecOn failed", testResult);

    }

    // TCP and Security On (ssl)
    @Test
    public void testCreateObjectMessage_TCP_SecOn() throws Exception {

        testResult = runInServlet("testCreateObjectMessage_TCP_SecOn");
        assertTrue("Test testCreateObjectMessage_TCP_SecOn failed", testResult);

    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text). Send and Receive

    // Bindings and Security On

    @Test
    public void testCreateTextMessageStr_B_SecOn() throws Exception {

        testResult = runInServlet("testCreateTextMessageStr_B_SecOn");
        assertTrue("Test testCreateTextMessageStr_B_SecOn failed", testResult);

    }

    // TCP and Security On (ssl)
    @Test
    public void testCreateTextMessageStr_TCP_SecOn() throws Exception {

        testResult = runInServlet("testCreateTextMessageStr_TCP_SecOn");
        assertTrue("Test testCreateTextMessageStr_TCP_SecOn failed", testResult);

    }

    // 118061_14 Verify set and get operation on Message header field
    // JMSCorrelationIDAsBytes
    // Bindings and Security On

    @Test
    public void testJMSCorrelationIDAsBytes_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSCorrelationIDAsBytes_B_SecOn");
        assertTrue("Test testJMSCorrelationIDAsBytes_B_SecOn failed",
                   testResult);

    }

    // TCP and Security On (ssl)
    @Test
    public void testJMSCorrelationIDAsBytes_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSCorrelationIDAsBytes_TCP_SecOn");
        assertTrue("Test testJMSCorrelationIDAsBytes_TCP_SecOn failed",
                   testResult);

    }

    // 118062_1 Test with createBrowser(Queue queue)
    // 118062_1_1 CreatesQueueBrowser object to peek at the messages on the
    // specified queue.

    // Bindings and Security On

    @Test
    public void testcreateBrowser_B_SecOn() throws Exception {

        testResult = runInServlet("testcreateBrowser_B_SecOn");
        assertTrue("Test testcreateBrowser_B_SecOn failed", testResult);

    }

    // TCP and Security On

    @Test
    public void testcreateBrowser_TCP_SecOn() throws Exception {
        testResult = runInServlet("testcreateBrowser_TCP_SecOn");
        assertTrue("Test testcreateBrowser_TCP_SecOn failed", testResult);

    }

    // 118062_2: QueueBrowser createBrowser(Queue queue,String messageSelector)
    // 118062_2_1 Creates a QueueBrowser object to peek the
    // messages on the specified queue using a message selector.

    // Bindings and Sec On

    @Test
    public void testcreateBrowser_MessageSelector_B_SecOn() throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_B_SecOn");
        assertTrue("Test testcreateBrowser_MessageSelector_B_SecOn failed",
                   testResult);

    }

    // TCP and Sec On

    @Test
    public void testcreateBrowser_MessageSelector_TCP_SecOn() throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_TCP_SecOn");
        assertTrue("Test testcreateBrowser_MessageSelector_TCP_SecOn failed",
                   testResult);

    }

    // 118071_1 JMSProducer send(Destination destination,Message message)
    // 118071_1_1_Q : Sends the message to the specified queue using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security On

    @Test
    public void testJMSProducerSendMessage_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_B_SecOn");
        assertTrue("Test testJMSProducerSendMessage_B_SecOn failed", testResult);

    }

    // TCP/IP and Security On

    @Test
    public void testJMSProducerSendMessage_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_TCP_SecOn");
        assertTrue("Test testJMSProducerSendMessage_TCP_SecOn failed",
                   testResult);

    }

    // 118071_1_7_T Sends a message to the specified topic using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security On

    @Test
    public void testJMSProducerSendMessage_Topic_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_Topic_B_SecOn");
        assertTrue("Test testJMSProducerSendMessage_Topic_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On

    @Test
    public void testJMSProducerSendMessage_Topic_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_Topic_TCP_SecOn");
        assertTrue("Test testJMSProducerSendMessage_Topic_TCP_SecOn failed",
                   testResult);

    }

    // 118071_2 JMSProducer send(Destination destination, String body)

    // 118071_2_1_Q Send a TextMessage with the specified body to the specified
    // queue, using any send options, message properties and message headers
    // that have been defined on this JMSProducer.

    // Bindings and Security On

    @Test
    public void testJMSProducerSendTextMessage_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_B_SecOn");
        assertTrue("Test testJMSProducerSendTextMessage_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On

    @Test
    public void testJMSProducerSendTextMessage_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_TCP_SecOn");
        assertTrue("Test testJMSProducerSendTextMessage_TCP_SecOn failed",
                   testResult);

    }

    // 118071_2_8_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic // Bindings and Security On

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOn() throws Exception {
        testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOn");
        assertTrue(
                   "Test testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOn");
        assertTrue(
                   "Test testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOn failed",
                   testResult);

    }

    // 118071_3_5_Topic Send a MapMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    // Bindings and Security On

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessage_Topic_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMapMessage_Topic_B_SecOn");
        assertTrue("Test testJMSProducerSendMapMessage_Topic_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessage_Topic_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendMapMessage_Topic_TCP_SecOn");
        assertTrue("Test testJMSProducerSendMapMessage_Topic_TCP_SecOn failed",
                   testResult);
    }

    // 118071_4 JMSProducer send(Destination destination,byte[] body)

    // 118071_4_1_Queue Send a BytesMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_B_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendByteMessage_B_SecOn");
        assertTrue("Test testJMSProducerSendByteMessage_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendByteMessage_TCP_SecOn");
        assertTrue("Test testJMSProducerSendByteMessage_TCP_SecOn failed",
                   testResult);
    }

    // 118071_5_5_Topic Send an ObjectMessage with the specified body to the
    // specified topic using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Test
    public void testJMSProducerSendObjectMessage_Topic_B_SecOn() throws Exception {
        testResult = runInServlet("testJMSProducerSendObjectMessage_Topic_B_SecOn");
        assertTrue(
                   "Test testJMSProducerSendObjectMessage_Topic_B_SecOn failed",
                   testResult);

    }

    // TCP/IP and Security On

    @Test
    public void testJMSProducerSendObjectMessage_Topic_TCP_SecOn() throws Exception {

        testResult = runInServlet("testJMSProducerSendObjectMessage_Topic_TCP_SecOn");
        assertTrue(
                   "Test testJMSProducerSendObjectMessage_Topic_TCP_SecOn failed",
                   testResult);

    }

    // 118073_2 boolean propertyExists(String name)
    // 118073_2_1 Returns true if a message property with the specified name has
    // been set on this JMSProducer
    // Bindings and Security On

    @Test
    public void testPropertyExists_B_SecOn() throws Exception {

        testResult = runInServlet("testPropertyExists_B_SecOn");
        assertTrue("Test testPropertyExists_B_SecOn failed", testResult);
    }

    // TCP and Security On

    @Test
    public void testPropertyExists_TCP_SecOn() throws Exception {
        testResult = runInServlet("testPropertyExists_TCP_SecOn");
        assertTrue("Test testPropertyExists_TCP_SecOn failed", testResult);

    }

    // 118073_3 JMSProducer setDisableMessageID(boolean value)
    // Bindings and Security On

    @Test
    public void testSetDisableMessageID_B_SecOn() throws Exception {
        testResult = runInServlet("testSetDisableMessageID_B_SecOn");
        assertTrue("Test testSetDisableMessageID_B_SecOn failed", testResult);
    }

    // TCP and Security On

    @Test
    public void testSetDisableMessageID_TCP_SecOn() throws Exception {
        testResult = runInServlet("testSetDisableMessageID_TCP_SecOn");
        assertTrue("Test testSetDisableMessageID_TCP_SecOn failed", testResult);

    }

    // 118073_5 JMSProducer setDisableMessageTimestamp(boolean value)
    // Bindings and Security Off

    @Test
    public void testSetDisableMessageTimestamp_B_SecOn() throws Exception {
        testResult = runInServlet("testSetDisableMessageTimestamp_B_SecOn");
        assertTrue("Test testSetDisableMessageTimestamp_B_SecOn failed",
                   testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetDisableMessageTimestamp_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetDisableMessageTimestamp_TCP_SecOn");
        assertTrue("Test testSetDisableMessageTimestamp_TCP_SecOn failed",
                   testResult);

    }

    // 118073_9 JMSProducer setPriority(int priority)
    // Bindings and Security On

    @Test
    public void testPriority_B_SecOn() throws Exception {
        testResult = runInServlet("testPriority_B_SecOn");
        assertTrue("Test testPriority_B_SecOn failed", testResult);

    }

    // TCP and Security On

    @Test
    public void testPriority_TCP_SecOn() throws Exception {
        testResult = runInServlet("testPriority_TCP_SecOn");
        assertTrue("Test testPriority_TCP_SecOn failed", testResult);

    }

    @Test
    public void testSetGetJMSReplyTo_B_SecOn() throws Exception {
        testResult = runInServlet("testSetGetJMSReplyTo_B_SecOn");
        assertTrue("Test testSetGetJMSReplyTo_B_SecOn failed", testResult);

    }

    @Test
    public void testSetGetJMSReplyTo_TCP_SecOn() throws Exception {

        testResult = runInServlet("testSetGetJMSReplyTo_TCP_SecOn");
        assertTrue("Test testSetGetJMSReplyTo_TCP_SecOn failed", testResult);

    }

    // Closes the JMSContext
    // If there are no other active (not closed) JMSContext objects using the
    // underlying connection then this method also closes the underlying
    // connection

    @Test
    public void testCloseAll_B_SecOn() throws Exception {
        testResult = runInServlet("testCloseAll_B_SecOn");
        assertTrue("Test testCloseAll_B_SecOn failed", testResult);

    }

    @Test
    public void testCloseAll_TCP_SecOn() throws Exception {

        testResult = runInServlet("testCloseAll_TCP_SecOn");
        assertTrue("Test testCloseAll_TCP_SecOn failed", testResult);

    }

    // Closing a connection causes all temporary destinations to be deleted.
    @Test
    public void testCloseTempDest_B_SecOn() throws Exception {

        testResult = runInServlet("testCloseTempDest_B_SecOn");
        assertTrue("Test testCloseTempDest_B_SecOn failed", testResult);

    }

    @Test
    public void testCloseTempDest_TCP_SecOn() throws Exception {

        testResult = runInServlet("testCloseTempDest_TCP_SecOn");
        assertTrue("Test testCloseTempDest_TCP_SecOn failed", testResult);

    }

    // Creates a JMSConsumer for queue
    // Once consumer is created, perform a receive operation
    // Once consumer is created, check the context is started
    @Test
    public void testQueueConsumer_B_SecOn() throws Exception {

        testResult = runInServlet("testQueueConsumer_B_SecOn");
        assertTrue("Test testQueueConsumer_B_SecOn failed", testResult);

    }

    @Test
    public void testQueueConsumer_TCP_SecOn() throws Exception {
        testResult = runInServlet("testQueueConsumer_TCP_SecOn");
        assertTrue("Test testQueueConsumer_TCP_SecOn failed", testResult);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_B_SecOn() throws Exception {
        testResult = runInServlet("testCreateConsumerWithMsgSelectorTopic_B_SecOn");
        assertTrue(
                   "Test testCreateConsumerWithMsgSelectorTopic_B_SecOn failed",
                   testResult);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOn() throws Exception {

        testResult = runInServlet("testCreateConsumerWithMsgSelectorTopic_TCP_SecOn");
        assertTrue(
                   "Test testCreateConsumerWithMsgSelectorTopic_TCP_SecOn failed",
                   testResult);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping client server");
            server.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Stopping engine server");
            server1.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
