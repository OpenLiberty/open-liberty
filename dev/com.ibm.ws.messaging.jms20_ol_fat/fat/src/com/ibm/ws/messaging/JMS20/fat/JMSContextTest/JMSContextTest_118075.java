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
public class JMSContextTest_118075 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118075_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118075_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext_118075?test="
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

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("JMSContext_118075_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer("JMSContext_118075_Client.log");
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
    public void testCreateConsumerInvalidDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerInvalidDest_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerInvalidDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerInvalidDest_Topic_B_SecOff failed", val);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerInvalidDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerNullDest_B_SecOff");
        assertTrue("testCreateConsumerNullDest_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerNullDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerNullDest_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerNullDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerNullDest_Topic_TcpIp_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff failed", val);

    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff"); //reusing existing servlet method which creates consumer
        assertTrue("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff"); //reusing existing servlet method which creates consumer
        assertTrue("testQueueConsumer_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff failed", val);
    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started
    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseConsumerDepth_B_SecOff() throws Exception {

        val = runInServlet("testCloseConsumerDepth_B_SecOff");
        assertTrue("testCloseConsumerDepth_B_SecOff failed", val);
    }

    // @Mode(TestMode.FULL) @Test
    public void testCloseConsumerDepth_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCloseConsumerDepth_TcpIp_SecOff");
        assertTrue("testCloseConsumerDepth_TcpIp_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseConsumerSubscription_B_SecOff() throws Exception {

        val = runInServlet("testListSubscriber_B");
        assertTrue("testCloseConsumerSubscription_B_SecOff failed", val);

    }

    //  @Mode(TestMode.FULL) @Test
    public void testCloseConsumerSubscription_TcpIp_SecOff() throws Exception {

        val = runInServlet("testListSubscriber_TcpIp");
        assertTrue("testCloseConsumerSubscription_TcpIp_SecOff failed", val);

    }

    //  @Mode(TestMode.FULL) @Test
    public void testNoLocalTrue_TcpIp_SecOff() throws Exception {

        val = runInServlet("testNoLocalTrue_TcpIp");
        assertTrue("testNoLocalTrue_TcpIp_SecOff failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testNoLocalTrueQueue_B_SecOff() throws Exception {

        val = runInServlet("testNoLocalTrueQueue_B");
        assertTrue("testNoLocalTrueQueue_B_SecOff failed", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNoLocalTrueQueue_TcpIp_SecOff() throws Exception {

        val = runInServlet("testNoLocalTrueQueue_TcpIp");
        assertTrue("testNoLocalTrueQueue_TcpIp_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorTopic_B_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff failed", val);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocal_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff failed", val);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff failed", val);
    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff failed", val);
    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelector_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_B_SecOff failed", val);

    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff failed", val);

    }

    //Creates a JMSConsumer for  queue
    //Once consumer is created, perform a receive operation
    //Once consumer is created, check the context is started

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff failed", val);
    }

    @Test
    public void testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff");
        assertTrue("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff failed", val);

    }

    @Test
    public void testNoLocalTrue_B_SecOff() throws Exception {
        val = runInServlet("testNoLocalTrue_B");
        assertTrue("testNoLocalTrue_B_SecOff failed", val);

    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSContext_118075war = ShrinkWrap.create(WebArchive.class, "JMSContext_118075.war")
            .addClass("web.JMSContext_118075Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118075.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118075.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118075war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118075war, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
