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
package com.ibm.ws.messaging.JMS20.fat.ContextInject;

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
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextInjectTest {

    private static final LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextInjectEngine");
    private static final LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextInjectClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String contextInjectAppName = "JMSContextInject";
    private static final String contextInjectContextRoot = "JMSContextInject";
    private static final String[] contextInjectPackages =
        new String[] { "jmscontextinject.ejb", "jmscontextinject.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, contextInjectContextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextInjectEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextInjectClient.xml");
        TestUtils.addDropinsWebApp(clientServer, contextInjectAppName, contextInjectPackages);

        engineServer.startServer("JMSContectInjectEngine.log");
        clientServer.startServer("JMSContectInjectClient.log");
    }

    @org.junit.AfterClass
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
    }

    //

    @Mode(TestMode.FULL)
    @Test
    public void testP2P_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testP2P_TCP_SecOff");
        assertTrue("testP2P_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSub_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSub_TCP_SecOff");
        assertTrue("testPubSub_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSubDurable_B_SecOff");
        assertTrue("testPubSubDurable_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPubSubDurable_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSubDurable_TCP_SecOff");
        assertTrue("testPubSubDurable_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testNegativeSetters_B_SecOff");
        assertTrue("testNegativeSetters_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testNegativeSetters_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testNegativeSetters_TCP_SecOff");
        assertTrue("testNegativeSetters_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMessageOrder_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testMessageOrder_B_SecOff");
        assertTrue("testMessageOrder_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test // TODO
    public void testMessageOrder_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testMessageOrder_TCP_SecOff");
        assertTrue("testMessageOrder_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetAutoStart_B_SecOff");
        assertTrue("testGetAutoStart_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetAutoStart_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetAutoStart_TCP_SecOff");
        assertTrue("testGetAutoStart_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBrowser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testBrowser_B_SecOff");
        assertTrue("testBrowser_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    // @Test // TODO
    public void testBrowser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testBrowser_TCP_SecOff");
        assertTrue("testBrowser_TCP_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testEJBCallSecOff() throws Exception {
        boolean testResult = runInServlet("testEJBCallSecOff");
        assertTrue("testEJBCallSecOff failed", testResult);
    }
}
