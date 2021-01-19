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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118068_TQ {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine_118068");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient_118068");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSTemporaryQueue";
    private static final String[] appPackages = new String[] { "jmstemporaryqueue.web" };
    private static final String contextRoot = "JMSTemporaryQueue";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextEngine_TQ.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextClient_TQ.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("JMSContextTest_118068_Engine.log");
        clientServer.startServer("JMSContextTest_118068_Client.log");
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

    @Mode(TestMode.FULL)
    @Test
    public void testgetTemporaryQueueNameSecOffBinding() throws Exception {
        boolean testResult = runInServlet("testgetTemporaryQueueNameSecOffBinding");
        assertTrue("testgetTemporaryQueueNameSecOffBinding failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testgetTemporaryQueueNameSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testgetTemporaryQueueNameSecOffTCPIP");
        assertTrue("testgetTemporaryQueueNameSecOffTCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryQueueNameSecOffBinding() throws Exception {
        boolean testResult = runInServlet("testToStringTemporaryQueueNameSecOffBinding");
        assertTrue("testToStringTemporaryQueueNameSecOffBinding failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryQueueNameSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testToStringTemporaryQueueNameSecOffTCPIP");
        assertTrue("testToStringTemporaryQueueNameSecOffTCPIP failed ", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException")
    @Mode(TestMode.FULL)
    @Test
    public void testDeleteTemporaryQueueNameSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testDeleteTemporaryQueueNameSecOffTCPIP");
        assertTrue("testDeleteTemporaryQueueNameSecOffTCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryQueueNameSecOFF_B() throws Exception {
        boolean testResult = runInServlet("testDeleteExceptionTemporaryQueueNameSecOFF_B");
        assertTrue("testDeleteExceptionTemporaryQueueNameSecOFF_B failed ", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException")
    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP() throws Exception {
        boolean testResult = runInServlet("testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP");
        assertTrue("testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetTemporaryTopicSecOffBinding() throws Exception {
        boolean testResult = runInServlet("testGetTemporaryTopicSecOffBinding");
        assertTrue("testGetTemporaryTopicSecOffBinding failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetTemporaryTopicSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testGetTemporaryTopicSecOffTCPIP");
        assertTrue("testGetTemporaryTopicSecOffTCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringTemporaryTopicSecOffBinding() throws Exception {
        boolean testResult = runInServlet("testToStringTemporaryTopicSecOffBinding");
        assertTrue("testToStringTemporaryTopicSecOffBinding failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testToStringeTemporaryTopicSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testToStringeTemporaryTopicSecOffTCPIP");
        assertTrue("testToStringeTemporaryTopicSecOffTCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testDeleteExceptionTemporaryTopicSecOff_B");
        assertTrue("testDeleteExceptionTemporaryTopicSecOff_B failed ", testResult);
    }

    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SIDestinationLockedException")
    @Mode(TestMode.FULL)
    @Test
    public void testDeleteExceptionTemporaryTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testDeleteExceptionTemporaryTopicSecOff_TCPIP");
        assertTrue("testDeleteExceptionTemporaryTopicSecOff_B failed ", testResult);
    }

    @Test
    public void testTemporaryTopicLifetimeSecOff_B() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicLifetimeSecOff_B");
        assertTrue("testTemporaryTopicLifetimeSecOff_B failed ", testResult);
    }

    @Test
    public void testTemporaryTopicLifetimeSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicLifetimeSecOff_TCPIP");
        assertTrue("testTemporaryTopicLifetimeSecOff_TCPIP failed ", testResult);
    }

    @Test
    public void testDeleteTemporaryTopicSecOffBinding() throws Exception {
        boolean testResult = runInServlet("testDeleteTemporaryTopicSecOffBinding");
        assertTrue("testDeleteTemporaryTopicSecOffBinding failed ", testResult);
    }

    @Test
    public void testDeleteTemporaryTopicSecOffTCPIP() throws Exception {
        boolean testResult = runInServlet("testDeleteTemporaryTopicSecOffTCPIP");
        assertTrue("testDeleteTemporaryTopicSecOffTCPIP failed ", testResult);
    }

    @Test
    public void testTemporaryTopicPubSubSecOff_B() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicPubSubSecOff_B");
        assertTrue("testTemporaryTopicPubSubSecOff_B failed ", testResult);
    }

    @Test
    public void testTemporaryTopicPubSubSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicPubSubSecOff_TCPIP");
        assertTrue("testTemporaryTopicPubSubSecOff_TCPIP failed ", testResult);
    }
}
