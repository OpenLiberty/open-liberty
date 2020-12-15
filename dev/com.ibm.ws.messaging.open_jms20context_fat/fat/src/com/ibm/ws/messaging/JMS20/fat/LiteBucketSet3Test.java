/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
public class LiteBucketSet3Test {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSTemporaryQueue";
    private static final String[] appPackages = new String[] { "jmstemporaryqueue.web" };
    private static final String contextRoot = "JMSTemporaryQueue";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test); // throws IOException
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

        engineServer.startServer("LiteBucketSet3_Engine.log");
        clientServer.startServer("LiteBucketSet3_Client.log");
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

    // -----118065 ----------
    // Test JMSContext transactional capabilities (commit, rollback, acknowledge, recover)

    @Test
    public void testCommitLocalTransaction_B() throws Exception {
        boolean testPassed = runInServlet("testCommitLocalTransaction_B");
        assertTrue("testCommitLocalTransaction_B failed ", testPassed);
    }

    @Test
    public void testCommitLocalTransaction_TCP() throws Exception {
        boolean testPassed = runInServlet("testCommitLocalTransaction_TCP");
        assertTrue("testCommitLocalTransaction_TCP failed ", testPassed);
    }

    @Test
    public void testRollbackLocalTransaction_B() throws Exception {
        boolean testPassed = runInServlet("testRollbackLocalTransaction_B");
        assertTrue("testRollbackLocalTransaction_B failed ", testPassed);
    }

    @Test
    public void testRollbackLocalTransaction_TCP() throws Exception {
        boolean testPassed = runInServlet("testRollbackLocalTransaction_TCP");
        assertTrue("testRollbackLocalTransaction_TCP failed ", testPassed);
    }

    // ----- 118066
    // Test JMSContext start and stop

    @Test
    public void testStartJMSContextSecOffBinding() throws Exception {
        boolean testPassed = runInServlet("testStartJMSContextSecOffBinding");
        assertTrue("testStartJMSContextSecOffBinding failed ", testPassed);
    }

    @Test
    public void testStartJMSContextSecOffTCP() throws Exception {
        boolean testPassed = runInServlet("testStartJMSContextSecOffTCP");
        assertTrue("testStartJMSContextSecOffTCP failed ", testPassed);
    }

    @Test
    public void testStopJMSContextSecOffBinding() throws Exception {
        boolean testPassed = runInServlet("testStopJMSContextSecOffBinding");
        assertTrue("testStopJMSContextSecOffBinding failed ", testPassed);
    }

    @Test
    public void testStopJMSContextSecOffTCPIP() throws Exception {
        boolean testPassed = runInServlet("testStopJMSContextSecOffTCP");
        assertTrue("testStopJMSContextSecOffTCPIP failed ", testPassed);
    }

    // ---- 118068
    // Test creation of temporary queues and topics.

    @Test
    public void testCreateTemporaryQueueSecOffBinding() throws Exception {
        boolean testPassed = runInServlet("testCreateTemporaryQueueSecOffBinding");
        assertTrue("testCreateTemporaryQueueSecOffBinding failed ", testPassed);
    }

    @Test
    public void testCreateTemporaryQueueSecOffTCPIP() throws Exception {
        boolean testPassed = runInServlet("testCreateTemporaryQueueSecOffTCPIP");
        assertTrue("testCreateTemporaryQueueSecOffTCPIP failed ", testPassed);

    }

    @Test
    public void testTemporaryQueueLifetimeSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testTemporaryQueueLifetimeSecOff_B");
        assertTrue("testTemporaryQueueLifetimeSecOff_B failed ", testPassed);
    }

    @Test
    public void testTemporaryQueueLifetimeSecOff_TCPIP() throws Exception {
        boolean testPassed = runInServlet("testTemporaryQueueLifetimeSecOff_TCPIP");
        assertTrue("testTemporaryQueueLifetimeSecOff_TCPIP failed ", testPassed);
    }

    @Test
    public void testDeleteTemporaryQueueNameSecOffBinding() throws Exception {
        boolean testPassed = runInServlet("testDeleteTemporaryQueueNameSecOffBinding");
        assertTrue("testDeleteTemporaryQueueNameSecOffBinding failed ", testPassed);
    }

    @Test
    public void testPTPTemporaryQueue_Binding() throws Exception {
        boolean testPassed = runInServlet("testPTPTemporaryQueue_Binding");
        assertTrue("testPTPTemporaryQueue_Binding failed ", testPassed);
    }

    @Test
    public void testPTPTemporaryQueue_TCP() throws Exception {
        boolean testPassed = runInServlet("testPTPTemporaryQueue_TCP");
        assertTrue("testPTPTemporaryQueue_TCP failed ", testPassed);
    }

    @Test
    public void testCreateTemporaryTopicSecOffBinding() throws Exception {
        boolean testPassed = runInServlet("testCreateTemporaryTopicSecOffBinding");
        assertTrue("testCreateTemporaryTopicSecOffBinding failed ", testPassed);
    }

    @Test
    public void testCreateTemporaryTopicSecOffTCPIP() throws Exception {
        boolean testPassed = runInServlet("testCreateTemporaryTopicSecOffTCPIP");
        assertTrue("testCreateTemporaryTopicSecOffTCPIP failed ", testPassed);
    }

    // --------------118077 ---------------------------
    // JMSConsumer tests

    @Test
    public void testReceiveMessageTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveMessageTopicSecOff_B");
        assertTrue("testReceiveMessageTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveMessageTopicSecOff_TCP() throws Exception {
        boolean testPassed = runInServlet("testReceiveMessageTopicSecOff_TCP");
        assertTrue("testReceiveMessageTopicSecOff_TCP failed ", testPassed);
    }

    @Test
    public void testReceiveTimeoutMessageTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveTimeoutMessageTopicSecOff_B");
        assertTrue("testReceiveTimeoutMessageTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveTimeoutMessageTopicSecOff_TCP() throws Exception {
        boolean testPassed = runInServlet("testReceiveTimeoutMessageTopicSecOff_TCP");
        assertTrue("testReceiveTimeoutMessageTopicSecOff_TCP failed ", testPassed);
    }

    @Test
    public void testReceiveNoWaitMessageTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveNoWaitMessageTopicSecOff_B");
        assertTrue("testReceiveNoWaitMessageTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveNoWaitMessageTopicSecOff_TCP() throws Exception {
        boolean testPassed = runInServlet("testReceiveNoWaitMessageTopicSecOff_TCP");
        assertTrue("testReceiveNoWaitMessageTopicSecOff_TCP failed ", testPassed);
    }

    @Test
    public void testReceiveBodyTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyTopicSecOff_B");
        assertTrue("testReceiveBodyTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveBodyTopicSecOff_TCPIP() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTopicSecOff_TCPIP failed ", testPassed);
    }

    @Test
    public void testReceiveBodyTimeOutTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyTimeOutTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveBodyTimeOutTopicSecOff_TCPIP() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyTimeOutTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTopicSecOff_TCPIP failed ", testPassed);
    }

    @Test
    public void testReceiveBodyNoWaitTopicSecOff_B() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyNoWaitTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitTopicSecOff_B failed ", testPassed);
    }

    @Test
    public void testReceiveBodyNoWaitTopicSecOff_TCPIP() throws Exception {
        boolean testPassed = runInServlet("testReceiveBodyNoWaitTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTopicSecOff_TCPIP failed ", testPassed);
    }
}
