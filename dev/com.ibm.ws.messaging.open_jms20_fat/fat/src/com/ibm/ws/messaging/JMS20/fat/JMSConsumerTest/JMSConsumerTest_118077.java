/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest;

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
public class JMSConsumerTest_118077 {

    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSConsumerEngine");
    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSConsumerClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String consumerAppName = "JMSConsumer_118077";
    private static final String consumerContextRoot = "JMSConsumer_118077";
    private static final String[] consumerPackages = new String[] { "jmsconsumer_118077.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, consumerContextRoot, test);
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSConsumerEngine.xml");
        engineServer.startServer("JMSConsumer_118077_Engine.log");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSConsumerClient.xml");
        TestUtils.addDropinsWebApp(clientServer, consumerAppName, consumerPackages);
        clientServer.startServer("JMSConsumer_118077_Client.log");
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

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTextMsg_B_SecOff");
        assertTrue("testReceiveBodyTextMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTextMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyByteMsg_B_SecOff");
        assertTrue("testReceiveBodyByteMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyByteMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMapMsg_B_SecOff");
        assertTrue("testReceiveBodyMapMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyMapMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyObjectMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyObjectMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTextMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutTextMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutTextMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutByteMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutByteMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutByteMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMapMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutMapMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutMapMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutObjectMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTextMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitTextMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitTextMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitByteMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitByteMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitByteMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMapMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitMapMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMsg_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitObjectMsg_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnspecifiedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyUnspecifiedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnspecifiedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyUnspecifiedType_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnsupportedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyUnsupportedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnsupportedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyUnsupportedType_TcpIp_SecOff failed ", testResult);
    }

    // There is no API available to test retrieve header. Has been tested manually.
    // @Mode(TestMode.FULL)
    // @Test
    public void testReceiveBodyRetrieveHeader_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyRetrieveHeader_B_SecOff");
        assertTrue("testSetMessageListener_B_SecOff failed ", testResult);
    }

    // There is no API available to test retrieve header. Has been tested manually.
    // @Mode(TestMode.FULL)
    // @Test
    public void testReceiveBodyRetrieveHeader_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyRetrieveHeader_TcpIp_SecOff");
        assertTrue("testSetMessageListener_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnsupportedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnsupportedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitEmptyBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyNoWaitEmptyBody_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnspecifiedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyNoWaitUnspecifiedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnsupportedType_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitFromEmptyQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitFromEmptyQueue_B_SecOff");
        assertTrue("testReceiveBodyNoWaitFromEmptyQueue_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTransactionSecOff_B");
        assertTrue("testReceiveBodyTransactionSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyTransactionSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTransactionSecOff_B");
        assertTrue("testReceiveBodyTimeOutTransactionSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTransactionSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTransactionSecOff_B");
        assertTrue("testReceiveBodyNoWaitTransactionSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTransactionSecOff_TCPIP failed ", testResult);
    }

    // New tests for Message getBody API
    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageStringBuffer() throws Exception {
        boolean testResult = runInServlet("testMapMessageStringBuffer");
        assertTrue("testMapMessageStringBuffer failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageMap() throws Exception {
        boolean testResult = runInServlet("testMapMessageMap");
        assertTrue("testMapMessageMap failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageHashMap() throws Exception {
        boolean testResult = runInServlet("testMapMessageHashMap");
        assertTrue("testMapMessageHashMap failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageObject() throws Exception {
        boolean testResult = runInServlet("testMapMessageObject");
        assertTrue("testMapMessageObject failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testStreamMessage() throws Exception {
        boolean testResult = runInServlet("testStreamMessage");
        assertTrue("testStreamMessage failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessageGetBody() throws Exception {
        boolean testResult = runInServlet("testMessageGetBody");
        assertTrue("testMessageGetBody failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessage() throws Exception {
        boolean testResult = runInServlet("testBytesMessage");
        assertTrue("testBytesMessage failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageObject() throws Exception {
        boolean testResult = runInServlet("testBytesMessageObject");
        assertTrue("testBytesMessageObject failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessagePropertyNameWithJMSPositive() throws Exception {
        boolean testResult = runInServlet("testMessagePropertyNameWithJMSPositive");
        assertTrue("testMessagePropertyNameWithJMSPositive failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessagePropertyNameWithJMSNegative() throws Exception {
        boolean testResult = runInServlet("testMessagePropertyNameWithJMSNegative");
        assertTrue("testMessagePropertyNameWithJMSNegative failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageStringBuffer() throws Exception {
        boolean testResult = runInServlet("testBytesMessageStringBuffer");
        assertTrue("testBytesMessageStringBuffer failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageStringBuffer_TCP() throws Exception {
        boolean testResult = runInServlet("testBytesMessageStringBuffer_TCP");
        assertTrue("testBytesMessageStringBuffer_TCP failed ", testResult);
    }

    @Test
    public void testReceiveBodyEmptyBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyEmptyBody_B_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveBodyEmptyBody_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyEmptyBody_TcpIp_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveBodyWithTimeOutEmptyBody_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutEmptyBody_B_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveBodyAfterTimeout_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyAfterTimeout_B_SecOff");
        assertTrue("testReceiveBodyAfterTimeout_B_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveBodyAfterTimeout_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyAfterTimeout_TcpIp_SecOff");
        assertTrue("testReceiveBodyAfterTimeout_TcpIp_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveNoWaitFromEmptyQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveNoWaitFromEmptyQueue_B_SecOff");
        assertTrue("testReceiveNoWaitFromEmptyQueue_B_SecOff failed ", testResult);
    }

    @Test
    public void testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff() throws Exception {
        boolean testResult = runInServlet("testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff");
        assertTrue("testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff failed ", testResult);
    }

    // Defect associated - 175025
    // @Mode(TestMode.FULL)
    @Test
    public void testMapMessageNullBody() throws Exception {
        boolean testResult = runInServlet("testMapMessageNullBody");
        assertTrue("testMapMessageNullBody failed ", testResult);
    }

    @Test
    public void testTextMessageNullBody() throws Exception {
        boolean testResult = runInServlet("testTextMessageNullBody");
        assertTrue("testTextMessageNullBody failed ", testResult);
    }

    @Test
    public void testObjectMessageNullBody() throws Exception {
        boolean testResult = runInServlet("testObjectMessageNullBody");
        assertTrue("testObjectMessageNullBody failed ", testResult);
    }

    @Test
    public void testBytesMessageNullBody() throws Exception {
        boolean testResult = runInServlet("testBytesMessageNullBody");
        assertTrue("testBytesMessageNullBody failed ", testResult);
    }
}
