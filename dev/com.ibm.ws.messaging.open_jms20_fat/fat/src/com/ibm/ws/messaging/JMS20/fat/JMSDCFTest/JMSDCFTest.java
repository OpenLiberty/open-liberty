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
package com.ibm.ws.messaging.JMS20.fat.JMSDCFTest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSDCFTest {

    private static final LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSDCFEngine");
    private static final LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSDCFClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHostName = clientServer.getHostname();

    private static final String dcfAppName = "JMSDCF";
    private static final String dcfContextRoot = "JMSDCF";
    private static final String[] dcfPackages = new String[] { "jmsdcf.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHostName, clientPort, dcfContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSDCFEngine.xml");

        clientServer.copyFileToLibertyInstallRoot( "lib/features", "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSDCFClient.xml");
        TestUtils.addDropinsWebApp(clientServer, dcfAppName, dcfPackages);

        engineServer.startServer();
        clientServer.startServer();
    }

    @AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //

    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) //TODO: injection problem
    @Test
    public void testP2P_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testP2P_B_SecOff");
        assertTrue("testP2P_B_SecOff failed ", testResult);
    }

    @Test
    public void testP2P_B_SecOff_inject() throws Exception {
        boolean testResult = runInServlet("testP2P_B_SecOff_inject");
        assertTrue("testP2P_B_SecOff failed ", testResult);
    }

    @Test
    public void testPubSub_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSub_B_SecOff");
        assertTrue("testPubSub_B_SecOff failed ", testResult);
    }

    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) //TODO: injection problem
    @Test
    public void testPubSub_B_SecOff_implicitBinding() throws Exception {
        boolean testResult = runInServlet("testPubSub_B_SecOff_implicitBinding");
        assertTrue("testPubSub_B_SecOff failed ", testResult);
    }

    @Test
    public void testPubSubDurable_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testPubSubDurable_B_SecOff");
        assertTrue("testPubSubDurable_B_SecOff failed ", testResult);
    }

    @Test
    public void testMessageOrder_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testMessageOrder_B_SecOff");
        assertTrue("testMessageOrder_B_SecOff failed ", testResult);
    }

    @Test
    public void testBrowser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testBrowser_B_SecOff");
        assertTrue("testBrowser_B_SecOff failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testObjects() throws Exception {
        boolean testResult = runInServlet("testObjects");
        assertTrue("testObjects failed ", testResult);
    }
}
