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
package com.ibm.ws.messaging.JMS20.fat.JMSProducerTest;

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
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

@Mode(TestMode.FULL)
public class JMSProducerTest_118071 {
    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSProducerClient");
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSProducerEngine");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String producerAppName = "JMSProducer";
    private static final String producerContextRoot = "JMSProducer";
    private static final String[] producerPackages = new String[] { "jmsproducer.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, producerContextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSProducerEngine.xml");
        engineServer.startServer("JMSProducerEngine_118071.log");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSProducerClient.xml");
        TestUtils.addDropinsWebApp(clientServer, producerAppName, producerPackages);
        clientServer.startServer("JMSProducerClient_118071.log");
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

    // 118071_1_5_Q Test with message as empty string

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_EmptyMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_EmptyMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_EmptyMessage_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_EmptyMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_EmptyMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_EmptyMessage_TCP_SecOff failed", testResult);
    }

    // 118071_1_6_Q MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_NotWriteable_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NotWriteable_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NotWriteable_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_NotWriteable_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NotWriteable_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NotWriteable_TCP_SecOff failed", testResult);
    }

    // 118071_1_8_T MessageFormatRuntimeException - if an invalid message is specified.
    // 118071_1_11_T Test with message as null

    // Bindings and security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_NullMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NullMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NullMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_1_9_T InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid topic

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_InvalidDestinationTopic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_InvalidDestinationTopic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_InvalidDestinationTopic_TCP_SecOff failed", testResult);
    }

    // 118071_1_12_T Test with message as empty string

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMessage_EmptyMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_EmptyMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_EmptyMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_2_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue

    // Bindings and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // 118071_2_4_Queue If a null value is specified for body then a TextMessage
    // with no body will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_NullMessageBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_NullMessageBody_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_NullMessageBody_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff failed", testResult);
    }

    // 118071_2_5_Queue Test with empty string for the body

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_EmptyMessage_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff failed", testResult);
    }

    // 118071_2_8_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff failed", testResult);
    }

    // 118071_2_9_Topic If a null value is specified for body then a TextMessage
    // with no body will be sent.
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_NullMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_NullMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_NullMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_2_10_Topic Test with empty string for the body

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_EmptyMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_3_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessage_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // 118071_3_4_Queue If a null value is specified then a MapMessage with no
    // map entries will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test public void testJMSProducerSendMapMessage_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_Null_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_Null_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessage_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_Null_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_Null_TCP_SecOff failed", testResult);
    }

    // 118071_3_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessageTopic_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessageTopic_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessageTopic_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // 118071_3_8_Topic If a null value is specified then a MapMessage with no
    // map entries will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessageTopic_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessageTopic_Null_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessageTopic_Null_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendMapMessageTopic_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessageTopic_Null_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessageTopic_Null_TCP_SecOff failed", testResult);
    }

    // 118071_4_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // 118071_4_4_Queue If a null value is specified then a BytesMessage with no
    // body will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test public void testJMSProducerSendByteMessage_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Null_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Null_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Null_TCP_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Null_TCP_SecOff failed", testResult);
    }

    // 118071_4_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_4_8_Topic If a null value is specified then a BytesMessage with no
    // body will be sent.

    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_Null_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Null_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Null_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_5_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // 118071_5_4_Queue If a null value is specified then an ObjectMessage with
    // no body will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Null_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Null_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Null_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Null_TCP_SecOff failed", testResult);
    }

    // 118071_5_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_5_8_Topic If a null value is specified then an ObjectMessage with
    // no body will be sent.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_Null_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Null_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Null_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSProducerSendObjectMessage_Null_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Null_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Null_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_1_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue

    // Bindings and Security Off
    @Test
    public void testJMSProducerSendMessage_InvalidDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_InvalidDestination_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_InvalidDestination_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendMessage_InvalidDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_InvalidDestination_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_InvalidDestination_TCP_SecOff failed", testResult);
    }

    // 118071_1_2_Q MessageFormatRuntimeException - if an invalid message is
    // specified.
    // 118071_1_4_Q Test with message as null
    // Bindings and Security Off

    @Test
    public void testJMSProducerSendMessage_NullMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NullMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NullMessage_B_SecOff failed", testResult);
    }

    // 118071_1_10_T MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only

    // Bindings and Security Off
    @Test
    public void testJMSProducerSendMessage_NotWriteableTopic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NotWriteableTopic_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NotWriteableTopic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff failed", testResult);
    }

    // 118071_3_1_Queue Send a MapMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    // Bindings and Security Off
    @Test public void testJMSProducerSendMapMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test public void testJMSProducerSendMapMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_TCP_SecOff failed", testResult);
    }

    // 118071_3_5_Topic Send a MapMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    // Bindings and Security Off
    @Test public void testJMSProducerSendMapMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendMapMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendMapMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMapMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_4 JMSProducer send(Destination destination,byte[] body)
    // 118071_4_1_Queue Send a BytesMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Test public void testJMSProducerSendByteMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off

    @Test public void testJMSProducerSendByteMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_TCP_SecOff failed", testResult);
    }

    // 118071_4_5_Topic Send a BytesMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Test
    public void testJMSProducerSendByteMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendByteMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendByteMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendByteMessage_Topic_TCP_SecOff failed", testResult);
    }

    // 118071_5_1_Queue Send an ObjectMessage with the specified body to the
    // specified queue using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Test public void testJMSProducerSendObjectMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test public void testJMSProducerSendObjectMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_TCP_SecOff failed", testResult);
    }

    // 118071_5_5_Topic Send an ObjectMessage with the specified body to the
    // specified topic using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    @Test
    public void testJMSProducerSendObjectMessage_Topic_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Topic_B_SecOff failed", testResult);
    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendObjectMessage_Topic_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendObjectMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendObjectMessage_Topic_TCP_SecOff failed", testResult);
    }

    @Test
    public void testQueueSender_InvalidDestinationNE_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueSender_InvalidDestinationNE_B_SecOff");
        assertTrue("Test testQueueSender_InvalidDestinationNE_B_SecOff failed", testResult);
    }

    @Test
    public void testMessageProducerWithNullDestination() throws Exception {
        boolean testResult = runInServlet("testMessageProducerWithNullDestination");
        assertTrue("Test testMessageProducerWithNullDestination failed", testResult);
    }

    @Test public void testMessageProducerWithValidDestination() throws Exception {
        boolean testResult = runInServlet("testMessageProducerWithValidDestination");
        assertTrue("Test testMessageProducerWithValidDestination failed", testResult);
    }
}
