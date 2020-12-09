/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import static org.junit.Assert.assertNotNull;
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
public class DelayLiteSecOffTest {
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
        engineServer.setServerConfigurationFile("DelayEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("DelayClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("DelayLiteOff_Engine.log");
        clientServer.startServer("DelayLiteOff_Client.log");
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
    public void testSetDeliveryDelay_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelay");
        assertTrue("testSetDeliveryDelay failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelay_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelay_Tcp");
        assertTrue("testSetDeliveryDelay_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopic_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopic");
        assertTrue("testSetDeliveryDelayTopic failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopic_Tcp");
        assertTrue("testSetDeliveryDelayTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSub_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSub");
        assertTrue("testSetDeliveryDelayTopicDurSub failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSub_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSub_Tcp");
        assertTrue("testSetDeliveryDelayTopicDurSub_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayClassic_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayClassicApi");
        assertTrue("testSetDeliveryDelayClassicApi failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicClassicApi");
        assertTrue("testSetDeliveryDelayTopicClassicApi failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi");
        assertTrue("testSetDeliveryDelayTopicDurSubClassicApi failed", testResult);
    }
    
    @Test
    public void testSetDeliveryDelayTopicDurSubClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi_Tcp");
        assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed", testResult);
    }
    
}
