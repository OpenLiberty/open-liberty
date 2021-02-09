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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

@Mode(TestMode.FULL)
public class JMSDCFVarTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("JMSDCFServer");

    private static final int serverPort = server.getHttpDefaultPort();
    private static final String serverHostName  = server.getHostname();

    private static final String dcfAppName = "JMSDCFVar";
    private static final String dcfContextRoot = "JMSDCFVar";
    private static final String[] dcfPackages = new String[] { "jmsdcfvar.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(serverHostName, serverPort, dcfContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        server.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");

        server.setServerConfigurationFile("JMSDCFServer.xml");

        TestUtils.addDropinsWebApp(server, dcfAppName, dcfPackages);

        server.startServer();
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            server.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //

    @Test
    public void testP2PWithoutLookupName() throws Exception {
        boolean testResult = runInServlet("testP2PWithoutLookupName");
        assertTrue("testP2PWithoutLookupName failed ", testResult);
    }

    @Test
    public void testP2PWithLookupName() throws Exception {
        boolean testResult = runInServlet("testP2PWithLookupName");
        assertTrue("testP2PWithLookupName failed ", testResult);
    }
}
