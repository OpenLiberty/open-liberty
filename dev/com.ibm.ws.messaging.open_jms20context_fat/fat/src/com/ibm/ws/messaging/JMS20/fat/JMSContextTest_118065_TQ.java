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
public class JMSContextTest_118065_TQ {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("JMSContextEngine_118065");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("JMSContextClient_118065");
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

        engineServer.startServer("JMSContextTest_118065_Engine.log");
        clientServer.startServer("JMSContextTest_118065_Client.log");
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
    public void testCommitNonLocalTransaction_B() throws Exception {
        boolean testResult = runInServlet("testCommitNonLocalTransaction_B");
        assertTrue("testCommitNonLocalTransaction_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCommitNonLocalTransaction_TCP() throws Exception {
        boolean testResult = runInServlet("testCommitNonLocalTransaction_TCP");
        assertTrue("testCommitNonLocalTransaction_TCP failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testRollbackNonLocalTransaction_B() throws Exception {
        boolean testResult = runInServlet("testRollbackNonLocalTransaction_B");
        assertTrue("testRollbackNonLocalTransaction_B failed ", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testRollbackNonLocalTransaction_TCP() throws Exception {
        boolean testResult = runInServlet("testRollbackNonLocalTransaction_TCP");
        assertTrue("testRollbackNonLocalTransaction_TCP failed ", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.LocalTransaction.RolledbackException")
    @Mode(TestMode.FULL)
    @Test
    public void testRecoverNonLocalTransaction_B() throws Exception {
        boolean testResult = runInServlet("testRecoverNonLocalTransaction_B");
        assertTrue("testRecoverNonLocalTransaction_B failed ", testResult);
    }

    @ExpectedFFDC("com.ibm.ws.LocalTransaction.RolledbackException")
    @Mode(TestMode.FULL)
    @Test
    public void testRecoverNonLocalTransaction_TCP() throws Exception {
        boolean testResult = runInServlet("testRecoverNonLocalTransaction_TCP");
        assertTrue("testRecoverNonLocalTransaction_TCP failed ", testResult);
    }
}
