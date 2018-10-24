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

public class LiteBucketSet2Test {

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("LiteBucketSet2Test_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("LiteBucketSet2Test_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private static boolean runInServlet(String test, String web) throws IOException {

        boolean result = false;
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

    //start of tests from JMSContextTest_118067

    // 118067_1 : Creates a new JMSProducer object which can be used to configure and send messages
    // 118067_2 :  Try send(Destination destination, String body) on JMSProducer object
    // Bindings and Security off

    @Test
    public void testCreateJmsProducerAndSend_B_SecOff() throws Exception {

        val = runInServlet("testCreateJmsProducerAndSend_B_SecOff", "JMSContext_118067");
        assertTrue("testCreateJmsProducerAndSend_B_SecOff failed ", val);

    }

    // 118067_5 : Creates a new JMSProducer object which can be used to configure and send messages
    // 118067_6 :  Try send(Destination destination, String body) on JMSProducer object
    // TCP/IP and Security off

    @Test
    public void testCreateJmsProducerAndSend_TCP_SecOff() throws Exception {

        val = runInServlet("testCreateJmsProducerAndSend_TCP_SecOff", "JMSContext_118067");
        assertTrue("testCreateJmsProducerAndSend_TCP_SecOff failed ", val);
    }

    // 118067_11 : Test create queue name as QUEUE/queue
    //Bindings and Security off

    @Test
    public void testQueueNameQUEUE_B_SecOff() throws Exception {

        val = runInServlet("testQueueNameQUEUE_B", "JMSContext_118067");
        assertTrue("testQueueNameQUEUE_B_SecOff failed ", val);
    }

    @Test
    public void testQueueNameQUEUE_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueNameQUEUE_TcpIp", "JMSContext_118067");
        assertTrue("testQueueNameQUEUE_TcpIp_SecOff failed ", val);
    }

    @Test
    public void testTopicNameTOPIC_B_SecOff() throws Exception {

        val = runInServlet("testTopicNameTOPIC_B", "JMSContext_118067");
        assertTrue("testTopicNameTOPIC_B_SecOff failed ", val);

    }

    @Test
    public void testTopicNameTOPIC_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicNameTOPIC_TcpIp", "JMSContext_118067");
        assertTrue("testTopicNameTOPIC_TcpIp_SecOff failed ", val);

    }

    //end of tests from JMSContextTest_118067

    //start of tests from JMSContextTest_118070
    //Closes the JMSContext 
    // If there are no other active (not closed) JMSContext objects using the underlying connection then this method also closes the underlying connection

    @Test
    public void testCloseAll_B_SecOff() throws Exception {

        val = runInServlet("testCloseAll_B_SecOff", "JMSContext_118070");
        assertTrue("testCloseAll_B_SecOff failed ", val);

    }

    @Test
    public void testCloseAll_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCloseAll_B_SecOff", "JMSContext_118070");
        assertTrue("testCloseAll_TcpIp_SecOff failed ", val);

    }

    //start of tests JMSConsumerTest_118077

    @Test
    public void testReceive_B_SecOff() throws Exception {

        val = runInServlet("testReceive_B_SecOff", "JMSConsumer_118077");
        assertTrue("testReceive_B_SecOff failed ", val);

    }

    @Test
    public void testReceive_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceive_TcpIp_SecOff", "JMSConsumer_118077");
        assertTrue("testReceive_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveBody_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBody_B_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveBody_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveBody_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBody_TcpIp_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveBody_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveWithTimeOut_B_SecOff() throws Exception {

        val = runInServlet("testReceiveWithTimeOut_B_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveWithTimeOut_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveWithTimeOut_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveWithTimeOut_TcpIp_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveWithTimeOut_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveNoWait_B_SecOff() throws Exception {

        val = runInServlet("testReceiveNoWait_B_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveNoWait_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveNoWait_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveNoWait_TcpIp_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveNoWait_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyNoWait_B_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWait_B_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveBodyNoWait_B_SecOff failed ", val);

    }

    @Test
    public void testReceiveBodyNoWait_TcpIp_SecOff() throws Exception {

        val = runInServlet("testReceiveBodyNoWait_TcpIp_SecOff", "JMSConsumer_118077");
        assertTrue("testReceiveBodyNoWait_TcpIp_SecOff failed ", val);

    }

    //end of tests of 118077

    //start of tests from JMSConsumerTest_118076

    //Closes the JMSConsumer. 

    @Test
    public void testCloseConsumer_B_SecOff() throws Exception {

        val = runInServlet("testCloseConsumer_B_SecOff", "JMSConsumer_118076");
        assertTrue("testCloseConsumer_B_SecOff failed ", val);
    }

    @Test
    public void testCloseConsumer_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCloseConsumer_TcpIp_SecOff", "JMSConsumer_118076");
        assertTrue("testCloseConsumer_TcpIp_SecOff failed ", val);

    }

    @Test
    public void testGetMessageSelector_B_SecOff() throws Exception {

        val = runInServlet("testGetMessageSelector_B_SecOff", "JMSConsumer_118076");
        assertTrue("testGetMessageSelector_B_SecOff failed ", val);

    }

    @Test
    public void testGetMessageSelector_TcpIp_SecOff() throws Exception {

        val = runInServlet("testGetMessageSelector_TcpIp_SecOff", "JMSConsumer_118076");
        assertTrue("testGetMessageSelector_TcpIp_SecOff failed ", val);

    }

    //end of tests from JMSConsumerTest_118076

    //start of tests of JMSContextTest_118075

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started

    @Test
    public void testQueueConsumer_B_SecOff() throws Exception {

        val = runInServlet("testQueueConsumer_B_SecOff", "JMSContext_118075");
        assertTrue("testQueueConsumer_B_SecOff failed", val);

    }

    @Test
    public void testQueueConsumer_TcpIp_SecOff() throws Exception {

        val = runInServlet("testQueueConsumer_TcpIp_SecOff", "JMSContext_118075");
        assertTrue("testQueueConsumer_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started

    @Test
    public void testTopicConsumer_B_SecOff() throws Exception {

        val = runInServlet("testTopicConsumer_B_SecOff", "JMSContext_118075");
        assertTrue("testTopicConsumer_B_SecOff failed", val);

    }

    @Test
    public void testTopicConsumer_TcpIp_SecOff() throws Exception {

        val = runInServlet("testTopicConsumer_TcpIp_SecOff", "JMSContext_118075");
        assertTrue("testTopicConsumer_TcpIp_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithMsgSelector_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelector_B_SecOff", "JMSContext_118075");
        assertTrue("testCreateConsumerWithMsgSelector_B_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithMsgSelector_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelector_TcpIp_SecOff", "JMSContext_118075");
        assertTrue("testCreateConsumerWithMsgSelector_TcpIp_SecOff failed", val);
    }

    //end of tests from JMSContextTest_118075

    //start of JMSProducer_Test118073

    @Test
    public void testSetGetJMSReplyTo_B_SecOff() throws Exception {

        val = runInServlet("testSetGetJMSReplyTo_B_SecOff", "JMSProducer_118073");

        assertTrue("testSetGetJMSReplyTo_B_SecOff failed", val);

    }

    @Test
    public void testSetGetJMSReplyTo_TCP_SecOff() throws Exception {

        val = runInServlet("testSetGetJMSReplyTo_TCP_SecOff", "JMSProducer_118073");

        assertNotNull("testSetGetJMSReplyTo_TCP_SecOff", val);

    }

    @Test
    public void testGetAsync_B_SecOff() throws Exception {

        val = runInServlet("testGetAsync_B_SecOff", "JMSProducer_118073");

        assertTrue("testGetAsync_B_SecOff", val);

    }

    @Test
    public void testGetAsync_TCP_SecOff() throws Exception {

        val = runInServlet("testGetAsync_TCP_SecOff", "JMSProducer_118073");

        assertTrue("testGetAsync_TCP_SecOff", val);

    }

    //end of tests from JMSProducer_test118073

    //start of tests from JMSContextInjectTest

    @Test
    public void testP2P_B_SecOff() throws Exception {

        val = runInServlet("testP2P_B_SecOff", "JMSContextInject");
        assertTrue("testP2P_B_SecOff failed ", val);

    }

    @Test
    public void testPubSub_B_SecOff() throws Exception {

        val = runInServlet("testPubSub_B_SecOff", "JMSContextInject");
        assertTrue("testPubSub_B_SecOff failed ", val);

    }

    //end of testcases from JMSContextInjectTest
    public static void setUpShirnkWrap() throws Exception {

        Archive JMSConsumer_118077war = ShrinkWrap.create(WebArchive.class, "JMSConsumer_118077.war")
            .addClass("web.JMSConsumer_118077Servlet")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118077.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118077.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSConsumer_118077war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSConsumer_118077war, OVERWRITE);
        Archive JMSProducer_118073war = ShrinkWrap.create(WebArchive.class, "JMSProducer_118073.war")
            .addClass("web.JMSProducer_118073Servlet")
            .add(new FileAsset(new File("test-applications//JMSProducer_118073.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSProducer_118073.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSProducer_118073war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSProducer_118073war, OVERWRITE);
        Archive JMSContext_118075war = ShrinkWrap.create(WebArchive.class, "JMSContext_118075.war")
            .addClass("web.JMSContext_118075Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118075.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118075.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118075war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118075war, OVERWRITE);
        Archive JMSConsumer_118076war = ShrinkWrap.create(WebArchive.class, "JMSConsumer_118076.war")
            .addClass("web.JMSConsumer_118076Servlet")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118076.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118076.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSConsumer_118076war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSConsumer_118076war, OVERWRITE);
        Archive JMSContext_118067war = ShrinkWrap.create(WebArchive.class, "JMSContext_118067.war")
            .addClass("web.JMSContext_118067Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118067.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118067.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118067war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118067war, OVERWRITE);
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
        Archive JMSContext_118070war = ShrinkWrap.create(WebArchive.class, "JMSContext_118070.war")
            .addClass("web.JMSContext_118070Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118070.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118070.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118070war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118070war, OVERWRITE);
        Archive JMSContextInjectwar = ShrinkWrap.create(WebArchive.class, "JMSContextInject.war")
            .addClass("ejb.SampleSecureStatelessBean")
            .addClass("web.JMSContextInjectServlet")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/WEB-INF/beans.xml")), "WEB-INF/beans.xml")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextInjectwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextInjectwar, OVERWRITE);
    }
}
