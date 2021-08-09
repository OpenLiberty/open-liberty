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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118067 {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSContext_118067";
    private static final String[] appPackages = new String[] { "jmscontext_118067.web" };
    private static final String contextRoot = "JMSContext_118067";

    private static boolean runInServlet(String test) throws IOException {
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

        engineServer.startServer("JMSContextTest_118067_Engine.log");
        clientServer.startServer("JMSContextTest_118067_Client.log");
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

    // 118067_10 : Create a queue with queue name as null and send message to this queue 
    // Message send options may be specified using one or more of the following methods:
    // setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameNull_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameNull_B");
        assertTrue("testQueueNameNull_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameNull_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameNull_TcpIp");
        assertTrue("testQueueNameNull_TcpIp_SecOff failed ", testResult);
    }

    // 118067_11 : Create a queue with queue name as empty String and send message to this queue 
    // Message send options may be specified using one or more of the following methods:
    // setDelivery, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameEmptyString_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameEmptyString_B");
        assertTrue("testQueueNameEmptyString_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameEmptyString_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameEmptyString_TcpIp");
        assertTrue("testQueueNameEmptyString_TcpIp_SecOff failed ", testResult);
    }

    // 118067_11 : Create a queue with queue name as empty String and send message to this queue 
    // Message send options may be specified using one or more of the following methods:
    // setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWildChars_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameWildChars_B");
        assertTrue("testQueueNameWildChars_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWildChars_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameWildChars_TcpIp");
        assertTrue("testQueueNameWildChars_TcpIp_SecOff failed ", testResult);
    }

    // 118067_11 : Create a queue with queue name containing spaces and send message to this queue 
    // Message send options may be specified using one or more of the following methods:
    // setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp,
    // setDisableMessageID and setAsync.

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWithSpaces_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameWithSpaces_B");
        assertTrue("testQueueNameWithSpaces_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWithSpaces_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameWithSpaces_TcpIp");
        assertTrue("testQueueNameWithSpaces_TcpIp_SecOff failed ", testResult);
    }

    // 118067_11 : Create a queue with queue name starting with underscore and send message to this queue
    // Create a queue with queue name starting with _temp and send message to this queue

    @Mode(TestMode.FULL)
    @Test
    public void testQueueName_temp_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueName_temp_B");
        assertTrue("testQueueName_temp_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueName_temp_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueName_temp_TcpIp");
        assertTrue("testQueueName_temp_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameLong_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameLong_B");
        assertTrue("testQueueNameLong_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameLong_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameLong_TcpIp");
        assertTrue("testQueueNameLong_TcpIp_SecOff failed ", testResult);
    }

    // 118067_11 :Test if queue name is case sensitive. Try to create a queue in small
    // case and send message to queuename in upper case 

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameCaseSensitive_B");
        assertTrue("testQueueNameCaseSensitive_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testQueueNameCaseSensitive_TcpIp");
        assertTrue("testQueueNameCaseSensitive_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameNull_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameNull_B");
        assertTrue("testTopicNameNull_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameNull_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameNull_TcpIp");
        assertTrue("testTopicNameNull_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameEmptyString_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameEmptyString_B");
        assertTrue("testTopicNameEmptyString_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameEmptyString_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameEmptyString_TcpIp");
        assertTrue("testTopicNameEmptyString_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWildChars_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameWildChars_B");
        assertTrue("testTopicNameWildChars_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWildChars_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameWildChars_TcpIp");
        assertTrue("testTopicNameWildChars_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWithSpaces_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameWithSpaces_B");
        assertTrue("testTopicNameWithSpaces_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWithSpaces_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameWithSpaces_TcpIp");
        assertTrue("testTopicNameWithSpaces_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicName_temp_B");
        assertTrue("testTopicName_temp_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicName_temp_TcpIp");
        assertTrue("testTopicName_temp_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameLong_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameLong_B");
        assertTrue("testTopicNameLong_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameLong_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameLong_TcpIp");
        assertTrue("testTopicNameLong_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameCaseSensitive_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameCaseSensitive_B");
        assertTrue("testTopicNameCaseSensitive_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameCaseSensitive_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testTopicNameCaseSensitive_TcpIp");
        assertTrue("testTopicNameCaseSensitive_TcpIp_SecOff failed ", testResult);
    }

    // 118067_9 : Test setting message properties on createProducer using method chaining. 
    // Message send options may be specified using one or more of the following methods:
    // setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.

    @Test
    public void testSetMessageProperty_Bindings_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetMessagePropertyBindings_Send");
        assertTrue("testSetMessageProperty_Bindings_SecOff failed ", testResult);
    }

    // 118067_9 : Test setting message properties on createProducer using method chaining. 
    // Message send options may be specified using one or more of the following methods:
    // setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.

    @Test
    public void testSetMessageProperty_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetMessagePropertyTcpIp_Send");
        assertTrue("testSetMessageProperty_TCP_SecOff failed ", testResult);
    }
}
