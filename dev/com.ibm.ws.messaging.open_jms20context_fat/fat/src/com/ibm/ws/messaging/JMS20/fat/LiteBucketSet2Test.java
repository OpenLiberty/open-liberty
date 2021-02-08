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
public class LiteBucketSet2Test {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private boolean runInServlet(String test, String contextRoot) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test); // throws IOException
    }

    private static final String appName_118067 = "JMSContext_118067";
    private static final String[] appPackages_118067 = new String[] { "jmscontext_118067.web" };
    private static final String contextRoot_118067 = "JMSContext_118067";

    private static final String appName_118070 = "JMSContext_118070";
    private static final String[] appPackages_118070 = new String[] { "jmscontext_118070.web" };
    private static final String contextRoot_118070 = "JMSContext_118070";

    private static final String appName_118075 = "JMSContext_118075";
    private static final String[] appPackages_118075 = new String[] { "jmscontext_118075.web" };
    private static final String contextRoot_118075 = "JMSContext_118075";

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
        TestUtils.addDropinsWebApp(clientServer, appName_118067, appPackages_118067);
        TestUtils.addDropinsWebApp(clientServer, appName_118070, appPackages_118070);
        TestUtils.addDropinsWebApp(clientServer, appName_118075, appPackages_118075);

        engineServer.startServer("LiteBucketSet2_Engine.log");
        clientServer.startServer("LiteBucketSet2_Client.log");
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

    // 118067_1 : Creates a new JMSProducer object which can be used to configure and send messages
    // 118067_2 : Try send(Destination destination, String body) on JMSProducer object

    @Test
    public void testCreateJmsProducerAndSend_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCreateJmsProducerAndSend_B_SecOff", contextRoot_118067);
        assertTrue("testCreateJmsProducerAndSend_B_SecOff failed ", testPassed);
    }

    // 118067_5 : Creates a new JMSProducer object which can be used to configure and send messages
    // 118067_6 : Try send(Destination destination, String body) on JMSProducer object

    @Test
    public void testCreateJmsProducerAndSend_TCP_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCreateJmsProducerAndSend_TCP_SecOff", contextRoot_118067);
        assertTrue("testCreateJmsProducerAndSend_TCP_SecOff failed ", testPassed);
    }

    // 118067_11 : Test create queue name as QUEUE/queue

    @Test
    public void testQueueNameQUEUE_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testQueueNameQUEUE_B", contextRoot_118067);
        assertTrue("testQueueNameQUEUE_B_SecOff failed ", testPassed);
    }

    @Test
    public void testQueueNameQUEUE_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testQueueNameQUEUE_TcpIp", contextRoot_118067);
        assertTrue("testQueueNameQUEUE_TcpIp_SecOff failed ", testPassed);
    }

    // @Test TODO Not yet working
    public void testTopicNameTOPIC_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testTopicNameTOPIC_B", contextRoot_118067);
        assertTrue("testTopicNameTOPIC_B_SecOff failed ", testPassed);
    }

    // @Test TODO Not yet working
    public void testTopicNameTOPIC_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testTopicNameTOPIC_TcpIp", contextRoot_118067);
        assertTrue("testTopicNameTOPIC_TcpIp_SecOff failed ", testPassed);
    }

    // JMSContextTest_118070
    // If there are no other active (not closed) JMSContext objects using the underlying
    // connection then this method also closes the underlying connection

    @Test
    public void testCloseAll_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCloseAll_B_SecOff", contextRoot_118070);
        assertTrue("testCloseAll_B_SecOff failed ", testPassed);
    }

    @Test
    public void testCloseAll_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCloseAll_B_SecOff", contextRoot_118070);
        assertTrue("testCloseAll_TcpIp_SecOff failed ", testPassed);
    }

    // JMSContextTest_118075

    @Test
    public void testQueueConsumer_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testQueueConsumer_B_SecOff", contextRoot_118075);
        assertTrue("testQueueConsumer_B_SecOff failed", testPassed);
    }

    @Test
    public void testQueueConsumer_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testQueueConsumer_TcpIp_SecOff", contextRoot_118075);
        assertTrue("testQueueConsumer_TcpIp_SecOff failed", testPassed);
    }

    @Test
    public void testTopicConsumer_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testTopicConsumer_B_SecOff", contextRoot_118075);
        assertTrue("testTopicConsumer_B_SecOff failed", testPassed);
    }

    @Test
    public void testTopicConsumer_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testTopicConsumer_TcpIp_SecOff", contextRoot_118075);
        assertTrue("testTopicConsumer_TcpIp_SecOff failed", testPassed);
    }

    @Test
    public void testCreateConsumerWithMsgSelector_B_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCreateConsumerWithMsgSelector_B_SecOff", contextRoot_118075);
        assertTrue("testCreateConsumerWithMsgSelector_B_SecOff failed", testPassed);
    }

    @Test
    public void testCreateConsumerWithMsgSelector_TcpIp_SecOff() throws Exception {
        boolean testPassed = runInServlet("testCreateConsumerWithMsgSelector_TcpIp_SecOff", contextRoot_118075);
        assertTrue("testCreateConsumerWithMsgSelector_TcpIp_SecOff failed", testPassed);
    }
}
