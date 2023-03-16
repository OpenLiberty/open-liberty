/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class FfdcCleanupTest {
    private static final String SERVER_NAME_XML = "com.ibm.ws.logging.ffdcCleanup";
    private static final int CONN_TIMEOUT = 10;
    private static LibertyServer server;

    @BeforeClass
    public static void initialSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server.saveServerConfiguration();
        ShrinkHelper.defaultDropinApp(server, "ffdc-servlet", "com.ibm.ws.logging.fat.ffdc.servlet");

    }

    @Before
    public void setUp() throws Exception {
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("com.ibm.ws.logging.fat.ffdc.servlet", "ArithmeticException", "SRVE0777E");
        }
    }

    @Test
    @ExpectedFFDC("java.lang.ArithmeticException")
    public void testFfdcCleanupDeletion() throws Exception {
        hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");

        ServerConfiguration serverConfig = server.getServerConfiguration();
        Logging loggingObj = serverConfig.getLogging();
        Thread.sleep(61000);
        loggingObj.setMaxFfdcAge("0m");
        server.updateServerConfiguration(serverConfig);

        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("TRAS3014I", 15000, messagesLogFile);
        assertTrue("The FFDC file was not deleted.", line.contains("TRAS3014I"));
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.ArithmeticException")
    public void testFfdcCleanupNoDeletion() {
        try {
            hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");

            ServerConfiguration serverConfig = server.getServerConfiguration();
            Logging loggingObj = serverConfig.getLogging();
            Thread.sleep(61000);
            loggingObj.setMaxFfdcAge("1d");
            server.updateServerConfiguration(serverConfig);

            RemoteFile messagesLogFile = server.getDefaultLogFile();
            String line = server.waitForStringInLog("TRAS3014I", 15000, messagesLogFile);
            assertNull("The FFDC file was incorrectly deleted.", line);
        } catch (Exception e) {

        }
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.ArithmeticException")
    public void testFfdcCleanupNotConfigured() throws Exception {
        hitWebPage("ffdc-servlet", "FFDCServlet", true, "?generateFFDC=true");
        Thread.sleep(61000);
        RemoteFile messagesLogFile = server.getDefaultLogFile();
        String line = server.waitForStringInLog("TRAS3014I", 15000, messagesLogFile);
        assertNull("The FFDC file was incorrectly deleted.", line);
    }

    private static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed,
                                   String params) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            urlStr = params != null ? urlStr + params : urlStr;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }

}