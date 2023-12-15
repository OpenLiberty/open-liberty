/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import com.ibm.websphere.simplicity.config.SameSite;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * A set of tests to verify the SameSite attribute on a Cookie
 *
 */
@RunWith(FATRunner.class)
public class WCSameSiteCookieAttributeTests {

    private static final Logger LOG = Logger.getLogger(WCSameSiteCookieAttributeTests.class.getName());
    private static final String APP_NAME_SAMESITE = "SameSiteTest";

    @Server("servlet40_sameSite")
    public static LibertyServer sameSiteServer;

    @BeforeClass
    public static void before() throws Exception {
        // Create the SameSiteTest.war application
        ShrinkHelper.defaultDropinApp(sameSiteServer, APP_NAME_SAMESITE + ".war", "samesite.servlets", "samesite.filters");

        // Start the server and use the class name so we can find logs easily.
        sameSiteServer.startServer(WCSameSiteCookieAttributeTests.class.getSimpleName() + ".log");
        sameSiteServer.waitForStringInLogUsingMark("CWWKO0219I*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (sameSiteServer != null && sameSiteServer.isStarted()) {
            // Allow the Warning message we have generated on purpose:
            //
            // When an invalid <httpSession cookieSameSite="InvalidValue"/> is used the following Warning is found
            // in the logs: WWKG0032W: Unexpected value specified for property [cookieSameSite], value = [InvalideValue].
            // Expected value(s) are: [Lax][Strict][None][Disabled]. Default value in use: [Disabled].
            //
            // W CWWKT0035W: A duplicate cookie name or pattern [cookieOne] was found in the SameSite [lax] configuration.
            // The [cookieOne] cookie name or pattern is ignored for all configuration lists. Any cookie name or pattern
            // that is defined by the lax, none, and strict configurations can be defined in only one of the three configurations.

            // W CWWKT0036W: An unsupported use of the wildcard character was attempted by the value [co*kieOne]. The SameSite
            // configuration is not set for this value.

            // W CWWKT0037W: A cookie name or pattern [cookieOne], which is marked as a duplicate, was found in the SameSite [strict]
            // configuration. The [cookieOne] cookie name or pattern is ignored. Any cookie name or pattern that is defined by the lax,
            // none, and strict configurations can be defined in only one of the three configurations.
            sameSiteServer.stopServer("CWWKG0032W", "CWWKT0035W", "CWWKT0036W", "CWWKT0037W");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSetCookie() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE + "/TestSetCookie";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Incorrect");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * It is expected that the SameSite attribute is written with an incorrect value.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteSetCookie_IncorrectSameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestSetCookie?testIncorrectSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Incorrect")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Lax; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * It is expected that the SameSite attribute is written with duplicate values.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteSetCookie_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Lax; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestSetCookie?testDuplicateSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax") || isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * It is expected that the SameSite attribute is kept with an empty value.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteSetCookie_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE + "/TestSetCookie?testEmptySameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="cookieOne"/> -> within the <httpEndpoint/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_Lax() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");


        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Strict:
     * <samesite strict="cookieOne"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_Strict() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * cookieOne.setSecure(true);
     * <samesite none="cookeOne"/>
     *
     * Configure the server to mark this Cookie as SameSite=None.
     *
     * The server should take no additional action since the Secure attribute is set already
     *
     * Verify that the Set-Cookie header is correct.
     *
     * TestMode.FULL because we are making sure a specific trace is not found so the search will timeout.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_None_Secure() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne with Secure!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setTraceMarkToEndOfDefaultTrace();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie_secure";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertNull("The Channel Framework incorrectly added the Secure attribute.",
                           sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=None:
     * <samesite none="cookieOne"/>
     *
     * Ensure that the server also adds the Secure attribute.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_None_NotSecure() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Wildcard() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="cookie*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Wildcard_CookieName() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookie*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server:
     * <samesite lax="*" strict="cookieTwo"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Wildcard_CookieName_Explicit_Strict() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");
        httpEndpoint.getSameSite().setStrict("cookieTwo");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="cookieOne,cookieOne"/>
     *
     * Ensure this isn't an error condition.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_DuplicateCookieName() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne, cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="*,*"/>
     *
     * Ensure this isn't an error condition.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_DuplicateCookieName_Wildcard() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*,*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="cookie* strict="cookieT*"/>
     *
     * Ensure this isn't an error condition.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_CookieName_Wildcards() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFound = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookie*");
        httpEndpoint.getSameSite().setStrict("cookieT*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="cookieOne" strict="cookieOne"/>
     *
     * W CWWKT0035W: should be found in the logs
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Strict_SameCookieName() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");
        httpEndpoint.getSameSite().setStrict("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="cookie*" strict="cookie*"/>
     *
     * W CWWKT0035W: should be found in the logs
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Strict_SameCookieName_Wildcard() throws Exception {
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookie*");
        httpEndpoint.getSameSite().setStrict("cookie*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwp = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server:
     * <samesite lax="*" strict="*" none="cookieOne"/>
     *
     * The wildcards that are specified should be dropped and Warning output. The
     * explicit cookie defined should be marked with the correct SameSite value. Ensure
     * that cookieTwo has no SameSite attribute.
     *
     * Ensure the following Warning is found in the logs and the cookieOne is marked with SameSite=None:
     *
     * W CWWKT0035W: should be found in the logs
     *
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Strict_Wildcards_None_Explicit() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFound = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");
        httpEndpoint.getSameSite().setStrict("*");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="cookieOne" strict="cookieOne" none="cookieOne"/>
     *
     * W CWWKT0037W: should be found in the logs
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Strict_None_SameCookieName() throws Exception {
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");
        httpEndpoint.getSameSite().setStrict("cookieOne");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertNotNull("The CWWKT0037W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0037W.*"));
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server:
     * <samesite lax="co*kieOne"/>
     *
     * W CWWKT0036W: should be found in the logs
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Lax_Unsupported_CookieName() throws Exception {
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("co*kieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertNotNull("The CWWKT0036W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0036W.*"));
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Strict:
     * <samesite strict="*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Strict_Wildcard() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Strict:
     * <samesite strict="cookie*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Strict_Wildcard_CookieName() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("cookie*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=None:
     * <samesite none="*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_None_NotSecure_Wildcard() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=None:
     * <samesite none="cookie*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_None_Wildcard_CookieName() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("cookie*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * cookieOne.setSecure(true);
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     * cookieTwo.setSecure(true);
     *
     * Configure the server to mark this Cookie as SameSite=None:
     * <samesite none="*"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * TestMode.FULL because we are making sure a specific trace is not found so the search will timeout.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_None_Secure_Wildcard() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        String expectedHeaderTwo = "Set-Cookie: cookieTwo=cookieTwo; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with Secure!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setTraceMarkToEndOfDefaultTrace();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies_secure";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertNull("The Channel Framework incorrectly added the Secure attribute.",
                           sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="cookieOne"/>
     *
     * Verify that there are two Set-Cookie headers with the same values.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Same_Name_Twice() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeaderTwo = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with the same name!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_same_cookie_twice";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    // Headers are the same, ensure both are found.
                    if (headerValue.equals(expectedHeaderOne) && !headerFoundOne) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="MySameSiteCookieNameSetHeader, MySameSiteCookieNameAddHeader"/>
     *
     * We also need to ensure that the response contains that header at the client with the correct SameSite attribute
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_Set_Add_Header_Lax() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; SameSite=Lax";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("MySameSiteCookieNameSetHeader, MySameSiteCookieNameAddHeader");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestSetCookie?testSameSiteConfigSetAddHeader=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="JSESSIONID"/>
     *
     * Ensure that the Set-Cookie header contains the correct SameSite Attribute
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SessionCookie_Explicit() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("JSESSIONID");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Lax")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="*"/>
     *
     * Ensure that the Set-Cookie header contains the correct SameSite Attribute
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SessionCookie_Wildcard() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Lax")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="JSESSIONID"/>
     *
     * Configure the httpSession to mark the Session Cookie SameSite=Strict
     * <httpSession cookieSameSite="Strict"/>
     *
     * Ensure that the Set-Cookie header contains the SameSite value specified by the httpSession configuration
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SessionCookie_Explicit_SessionConfig_Strict() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("JSESSIONID");
        configuration.getHttpSession().setCookieSameSite("Strict");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Strict")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="*"/>
     *
     * Configure the httpSession to mark the Session Cookie SameSite=Strict
     * <httpSession cookieSameSite="Strict"/>
     *
     * Ensure that the Set-Cookie header contains the SameSite value specified by the httpSession configuration
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SessionCookie_Wildcard_SessionConfig_Strict() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");
        configuration.getHttpSession().setCookieSameSite("Strict");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Strict")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieone", "cookieone");
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark cookieOne as SameSite=Lax:
     * <samesite lax="cookieOne"/>
     *
     * Verify that only cookieOne has a SameSite attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_SameName_DifferentCase() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieone=cookieone";
        String expectedHeaderTwo = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with the same name but different case!";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies_different_case";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (headerValue.equals(expectedHeaderTwo)) {
                        headerFoundTwo = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax:
     * <samesite lax="coo*" strict="cookie*" none="*"/>
     *
     * Verify that the SameSite value is set to Strict as that is the most specific wildcard match.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookie_Multiple_Wildcards() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFoundOne = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("coo*");
        httpEndpoint.getSameSite().setStrict("cookie*");
        httpEndpoint.getSameSite().setNone("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeaderOne)) {
                        headerFoundOne = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Strict:
     * <samesite strict="cookieOne" id="samesiteReferenceTest"/>
     * <httpEndpoint samesiteRef="samesiteReferenceTest"/>
     *
     * This test ensures that we can reference a <samesite/> configuration outside of the
     * <httpEndpoint/>.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_Use_Reference() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne!";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.setSameSiteRef("samesiteReferenceTest");
        SameSite sameSite = new SameSite();
        sameSite.setId("samesiteReferenceTest");
        sameSite.setStrict("cookieOne");
        configuration.addSameSite(sameSite);

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedHeader)) {
                        headerFound = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Lax; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
     *
     * Configure the server with:
     * <samesite strict="*"/>
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * The last SameSite value is used. This should be the same behavior as not having <samesite strict="*"/> enabled.
     * Adding a Cookie will always cause the channel to parse the headers into Cookies. The samesite configuration
     * being enabled should not change that in this instance since the test is adding a Cookie.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookieSetHeader_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testDuplicateSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Lax; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
     *
     * Configure the server with:
     * <samesite strict="*"/>
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * Without a samesite configuration it is expected that the SameSite attribute is written with duplicate values.
     *
     * The last SameSite value is used. This is different than if a samesite configuration was not used
     * because with a samesite configuration the channel will parse the headers into cookies and the same
     * behavior should be seen here as when a cookie is added to the response.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SameSiteSetCookie_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestSetCookie?testDuplicateSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers. The Filter
     * also adds a Cookie via the Servlet API.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
     *
     * Configure the server with:
     * <samesite strict="*"/>
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * If no <samesite> configuration tt is expected that the SameSite attribute is dropped.
     * In this test the <samesite strict="*"/> is used so after it is dropped by the channel
     * it is then added as it matches the wildcard. Adding a Cookie will always case the channel to parse
     * the headers into Cookies which is why the Samesite attribute without a value is dropped.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_AddCookieSetHeader_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Strict";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testEmptySameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
     *
     * Configure the server with:
     * <samesite strict="*"/>
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * Without a samesite configuration it is expected that the SameSite attribute is kept with an incorrect value.
     *
     * It is expected that the SameSite attribute is dropped. This is different than if a samesite configuration was not used
     * because with a samesite configuration the channel will parse the headers into cookies and the same
     * behavior should be seen here as when a cookie is added to the response.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SameSiteSetCookie_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Strict";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestSetCookie?testEmptySameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Incorrect");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect");
     *
     * Configure the server with:
     * <samesite lax="*"/>
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * The invalid value should still be in the header. The header values are not validated and the SameSite
     * attribute has a value and is not stand alone.
     *
     * The Cookie that is added should have SameSite=Lax.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SameSiteAddCookieSetCookie_IncorrectSameSite_Lax() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect";
        String expectedCookie = "Set-Cookie: AddTestCookieAfterSetHeader=AddTestCookieAfterSetHeader; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean foundCookieHeader = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testIncorrectSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (headerValue.equals(expectedCookie)) {
                        foundCookieHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Incorrect") ||
                               isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedCookie, foundCookieHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute.
     * Ensure that the Set-Cookie header does not contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_Lax() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("Lax");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Lax") && !header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Strict
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute
     * Ensure that the Set-Cookie header does not contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_Strict() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("Strict");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Strict") && !header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute or the Secure attribute was found.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=None
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute
     *
     * Ensure that the SameSite attribute is set to None and the Secure attribute is also
     * set when explicitly defined by the <httpSession/> configuration.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_None_Secure() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("None");
        configuration.getHttpSession().setCookieSecure(true);
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=None") && header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None and Secure attributes.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=None
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute
     *
     * Ensure that the Secure attribute is automatically added when the
     * <httpSession cookieSameSite="None"/> and the secure attribute cookieSecure isn't
     * set in the httpSession configuration.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_None_Not_Secure() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("None");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=None") && header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None and Secure attributes.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server via <httpSession cookieSamesite="Disabled"/>
     *
     * Ensure that the Set-Cookie header contains no SameSite attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_Disabed() throws Exception {
        boolean headerFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("Disabled");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        // No need to wait for CWWKT0016I:.*SameSiteTest.* as this test doesn't cause the app to be restarted.
        // sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");//sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));


        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite")) {
                            headerFound = true;
                        }
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertFalse("The JSESSIONID Set-Cookie header contained the SameSite attribute and it should not have.", headerFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            // No need to wait for CWWKT0016I:.*SameSiteTest.* as this test doesn't cause the app to be restarted.
            // sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");//sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Not specifying the cookieSameSite in the httpSession config should be the same
     * as setting cookieSameSite="Disabled".
     *
     * Ensure that the Set-Cookie header contains no SameSite attribute.
     * Ensure that the Set-Cookie header does not contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_NothingSpecified() throws Exception {
        boolean headerFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite") || header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertFalse("The JSESSIONID Set-Cookie header contained the SameSite or Secure attributes and it should not have.", headerFound);
            }
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server via <httpSession cookieSamesite="InvalidValue"/>
     *
     * Ensure that the Set-Cookie header contains no SameSite attribute.
     *
     * Ensure the logs contain an error message for the InvalidValue:
     * CWWKG0032W: Unexpected value specified for property [cookieSameSite], value = [InvalidValue]. Expected value(s) are: [Lax][Strict][None][Disabled]. Default value in use:
     * [Disabled].
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_InvalidValue() throws Exception {
        boolean headerFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("InvalidValue");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        // No need to wait for CWWKT0016I:.*SameSiteTest.* as this test doesn't cause the app to be restarted.
        // sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");//sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite")) {
                            headerFound = true;
                        }
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertFalse("The JSESSIONID Set-Cookie header contained a SameSite attribute and it should not have.", headerFound);
                assertTrue("The following Warning was not found in the logs: CWWKG0032W: Unexpected value specified for property [cookieSameSite]",
                           sameSiteServer.waitForStringInLogUsingMark("CWWKG0032W.*cookieSameSite.*InvalidValue") != null);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            // No need to wait for CWWKT0016I:.*SameSiteTest.* as this test doesn't cause the app to be restarted.
            // sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");//sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE));
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax with the following configuration:
     *
     * <httpSession cookieSamesite="LaX"/>
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute.
     *
     * The configuration is not case sensitive but we should see the proper Lax SameSite attribute.
     * Ensure that the Set-Cookie header does not contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_MixedCaseValue() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("LaX");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Lax") && !header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute or the Secure attribute was found.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     *
     * Configure the Server via <httpSession cookieName="uniqueSessionIdCookieName" cookieSameSite="Lax"/>
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute.
     * Ensure that the Set-Cookie header does not contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_UniqueName() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("Lax");
        configuration.getHttpSession().setCookieName("uniqueSessionIdCookieName");
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);

                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("uniqueSessionIdCookieName=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Lax") && !header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The uniqueSessionIdCookieName Set-Cookie header did not contain the SameSite=None attribute or the Secure attribute was found.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Strict and Secure.
     *
     * Ensure that the Set-Cookie header contains the correct SameSite attribute
     * Ensure that the Set-Cookie header does contain the Secure attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_Strict_Secure() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getHttpSession().setCookieSameSite("Strict");
        configuration.getHttpSession().setCookieSecure(true);
        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);
        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSessionCreationServlet";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie header was found by the client.
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                        LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                        if (header.getValue().contains("SameSite=Strict") && header.getValue().contains("Secure")) {
                            headerFound = true;
                        }
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Strict")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                           responseText.equals(expectedResponse));
                assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict and Secure attributes.", headerFound);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME_SAMESITE), false, "CWWKT0016I:.*SameSiteTest.*");//sameSiteServer.waitForStringInLogUsingMark("CWWKF0008I*");
        }
    }

    /**
     * Drive a request to a JSP that does the following:
     *
     * response.setHeader("Set-Cookie" , "jspSetHeaderCookie=jspSetHeaderCookie; Secure; SameSite=None");
     * response.addHeader("Set-Cookie" , "jspAddHeaderCookie=jspAddHeaderCookie; Secure; SameSite=None");
     *
     * Verify that the Set-Cookie headers are correct.
     *
     * This test is necessary as there is a Session created during the processing of the JSP. This causes a Cookie to
     * be added via the Servlet API for the JSESSIONID so we see behavior similar to the addCookieSetHeader tests.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSetCookieJSP() throws Exception {
        String expectedSetHeader = "Set-Cookie: jspSetHeaderCookie=jspSetHeaderCookie; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: jspAddHeaderCookie=jspAddHeaderCookie; Secure; SameSite=None";
        String expectedResponse = "SameSite Set-Cookie JSP Test!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/SameSiteSetCookie.jsp";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers. The Filter also
     * add a Cookie via the Servlet API.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * @throws Exception
     */
    @Test
    public void testAddCookieSetHeader() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedAddedCookieHeader = "Set-Cookie: AddTestCookie=AddTestCookie";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean foundAddedCookieHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testSameSiteAddCookieFirst=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (headerValue.equals(expectedAddedCookieHeader)) {
                        foundAddedCookieHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertTrue("The response did not contain the expected Set-Cookie header for the Cookie added: " + expectedAddedCookieHeader, foundAddedCookieHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers. The Filter also
     * add a Cookie via the Servlet API. In this test the Cookie is added after a call to
     * set/add Header vs before.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * @throws Exception
     */
    @Test
    public void testSetHeaderAddCookie() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedAddedCookieHeader = "Set-Cookie: AddTestCookieAfterSetHeader=AddTestCookieAfterSetHeader";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean foundAddedCookieHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testSameSiteAddCookieFirst=false";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (headerValue.equals(expectedAddedCookieHeader)) {
                        foundAddedCookieHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertTrue("The response did not contain the expected Set-Cookie header for the Cookie added: " + expectedAddedCookieHeader, foundAddedCookieHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers. The Filter
     * also adds a Cookie via the Servlet API.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Incorrect");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * It is expected that the SameSite attribute is written with an incorrect value.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testAddCookieSetHeader_IncorrectSameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testIncorrectSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Incorrect")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite=Lax; SameSite=None");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * The last SameSite value is used.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testAddCookieSetHeader_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testDuplicateSameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                        splitSameSiteHeaderFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
                assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
            }
        }
    }

    /**
     * Drive a request to a Servlet that prints all of the Set-Cookie headers in the
     * HttpServletResponse. A filter should be invoked before the Servlet that adds
     * a Set-Cookie header, we should verify that it contains those headers. The Filter
     * also adds a Cookie via the Servlet API.
     *
     * The Filter performs two actions:
     * ((HttpServletResponse) response).setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHead; Secure; SameSite");
     * ((HttpServletResponse) response).addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
     *
     * We also need to ensure that the response contains that header at the client.
     *
     * It is expected that the SameSite attribute is dropped.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testAddCookieSetHeader_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;

        String url = "http://" + sameSiteServer.getHostname() + ":" + sameSiteServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE
                     + "/TestAddCookieSetCookieHeader?testEmptySameSiteValue=true";
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
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.equals(expectedSetHeader)) {
                        foundSetHeader = true;
                    } else if (headerValue.equals(expectedAddHeader)) {
                        foundAddHeader = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            }
        }
    }

    /*
     * In some instances the server incorrect considered the SameSite=SomeValue to be a
     * new Set-Cookie header and added it as such. If this is seen we should fail the test.
     *
     * A Set-Cookie header of this sort would look like the following: Set-Cookie: SameSite=Incorrect
     *
     */
    private boolean isSplitSameSiteSetCookieHeader(String headerValue, String testedSameSiteValue) {
        boolean isSplitSameSiteSetCookieHeader = false;

        LOG.info("isSplitSameSiteSetCookieHeader -> testedSameSiteValue: " + testedSameSiteValue);
        LOG.info("isSplitSameSiteSetCookieHeader -> headerValue before processing: " + headerValue);

        headerValue = headerValue.replace("Set-Cookie:", "").trim();

        LOG.info("isSplitSameSiteSetCookieHeader -> headerValue after processing: " + headerValue);

        if (headerValue.equals(testedSameSiteValue)) {
            isSplitSameSiteSetCookieHeader = true;
        }

        return isSplitSameSiteSetCookieHeader;
    }
}
