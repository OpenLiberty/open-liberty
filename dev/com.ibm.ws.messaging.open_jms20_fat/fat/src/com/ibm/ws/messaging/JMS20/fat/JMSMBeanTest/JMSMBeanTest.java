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
package com.ibm.ws.messaging.JMS20.fat.JMSMBeanTest;

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
import org.junit.runner.RunWith;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

// This test cannot be run under jakarta, as jakarta has no replacement
// for j2ee-management.
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
// The FAT runner must be used explicitly to trigger the necessary
// repeat steps, including filtering based on 'SkipForRepeat'.
@RunWith(FATRunner.class)
public class JMSMBeanTest {

    private static LibertyServer server =
        LibertyServerFactory.getLibertyServer("JMSMBeanServer");

    private static final int serverPort = server.getHttpDefaultPort();
    private static final String serverHostName = server.getHostname();

    private static final String mbeanAppName = "JMSMBeans";
    private static final String mbeanContextRoot = "JMSMBeans";
    private static final String[] mbeanPackages = new String[] { "jmsmbeans.web" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(serverHostName, serverPort, mbeanContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        server.copyFileToLibertyInstallRoot(
            "lib/features",
            "features/testjmsinternals-1.0.mf");
        server.setServerConfigurationFile("JMSMBean.xml");
        TestUtils.addDropinsWebApp(server, mbeanAppName, mbeanPackages);
        server.startServer("JMSMBeanTest_Server.log");
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
    public void testJMSProviderMBean() throws Exception {
        boolean testResult = runInServlet("testJMSProviderMBean");
        assertTrue("testJMSProviderMBean failed ", testResult);
    }
}
