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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for CHIPS (Partitioned Cookies) via samesite partitioned metatype
 * 
 * Remaining Work: 
 * - Test Precedence for Paritioned (Security and Session Cookies)
 * - Test Paritioned via JSP
 */
@RunWith(FATRunner.class)
public class WCPartitionedAttributeTests {

    private static final Logger LOG = Logger.getLogger(WCSameSiteCookieAttributeTests.class.getName());
    private static final String APP_NAME = "PartitionedTest";

    @Server("servlet40_partitioned")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        // Create the PartitionedTest.war application
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "partitioned.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCSameSiteCookieAttributeTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that Partitioned is added to all SameSite=None Cookies via addCookie, setHeader, and addHeader
     * Default configuration Tested:
     *    <samesite none="*" partitioned="true"/>
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
    }

    /**
     * Ensure Paritioned is not set twice if already set programmatically within the servlet and specifed via server.xml config. 
     * Configuration Tested:
     *    <samesite none="*" partitioned="true"/>
     */
    @Test
    public void testDuplicateParitionDoesNotOccur() throws Exception {
        String expectedSetHeader = "Set-Cookie: PartitionedCookieName_SetHeader=PartitionedCookieValue_SetHeader; SameSite=None; Partitioned";
        String expectedAddHeader = "Set-Cookie: PartitionedCookieName_AddHeader=PartitionedCookieValue_AddHeader; SameSite=None; Partitioned";

        String expectedResponse = "Welcome to the TestDuplicatePartitionedCookieServlet!";
        boolean setHeaderFound = false;
        boolean addHeaderFound = false;

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("\n" + configuration);

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestDuplicatePartitionedCookie";
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
                    if(headerValue.contains("Set-Cookie:")){
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
    }

    /**
     * Checks the default behavior (paritioned is false - therefore not added). 
     * Configuration Tested:
     *    <samesite none="*" />
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
                    if(headerValue.contains("Set-Cookie:")){
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
     * Verify Paritioned is not set on Lax Cookies. 
     * Configuration Tested:
     *    <samesite lax="*" partitioned="true" />
     */
    @Test
    public void testParitionedNotSetOnLax() throws Exception {
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
                    if(headerValue.contains("Set-Cookie:")){
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
     * Verify Paritioned is not set on Strict Cookies. 
     * Configuration Tested:
     *    <samesite strict="*" partitioned="true" />
     */
    @Test
    public void testParitionedNotSetOnStrict() throws Exception {
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
                    if(headerValue.contains("Set-Cookie:")){
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
     * Paritioned should be a recongized cookie attribute by the server
     * See more information here: https://github.com/OpenLiberty/open-liberty/issues/22324#issuecomment-1961939339
     */
    @Test
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
                    if(headerValue.contains("Set-Cookie:")){
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
     *    <samesite strict="PartitionedCookieName_AddHeader" lax="PartitionedCookieName_SetHeader" none="PartitionedCookieName_AddCookie" partitioned="true" /> 
     */
    @Test
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
                     
                    if(headerValue.contains("Set-Cookie:")){
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
}
