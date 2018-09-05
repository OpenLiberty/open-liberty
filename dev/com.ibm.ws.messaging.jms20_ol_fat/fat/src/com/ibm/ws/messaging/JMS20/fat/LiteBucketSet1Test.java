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
package com.ibm.ws.messaging.JMS20.fat;

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

import static org.junit.Assert.assertNotNull;
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

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class LiteBucketSet1Test {

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("LiteBucketSet1Test_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("LiteBucketSet1Test_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private static boolean runInServlet(String test, String web) throws IOException {
        boolean result = false;

        // URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext?test="
        //                   + test);
        URL url = new URL("http://" + HOST + ":" + PORT + "/" + web + "?test=" + test);
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
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("JMSContextTestClient_118058.log");
        server1.startServer("JMSContextTestServer_118058.log");

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {

            server.stopServer();
            server1.stopServer();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //Tests from JMSContectTest_118058

    // Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext()

    // 118058_1_1 :Creation of JMSContext from Connection factory.

    // Bindings and Security off

    @Test
    public void testCreateContext_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateContext_B_SecOff", "JMSContext");

        assertTrue("Test testCreateContext_B_SecOff failed", testResult);

    }

    // TCP and Security off

    @Test
    public void testCreateContext_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateContext_TCP_SecOff", "JMSContext");

        assertTrue("Test testCreateContext_TCP_SecOff failed", testResult);

    }

    // 118058_1_4 : Verify default autostart value 
    //Bindings and Security off

    @Test
    public void testautoStart_B_SecOff() throws Exception {

        testResult = runInServlet("testautoStart_B_SecOff", "JMSContext");

        assertNotNull("Test testautoStart_B_SecOff failed", testResult);

    }

    // TCP/IP and Security Off

    @Test
    public void testautoStart_TCP_SecOff() throws Exception {

        testResult = runInServlet("testautoStart_TCP_SecOff", "JMSContext");

        assertNotNull("Test testautoStart_TCP_SecOff failed", testResult);

    }

    //end of tests from 118058

    //Tests from JMSContextTest_118061

    // 118061_1 : Verify creation of message from JMSContext. createMessage().
    // Send and Receive.

    // Bindings and Security Off

    @Test
    public void testCreateMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateMessage_B_SecOff", "JMSContext");

        assertTrue("Test testCreateMessage_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testCreateMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateMessage_TCP_SecOff", "JMSContext");

        assertTrue("Test testCreateMessage_TCP_SecOff failed", testResult);

    }

    @Test
    public void testTextMessageGetBody_B_SecOff() throws Exception {

        testResult = runInServlet("testTextMessageGetBody_B_SecOff", "JMSContext");

        assertTrue("Test testTextMessageGetBody_B_SecOff failed", testResult);

    }

    @Test
    public void testJMSReplyTo() throws Exception {
        testResult = runInServlet("testJMSReplyTo", "JMSContext");

        assertTrue("Test testJMSReplyTo failed", testResult);

    }

    //end of tests from118061

    //start of tests from JMSContextTest_118062

    // 118062_1 Test with createBrowser(Queue queue)
    // 118062_1_1 Creates a QueueBrowser object to peek at the messages on the
    // specified queue.

    // Bindings and Security Off

    @Test
    public void testcreateBrowser_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateBrowser_B_SecOff", "JMSContext");

        assertTrue("Test testcreateBrowser_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testcreateBrowser_TCP_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowser_TCP_SecOff", "JMSContext");

        assertTrue("Test testcreateBrowser_TCP_SecOff failed", testResult);
    }

    //end of tests from 118062

    //start of tests from JMSProducerTest_118071

    // 118071_1 JMSProducer send(Destination destination,Message message)
    // 118071_1_1_Q : Sends the message to the specified queue using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security Off
    @Test
    public void testJMSProducerSendMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_B_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendMessage_B_SecOff failed", testResult);

    }

    // TCP/IP and Security Off
    @Test
    public void testJMSProducerSendMessage_TCP_SecOff() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_TCP_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendMessage_TCP_SecOff failed", testResult);

    }

    // 118071_1_7_T Sends a message to the specified topic using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    // Bindings and Security Off

    @Test
    public void testJMSProducerSendMessage_Topic_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSProducerSendMessage_Topic_B_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendMessage_Topic_B_SecOff failed",
                   testResult);

    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendMessage_Topic_TCP_SecOff() throws Exception {
        testResult = runInServlet("testJMSProducerSendMessage_Topic_TCP_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendMessage_Topic_TCP_SecOff failed",
                   testResult);
    }

    // 118071_2 JMSProducer send(Destination destination, String body)

    // 118071_2_1_Q Send a TextMessage with the specified body to the specified
    // queue, using any send options, message properties and message headers
    // that have been defined on this JMSProducer.

    // Bindings and Security Off

    @Test
    public void testJMSProducerSendTextMessage_B_SecOff() throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_B_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendTextMessage_B_SecOff failed",
                   testResult);

    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendTextMessage_TCP_SecOff() throws Exception {
        testResult = runInServlet("testJMSProducerSendTextMessage_TCP_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendTextMessage_TCP_SecOff failed",
                   testResult);

    }

    // 118071_2_6_Topic Send a TextMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    // Bindings and Security Off

    @Test
    public void testJMSProducerSendTextMessage_Topic_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_Topic_B_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendTextMessage_Topic_B_SecOff failed",
                   testResult);
    }

    // TCP/IP and Security Off

    @Test
    public void testJMSProducerSendTextMessage_Topic_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testJMSProducerSendTextMessage_Topic_TCP_SecOff", "JMSProducer");

        assertTrue("Test testJMSProducerSendTextMessage_Topic_TCP_SecOff failed",
                   testResult);
    }

    //end of tests of 118071
    //start of tests from JMSProducerTest_118073

    // 118073_2 boolean propertyExists(String name)
    // 118073_2_1 Returns true if a message property with the specified name has
    // been set on this JMSProducer
    // Bindings and Security Off

    @Test
    public void testPropertyExists_B_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_B_SecOff", "JMSProducer");

        assertTrue("Test testPropertyExists_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testPropertyExists_TCP_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_TCP_SecOff", "JMSProducer");

        assertTrue("Test testPropertyExists_TCP_SecOff failed", testResult);

    }

    // 118073_31 Set<String> getPropertyNames()
    // 118073_31_1 Returns an unmodifiable Set view of the names of all the
    // message properties that have been set on this JMSProducer.
    // 118073_31_2 JMS standard header fields are not considered properties and
    // are not returned in this Set.

    // Bindings and Security Off

    @Test
    public void testGetPropertyNames_B_SecOff() throws Exception {
        testResult = runInServlet("testGetPropertyNames_B_SecOff", "JMSProducer");

        assertTrue("Test testGetPropertyNames_B_SecOff failed", testResult);

    }

    // TCP and Security OFf

    @Test
    public void testGetPropertyNames_TCP_SecOff() throws Exception {
        testResult = runInServlet("testGetPropertyNames_TCP_SecOff", "JMSProducer");

        assertTrue("Test testGetPropertyNames_TCP_SecOff failed", testResult);

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
        testResult = runInServlet("testSetJMSCorrelationID_B_SecOff", "JMSProducer");

        assertTrue("Test testSetJMSCorrelationID_B_SecOff failed", testResult);

    }

    // TCP and Security OFf

    @Test
    public void testSetJMSCorrelationID_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSCorrelationID_TCP_SecOff", "JMSProducer");

        assertTrue("Test testSetJMSCorrelationID_TCP_SecOff failed", testResult);

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
        testResult = runInServlet("testSetJMSCorrelationIDAsBytes_B_SecOff", "JMSProducer");

        assertTrue("Test testSetJMSCorrelationIDAsBytes_B_SecOff failed", testResult);

    }

    // TCP and Security OFf

    @Test
    public void testSetJMSCorrelationIDAsBytes_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSCorrelationIDAsBytes_TCP_SecOff", "JMSProducer");

        assertTrue("Test testSetJMSCorrelationIDAsBytes_TCP_SecOff failed", testResult);

    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSProducerwar = ShrinkWrap.create(WebArchive.class, "JMSProducer.war")
            .addClass("web.JMSProducerServlet")
            .add(new FileAsset(new File("test-applications//JMSProducer.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSProducer.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSProducerwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSProducerwar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
