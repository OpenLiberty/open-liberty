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

@Mode(TestMode.FULL)
public class JMSConsumerTest_118077 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSConsumer_118077?test="
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
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            }
            else
                result = true;

            return result;

        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("JMSConsumer_118077_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer("JMSConsumer_118077_Client.log");
        changedMessageFromLog = server.waitForStringInLog(
                                                          "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in the new file",
                      changedMessageFromLog);

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
            server1.stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTextMsg_B_SecOff");
        assertTrue("testReceiveBodyTextMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTextMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyByteMsg_B_SecOff");
        assertTrue("testReceiveBodyByteMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyByteMsg_TcpIp_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyMapMsg_B_SecOff");
        assertTrue("testReceiveBodyMapMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyMapMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyObjectMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyObjectMsg_TcpIp_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTextMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutTextMsg_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutTextMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutByteMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutByteMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutByteMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMapMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutMapMsg_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutMapMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyTimeOutObjectMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTextMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitTextMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTextMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitTextMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitByteMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitByteMsg_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitByteMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitByteMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMapMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitMapMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMsg_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitObjectMsg_B_SecOff");
        assertTrue("testReceiveBodyNoWaitObjectMsg_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnspecifiedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyUnspecifiedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnspecifiedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyUnspecifiedType_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnsupportedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyUnsupportedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyUnsupportedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyUnsupportedType_TcpIp_SecOff failed ", val);

    }

    //there is no API available to test retrieve header. Has been tested manually
    //  //@Mode(TestMode.FULL) @Test
    public void testReceiveBodyRetrieveHeader_B_SecOff() throws Exception {

        System.out.println("Running testTopicNameTOPIC_TcpIp_SecOn");
        server.setServerConfigurationFile("JMSContext_Client.xml");

        server.startServer();

        String changedMessageFromLog = server.waitForStringInLog(
                                                                 "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in the new file",
                      changedMessageFromLog);

        val = runInServlet("testReceiveBodyRetrieveHeader_B_SecOff");
        assertTrue("testSetMessageListener_B_SecOff failed ", val);
        //    String msg = server.waitForStringInLog("Expected MessageFormatRuntimeException was found in testReceiveBodyUnsupportedType_B_SecOff", server.getMatchingLogFile("trace.log"));
        //    assertNotNull("Expected MessageFormatRuntimeException not found", msg);

        server.stopServer();
    }

    //there is no API available to test retrieve header. Has been tested manually
    //  //@Mode(TestMode.FULL) @Test
    public void testReceiveBodyRetrieveHeader_TcpIp_SecOff() throws Exception {

        System.out.println("Running testTopicNameTOPIC_TcpIp_SecOn");
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer();
        changedMessageFromLog = server.waitForStringInLog(
                                                          "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in the new file",
                      changedMessageFromLog);

        val = runInServlet("testReceiveBodyRetrieveHeader_TcpIp_SecOff");
        assertTrue("testSetMessageListener_B_SecOff failed ", val);
        server1.stopServer();
        server.stopServer();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnsupportedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnsupportedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitEmptyBody_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyNoWaitEmptyBody_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnspecifiedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnspecifiedType_B_SecOff");
        assertTrue("testReceiveBodyNoWaitUnspecifiedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnsupportedType_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedType_B_SecOff");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitFromEmptyQueue_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitFromEmptyQueue_B_SecOff");
        assertTrue("testReceiveBodyNoWaitFromEmptyQueue_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff");
        assertTrue("testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTransactionSecOff_B");
        assertTrue("testReceiveBodyTransactionSecOff_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyTransactionSecOff_TCPIP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTransactionSecOff_B");
        assertTrue("testReceiveBodyTimeOutTransactionSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTransactionSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTransactionSecOff_B");
        assertTrue("testReceiveBodyNoWaitTransactionSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTransactionSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTransactionSecOff_TCPIP failed ", val);
    }

    //New tests for Message getBody API
    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageStringBuffer() throws Exception {

        val = runInServlet("testMapMessageStringBuffer");
        assertTrue("testMapMessageStringBuffer failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageMap() throws Exception {

        val = runInServlet("testMapMessageMap");
        assertTrue("testMapMessageMap failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageHashMap() throws Exception {

        val = runInServlet("testMapMessageHashMap");
        assertTrue("testMapMessageHashMap failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMapMessageObject() throws Exception {

        val = runInServlet("testMapMessageObject");
        assertTrue("testMapMessageObject failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testStreamMessage() throws Exception {

        val = runInServlet("testStreamMessage");
        assertTrue("testStreamMessage failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessageGetBody() throws Exception {

        val = runInServlet("testMessageGetBody");
        assertTrue("testMessageGetBody failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessage() throws Exception {

        val = runInServlet("testBytesMessage");
        assertTrue("testBytesMessage failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageObject() throws Exception {

        val = runInServlet("testBytesMessageObject");
        assertTrue("testBytesMessageObject failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessagePropertyNameWithJMSPositive() throws Exception {

        val = runInServlet("testMessagePropertyNameWithJMSPositive");
        assertTrue("testMessagePropertyNameWithJMSPositive failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessagePropertyNameWithJMSNegative() throws Exception {

        val = runInServlet("testMessagePropertyNameWithJMSNegative");
        assertTrue("testMessagePropertyNameWithJMSNegative failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageStringBuffer() throws Exception {

        val = runInServlet("testBytesMessageStringBuffer");
        assertTrue("testBytesMessageStringBuffer failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBytesMessageStringBuffer_TCP() throws Exception {

        val = runInServlet("testBytesMessageStringBuffer_TCP");
        assertTrue("testBytesMessageStringBuffer_TCP failed ", val);
    }

    @Test
    public void testReceiveBodyEmptyBody_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyEmptyBody_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyEmptyBody_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyEmptyBody_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyWithTimeOutEmptyBody_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutEmptyBody_B_SecOff");
        assertTrue("testReceiveBodyWithTimeOutEmptyBody_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff");
        assertTrue("testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyAfterTimeout_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyAfterTimeout_B_SecOff");
        assertTrue("testReceiveBodyAfterTimeout_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyAfterTimeout_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyAfterTimeout_TcpIp_SecOff");
        assertTrue("testReceiveBodyAfterTimeout_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveNoWaitFromEmptyQueue_B_SecOff() throws Exception {

        val = runInServlet("testReceiveNoWaitFromEmptyQueue_B_SecOff");
        assertTrue("testReceiveNoWaitFromEmptyQueue_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff");
        assertTrue("testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff failed ", val);

    }

    // Defect associated - 175025
    //@Mode(TestMode.FULL)
    @Test
    public void testMapMessageNullBody() throws Exception {

        val = runInServlet("testMapMessageNullBody");
        assertTrue("testMapMessageNullBody failed ", val);
    }

    @Test
    public void testTextMessageNullBody() throws Exception {

        val = runInServlet("testTextMessageNullBody");
        assertTrue("testTextMessageNullBody failed ", val);
    }

    @Test
    public void testObjectMessageNullBody() throws Exception {

        val = runInServlet("testObjectMessageNullBody");
        assertTrue("testObjectMessageNullBody failed ", val);
    }

    @Test
    public void testBytesMessageNullBody() throws Exception {

        val = runInServlet("testBytesMessageNullBody");
        assertTrue("testBytesMessageNullBody failed ", val);
    }

}
