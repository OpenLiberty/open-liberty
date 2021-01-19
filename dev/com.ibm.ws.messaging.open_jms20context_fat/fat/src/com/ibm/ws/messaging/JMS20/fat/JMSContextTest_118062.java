/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118062 {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");
    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSContext";
    private static final String[] appPackages = new String[] { "jmscontext.web" };
    private static final String contextRoot = "JMSContext";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("JMSContextTest_118062_Engine.log");
        clientServer.startServer("JMSContextTest_118062_Client.log");
    }

    @AfterClass
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

        ShrinkHelper.cleanAllExportedArchives();
    }

    // 118062_1_3 : InvalidRuntimeDestinationException - if an invalid
    // destination is specified

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowserNEQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowserNEQueue_B_SecOff");
        assertTrue("Test testcreateBrowserNEQueue_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowserNEQueue_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowserNEQueue_TCP_SecOff");
        assertTrue("Test testcreateBrowserNEQueue_TCP_SecOff failed", testResult);
    }

    // 118062_2_2 InvalidRuntimeDestinationException - if an invalid destination
    // is specified

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_InvalidQ_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_InvalidQ_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_InvalidQ_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff failed", testResult);
    }

    // 118062_2_4 Test when queue is null

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_NullQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_NullQueue_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_NullQueue_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff failed", testResult);
    }

    // 118062_2_5 :Test when Message Selector is provided as empty string

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Empty_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Empty_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Empty_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Empty_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Empty_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Empty_TCP_SecOff failed", testResult);
    }

    // 118062_2_6 Test when message selector is provided as null

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_MessageSelector_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Null_B_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Null_B_SecOff failed", testResult);
    }

    @Test
    public void testcreateBrowser_MessageSelector_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_MessageSelector_Null_TCP_SecOff");
        assertTrue("Test testcreateBrowser_MessageSelector_Null_TCP_SecOff failed", testResult);
    }

    // 118062_3_1:Gets the queue associated with this queue browser.

    @Mode(TestMode.FULL)
    public void testcreateBrowser_getQueue_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_getQueue_B_SecOff");
        assertTrue("Test testcreateBrowser_getQueue_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testcreateBrowser_getQueue_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateBrowser_getQueue_TCP_SecOff");
        assertTrue("Test testcreateBrowser_getQueue_TCP_SecOff failed", testResult);
    }

    // 118062_6 : void close()

    @Mode(TestMode.FULL)
    @Test
    public void testClose_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testClose_B_SecOff");
        assertTrue("Test testClose_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClose_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testClose_TCP_SecOff");
        assertTrue("Test testClose_TCP_SecOff failed", testResult);
    }

    // 118062_4_2:Test when no message selector exists for the message consumer,
    // it returns null

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Consumer_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Consumer_B_SecOff");
        assertTrue("Test testGetMessageSelector_Consumer_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Consumer_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Consumer_TCP_SecOff");
        assertTrue("Test testGetMessageSelector_Consumer_TCP_SecOff failed", testResult);
    }

    // 118062_4_3 Test when message selector is set to null, it returns null

    // @Test
    public void testGetMessageSelector_Null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Null_B_SecOff");
        assertTrue("Test testGetMessageSelector_Null_B_SecOff failed", testResult);
    }

    // @Test
    public void testGetMessageSelector_Null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Null_TCP_SecOff");
        assertTrue("Test testGetMessageSelector_Null_TCP_SecOff failed", testResult);
    }

    // 118062_4_4: Test when message selector is set to empty string, it returns null

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Empty_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Empty_B_SecOff");
        assertTrue("Test testGetMessageSelector_Empty_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_Empty_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_Empty_TCP_SecOff");
        assertTrue("Test testGetMessageSelector_Empty_TCP_SecOff failed", testResult);
    }

    // 118062_4_5 Test when message selector is not set , it returns null

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_notSet_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_notSet_B_SecOff");
        assertTrue("Test testGetMessageSelector_notSet_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetMessageSelector_notSet_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMessageSelector_notSet_TCP_SecOff");
        assertTrue("Test testGetMessageSelector_notSet_TCP_SecOff failed", testResult);
    }
}
