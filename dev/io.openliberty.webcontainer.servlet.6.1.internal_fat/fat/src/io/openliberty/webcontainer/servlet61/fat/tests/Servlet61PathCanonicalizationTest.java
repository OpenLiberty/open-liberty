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
 * Test path canonicalization path for ServletContext methods that use path parameter
 * Use the request header "runTest" to specify a test to run
 *
 * The rules from Servlet 6.0, 3.5.2 are applied to all path parameter
 *
 * Servlet will verify all the results and Log PASS or FAIL for this client to assert on
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61PathCanonicalizationTest {

    private static final Logger LOG = Logger.getLogger(Servlet61PathCanonicalizationTest.class.getName());
    private static final String TEST_APP_NAME = "PathCanonicalizationTest";

    @Server("servlet61_PathCanonicalizationTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_PathCanonicalizationTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "pathcanonicalization");
        server.startServer(Servlet61PathCanonicalizationTest.class.getSimpleName() + ".log");
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

    @Test
    public void test_Path_Context_getRealPath() throws Exception {
        LOG.info("====== <test_Path_Context_getRealPath> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testGetRealPath");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }

    @Test
    public void test_Path_Context_getRequestDispatcher() throws Exception {
        LOG.info("====== <test_Path_Context_getRequestDispatcher> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testGetRequestDispatcher");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }

    @Test
    public void test_Path_Context_getResource() throws Exception {
        LOG.info("====== <test_Path_Context_getResource> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testGetResource");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }

    @Test
    public void test_Path_Context_getResourceAsStream() throws Exception {
        LOG.info("====== <test_Path_Context_getResourceAsStream> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testGetResourceAsStream");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }

    @Test
    public void test_Path_Context_getResourcePaths() throws Exception {
        LOG.info("====== <test_Path_Context_getResourcePaths> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testGetResourcePaths");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }

    @Test
    public void test_Path_Request_getRequestDispatcher() throws Exception {
        LOG.info("====== <test_Path_Request_getRequestDispatcher> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestPathCanonicalization";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testRequestGetRequestDispatcher");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result has FAIL test", !responseText.contains("FAIL"));
            }
        }
    }
}
