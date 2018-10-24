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

public class LiteBucketSet3Test {

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("LiteBucketSet3Test_TestServer");
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("LiteBucketSet3Test_TestServer2");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean val = false;

    private boolean runInServlet(String test, String web) throws IOException {
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

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);

                result = false;
            } else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
            setUpShirnkWrap();


        server.setServerConfigurationFile("ApiTD.xml");
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");

        server1.setHttpDefaultPort(8030);
        server.startServer("JMSContextTest_118065_Client.log");
        server1.startServer("JMSContextTest_118065_Server.log");

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

    //start of tests from JMSContextTest_118065

    // -----118065 ----------
    //JMSContext: Handle transactional capabilities for JMSContext (commit, rollback, acknowledge, recover)
    // Below test cases are the various APIs for handling transactional capabilities for JMSContext (commit, rollback,  recover)

    @Test
    public void testCommitLocalTransaction_B() throws Exception {

        val = runInServlet("testCommitLocalTransaction_B", "TemporaryQueue");
        assertTrue("testCommitLocalTransaction_B failed ", val);

    }

    @Test
    public void testCommitLocalTransaction_TCP() throws Exception {

        val = runInServlet("testCommitLocalTransaction_TCP", "TemporaryQueue");
        assertTrue("testCommitLocalTransaction_TCP failed ", val);

    }

    @Test
    public void testRollbackLocalTransaction_B() throws Exception {

        val = runInServlet("testRollbackLocalTransaction_B", "TemporaryQueue");
        assertTrue("testRollbackLocalTransaction_B failed ", val);

    }

    @Test
    public void testRollbackLocalTransaction_TCP() throws Exception {

        val = runInServlet("testRollbackLocalTransaction_TCP", "TemporaryQueue");
        assertTrue("testRollbackLocalTransaction_TCP failed ", val);

    }

    //end of tests from JMSContextTest_118065

    //start of tests JMSContextTest_118066
    // ----- 118066
    // This story is used to implement the functionality of Start and stop for JMSContext
    // Below test cases are the APIs  of Start and stop for JMSContext

    @Test
    public void testStartJMSContextSecOffBinding() throws Exception {

        val = runInServlet("testStartJMSContextSecOffBinding", "TemporaryQueue");
        assertTrue("testStartJMSContextSecOffBinding failed ", val);

    }

    @Test
    public void testStartJMSContextSecOffTCP() throws Exception {

        val = runInServlet("testStartJMSContextSecOffTCP", "TemporaryQueue");
        assertTrue("testStartJMSContextSecOffTCP failed ", val);

    }

    @Test
    public void testStopJMSContextSecOffBinding() throws Exception {
        val = runInServlet("testStopJMSContextSecOffBinding", "TemporaryQueue");
        assertTrue("testStopJMSContextSecOffBinding failed ", val);

    }

    @Test
    public void testStopJMSContextSecOffTCPIP() throws Exception {
        val = runInServlet("testStopJMSContextSecOffTCP", "TemporaryQueue");
        assertTrue("testStopJMSContextSecOffTCPIP failed ", val);
    }

    //end of tests from JMSContextTest_118066

    //start of tests from JMSContextTest_118068

    // ---- 118068
    // Below test cases are the APIs to implement the funtionality to support the creation of temporary queues and topics for JMSContext

    @Test
    public void testCreateTemporaryQueueSecOffBinding() throws Exception {

        val = runInServlet("testCreateTemporaryQueueSecOffBinding", "TemporaryQueue");
        assertTrue("testCreateTemporaryQueueSecOffBinding failed ", val);
    }

    @Test
    public void testCreateTemporaryQueueSecOffTCPIP() throws Exception {
        val = runInServlet("testCreateTemporaryQueueSecOffTCPIP", "TemporaryQueue");
        assertTrue("testCreateTemporaryQueueSecOffTCPIP failed ", val);

    }

    @Test
    public void testTemporaryQueueLifetimeSecOff_B() throws Exception {

        val = runInServlet("testTemporaryQueueLifetimeSecOff_B", "TemporaryQueue");
        assertTrue("testTemporaryQueueLifetimeSecOff_B failed ", val);
    }

    @Test
    public void testTemporaryQueueLifetimeSecOff_TCPIP() throws Exception {
        val = runInServlet("testTemporaryQueueLifetimeSecOff_TCPIP", "TemporaryQueue");
        assertTrue("testTemporaryQueueLifetimeSecOff_TCPIP failed ", val);

    }

    @Test
    public void testDeleteTemporaryQueueNameSecOffBinding() throws Exception {
        val = runInServlet("testDeleteTemporaryQueueNameSecOffBinding", "TemporaryQueue");
        assertTrue("testDeleteTemporaryQueueNameSecOffBinding failed ", val);

    }

    @Test
    public void testPTPTemporaryQueue_Binding() throws Exception {

        val = runInServlet("testPTPTemporaryQueue_Binding", "TemporaryQueue");

        assertTrue("testPTPTemporaryQueue_Binding failed ", val);

    }

    @Test
    public void testPTPTemporaryQueue_TCP() throws Exception {

        val = runInServlet("testPTPTemporaryQueue_TCP", "TemporaryQueue");

        assertTrue("testPTPTemporaryQueue_TCP failed ", val);
    }

    @Test
    public void testCreateTemporaryTopicSecOffBinding() throws Exception {

        val = runInServlet("testCreateTemporaryTopicSecOffBinding", "TemporaryQueue");

        assertTrue("testCreateTemporaryTopicSecOffBinding failed ", val);

    }

    @Test
    public void testCreateTemporaryTopicSecOffTCPIP() throws Exception {
        val = runInServlet("testCreateTemporaryTopicSecOffTCPIP", "TemporaryQueue");

        assertTrue("testCreateTemporaryTopicSecOffTCPIP failed ", val);

    }

    //end of tests from JMSContextTest_118068

    //start of tests from JMSContextTest_118077

    // --------------118077 ---------------------------
    //JMSConsumer:Implement the various receive API's to allow messages to be consumed from the ME using JMSConsumer object
    // Below test cases are the various receive API's to allow messages to be consumed from the ME using JMSConsumer object with Topics

    @Test
    public void testReceiveMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveMessageTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveMessageTopicSecOff_B failed ", val);

    }

    @Test
    public void testReceiveMessageTopicSecOff_TCP() throws Exception {
        val = runInServlet("testReceiveMessageTopicSecOff_TCP", "TemporaryQueue");
        assertTrue("testReceiveMessageTopicSecOff_TCP failed ", val);
    }

    @Test
    public void testReceiveTimeoutMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveTimeoutMessageTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveTimeoutMessageTopicSecOff_B failed ", val);

    }

    @Test
    public void testReceiveTimeoutMessageTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveTimeoutMessageTopicSecOff_TCP", "TemporaryQueue");
        assertTrue("testReceiveTimeoutMessageTopicSecOff_TCP failed ", val);

    }

    @Test
    public void testReceiveNoWaitMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveNoWaitMessageTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveNoWaitMessageTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveNoWaitMessageTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveNoWaitMessageTopicSecOff_TCP", "TemporaryQueue");
        assertTrue("testReceiveNoWaitMessageTopicSecOff_TCP failed ", val);
    }

    @Test
    public void testReceiveBodyTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveBodyTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveBodyTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTopicSecOff_TCPIP", "TemporaryQueue");
        assertTrue("testReceiveBodyTopicSecOff_TCPIP failed ", val);
    }

    @Test
    public void testReceiveBodyTimeOutTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveBodyTimeOutTopicSecOff_B failed ", val);

    }

    @Test
    public void testReceiveBodyTimeOutTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTopicSecOff_TCPIP", "TemporaryQueue");
        assertTrue("testReceiveBodyTimeOutTopicSecOff_TCPIP failed ", val);
    }

    @Test
    public void testReceiveBodyNoWaitTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTopicSecOff_B", "TemporaryQueue");
        assertTrue("testReceiveBodyNoWaitTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveBodyNoWaitTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTopicSecOff_TCPIP", "TemporaryQueue");
        assertTrue("testReceiveBodyNoWaitTopicSecOff_TCPIP failed ", val);
    }

    public static void setUpShirnkWrap() throws Exception {

        Archive TemporaryQueuewar = ShrinkWrap.create(WebArchive.class, "TemporaryQueue.war")
            .addClass("web.JMSContextTestServlet")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, TemporaryQueuewar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, TemporaryQueuewar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
