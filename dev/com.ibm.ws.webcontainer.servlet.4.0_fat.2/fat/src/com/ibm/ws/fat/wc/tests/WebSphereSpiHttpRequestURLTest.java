/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the com.ibm.wsspi.http.HttpRequest.getURL returns the host name format (numerical or textual) accordingly
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WebSphereSpiHttpRequestURLTest {

    private static final Logger LOG = Logger.getLogger(WebSphereSpiHttpRequestURLTest.class.getName());
    private static final String APP_NAME = "WebSphereSpiHttpRequestURL";
    private static final String SERVER_NAME = "servlet40_webSphereSpiHttpRequestURL";

    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : " + SERVER_NAME);
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "spi.servlet");
        server.installSystemFeature("webcontainerlibertyinternals");
        server.startServer(WebSphereSpiHttpRequestURLTest.class.getSimpleName() + ".log");

        String tcpChannelMessage = server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint");
        runningNetty = tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);

        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Send a request using numerical IP address hostname.
     * Check for the response for these IP address.
     */
    @Test
    public void test_SPI_HttpRequest_getURL_Numerical_Hostname() throws Exception {
        LOG.info("====== <test_SPI_HttpRequest_getURL_Numerical_Hostname> ======");

        String hostnameString = server.getHostname();
        String numericalHostAddress = InetAddress.getByName(hostnameString).getHostAddress();
        String url = "http://" + numericalHostAddress + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/testSPIGetURL";

        LOG.info("Numerial Host Address [" + numericalHostAddress + "], Textual Host Address [" + hostnameString + "]");
        LOG.info("Send Request: [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");
                assertTrue("Expecting numerial host name [" + numericalHostAddress + "] not found in the response \n[" + responseText + "]",
                           responseText.contains(numericalHostAddress));
            }
        }
    }

    /*
     * Send a request using textual hostname.
     */
    @Test
    public void test_SPI_HttpRequest_getURL_String_Hostname() throws Exception {
        LOG.info("====== <test_SPI_HttpRequest_getURL_String_Hostname> ======");

        String hostnameString = server.getHostname();
        String url = "http://" + hostnameString + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/testSPIGetURL";

        LOG.info("Textual Host Address [" + hostnameString + "]");
        LOG.info("Send Request: [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");
                assertTrue("Expecting textual host name [" + hostnameString + "] not found in the response \n[" + responseText + "]",
                           responseText.contains(hostnameString));
            }
        }
    }
}
