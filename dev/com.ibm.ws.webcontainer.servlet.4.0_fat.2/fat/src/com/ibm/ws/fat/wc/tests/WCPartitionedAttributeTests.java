/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.HttpSession;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for CHIPS (Partitioned Cookies) via samesite partitioned metatype
 *
 * Remaining Work:
 * - Test Precedence for partitioned (Security and Session Cookies)
 * - Test partitioned via JSP
 */
@RunWith(FATRunner.class)
public class WCPartitionedAttributeTests {

    private static final Logger LOG = Logger.getLogger(WCPartitionedAttributeTests.class.getName());
    private static final String APP_NAME = "PartitionedTest";

    @Server("servlet40_partitioned")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        // Create the PartitionedTest.war application
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "partitioned.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCPartitionedAttributeTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server

        // CWWKG0032W: Unexpected value specified for property [cookiePartitioned], value = [InvalidValue].

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0032W");
        }
    }

    /**
     * Test that Partitioned is added to all SameSite=None Cookies via addCookie, setHeader, and addHeader
     * Default configuration Tested:
     * <samesite none="*" partitioned="true"/>
     */
    @Test
    public void testPartitionedCookieBehavior() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; Secure; SameSite=None; Partitioned";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; Secure; SameSite=None; Partitioned";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; Secure; SameSite=None; Partitioned";

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try {
            try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
                try (final CloseableHttpResponse response = client.execute(getMethod)) {
                    String responseText = EntityUtils.toString(response.getEntity());
                    LOG.info("\n" + "Response Text:");
                    LOG.info("\n" + responseText);

                    Header[] headers = response.getHeaders("Set-Cookie");
                    LOG.info("\n" + "Set-Cookie headers contained in the response:");

                    // Verify that the expected Set-Cookie headers were found by the client.
                    int cookieCount = 0;
                    String headerValue;
                    for (Header header : headers) {
                        headerValue = header.toString();
                        LOG.info("\n" + headerValue);
                        if (headerValue.contains("Set-Cookie:")) {
                            cookieCount++;
                        }
                        if (headerValue.equals(expectedSetHeader)) {
                            setHeaderFound = true;
                        } else if (headerValue.equals(expectedAddHeader)) {
                            addHeaderFound = true;
                        } else if (headerValue.equals(expectedAddCookie)) {
                            addCookieFound = true;
                        }
                    }

                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                    assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                    assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                    assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
                    assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 3);
                }
            }

        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Ensure partitioned is not set twice if already set programmatically within the servlet and specifed via server.xml config.
     * Configuration Tested:
     * <samesite none="*" partitioned="true"/>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDuplicatepartitionDoesNotOccur() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; SameSite=None; Partitioned";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; SameSite=None; Partitioned";

        String expectedResponse = "Welcome to the TestDuplicatePartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestDuplicatePartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try {
            try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
                try (final CloseableHttpResponse response = client.execute(getMethod)) {
                    String responseText = EntityUtils.toString(response.getEntity());
                    LOG.info("\n" + "Response Text:");
                    LOG.info("\n" + responseText);

                    Header[] headers = response.getHeaders("Set-Cookie");
                    LOG.info("\n" + "Set-Cookie headers contained in the response:");

                    // Verify that the expected Set-Cookie headers were found by the client.
                    int cookieCount = 0;
                    String headerValue;
                    for (Header header : headers) {
                        headerValue = header.toString();
                        LOG.info("\n" + headerValue);
                        if (headerValue.contains("Set-Cookie:")) {
                            cookieCount++;
                        }
                        // must use equals
                        if (headerValue.equals(expectedSetHeader)) {
                            setHeaderFound = true;
                        } else if (headerValue.equals(expectedAddHeader)) {
                            addHeaderFound = true;
                        }
                    }

                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                    assertTrue("The response did not contain th e expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                    assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                    assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 2);
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Checks the default behavior (partitioned is false - therefore not added).
     * Configuration Tested:
     * <samesite none="*" />
     */
    @Test
    public void testPartitionIsFalseByDefault() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; Secure; SameSite=None";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; Secure; SameSite=None";

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict(null);
        httpEndpoint.getSameSite().setPartitioned(null);
        httpEndpoint.getSameSite().setNone("*");
        httpEndpoint.getSameSite().setLax(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    // must use equals
                    if (headerValue.equals(expectedSetHeader)) {
                        setHeaderFound = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        addHeaderFound = true;
                    } else if (headerValue.equals(expectedAddCookie)) {
                        addCookieFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
                assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 3);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }

    }

    /**
     * Verify partitioned is not set on Lax Cookies.
     * Configuration Tested:
     * <samesite lax="*" partitioned="true" />
     */
    @Test
    public void testpartitionedNotSetOnLax() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; SameSite=Lax";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; SameSite=Lax";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; SameSite=Lax";

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    // must use equals
                    if (headerValue.equals(expectedSetHeader)) {
                        setHeaderFound = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        addHeaderFound = true;
                    } else if (headerValue.equals(expectedAddCookie)) {
                        addCookieFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
                assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 3);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify partitioned is not set on Strict Cookies.
     * Configuration Tested:
     * <samesite strict="*" partitioned="true" />
     */
    @Test
    public void testpartitionedNotSetOnStrict() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; SameSite=Strict";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; SameSite=Strict";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; SameSite=Strict";

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone(null);
        httpEndpoint.getSameSite().setLax(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    // must use equals
                    if (headerValue.equals(expectedSetHeader)) {
                        setHeaderFound = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        addHeaderFound = true;
                    } else if (headerValue.equals(expectedAddCookie)) {
                        addCookieFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
                assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 3);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Ensure cookies are not split when Partitioned is specified via setHeader/addHeader and addCookie is invoked.
     * partitioned should be a recognized cookie attribute by the server
     * See more information here: https://github.com/OpenLiberty/open-liberty/issues/22324#issuecomment-1961939339
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCookiesAreNotSplitWithPartitioned() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; Secure; SameSite=None; Partitioned";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; Secure; SameSite=None; Partitioned";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; Secure; SameSite=None";
        int expectedNumberOfCookies = 3;

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict(null);
        httpEndpoint.getSameSite().setPartitioned(null);
        httpEndpoint.getSameSite().setNone("*");
        httpEndpoint.getSameSite().setLax(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSplitCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    // must use equals
                    if (headerValue.equals(expectedSetHeader)) {
                        setHeaderFound = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        addHeaderFound = true;
                    } else if (headerValue.equals(expectedAddCookie)) {
                        addCookieFound = true;
                    }
                }

                assertEquals("The response did not contain the expected number of cookie headers", expectedNumberOfCookies, cookieCount);
                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Previous test used the wildcard within the samesite config, but this test references them by name.
     * Configuration Tested:
     * <samesite strict="PartitionedCookieName_AddHeader" lax="PartitionedCookieName_SetHeader" none="PartitionedCookieName_AddCookie" partitioned="true" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPartitionCookieByName() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; SameSite=Lax";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; SameSite=Strict";
        String expectedAddCookie = "Set-Cookie: PartitionedCookieName_AddCookie=PartitionedCookieValue_AddCookie; Secure; SameSite=None; Partitioned";

        String expectedResponse = "Welcome to the TestPartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;
        boolean addCookieFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("PartitionedCookieName_AddHeader");
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone("PartitionedCookieName_AddCookie");
        httpEndpoint.getSameSite().setLax("PartitionedCookieName_SetHeader");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    // must use equals
                    if (headerValue.equals(expectedSetHeader)) {
                        setHeaderFound = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        addHeaderFound = true;
                    } else if (headerValue.equals(expectedAddCookie)) {
                        addCookieFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, setHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, addHeaderFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddCookie, addCookieFound);
                assertTrue("The response did not contain the expected number of cookie headers" + expectedAddHeader, cookieCount == 3);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }

    }

    /**
     * Verify partitioned is Not Set by Default
     * No Configurations enabled in the server.xml
     */
    @Test
    public void testPartitionSessionCookieDefault() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setPartitioned(null);
        httpEndpoint.getSameSite().setNone(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                        // Assert SameSite is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite="));
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify partitioned is set when CookiePartitioned=true
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" cookieSameSite="none"/>
     */
    @Test
    public void testPartitionSessionCookiePartitioned() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        Boolean restoreConfig = false;
        HttpSession httpSession = configuration.getHttpSession();

        httpSession.setCookieSameSite("none");
        httpSession.setCookiePartitioned("true");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None; Partitioned"));
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify partitioned is not set on the SameSite=Lax Cookie
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" cookieSameSite="*"/>
     */
    @Test
    public void testpartitionedNotSetonLaxSessionCookie() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        Boolean restoreConfig = false;
        HttpSession httpSession = configuration.getHttpSession();

        httpSession.setCookieSameSite("lax");
        httpSession.setCookiePartitioned("true");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert Lax IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Lax"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify partitioned is not set on SameSite=Strict
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" cookieSameSite="strict"/>
     */
    @Test
    public void testpartitionedNotSetonStrictSessionCookie() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        Boolean restoreConfig = false;
        HttpSession httpSession = configuration.getHttpSession();

        httpSession.setCookieSameSite("strict");
        httpSession.setCookiePartitioned("true");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert Lax IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Strict"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify HttpSession's Config overrides the HttpChannel Config.
     * All cookies except the JSESSIONID should be Partitioned.
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="false" cookieSameSite="none"/>
     * <samesite none="*" partitioned="true"/>
     */
    @Test
    public void testHttpSessionOverridesHttpChannelConfigCase1() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();

        httpSession.setCookieSameSite("none");
        httpSession.setCookiePartitioned("false");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify HttpSession's Config overrides the HttpChannel Config.
     * All cookies except the Session Cookie is Partitioned.
     * This differs from the previous test because "cookieSameSite" is not set.
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="false" />
     * <samesite none="*" partitioned="true" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHttpSessionOverridesHttpChannelConfigCase2() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("false");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify HttpSession's Config overrides the HttpChannel Config.
     * Only the HTTP Session cookie should be Partitioned
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" />
     * <samesite none="*" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHttpSessionOverridesHttpChannelConfigCase3() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("true");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setNone("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Only the session cookie is partitioned.
     *
     * Configuration Tested:
     * <httpSession cookieSameSite="None" />
     * <samesite partitioned="true" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHttpSessionOverridesHttpChannelConfigCase4() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookieSameSite("none");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setPartitioned(true);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Only the session cookie is partitioned. Same as below, but we explictly set defer.
     *
     * Configuration Tested:
     * <httpSession cookieSameSite="None" cookiePartitioned="defer" />
     * <samesite partitioned="true" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHttpSessionOverridesHttpChannelConfigCase5() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("defer");
        httpSession.setCookieSameSite("none");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setPartitioned(true);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * The session cookie should NOT be partitioned.
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" />
     */
    @Test
    public void testHttpSessionPartitionedOnly() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("true");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite="));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify HttpSession's Config overrides the HttpChannel Config.
     * HttpSession Cookie should not be Partitioned
     * All cookies should have SameSite=Strict
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" />
     * <samesite strict="*" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPartitionedHierarchy_NotAddedtoStrictSessionCookie() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("true");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setStrict("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Strict"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Strict"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Verify HttpSession's Config overrides the HttpChannel Config.
     * HttpSession Cookie should not be Partitioned
     * All cookies should have SameSite=Lax
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="true" />
     * <samesite lax="*" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPartitionedHierarchy_NotAddedtoLaxSessionCookie() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("true");

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");

        httpEndpoint.getSameSite().setLax("*");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Lax"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=Lax"));
                        // Assert Partitioned is NOT contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

    /**
     * Only the session cookie is partitioned. Same as below, but we explictly set defer.
     *
     * Configuration Tested:
     * <httpSession cookiePartitioned="InvalidValue" />
     */
    @Test
    @Mode(TestMode.FULL)
    public void testHttpSessionInvalidValue() throws Exception {
        String expectedResponse = "Welcome to the TestPartitionedSessionServlet!";

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpSession httpSession = configuration.getHttpSession();
        httpSession.setCookiePartitioned("InvalidValue");
        httpSession.setCookieSameSite("none");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestPartitionedSession";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (headerValue.contains("Set-Cookie: JSESSIONID=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", headerValue.contains("SameSite=None"));
                        // Assert Partitioned is contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else if (headerValue.contains("Set-Cookie: AddCookieName=")) {
                        // Assert SameSite=None IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("SameSite=None"));
                        // Assert Partitioned IS contained in the Cookie
                        assertTrue("The response did not contain the expected cookies header for the session", !headerValue.contains("Partitioned"));
                    } else {
                        fail("Unknown cookie encountered: " + headerValue);
                    }
                }
                assertTrue("The following Warning was not found in the logs: CWWKG0032W: Unexpected value specified for property [cookiePartitioned]",
                           server.waitForStringInLogUsingMark("CWWKG0032W.*cookiePartitioned.*InvalidValue") != null);
            }
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedTest.*");
        }
    }

}
