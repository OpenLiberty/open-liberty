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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class DelayLiteSecOnTest {

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

        engineServer.setServerConfigurationFile(
            "DelayEngine_SSL.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");

        clientServer.copyFileToLibertyServerRoot(
            "resources/security", "clientLTPAKeys/mykey.jks");

        clientServer.setServerConfigurationFile(
            "DelayClient_SSL.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("DelayLiteSec_Engine.log");
        clientServer.startServer("DelayLiteSec_Client.log");
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

    @Test
    public void testSetDeliveryDelaySecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelay");
        assertTrue("testSetDeliveryDelay_B failed", testResult);
    }

    @Test
    public void testSetDeliveryDelaySecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelay_Tcp");
        assertTrue("testSetDeliveryDelay_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicSecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopic");
        assertTrue("testSetDeliveryDelayTopic_B failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopic_Tcp");
        assertTrue("testSetDeliveryDelayTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubSecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSub");
        assertTrue("testSetDeliveryDelayTopicDurSub_B failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSub_Tcp");
        assertTrue("testSetDeliveryDelayTopicDurSub_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayClassicSecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayClassicApi");
        assertTrue("testSetDeliveryDelayClassicApi_B failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicClassicApiSecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicClassicApi");
        assertTrue("testSetDeliveryDelayTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubClassicApiSecOn_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi");
        assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_B failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubClassicApiSecOn_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed", testResult);
    }
}
