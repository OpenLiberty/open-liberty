/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DelayFullSecOnTest {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "DeliveryDelay";
    private static final String[] appPackages = new String[] { "deliverydelay.web" };
    private static final String contextRoot = "DeliveryDelay";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");
        engineServer.copyFileToLibertyServerRoot(
            "resources/security", "engineLTPAKeys/cert.der");
        engineServer.copyFileToLibertyServerRoot(
            "resources/security", "engineLTPAKeys/ltpa.keys");
        engineServer.copyFileToLibertyServerRoot(
            "resources/security", "engineLTPAKeys/ltpaFIPS.keys");
        engineServer.copyFileToLibertyServerRoot(
            "resources/security", "engineLTPAKeys/mykey.jks");
        engineServer.setServerConfigurationFile("DelayEngine_SSL.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");
        clientServer.copyFileToLibertyServerRoot(
            "resources/security", "clientLTPAKeys/mykey.jks");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);
        clientServer.setServerConfigurationFile("DelayClient_SSL.xml");

        engineServer.startServer("DelayFullOn_Engine.log");
        clientServer.startServer("DelayFullOn_Client.log");
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

    //

    @Test
    public void testDeliveryMultipleMsgsSecOn_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgs");
        assertTrue("testDeliveryMultipleMsgs failed", testResult);
    }

    @Test
    public void testDeliveryMultipleMsgsSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgs_Tcp");
        assertTrue("testDeliveryMultipleMsgs_Tcp failed", testResult);
    }

    @Test
    public void testTimingTopicSecOn_B() throws Exception {
        boolean testResult = runInServlet("testTimingTopic_B");
        assertTrue("testTimingTopic_B failed", testResult);
    }

    @Test
    public void testTimingTopicSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimingTopic_Tcp");
        assertTrue("testTimingTopic_Tcp failed", testResult);
    }

    @Test
    public void testGetDeliveryDelayTopicSecOn_B() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayTopic");
        assertTrue("testGetDeliveryDelayTopic failed", testResult);
    }

    @Test
    public void testGetDeliveryDelayTopicSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayTopic_Tcp");
        assertTrue("testGetDeliveryDelayTopic_Tcp failed", testResult);
    }

    @Test
    public void testDeliveryMultipleMsgsTopicClassicApiSecOn_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopicClassicApi");
        assertTrue("testDeliveryMultipleMsgsTopicClassicApi failed", testResult);
    }

    @Test
    public void testDeliveryMultipleMsgsTopicClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopicClassicApi_Tcp");
        assertTrue("testDeliveryMultipleMsgsTopicClassicApi_Tcp failed", testResult);
    }

    @Test
    public void testTransactedSendTopicClassicApiSecOn_B() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopicClassicApi_B");
        assertTrue("testTransactedSendTopicClassicApi_B failed", testResult);
    }

    @Test
    public void testTransactedSendTopicClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopicClassicApi_Tcp");
        assertTrue("testTransactedSendTopicClassicApi_Tcp failed", testResult);
    }

    @Test
    public void testGetDeliveryDelayClassicApiSecOn_B() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApi");
        assertTrue("testGetDeliveryDelayClassicApi failed", testResult);
    }

    @Test
    public void testGetDeliveryDelayClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApi_Tcp");
        assertTrue("testGetDeliveryDelayClassicApi_Tcp failed", testResult);
    }
}
