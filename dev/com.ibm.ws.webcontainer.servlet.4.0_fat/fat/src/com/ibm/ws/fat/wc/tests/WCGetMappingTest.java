/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

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
 * These are tests for the Servlet 4.0 HttpServletRequest.getMapping()
 * functionality.
 *
 */
@RunWith(FATRunner.class)
public class WCGetMappingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());
    private static final String APP_NAME = "TestGetMapping";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestGetMapping to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "testgetmapping.war.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCGetMappingTest.class.getSimpleName() + ".log");

        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ContextRootMapping() throws Exception {
        String expectedResponse = "Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Forward() throws Exception {
        String expectedResponse = "Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathFwdMatch?dispatchPath=/";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathIncMatch?dispatchPath=/";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Async() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=/";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_PathMapping() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathMatch/testPath";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Forward() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathFwdMatch?dispatchPath=pathMatch/testPath";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathIncMatch?dispatchPath=pathMatch/testPath";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Async() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=pathMatch/testPath";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_DefaultMapping() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/invalid";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Forward() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathFwdMatch?dispatchPath=invalid";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathIncMatch?dispatchPath=invalid";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Async() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=invalid";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ExactMapping() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/exactMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Forward() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathFwdMatch?dispatchPath=exactMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathIncMatch?dispatchPath=exactMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Async() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=exactMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ExtensionMapping() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXTENSION matchValue: extensionMatch pattern: *.extension servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/extensionMatch.extension";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Forward() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXTENSION matchValue: extensionMatch pattern: *.extension servletName: GetMappingTestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathFwdMatch?dispatchPath=extensionMatch.extension";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathIncMatch?dispatchPath=extensionMatch.extension";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Async() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=extensionMatch.extension";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses named dispatcher has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_NamedDispatcher_Forward() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathNamedDispatcherFwdMatch pattern: /pathNamedDispatcherFwdMatch servletName: GetMappingNamedDispatcherFwdServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathNamedDispatcherFwdMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    /**
     * Test to ensure that a request that uses named dispatcher has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_NamedDispatcher_Include() throws Exception {
        String expectedResponse = "ServletMapping values: mappingMatch: EXACT matchValue: pathNamedDispatcherIncMatch pattern: /pathNamedDispatcherIncMatch servletName: GetMappingNamedDispatcherIncServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/pathNamedDispatcherIncMatch";

        testHttpServletRequestGetMapping(url, expectedResponse);
    }

    private void testHttpServletRequestGetMapping(String url, String expectedResponse) throws Exception {
        LOG.info("url: " + url);
        LOG.info("expectedResponse: " + expectedResponse);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            }
        }
    }

}
