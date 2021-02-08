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
public class JMSContextTest_118077_TQ {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine_118077");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient_118077");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSTemporaryQueue";
    private static final String[] appPackages = new String[] { "jmstemporaryqueue.web" };
    private static final String contextRoot = "JMSTemporaryQueue";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
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

        engineServer.startServer("JMSContextTest_118077_Engine.log");
        clientServer.startServer("JMSContextTest_118077_Client.log");
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
    public void testReceiveMessageTopicTranxSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveMessageTopicTranxSecOff_B");
        assertTrue("testReceiveMessageTopicTranxSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveMessageTopicTranxSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveMessageTopicTranxSecOff_TCP");
        assertTrue("testReceiveMessageTopicTranxSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyTransactionTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTransactionTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTextMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTextMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyObjectMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyObjectMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyMapMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyMapMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyByteMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyByteMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutTransactionTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutTextMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutObjectMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMapMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutByteMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitTransactionTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitTextMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitObjectMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMapMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitByteMessageTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP failed ", testResult);
    }

    // ----mfe

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyMFEUnsupportedTypeTopicSecOff_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP failed ", testResult);
    }

    @Test
    public void testReceiveNoWaitNoMessageTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveNoWaitNullMessageTopicSecOff_B");
        assertTrue("testReceiveNoWaitNoMessageTopicSecOff_B failed ", testResult);
    }

    @Test
    public void testReceiveNoWaitNoMessageTopicSecOff_TCPIP() throws Exception {
        boolean testResult = runInServlet("testReceiveNoWaitNullMessageTopicSecOff_TCP");
        assertTrue("testReceiveNoWaitNoMessageTopicSecOff_TCPIP failed ", testResult);
    }

    @Test
    public void testReceiveBodyMFENoBodyTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyMFENoBodyTopicSecOff_B failed ", testResult);

    }

    @Test
    public void testReceiveBodyMFENoBodyTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFENoBodyTopicSecOff_TCP failed ", testResult);
    }

    @Test
    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFENoBodyTopicSecOff_B failed ", testResult);
    }

    @Test
    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP failed ", testResult);
    }

    @Test
    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_B() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFENoBodyTopicSecOff_B failed ", testResult);
    }

    @Test
    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP failed ", testResult);
    }
}
