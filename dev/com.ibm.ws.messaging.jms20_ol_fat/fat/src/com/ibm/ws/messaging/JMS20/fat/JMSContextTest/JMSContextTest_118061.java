/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.junit.BeforeClass;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.websphere.simplicity.ShrinkHelper;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextTest_118061 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118061_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118061_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;

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
            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
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
            setUpShirnkWrap();


        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");

        server.setServerConfigurationFile("JMSContext.xml");
        server1.setHttpDefaultPort(8030);
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("JMSContextTestClient_118061.log");
        server1.startServer("JMSContextTestServer_118061.log");

    }

    // /==========================================================================================================================================
    // 118061 :JMSContext: Handle creation of various Message types for the
    // JMSContext to be used by applications to send and receive JMS messages
    // /==========================================================================================================================================

    // 118061_3 Verify creation of Object message from JMSContext.
    // createObjectMessage(Serializable object).Perform a getObject,setObject
    // and getBody. Send and Receive

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateObjectMessageSer_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateObjectMessageSer_B_SecOff");

        assertTrue("Test testCreateObjectMessageSer_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateObjectMessageSer_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateObjectMessageSer_TCP_SecOff");

        assertTrue("Test testCreateObjectMessageSer_TCP_SecOff failed", testResult);

    }

    // 118061_4 Verify creation of Stream Message from
    // JMSContext.createStreamMessage(), Perform operation for setdata and
    // reading data. Send and Receive
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateStreamMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateStreamMessage_B_SecOff");

        assertTrue("Test testCreateStreamMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateStreamMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateStreamMessage_TCP_SecOff");

        assertTrue("Test testCreateStreamMessage_TCP_SecOff failed", testResult);
    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text). Send and Receive

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateTextMessageStr_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateTextMessageStr_B_SecOff");

        assertTrue("Test testCreateTextMessageStr_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateTextMessageStr_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateTextMessageStr_TCP_SecOff");

        assertTrue("Test testCreateTextMessageStr_TCP_SecOff failed", testResult);

    }

    // 118061_7 Verify creation of Map Message from
    // JMSContext.createMapMessage() .Perform set and get operation. Send and
    // receive
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateMapMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateMapMessage_B_SecOff");

        assertTrue("Test testCreateMapMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateMapMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateMapMessage_TCP_SecOff");

        assertTrue("Test testCreateMapMessage_TCP_SecOff failed", testResult);

    }

    // 118061_8 Verify creation of ByteMessage from
    // JMSContext.createBytesMessage(). Peform writeBytes, readBytes and getBody
    // operation. Send and Receive.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateBytesMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateBytesMessage_B_SecOff");

        assertTrue("Test testCreateBytesMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateBytesMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateBytesMessage_TCP_SecOff");

        assertTrue("Test testCreateBytesMessage_TCP_SecOff failed", testResult);
    }

    // 118061_9 Verify set and get operation on Message header field
    // JMSDestination-

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDestination_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSDestination_B_SecOff");

        assertTrue("Test testJMSDestination_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDestination_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSDestination_TCP_SecOff");

        assertTrue("Test testJMSDestination_TCP_SecOff failed", testResult);
    }

    // 118061_10 Verify set and get operation on Message header field
    // JMSDeliveryMode-

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryMode_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSDeliveryMode_B_SecOff");

        assertTrue("Test testJMSDeliveryMode_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryMode_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSDeliveryMode_TCP_SecOff");

        assertTrue("Test testJMSDeliveryMode_TCP_SecOff failed", testResult);

    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSTimestamp_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSTimestamp_B_SecOff");

        assertTrue("Test testJMSTimestamp_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSTimestamp_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSTimestamp_TCP_SecOff");

        assertTrue("Test testJMSTimestamp_TCP_SecOff failed", testResult);
    }

    // 118061_13 Verify set and get operation on Message header field
    // JMSCorrelationID

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationID_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSCorrelationID_B_SecOff");

        assertTrue("Test testJMSCorrelationID_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationID_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSCorrelationID_TCP_SecOff");

        assertTrue("Test testJMSCorrelationID_TCP_SecOff failed", testResult);

    }

    // 118061_14 Verify set and get operation on Message header field
    // JMSCorrelationIDAsBytes

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationIDAsBytes_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSCorrelationIDAsBytes_B_SecOff");

        assertTrue("Test testJMSCorrelationIDAsBytes_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSCorrelationIDAsBytes_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSCorrelationIDAsBytes_TCP_SecOff");

        assertTrue("Test testJMSCorrelationIDAsBytes_TCP_SecOff failed", testResult);
    }

    // 118061_15 Verify set and get operation on Message header field JMSReplyTo

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSReplyTo_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSReplyTo_B_SecOff");

        assertTrue("Test testJMSReplyTo_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSReplyTo_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSReplyTo_TCP_SecOff");

        assertTrue("Test testJMSReplyTo_TCP_SecOff failed", testResult);
    }

    // 118061_16 Verify set and get operation on Message header field
    // JMSRedelivered

    // Bindings and Security Off
    ////@Test
    public void testJMSRedelivered_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSRedelivered_B_SecOff");

        assertTrue("Test testJMSRedelivered_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    ////@Test
    public void testJMSRedelivered_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSRedelivered_TCP_SecOff");

        assertTrue("Test testJMSRedelivered_TCP_SecOff failed", testResult);

    }

    // 118061_17 Verify set and get operation on Message header field JMSType
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSType_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSType_B_SecOff");

        assertTrue("Test testJMSType_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSType_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSType_TCP_SecOff");

        assertTrue("Test testJMSType_TCP_SecOff failed", testResult);
    }

    // 118061_18 Verify set and get operation on Message header field
    // JMSExpiration

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSExpiration_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSExpiration_B_SecOff");

        assertTrue("Test testJMSExpiration_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSExpiration_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSExpiration_TCP_SecOff");

        assertTrue("Test testJMSExpiration_TCP_SecOff failed", testResult);

    }

    // 118061_20 Verify set and get operation on Message header field
    // JMSDeliveryTime

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryTime_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSDeliveryTime_B_SecOff");

        assertTrue("Test testJMSDeliveryTime_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testJMSDeliveryTime_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSDeliveryTime_TCP_SecOff");

        assertTrue("Test testJMSDeliveryTime_TCP_SecOff failed", testResult);
    }

    // 118061_2 Verify creation of Object message from JMSContext.
    // createObjectMessage() . Perform a getObject,setObject and getBody. Send
    // and Receive

    // Bindings and Security Off

    @Test
    public void testCreateObjectMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateObjectMessage_B_SecOff");

        assertTrue("Test testCreateObjectMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testCreateObjectMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateObjectMessage_TCP_SecOff");

        assertTrue("Test testCreateObjectMessage_TCP_SecOff failed", testResult);

    }

    // 118061_5 Verify creation of Text Message from
    // JMSContext.createTextMessage().Perform setText and getTest operations.
    // Send and Receive
    // Bindings and Security Off
    @Test
    public void testCreateTextMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateTextMessage_B_SecOff");

        assertTrue("Test testCreateTextMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testCreateTextMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateTextMessage_TCP_SecOff");

        assertTrue("Test testCreateTextMessage_TCP_SecOff failed", testResult);

    }

    // 118061_11 Verify set and get operation on Message header field
    // JMSMessageID

    // Bindings and Security Off
    @Test
    public void testJMSMessageID_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSMessageID_B_SecOff");

        assertTrue("Test testJMSMessageID_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testJMSMessageID_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSMessageID_TCP_SecOff");

        assertTrue("Test testJMSMessageID_TCP_SecOff failed", testResult);
    }

    // 118061_19 Verify set and get operation on Message header field
    // JMSPriority

    // Bindings and Security Off

    @Test
    public void testJMSPriority_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSPriority_B_SecOff");

        assertTrue("Test testJMSPriority_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testJMSPriority_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSPriority_TCP_SecOff");

        assertTrue("Test testJMSPriority_TCP_SecOff failed", testResult);

    }

    @Test
    public void testByteMessageGetBody_B_SecOff() throws Exception {

        testResult = runInServlet("testByteMessageGetBody_B_SecOff");

        assertTrue("Test testByteMessageGetBody_B_SecOff failed", testResult);

    }

    @Test
    public void testObjectMessageisBodyAssignable_B_SecOff() throws Exception {

        testResult = runInServlet("testObjectMessageisBodyAssignable_B_SecOff");

        assertTrue("Test testObjectMessageisBodyAssignable_B_SecOff failed", testResult);

    }

    @Test
    public void testObjectMessagegetBody_B_SecOff() throws Exception {

        testResult = runInServlet("testObjectMessagegetBody_B_SecOff");

        assertTrue("Test testObjectMessagegetBody_B_SecOff failed", testResult);

    }

    // 118062_2: QueueBrowser createBrowser(Queue queue,String messageSelector)
    // 118062_2_1 Creates a QueueBrowser object to peek at the messages on the
    // specified queue using a message selector.

    @Test
    public void testcreateBrowser_MessageSelector_B_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_B_SecOff failed", testResult);

    }

    @Test
    public void testcreateBrowser_MessageSelector_TCP_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_TCP_SecOff failed", testResult);

    }

    // 118062_2_3 InvalidRuntimeSelectorException - if the message selector is
    // invalid.

    @Test
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    public void testcreateBrowser_MessageSelector_Invalid_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Invalid_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Invalid_B_SecOff failed", testResult);

    }

    @Test
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    public void testcreateBrowser_MessageSelector_Invalid_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Invalid_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Invalid_TCP_SecOff failed", testResult);

    }

    // 118062_4 String getMessageSelector()

    // 118062_4_1 Gets this queue browser's message selector expression.

    // Bindings and Sec Off

    @Test
    public void testGetMessageSelector_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_B_SecOff");

        assertTrue("Test testGetMessageSelector_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    @Test
    public void testGetMessageSelector_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_TCP_SecOff");

        assertTrue("Test testGetMessageSelector_TCP_SecOff failed", testResult);

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

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
