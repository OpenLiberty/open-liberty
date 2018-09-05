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
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextTest_118062 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();
    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118062_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118062_TestServer1");

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
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("JMSContextTestClient_118062.log");
        server1.startServer("JMSContextTestServer_118062.log");

    }

    // 118062_1_3 : InvalidRuntimeDestinationException - if an invalid
    // destination is specified
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowserNEQueue_B_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowserNEQueue_B_SecOff");

        assertTrue("Test testcreateBrowserNEQueue_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException")
    public void testcreateBrowserNEQueue_TCP_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowserNEQueue_TCP_SecOff");

        assertTrue("Test testcreateBrowserNEQueue_TCP_SecOff failed", testResult);

    }

    // 118062_2_2 InvalidRuntimeDestinationException - if an invalid destination
    // is specified
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_InvalidQ_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_InvalidQ_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_InvalidQ_B_SecOff failed", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff failed", testResult);
    }

    // 118062_2_4 Test when queue is null
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_NullQueue_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_NullQueue_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_NullQueue_B_SecOff failed", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff failed", testResult);

    }

    // 118062_2_5 :Test when Message Selector is provided as empty string
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Empty_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Empty_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Empty_B_SecOff failed", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Empty_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Empty_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Empty_TCP_SecOff failed", testResult);

    }

    // 118062_2_6 Test when message selector is provided as null

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Null_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Null_B_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Null_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateBrowser_MessageSelector_Null_TCP_SecOff");

        assertTrue("Test testcreateBrowser_MessageSelector_Null_TCP_SecOff failed", testResult);

    }

    // 118062_3_1:Gets the queue associated with this queue browser.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_getQueue_B_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowser_getQueue_B_SecOff");

        assertTrue("Test testcreateBrowser_getQueue_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_getQueue_TCP_SecOff() throws Exception {
        testResult = runInServlet("testcreateBrowser_getQueue_TCP_SecOff");

        assertTrue("Test testcreateBrowser_getQueue_TCP_SecOff failed", testResult);

    }

    // 118062_6 : void close()

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testClose_B_SecOff() throws Exception {

        testResult = runInServlet("testClose_B_SecOff");

        assertTrue("Test testClose_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testClose_TCP_SecOff() throws Exception {

        testResult = runInServlet("testClose_TCP_SecOff");

        assertTrue("Test testClose_TCP_SecOff failed", testResult);
    }

    // 118062_4_2:Test when no message selector exists for the message consumer,
    // it returns null
    // Bindings and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Consumer_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Consumer_B_SecOff");

        assertTrue("Test testGetMessageSelector_Consumer_B_SecOff failed", testResult);

    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Consumer_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Consumer_TCP_SecOff");

        assertTrue("Test testGetMessageSelector_Consumer_TCP_SecOff failed", testResult);

    }

    // 118062_4_3 Test when message selector is set to null, it returns null

    // Bindings and Sec Off

    // @Test
    public void testGetMessageSelector_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Null_B_SecOff");

        assertTrue("Test testGetMessageSelector_Null_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    // @Test
    public void testGetMessageSelector_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Null_TCP_SecOff");

        assertTrue("Test testGetMessageSelector_Null_TCP_SecOff failed", testResult);

    }

    // 118062_4_4: Test when message selector is set to empty string, it returns
    // null

    // Bindings and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Empty_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Empty_B_SecOff");

        assertTrue("Test testGetMessageSelector_Empty_B_SecOff failed", testResult);
    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Empty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_Empty_TCP_SecOff");

        assertTrue("Test testGetMessageSelector_Empty_TCP_SecOff failed", testResult);

    }

    // 118062_4_5 Test when message selector is not set , it returns null

    // Bindings and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_notSet_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_notSet_B_SecOff");

        assertTrue("Test testGetMessageSelector_notSet_B_SecOff failed", testResult);

    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_notSet_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMessageSelector_notSet_TCP_SecOff");

        assertTrue("Test testGetMessageSelector_notSet_TCP_SecOff failed", testResult);

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
