/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.impl.bootstrap.HttpRequester;
import org.apache.hc.core5.http.impl.bootstrap.RequesterBootstrap;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.util.Timeout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
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
import junit.framework.Assert;

/**
 * A set of tests to verify the SameSite attribute on a Cookie
 *
 */
@RunWith(FATRunner.class)
public class WCSameSiteCookieAttributeTests {

    private static final Logger LOG = Logger.getLogger(WCSameSiteCookieAttributeTests.class.getName());
    private static final String APP_NAME = "SameSiteTest";
    private static final String APP_NAME_DUPLICATE = "SameSiteDuplicateParamTest";

    @Rule
    public TestName name = new TestName();

    @Server("servlet40_sameSite")
    public static LibertyServer sameSiteServer;

    @BeforeClass
    public static void before() throws Exception {
        // Create the SameSiteTest.war application
        ShrinkHelper.defaultDropinApp(sameSiteServer, APP_NAME + ".war", "samesite.servlets", "samesite.filters");
        ShrinkHelper.defaultDropinApp(sameSiteServer, APP_NAME_DUPLICATE + ".war", "samesite.servlets");

        // Start the server and use the class name so we can find logs easily.
        sameSiteServer.startServer(WCSameSiteCookieAttributeTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (sameSiteServer != null && sameSiteServer.isStarted()) {
            // Allow the Warning message we have generated on purpose.
            // When an invalid <httpSession cookieSameSite="InvalidValue"/> is used the following Warning is found
            // in the logs: WWKG0032W: Unexpected value specified for property [cookieSameSite], value = [InvalideValue].
            // Expected value(s) are: [Lax][Strict][None][Disabled]. Default value in use: [Disabled].

            // Example of Warnings being tested:
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

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestSetCookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
    public void testSameSiteSetCookie_IncorrectSameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestSetCookie?testIncorrectSameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
    public void testSameSiteSetCookie_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Lax; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestSetCookie?testDuplicateSameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
     * It is expected that the SameSite attribute is kept with an incorrect value.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSetCookie_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite";
        String expectedResponse = "Welcome to the SameSiteSetCookieServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestSetCookie?testEmptySameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
     * <samesite lax="cookieOne"/> -> within the <httpEndpoint/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_Lax() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Strict and verify.
     * <samesite strict="cookieOne"/>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_Strict() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * cookieOne.setSecure(true);
     * <samesite none="cookeOne"/>
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne with Secure";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie_secure";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertNull("The Channel Framework incorrectly added the Secure attribute.",
                       sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne, cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("*,*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");
        httpEndpoint.getSameSite().setStrict("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
            Header[] headers = response.getHeaders("Set-Cookie");
            LOG.info("\n" + "Set-Cookie headers contained in the response:");

            String headerValue;
            for (Header header : headers) {
                headerValue = header.toString();
                LOG.info("\n" + headerValue);
                if (headerValue.equals(expectedHeader)) {
                    headerFound = true;
                } else if (headerValue.contentEquals(expectedHeaderTwo)) {
                    headerFoundTwo = true;
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertNotNull("The CWWKT0035W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0035W.*"));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertNotNull("The CWWKT0037W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0037W.*"));
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne";
        boolean headerFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("co*kieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertNotNull("The CWWKT0036W should have been logged for an invalid configuration but was not.", sameSiteServer.waitForStringInLogUsingMark("CWWKT0036W.*"));
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Strict and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Strict and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
     * Configure the server to mark this Cookie as SameSite=None and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with Secure";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("*");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies_secure";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertNull("The Channel Framework incorrectly added the Secure attribute.",
                       sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with the same name";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_same_cookie_twice";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertNull("The Channel Framework incorrectly added the Secure attribute.",
                       sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestSetCookie?testSameSiteConfigSetAddHeader=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="JSESSIONID"/>
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite lax="*"/>
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
     * Ensure that the SET-COOKIE header contains the SameSite value specified by the httpSession configuration
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
     * Ensure that the SET-COOKIE header contains the SameSite value specified by the httpSession configuration
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     * <samesite none="*"/>
     *
     * Configure the httpSession to mark the Session Cookie SameSite=Strict
     * <httpSession cookieSameSite="None"/>
     *
     * Ensure that the SET-COOKIE header contains the SameSite value specified by the httpSession configuration.
     * Also ensure that the SET-COOKIE header does not contain the Secure attribute. We do this to ensure
     * that the Channel Framework does not touch this header since it already has SameSite defined.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSameSiteConfig_SessionCookie_None_SessionConfig_None() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setNone("*");
        configuration.getHttpSession().setCookieSameSite("None");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
            Header[] headers = response.getHeaders("Set-Cookie");
            LOG.info("\n" + "Set-Cookie headers contained in the response:");

            // Verify that the expected Set-Cookie header was found by the client.
            String headerValue;
            for (Header header : headers) {
                headerValue = header.toString();
                LOG.info("\n" + headerValue);
                if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                    LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                    if (header.getValue().contains("SameSite=None") && !header.getValue().contains("Secure")) {
                        headerFound = true;
                    }
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None attribute, or the Secure attribute was added by the Channel Framework.",
                              headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieone", "cookieone");
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark cookieOne as SameSite=Lax and verify.
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
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies with the same name but different case";
        boolean headerFoundOne = false;
        boolean headerFoundTwo = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setLax("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies_different_case";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertNull("The Channel Framework incorrectly added the Secure attribute.",
                       sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderTwo, headerFoundTwo);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieTwo = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
     * <samesite lax="coo*" strict="cookie*" none="*"/>
     *
     * Verify that the SameSite value is set to Strict as that is the most specific wildcard match.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_AddCookie_Multiple_Wildcards() throws Exception {
        String expectedHeaderOne = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
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
        sameSiteServer.setMarkToEndOfLog(sameSiteServer.getDefaultTraceFile());
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertNull("The Channel Framework incorrectly added the Secure attribute.",
                       sameSiteServer.waitForStringInTraceUsingMark("Setting the Secure attribute for SameSite=None"));
            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeaderOne, headerFoundOne);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Strict and verify.
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
    public void testSameSiteConfig_Use_Reference() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Strict
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=None
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
     *
     * The assumption here is that the Secure attribute is added via the server's configuration
     * as well using the cookieSecure httpSession attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_None() throws Exception {
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None and Secure attributes.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=None
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
     *
     * The assumption here is that the Secure attribute will not be added since it is
     * not added in the httpSession configuration.
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
            Header[] headers = response.getHeaders("Set-Cookie");
            LOG.info("\n" + "Set-Cookie headers contained in the response:");

            // Verify that the expected Set-Cookie header was found by the client.
            String headerValue;
            for (Header header : headers) {
                headerValue = header.toString();
                LOG.info("\n" + headerValue);
                if (header.getName().equals("Set-Cookie") && header.getValue().contains("JSESSIONID=")) {
                    LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                    if (header.getValue().contains("SameSite=None") && !header.getValue().contains("Secure")) {
                        headerFound = true;
                    }
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None and not the Secure attributes.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server via <httpSession cookieSamesite="Disabled"/>
     *
     * Ensure that the SET-COOKIE header contains no SameSite attribute.
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertFalse("The JSESSIONID Set-Cookie header contained the SameSite attribute and it should not have.", headerFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Not specifying the cookieSameSite in the httpSession config should be the same
     * as setting cookieSameSite="Disabled".
     *
     * Ensure that the SET-COOKIE header contains no SameSite attribute.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_NothingSpecified() throws Exception {
        boolean headerFound = false;
        String expectedResponse = "Welcome to the SameSiteSessionCreationServlet!";

        sameSiteServer.saveServerConfiguration();

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertFalse("The JSESSIONID Set-Cookie header contained the SameSite attribute and it should not have.", headerFound);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server via <httpSession cookieSamesite="InvalidValue"/>
     *
     * Ensure that the SET-COOKIE header contains no SameSite attribute.
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertFalse("The JSESSIONID Set-Cookie header contained a SameSite attribute and it should not have..", headerFound);
            Assert.assertTrue("The following Warning was not found in the logs: CWWKG0032W: Unexpected value specified for property [cookieSameSite]",
                              sameSiteServer.waitForStringInLogUsingMark("CWWKG0032W.*cookieSameSite.*InvalidValue") != null);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax with the following configuration:
     *
     * <httpSession cookieSamesite="LaX"/>
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute.
     *
     * The configuration is not case sensitive but we should see the proper Lax SameSite Attribute.
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     *
     * Configure the Server via <httpSession cookieName="uniqueSessionIdCookieName" cookieSameSite="Lax"/>
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
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

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSessionCreationServlet";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
            Header[] headers = response.getHeaders("Set-Cookie");
            LOG.info("\n" + "Set-Cookie headers contained in the response:");

            // Verify that the expected Set-Cookie header was found by the client.
            String headerValue;
            for (Header header : headers) {
                headerValue = header.toString();
                LOG.info("\n" + headerValue);

                if (header.getName().equals("Set-Cookie") && header.getValue().contains("uniqueSessionIdCookieName=")) {
                    LOG.info("\n" + "Set-Cookie header for JSESSIONID found.");
                    if (header.getValue().contains("SameSite=Lax")) {
                        headerFound = true;
                    }
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            Assert.assertTrue("The uniqueSessionIdCookieName Set-Cookie header did not contain the SameSite=None attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
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
        String expectedResponse = "SameSite Set-Cookie Test";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteSetCookie.jsp";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestAddCookieSetCookieHeader?testSameSiteAddCookieFirst=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertTrue("The response did not contain the expected Set-Cookie header for the Cookie added: " + expectedAddedCookieHeader, foundAddedCookieHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestAddCookieSetCookieHeader?testSameSiteAddCookieFirst=false";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertTrue("The response did not contain the expected Set-Cookie header for the Cookie added: " + expectedAddedCookieHeader, foundAddedCookieHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
    public void testAddCookieSetHeader_IncorrectSameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestAddCookieSetCookieHeader?testIncorrectSameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
    public void testAddCookieSetHeader_DuplicateSameSiteAttribute() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestAddCookieSetCookieHeader?testDuplicateSameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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
    public void testAddCookieSetHeader_EmptySameSiteValue() throws Exception {
        String expectedSetHeader = "Set-Cookie: MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure";
        String expectedAddHeader = "Set-Cookie: MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure";
        String expectedResponse = "Welcome to the SameSiteAddCookieSetCookieHeaderServlet!";
        boolean foundAddHeader = false;
        boolean foundSetHeader = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/TestAddCookieSetCookieHeader?testEmptySameSiteValue=true";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSetHeader, foundSetHeader);
            assertTrue("The response did not contain the expected Set-Cookie header: " + expectedAddHeader, foundAddHeader);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookies:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * The following context parameter is configured in the web.xml:
     * <context-param>
     * <param-name>SameSiteCookies_Lax</param-name>
     * <param-value>cookieOne,cookieTwo</param-value>
     * </context-param>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_Lax() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedHeader2 = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding two Cookies";
        boolean headerFound = false;
        boolean headerFound2 = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_two_cookies";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
            Header[] headers = response.getHeaders("Set-Cookie");
            LOG.info("\n" + "Set-Cookie headers contained in the response:");

            // Verify that the expected Set-Cookie header was found by the client.
            String headerValue;
            for (Header header : headers) {
                headerValue = header.toString();
                LOG.info("\n" + headerValue);
                if (headerValue.equals(expectedHeader)) {
                    headerFound = true;
                } else if (headerValue.equals(expectedHeader2)) {
                    headerFound2 = true;
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=Lax")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader2, headerFound2);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieThree", "cookieThree");
     *
     * The following context-param is configured in the web.xml:
     * <context-param>
     * <param-name>SameSiteCookies_Strict</param-name>
     * <param-value>cookieThree</param-value>
     * </context-param>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_Strict() throws Exception {
        String expectedHeader = "Set-Cookie: cookieThree=cookieThree; SameSite=Strict";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding a Cookie with the following name: cookieThree";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieNameToAdd=cookieThree";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieFour", "cookieFour");
     *
     * The following context-param is configured in the web.xml:
     * <context-param>
     * <param-name>SameSiteCookies_None</param-name>
     * <param-value>cookieFour,*</param-value>
     * </context-param>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * Also verify that the Secure attribute is added.
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_None() throws Exception {
        String expectedHeader = "Set-Cookie: cookieFour=cookieFour; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding a Cookie with the following name: cookieFour";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieNameToAdd=cookieFour";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieFive", "cookieFive");
     *
     * The following context-param is configured in the web.xml:
     * <context-param>
     * <param-name>SameSiteCookies_None</param-name>
     * <param-value>cookieFour,*</param-value>
     * </context-param>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * Also verify that the Secure attribute is added.
     *
     * Adding a Cookie with a name not explicitly defined in the context-param should
     * match the wildcard that is specified.
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_None_Wildcard() throws Exception {
        String expectedHeader = "Set-Cookie: cookieFive=cookieFive; Secure; SameSite=None";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding a Cookie with the following name: cookieFive";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieNameToAdd=cookieFive";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieOne = new Cookie("cookieOne", "cookieOne");
     *
     * Configure the server to mark this Cookie as SameSite=Lax and verify.
     * <samesite strict="cookieOne"/> -> within the <httpEndpoint/>
     *
     * The web.xml contains the following context-param:
     * <context-param>
     * <param-name>SameSiteCookies_Lax</param-name>
     * <param-value>cookieOne,cookieTwo</param-value>
     * </context-param>
     *
     * Verify that the Set-Cookie header is correct.
     *
     * Ensure that the context-param configuration takes priority over the
     * <httpEndpoint/> configuration. The SameSite attribute should be set to Lax
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_Priority_HttpEndpoint() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        String expectedResponse = "Welcome to the SameSiteAddCookieServlet! Adding cookieOne";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict("cookieOne");

        sameSiteServer.setMarkToEndOfLog();
        sameSiteServer.updateServerConfiguration(configuration);

        sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        LOG.info("Updated server configuration: " + configuration);

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=add_one_cookie";

        LOG.info("requestUri : " + requestUri);

        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", requestUri);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());
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

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue("The response did not contain the expected Servlet output: " + expectedResponse,
                              responseText.equals(expectedResponse));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * <!-- Defining cookieOne twice, the duplicate entry should just be ignored. -->
     * <context-param>
     * <param-name>SameSiteCookies_Lax</param-name>
     * <param-value>*,cookieOne</param-value>
     * </context-param>
     *
     * <context-param>
     * <param-name>SameSiteCookies_Strict</param-name>
     * <param-value>*,cookieOne</param-value>
     * </context-param>
     *
     * <!-- Defining * twice, the duplicate entry should just be ignored. -->
     * <context-param>
     * <param-name>SameSiteCookies_None</param-name>
     * <param-value>cookieOne</param-value>
     * </context-param>
     *
     * Verify the following Warnings in the logs:
     * [2/21/20, 23:59:55:818 EST] 00000060 com.ibm.ws.webcontainer.webapp W CWWWC0003W: A cookieOne cookie name with the existing SameSite attribute value Lax is configured with
     * the None new value. No SameSite attribute is added for this cookie.
     *
     * [2/21/20, 23:59:55:819 EST] 00000060 com.ibm.ws.webcontainer.webapp W CWWWC0003W: A * cookie name with the existing SameSite attribute value Lax is configured with the
     * Strict new value. No SameSite attribute is added for this cookie.
     *
     * [2/21/20, 23:59:55:819 EST] 00000060 com.ibm.ws.webcontainer.webapp W CWWWC0004W: A cookieOne cookie name that is configured for the Strict SameSite attribute value was
     * previously identified as a duplicate. No SameSite attribute is added for this cookie.
     *
     * @throws Exception
     */
    @Test
    public void testSameSite_ContextParameter_Duplicates() throws Exception {
        // TODO
        // no need for a request
        // we should just use the application element and add the application and remove
        // it dynamically here otherwise we'll get warnings for every test run on the server
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
