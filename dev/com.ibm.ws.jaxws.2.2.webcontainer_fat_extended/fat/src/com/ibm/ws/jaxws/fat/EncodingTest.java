/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * The purpose of this test is to check the encoding we set in the Response's HTTP Headers
 */
@RunWith(FATRunner.class)
public class EncodingTest {

    private static final String APP_NAME = "encodingApp";
    private static String SERVLET_URL;
    private final static int REQUEST_TIMEOUT = 10;

    @Server("EncodingTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.jaxws.test.wsr.server",
                                      "com.ibm.ws.jaxws.test.wsr.test.servlet",
                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        server.startServer();

        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));
        SERVLET_URL = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/").append(APP_NAME).append("/EncodingTestServlet?target=").toString();//?method=
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @After
    public void restartServer() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
            server.waitForStringInLog("CWWKE0036I:.*" + APP_NAME); // Wait for server to stop
            server.startServer();
        }
    }

    /*
     * Tests if response encoding is default(UTF-8) encoding when another encoding set explicitly
     * Default is UTF-8
     */
    @Test
    public void defaultEncodedResponseReturnTest() throws Exception {
        String response = runTest("defaultEncodedResponseReturnTest");
        assertTrue(response, "Pass".equals(response));
    }

    /*
     * Tests if request encoding matches response encoding
     */
    @Test
    public void setEncodedResponseReturnTest() throws Exception {
        String response = runTest("setEncodedResponseReturnTest");
        assertTrue(response, "Pass".equals(response));
    }

    /*
     * Connect to test servlet passing which test method to run and get the result
     * Assertions happens on servlet side
     */
    private String runTest(String testMethod) throws ProtocolException, IOException {
        URL url = new URL(SERVLET_URL + testMethod);
        Log.info(this.getClass(), "runTest", "Calling Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;
    }

}
