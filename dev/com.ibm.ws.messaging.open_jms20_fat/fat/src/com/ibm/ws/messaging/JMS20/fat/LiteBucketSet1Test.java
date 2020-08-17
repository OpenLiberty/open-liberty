/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class LiteBucketSet1Test {

    private static LibertyServer clientServer = LibertyServerFactory.getLibertyServer("LiteSet1Client");
    private static LibertyServer engineServer = LibertyServerFactory.getLibertyServer("LiteSet1Engine");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String producerAppName = "JMSProducer";
    private static final String producerContextRoot = "JMSProducer";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, producerContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        // Prepare the server which runs the messaging engine.

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("Lite1Engine.xml");

        // Prepare the server which runs the messaging client and which
        // runs the test application.

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        TestUtils.addDropinsWebApp(clientServer, producerAppName, "jmsproducer.web");
        clientServer.setServerConfigurationFile("Lite1Client.xml");

        // Start both servers.  Start the engine first, so that its resources
        // are available when the client starts.

        engineServer.startServer("LiteBucketSet1_Engine.log");
        clientServer.startServer("LiteBucketSet1_Client.log");
    }

    @org.junit.AfterClass
    public static void tearDown() {
        // Stop the messaging client ...

        try {
            clientServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ... then stop the messaging engine.
        try {
            engineServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // start of tests from JMSProducerTest_118071

    // 118071_1 JMSProducer send(Destination destination,Message message)
    // 118071_1_1_Q : Sends the message to the specified queue using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security Off
    @Test
    public void testJMSProducerSendMessage_B_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_B_SecOff failed", result);
    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendMessage_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_TCP_SecOff failed", result);
    }

    // 118071_1_7_T Sends a message to the specified topic using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security Off

    @Test
    public void testJMSProducerSendMessage_Topic_B_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendMessage_Topic_B_SecOff failed", result);
    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendMessage_Topic_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendMessage_Topic_TCP_SecOff failed", result);
    }

    // 118071_2 JMSProducer send(Destination destination, String body)

    // 118071_2_1_Q Send a TextMessage with the specified body to the specified
    // queue, using any send options, message properties and message headers
    // that have been defined on this JMSProducer.

    // Bindings and Security Off

    @Test
    public void testJMSProducerSendTextMessage_B_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendTextMessage_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_B_SecOff failed", result);

    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendTextMessage_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendTextMessage_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_TCP_SecOff failed", result);
    }

    // 118071_2_6_Topic Send a TextMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    // Bindings and Security Off

    @Test
    public void testJMSProducerSendTextMessage_Topic_B_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendTextMessage_Topic_B_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_Topic_B_SecOff failed", result);
    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendTextMessage_Topic_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testJMSProducerSendTextMessage_Topic_TCP_SecOff");
        assertTrue("Test testJMSProducerSendTextMessage_Topic_TCP_SecOff failed", result);
    }

    // end of tests of 118071
    // start of tests from JMSProducerTest_118073

    // 118073_2 boolean propertyExists(String name)
    // 118073_2_1 Returns true if a message property with the specified name has
    // been set on this JMSProducer
    // Bindings and Security Off

    @Test
    public void testPropertyExists_B_SecOff() throws Exception {
        boolean result = runInServlet("testPropertyExists_B_SecOff");
        assertTrue("Test testPropertyExists_B_SecOff failed", result);
    }

    // TCP and Security Off

    @Test
    public void testPropertyExists_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testPropertyExists_TCP_SecOff");
        assertTrue("Test testPropertyExists_TCP_SecOff failed", result);
    }

    // 118073_31 Set<String> getPropertyNames()
    // 118073_31_1 Returns an unmodifiable Set view of the names of all the
    // message properties that have been set on this JMSProducer.
    // 118073_31_2 JMS standard header fields are not considered properties and
    // are not returned in this Set.

    // Bindings and Security Off

    @Test
    public void testGetPropertyNames_B_SecOff() throws Exception {
        boolean result = runInServlet("testGetPropertyNames_B_SecOff");
        assertTrue("Test testGetPropertyNames_B_SecOff failed", result);
    }

    // TCP and Security OFf

    @Test
    public void testGetPropertyNames_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testGetPropertyNames_TCP_SecOff");
        assertTrue("Test testGetPropertyNames_TCP_SecOff failed", result);
    }

    // 118073_34 JMSProducer setJMSCorrelationID(String correlationID)
    // 118073_34_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as a String.

    // 118073_35 String getJMSCorrelationID()
    // 118073_35_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as a String.

    // Bindings and Security Off

    @Test
    public void testSetJMSCorrelationID_B_SecOff() throws Exception {
        boolean result = runInServlet("testSetJMSCorrelationID_B_SecOff");
        assertTrue("Test testSetJMSCorrelationID_B_SecOff failed", result);
    }

    // TCP and Security OFf

    @Test
    public void testSetJMSCorrelationID_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testSetJMSCorrelationID_TCP_SecOff");
        assertTrue("Test testSetJMSCorrelationID_TCP_SecOff failed", result);
    }

    // 118073_32 JMSProducer setJMSCorrelationIDAsBytes(byte[] correlationID)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as an array of bytes.
    // 118073_33_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as an array of bytes.
    // Bindings and Security Off

    @Test
    public void testSetJMSCorrelationIDAsBytes_B_SecOff() throws Exception {
        boolean result = runInServlet("testSetJMSCorrelationIDAsBytes_B_SecOff");
        assertTrue("Test testSetJMSCorrelationIDAsBytes_B_SecOff failed", result);
    }

    // TCP and Security OFf

    @Test
    public void testSetJMSCorrelationIDAsBytes_TCP_SecOff() throws Exception {
        boolean result = runInServlet("testSetJMSCorrelationIDAsBytes_TCP_SecOff");
        assertTrue("Test testSetJMSCorrelationIDAsBytes_TCP_SecOff failed", result);
    }
}
