/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

import static org.junit.Assert.assertTrue;

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
 * Test an empty URL pattern mapping servlet which is mapped exactly to the context-root
 * for any request to the context-root, with or without the ending slash
 *
 * Expecting:
 * request.contextPath = "/context-root"
 * request.servletPath = "" //i.e empty
 * request.pathInfo = "/"
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61EmptyURLPatternMappingTest {

    private static final Logger LOG = Logger.getLogger(Servlet61EmptyURLPatternMappingTest.class.getName());
    private static final String TEST_APP_NAME = "EmptyURLPatternMappingTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet61_EmptyUrlPatternMappingTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_EmptyUrlPatternMappingTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "emptymapping.servlets");
        server.startServer(Servlet61EmptyURLPatternMappingTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Send request to context root WITHOUT the ending slash. Server will append the ending slash and redirect
     * Servlet will verify of the getContextpath, getServletPath and getPathInfo and report the result in the response.
     */
    @Test
    public void test_EmptyPatternMappingNoEndingSlash() throws Exception {
        LOG.info("====== <test_EmptyPatternMappingNoEndingSlash> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME;
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Found a FAIL test result", !responseText.contains("FAIL"));
            }
        }
    }

    /**
     * Send request to context root WITH the ending slash.
     * Servlet will verify the getContextpath, getServletPath and getPathInfo and report the result in the response.
     */
    @Test
    public void test_EmptyPatternMappingWithEndingSlash() throws Exception {
        LOG.info("====== <test_EmptyPatternMappingWithEndingSlash> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/";
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Found a FAIL test result", !responseText.contains("FAIL"));
            }
        }
    }
}
