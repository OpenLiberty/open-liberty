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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextTest_118067 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118067_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118067_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private static boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext_118067?test="
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
            setUpShirnkWrap();


        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server1.setHttpDefaultPort(8030);
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("JMSContext_118067_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");

        server.startServer("JMSContext_118067_Client.log");

        changedMessageFromLog = server.waitForStringInLog(
                                                          "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

    }

    // 118067_10 : Create a queue with queue name as null and send message to this queue 
    //Message send options may be specified using one or more of the following methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameNull_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameNull_B");
        assertTrue("testQueueNameNull_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameNull_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameNull_TcpIp");
        assertTrue("testQueueNameNull_TcpIp_SecOff failed ", val);
    }

    // 118067_11 : Create a queue with queue name as empty String and send message to this queue 
    //Message send options may be specified using one or more of the following methods: setDelivery , setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameEmptyString_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameEmptyString_B");
        assertTrue("testQueueNameEmptyString_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    //  @ExpectedFFDC(value = { "com.ibm.wsspi.injectionengine.InjectionException", "javax.resource.spi.InvalidPropertyException" })
    public void testQueueNameEmptyString_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameEmptyString_TcpIp");
        assertTrue("testQueueNameEmptyString_TcpIp_SecOff failed ", val);
    }

    // 118067_11 : Create a queue with queue name as empty String and send message to this queue 
    //Message send options may be specified using one or more of the following methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWildChars_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameWildChars_B");
        assertTrue("testQueueNameWildChars_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWildChars_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameWildChars_TcpIp");
        assertTrue("testQueueNameWildChars_TcpIp_SecOff failed ", val);
    }

    // 118067_11 : Create a queue with queue name containing spaces and send message to this queue 
    //Message send options may be specified using one or more of the following methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWithSpaces_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameWithSpaces_B");
        assertTrue("testQueueNameWithSpaces_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameWithSpaces_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameWithSpaces_TcpIp");
        assertTrue("testQueueNameWithSpaces_TcpIp_SecOff failed ", val);
    }

    // 118067_11 : Create a queue with queue name starting with underscore and send message to this queue
    //Create a queue with queue name starting with _temp and send message to this queue

    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueName_temp_B_SecOff() throws Exception {

        val = runInServlet("testQueueName_temp_B");
        assertTrue("testQueueName_temp_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueName_temp_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueName_temp_TcpIp");
        assertTrue("testQueueName_temp_TcpIp_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameLong_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameLong_B");
        assertTrue("testQueueNameLong_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameLong_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameLong_TcpIp");
        assertTrue("testQueueNameLong_TcpIp_SecOff failed ", val);

    }

    // 118067_11 :Test if queue name is case sensitive. Try to create a queue in small case and send message to queuename in upper case 
    //Bindings and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameCaseSensitive_B");
        assertTrue("testQueueNameCaseSensitive_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameCaseSensitive_TcpIp");
        assertTrue("testQueueNameCaseSensitive_TcpIp_SecOff failed ", val);

    }

//topic name null

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameNull_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameNull_B");
        assertTrue("testTopicNameNull_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameNull_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameNull_TcpIp");
        assertTrue("testTopicNameNull_TcpIp_SecOff failed ", val);

    }

//topic name empty string

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameEmptyString_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameEmptyString_B");
        assertTrue("testTopicNameEmptyString_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    //  @ExpectedFFDC(value = { "com.ibm.wsspi.injectionengine.InjectionException", "javax.resource.spi.InvalidPropertyException" })
    public void testTopicNameEmptyString_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameEmptyString_TcpIp");
        assertTrue("testTopicNameEmptyString_TcpIp_SecOff failed ", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWildChars_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameWildChars_B");
        assertTrue("testTopicNameWildChars_B_SecOff failed ", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWildChars_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameWildChars_TcpIp");
        assertTrue("testTopicNameWildChars_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWithSpaces_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameWithSpaces_B");
        assertTrue("testTopicNameWithSpaces_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameWithSpaces_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameWithSpaces_TcpIp");
        assertTrue("testTopicNameWithSpaces_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_B_SecOff() throws Exception {

        val = runInServlet("testTopicName_temp_B");
        assertTrue("testTopicName_temp_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicName_temp_TcpIp");
        assertTrue("testTopicName_temp_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameLong_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameLong_B");
        assertTrue("testTopicNameLong_B_SecOff failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameLong_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameLong_TcpIp");
        assertTrue("testTopicNameLong_TcpIp_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameCaseSensitive_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameCaseSensitive_B");
        assertTrue("testTopicNameCaseSensitive_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicNameCaseSensitive_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameCaseSensitive_TcpIp");
        assertTrue("testTopicNameCaseSensitive_TcpIp_SecOff failed ", val);

    }

    // 118067_9 : Test setting message properties on createProducer using method chaining. 
    //Message send options may be specified using one or more of the following methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    // Bindings and Security off

    @Test
    public void testSetMessageProperty_Bindings_SecOff() throws Exception {

        val = runInServlet("testSetMessagePropertyBindings_Send");
        assertTrue("testSetMessageProperty_Bindings_SecOff failed ", val);
    }

    // 118067_9 : Test setting message properties on createProducer using method chaining. 
    //Message send options may be specified using one or more of the following methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay, setDisableMessageTimestamp, setDisableMessageID and setAsync.
    // TCP/IP and Security off

    @Test
    public void testSetMessageProperty_TCP_SecOff() throws Exception {

        val = runInServlet("testSetMessagePropertyTcpIp_Send");
        assertTrue("testSetMessageProperty_TCP_SecOff failed ", val);

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

        Archive JMSContext_118067war = ShrinkWrap.create(WebArchive.class, "JMSContext_118067.war")
            .addClass("web.JMSContext_118067Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118067.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118067.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118067war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118067war, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
