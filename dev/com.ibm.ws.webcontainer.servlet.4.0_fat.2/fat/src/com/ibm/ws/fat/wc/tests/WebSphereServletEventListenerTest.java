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
import static org.junit.Assert.fail;

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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html?path=25.0.0.4/com.ibm.websphere.appserver.api.servlet_1.1-javadoc/com/ibm/websphere/servlet/event/package-summary.html
 *
 * These APIs were from WAS 4.0+ (?) time frame. Application should use the servlet standard APIs instead!
 *
 * Since these APIs are still around, this test is added to cover the test gap in this area.
 *
 * Data are appended to a static StringBuffer OUTBUFFER for all resources, duplicate entries are expected in the response for each test case.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WebSphereServletEventListenerTest {

    private static final Logger LOG = Logger.getLogger(WebSphereServletEventListenerTest.class.getName());
    private static final String APP_NAME = "WebSphereServletEvent";

    //Verify keywords (WEBSPHERE Servlet API and STANDARD Servlet API are context attributes,
    //                                                  "EventFilter",          is the filter name
    //                                                  "EventServlet"          is the servlet name )
    //                                                  "EventErrorFilter"
    private final String[] expectedKeys = { "WebSpshereApplicationListener.onApplicationStart",
                                            "WebSphereFilterListener.onFilterStartInit",
                                            "WebSphereFilterListenerImplExt.onFilterStartInit",
                                            "WebSphereFilterListener.onFilterFinishInit",
                                            "WebSphereFilterInvocationListener.onFilterStartDoFilter",
                                            "WebSphereServletListener.onServletStartInit",
                                            "WebSphereServletListener.onServletFinishInit",
                                            "WebSphereServletListener.onServletAvailableForService",
                                            "WebSphereServletInvocationListener.onServletStartService",
                                            "WEBSPHERE Servlet API",
                                            "STANDARD Servlet API",
                                            "WebSphereServletInvocationListener.onServletFinishService",
                                            "WebSphereFilterInvocationListener.onFilterFinishDoFilter",
                                            "EventFilter",
                                            "EventServlet",
                                            "EventErrorFilter"
    };

    @Server("servlet40_webSphereServletEvent")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet40_webSphereServletEvent");
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "websphere.servlet", "websphere.listener", "websphere.filter");
        server.startServer(WebSphereServletEventListenerTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        /*
         * SRVE0777E - intentional Exception throw by a servlet to trigger ServletErrorListener
         */
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0777E");
        }
    }

    /*
     *
     * [(MAIN) WebSpshereApplicationListener.onApplicationStart
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventFilter]
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventErrorFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventErrorFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventErrorFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]. OutputStream obtained.
     * >>>(service)>>> Context attribute from WebSphere API [WEBSPHERE Servlet API using ApplicationListener.]
     * >>>(service)>>> Context attribute from Standard API [STANDARD Servlet API using ServletContextListener.]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     *
     *
     * Test:
     * - servlet context attribute added from the WebSphere listeners and standard listener
     * - verify all the WebSphere listeners and their APIs are invoked (from the response)
     */
    @Test
    public void test_ServletContext_Attributes() throws Exception {
        LOG.info("====== <test_ServletContext_Attributes> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/ServletEvent";
        LOG.info("Send Request: [" + url + "]");

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                for (String keyword : expectedKeys) {
                    if (!responseText.contains(keyword)) {
                        fail("Response does not contain [" + keyword + "]");
                    }
                }

                LOG.info("===== All key words are found. Test PASS. =====");
            }
        }
    }

    /*
     * Response Text:
     * [(MAIN) WebSpshereApplicationListener.onApplicationStart
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventFilter]
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventErrorFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventErrorFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventErrorFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]. OutputStream obtained.
     * >>>(service)>>> Context attribute from WebSphere API [WEBSPHERE Servlet API using ApplicationListener.]
     * >>>(service)>>> Context attribute from Standard API [STANDARD Servlet API using ServletContextListener.]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletError]. OutputStream obtained.
     * >> 3. WebSphereServletErrorListener.onServletServiceError , error [java.lang.RuntimeException: websphere.servlet.EventErrorServlet throws Runtime NPE]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletError]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     * ]
     *
     * Test:
     * - "3. WebSphereServletErrorListener.onServletServiceError , error " response.
     * - "EventErrorServlet"
     */

    @Test
    public void test_ServletErrorListener_onServletServiceError() throws Exception {
        LOG.info("====== <test_ServletErrorListener_onServletServiceError> ======");

        String expectedKey = "WebSphereServletErrorListener.onServletServiceError";
        String expectedKey2 = "EventErrorServlet";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/ServletError";
        LOG.info("Send Request: [" + url + "]");

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Expected string [" + expectedKey + "] not found in the response", responseText.contains(expectedKey));
                assertTrue("Expected string [" + expectedKey2 + "] not found in the response", responseText.contains(expectedKey2));
            }

        }
    }

    /*
     * Filter throws ServletException to trigger FilterErrorListener event. RuntimeException does not trigger a filter event!
     *
     * Response Text:
     * [(MAIN) WebSpshereApplicationListener.onApplicationStart
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventFilter]
     * >> 1. WebSphereFilterListener.onFilterStartInit for filter [EventErrorFilter]
     * >> 5. WebSphereFilterListenerImplExt.onFilterStartInit for filter [EventErrorFilter]
     * << 1. WebSphereFilterListener.onFilterFinishInit for filter [EventErrorFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]. OutputStream obtained.
     * >>>(service)>>> Context attribute from WebSphere API [WEBSPHERE Servlet API using ApplicationListener.]
     * >>>(service)>>> Context attribute from Standard API [STANDARD Servlet API using ServletContextListener.]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventErrorServlet]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletError]. OutputStream obtained.
     * >> 3. WebSphereServletErrorListener.onServletServiceError , error [java.lang.RuntimeException: websphere.servlet.EventErrorServlet throws Runtime NPE]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletError]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventFilter]
     * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for filter [EventErrorFilter]
     * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/FilterErrorEvent]. OutputStream obtained.
     * >>>(service)>>> Context attribute from WebSphere API [WEBSPHERE Servlet API using ApplicationListener.]
     * >>>(service)>>> Context attribute from Standard API [STANDARD Servlet API using ServletContextListener.]
     * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/FilterErrorEvent]
     * >> 4. WebSphereFilterErrorListener.onFilterDoFilterError , filter error [javax.servlet.ServletException: websphere.filter.EventErrorFilterthrows ServletException ]
     * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [EventFilter]
     *
     * Test:
     * "WebSphereFilterErrorListener.onFilterDoFilterError , filter error"
     *
     */

    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void test_FilterErrorListener_onFilterDoFilterError() throws Exception {
        LOG.info("====== <test_FilterErrorListener_onFilterDoFilterError> ======");

        String expectedKey = "WebSphereFilterErrorListener.onFilterDoFilterError";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/FilterErrorEvent";
        LOG.info("Send Request: [" + url + "]");

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Expected string [" + expectedKey + "] not found in the response", responseText.contains(expectedKey));
            }

        }
    }
}
