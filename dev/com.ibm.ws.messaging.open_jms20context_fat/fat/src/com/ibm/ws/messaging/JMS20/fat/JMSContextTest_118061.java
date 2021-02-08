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
package com.ibm.ws.messaging.JMS20.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118061 {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSContext";
    private static final String[] appPackages = new String[] { "jmscontext.web" };
    private static final String contextRoot = "JMSContext";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("JMSContextTest_118061_Engine.log");
        clientServer.startServer("JMSContextTest_118061_Client.log");
    }

    @AfterClass
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

        ShrinkHelper.cleanAllExportedArchives();
    }

    // 118061 :JMSContext: Handle creation of various Message types for the
    // JMSContext to be used by applications to send and receive JMS messages

    // 118061_3 Verify creation of Object message from JMSContext.
    // createObjectMessage(Serializable object).Perform a getObject,setObject
    // and getBody. Send and Receive

    @Mode(TestMode.FULL)
    @Test
    public void testCreateObjectMessageSer_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateObjectMessageSer_B_SecOff");
        assertTrue("Test testCreateObjectMessageSer_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateObjectMessageSer_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateObjectMessageSer_TCP_SecOff");
        assertTrue("Test testCreateObjectMessageSer_TCP_SecOff failed", testResult);
    }

    // 118061_4 Verify creation of Stream Message from
    // JMSContext.createStreamMessage(), Perform operation for setdata and
    // reading data. Send and Receive

    @Mode(TestMode.FULL)
    @Test
    public void testCreateStreamMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateStreamMessage_B_SecOff");
        assertTrue("Test testCreateStreamMessage_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateStreamMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateStreamMessage_TCP_SecOff");
        assertTrue("Test testCreateStreamMessage_TCP_SecOff failed", testResult);
    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text). Send and Receive

    @Mode(TestMode.FULL)
    @Test
    public void testCreateTextMessageStr_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateTextMessageStr_B_SecOff");
        assertTrue("Test testCreateTextMessageStr_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateTextMessageStr_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateTextMessageStr_TCP_SecOff");
        assertTrue("Test testCreateTextMessageStr_TCP_SecOff failed", testResult);
    }

    // 118061_7 Verify creation of Map Message from
    // JMSContext.createMapMessage() .Perform set and get operation. Send and
    // receive

    @Mode(TestMode.FULL)
    @Test
    public void testCreateMapMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateMapMessage_B_SecOff");
        assertTrue("Test testCreateMapMessage_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateMapMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateMapMessage_TCP_SecOff");
        assertTrue("Test testCreateMapMessage_TCP_SecOff failed", testResult);
    }

    // 118061_8 Verify creation of ByteMessage from
    // JMSContext.createBytesMessage(). Peform writeBytes, readBytes and getBody
    // operation. Send and Receive.

    @Mode(TestMode.FULL)
    @Test
    public void testCreateBytesMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateBytesMessage_B_SecOff");
        assertTrue("Test testCreateBytesMessage_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateBytesMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateBytesMessage_TCP_SecOff");
        assertTrue("Test testCreateBytesMessage_TCP_SecOff failed", testResult);
    }

    // 118061_9 Verify set and get operation on Message header field
    // JMSDestination-

    @Mode(TestMode.FULL)
    @Test
    public void testJMSDestination_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDestination_B_SecOff");
        assertTrue("Test testJMSDestination_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSDestination_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDestination_TCP_SecOff");
        assertTrue("Test testJMSDestination_TCP_SecOff failed", testResult);
    }

    // 118061_10 Verify set and get operation on Message header field
    // JMSDeliveryMode-

    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDeliveryMode_B_SecOff");
        assertTrue("Test testJMSDeliveryMode_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDeliveryMode_TCP_SecOff");
        assertTrue("Test testJMSDeliveryMode_TCP_SecOff failed", testResult);
    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    @Mode(TestMode.FULL)
    @Test
    public void testJMSTimestamp_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSTimestamp_B_SecOff");
        assertTrue("Test testJMSTimestamp_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSTimestamp_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSTimestamp_TCP_SecOff");
        assertTrue("Test testJMSTimestamp_TCP_SecOff failed", testResult);
    }

    // 118061_13 Verify set and get operation on Message header field
    // JMSCorrelationID

    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationID_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSCorrelationID_B_SecOff");
        assertTrue("Test testJMSCorrelationID_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationID_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSCorrelationID_TCP_SecOff");
        assertTrue("Test testJMSCorrelationID_TCP_SecOff failed", testResult);
    }

    // 118061_14 Verify set and get operation on Message header field
    // JMSCorrelationIDAsBytes

    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationIDAsBytes_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSCorrelationIDAsBytes_B_SecOff");
        assertTrue("Test testJMSCorrelationIDAsBytes_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationIDAsBytes_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSCorrelationIDAsBytes_TCP_SecOff");
        assertTrue("Test testJMSCorrelationIDAsBytes_TCP_SecOff failed", testResult);
    }

    // 118061_15 Verify set and get operation on Message header field JMSReplyTo

    @Mode(TestMode.FULL)
    @Test
    public void testJMSReplyTo_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSReplyTo_B_SecOff");
        assertTrue("Test testJMSReplyTo_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSReplyTo_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSReplyTo_TCP_SecOff");
        assertTrue("Test testJMSReplyTo_TCP_SecOff failed", testResult);
    }

    // 118061_16 Verify set and get operation on Message header field
    // JMSRedelivered

    // @Test // Disabled in commercial liberty
    public void testJMSRedelivered_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSRedelivered_B_SecOff");
        assertTrue("Test testJMSRedelivered_B_SecOff failed", testResult);
    }

    // @Test // Disabled in commercial liberty
    public void testJMSRedelivered_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSRedelivered_TCP_SecOff");
        assertTrue("Test testJMSRedelivered_TCP_SecOff failed", testResult);
    }

    // 118061_17 Verify set and get operation on Message header field JMSType

    @Mode(TestMode.FULL)
    @Test
    public void testJMSType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSType_B_SecOff");
        assertTrue("Test testJMSType_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSType_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSType_TCP_SecOff");
        assertTrue("Test testJMSType_TCP_SecOff failed", testResult);
    }

    // 118061_18 Verify set and get operation on Message header field
    // JMSExpiration

    @Mode(TestMode.FULL)
    @Test
    public void testJMSExpiration_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSExpiration_B_SecOff");
        assertTrue("Test testJMSExpiration_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSExpiration_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSExpiration_TCP_SecOff");
        assertTrue("Test testJMSExpiration_TCP_SecOff failed", testResult);
    }

    // 118061_20 Verify set and get operation on Message header field
    // JMSDeliveryTime

    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryTime_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDeliveryTime_B_SecOff");
        assertTrue("Test testJMSDeliveryTime_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryTime_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSDeliveryTime_TCP_SecOff");
        assertTrue("Test testJMSDeliveryTime_TCP_SecOff failed", testResult);
    }

    // 118061_2 Verify creation of Object message from JMSContext.
    // createObjectMessage() . Perform a getObject,setObject and getBody. Send
    // and Receive

    @Test
    public void testCreateObjectMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateObjectMessage_B_SecOff");
        assertTrue("Test testCreateObjectMessage_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateObjectMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateObjectMessage_TCP_SecOff");
        assertTrue("Test testCreateObjectMessage_TCP_SecOff failed", testResult);
    }

    // 118061_5 Verify creation of Text Message from
    // JMSContext.createTextMessage().Perform setText and getTest operations.
    // Send and Receive

    @Test
    public void testCreateTextMessage_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateTextMessage_B_SecOff");
        assertTrue("Test testCreateTextMessage_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateTextMessage_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testCreateTextMessage_TCP_SecOff");
        assertTrue("Test testCreateTextMessage_TCP_SecOff failed", testResult);
    }

    // 118061_11 Verify set and get operation on Message header field
    // JMSMessageID

    @Test
    public void testJMSMessageID_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSMessageID_B_SecOff");
        assertTrue("Test testJMSMessageID_B_SecOff failed", testResult);
    }

    @Test
    public void testJMSMessageID_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSMessageID_TCP_SecOff");
        assertTrue("Test testJMSMessageID_TCP_SecOff failed", testResult);
    }

    // 118061_19 Verify set and get operation on Message header field
    // JMSPriority

    @Test
    public void testJMSPriority_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSPriority_B_SecOff");
        assertTrue("Test testJMSPriority_B_SecOff failed", testResult);
    }

    @Test
    public void testJMSPriority_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testJMSPriority_TCP_SecOff");
        assertTrue("Test testJMSPriority_TCP_SecOff failed", testResult);
    }

    @Test
    public void testByteMessageGetBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testByteMessageGetBody_B_SecOff");
        assertTrue("Test testByteMessageGetBody_B_SecOff failed", testResult);
    }

    @Test
    public void testObjectMessageisBodyAssignable_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testObjectMessageisBodyAssignable_B_SecOff");
        assertTrue("Test testObjectMessageisBodyAssignable_B_SecOff failed", testResult);
    }

    @Test
    public void testObjectMessagegetBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testObjectMessagegetBody_B_SecOff");
        assertTrue("Test testObjectMessagegetBody_B_SecOff failed", testResult);
    }

    // 118062_2: QueueBrowser createBrowser(Queue queue,String messageSelector)
    // 118062_2_1 Creates a QueueBrowser object to peek at the messages on the
    // specified queue using a message selector.

    @Test
    public void testcreateBrowser_MessageSelector_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_B_SecOff failed", testResult);
    }

    @Test
    public void testcreateBrowser_MessageSelector_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_TCP_SecOff failed", testResult);
    }

    // 118062_2_3 InvalidRuntimeSelectorException - if the message selector is
    // invalid.

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Test
    public void testcreateBrowser_MessageSelector_Invalid_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Invalid_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Invalid_B_SecOff failed", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Test
    public void testcreateBrowser_MessageSelector_Invalid_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Invalid_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Invalid_TCP_SecOff failed", testResult);
    }

    // 118062_4 String getMessageSelector()
    // 118062_4_1 Gets this queue browser's message selector expression.

    @Test
    public void testGetMessageSelector_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_B_SecOff");
        assertTrue("Test testGetMessageSelector_B_SecOff failed", testResult);
    }

    @Test
    public void testGetMessageSelector_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_TCP_SecOff");
        assertTrue("Test testGetMessageSelector_TCP_SecOff failed", testResult);
    }
}
