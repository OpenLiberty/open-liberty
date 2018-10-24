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
package com.ibm.ws.messaging.JMS20.fat.ContextInject;

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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextInjectTest {

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextInjectTest_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextInjectTest_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContextInject?test="
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
    public void testP2P_TCP_SecOff() throws Exception {

        val = runInServlet("testP2P_TCP_SecOff");
        assertTrue("testP2P_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSub_TCP_SecOff() throws Exception {

        val = runInServlet("testPubSub_TCP_SecOff");
        assertTrue("testPubSub_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_B_SecOff() throws Exception {

        val = runInServlet("testPubSubDurable_B_SecOff");
        assertTrue("testPubSubDurable_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_TCP_SecOff() throws Exception {

        val = runInServlet("testPubSubDurable_TCP_SecOff");
        assertTrue("testPubSubDurable_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_B_SecOff() throws Exception {

        val = runInServlet("testNegativeSetters_B_SecOff");
        assertTrue("testNegativeSetters_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_TCP_SecOff() throws Exception {

        val = runInServlet("testNegativeSetters_TCP_SecOff");
        assertTrue("testNegativeSetters_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessageOrder_B_SecOff() throws Exception {

        val = runInServlet("testMessageOrder_B_SecOff");
        assertTrue("testMessageOrder_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    //   @Test
    public void testMessageOrder_TCP_SecOff() throws Exception {

        val = runInServlet("testMessageOrder_TCP_SecOff");
        assertTrue("testMessageOrder_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_B_SecOff() throws Exception {

        val = runInServlet("testGetAutoStart_B_SecOff");
        assertTrue("testGetAutoStart_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_TCP_SecOff() throws Exception {

        val = runInServlet("testGetAutoStart_TCP_SecOff");
        assertTrue("testGetAutoStart_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testBrowser_B_SecOff() throws Exception {

        val = runInServlet("testBrowser_B_SecOff");
        assertTrue("testBrowser_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    //  @Test
    public void testBrowser_TCP_SecOff() throws Exception {

        val = runInServlet("testBrowser_TCP_SecOff");
        assertTrue("testBrowser_TCP_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testEJBCallSecOff() throws Exception {

        val = runInServlet("testEJBCallSecOff");
        assertTrue("testEJBCallSecOff failed ", val);

    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSConsumer_118077war = ShrinkWrap.create(WebArchive.class, "JMSConsumer_118077.war")
            .addClass("web.JMSConsumer_118077Servlet")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118077.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSConsumer_118077.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSConsumer_118077war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSConsumer_118077war, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
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
