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
@Mode(TestMode.FULL)
public class WCSameSiteCookieAttributeTests {

    private static final Logger LOG = Logger.getLogger(WCSameSiteCookieAttributeTests.class.getName());
    private static final String APP_NAME = "SameSiteTest";

    @Rule
    public TestName name = new TestName();

    @Server("servlet40_sameSite")
    public static LibertyServer sameSiteServer;

    @BeforeClass
    public static void before() throws Exception {
        // Create the SameSiteTest.war application
        ShrinkHelper.defaultDropinApp(sameSiteServer, APP_NAME + ".war", "samesite.servlet", "samesite.filter");

        // Start the server and use the class name so we can find logs easily.
        sameSiteServer.startServer(WCSameSiteCookieAttributeTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (sameSiteServer != null && sameSiteServer.isStarted()) {
            // Allow the Warning message we have generated on purpose.
            // When an invalid <httpSession cookieSameSite="InvalidValue"/> is used the following Warning is found
            // in the logs: WWKG0032W: Unexpected value specified for property [cookieSameSite], value = [InvalideValue]. Expected value(s) are: [Lax][Strict][None][Disabled]. Default value in use: [Disabled].
            sameSiteServer.stopServer("CWWKG0032W");
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
     * TODO: Should we validate?
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
     * TODO: Currently we just let anything pass through same for any attribute unless we have a
     * cookie cache we don't do any validation.
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
                    // to do fix
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
     * TODO: Verify that this behavior is correct or if SameSite should be removed. The
     * behavior here differs slightly from the testAddCookieSetHeader_EmptySameSiteValue as the header
     * is just passed straight through so a SameSite attribute will be added without a value. The other
     * mentioned test case there will be a Cookie cache so all the headers are parsed into HttpCookies
     * in this case the SameSite attribute will be stripped since it is not valid without a value.
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
     *
     * Verify that the Set-Cookie header is correct.
     *
     *
     * @throws Exception
     */
    //@Test
    public void testSameSiteAddCookieOneLax() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; SameSite=Lax";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=cookieOne";

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

            assertTrue(responseText.contains("Welcome to the SameSiteAddCookieServlet!"));
            assertTrue(responseText.contains("Adding cookieOne"));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieTwo = new Cookie("cookieTwo", "cookieTwo");
     *
     * Configure the server to mark this Cookie as SameSite=Strict and verify.
     *
     * Verify that the Set-Cookie header is correct.
     *
     * @throws Exception
     */
    //@Test
    public void testSameSiteAddCookieTwoStrict() throws Exception {
        String expectedHeader = "Set-Cookie: cookieTwo=cookieTwo; SameSite=Strict";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=cookieTwo";

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

            assertTrue(responseText.contains("Welcome to the SameSiteAddCookieServlet!"));
            assertTrue(responseText.contains("Adding cookieTwo"));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieThree = new Cookie("cookieThree", "cookieThree");
     * cookieThree.setSecure(true);
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
     *
     * The server should take no additional action since the Secure attribute is set already
     *
     * Verify that the Set-Cookie header is correct.
     *
     *
     * @throws Exception
     */
    //@Test
    public void testSameSiteAddCookieThreeNoneSecure() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=cookieThree";

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

            assertTrue(responseText.contains("Welcome to the SameSiteAddCookieServlet!"));
            assertTrue(responseText.contains("Adding cookieThree"));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        }
    }

    /**
     * Drive a requests to a Servlet that adds the following cookie:
     *
     * Cookie cookieFour = new Cookie("cookieFour", "cookieFour");
     *
     * Configure the server to mark this Cookie as SameSite=None and verify.
     *
     * Ensure that the server also adds the Secure attribute.
     *
     * Verify that the Set-Cookie header is correct.
     *
     *
     * @throws Exception
     */
    //@Test
    public void testSameSiteAddCookieFourNoneNotSecure() throws Exception {
        String expectedHeader = "Set-Cookie: cookieOne=cookieOne; Secure; SameSite=None";
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(sameSiteServer.getHostname(), sameSiteServer.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/" + APP_NAME + "/SameSiteAddCookieServlet?cookieToAdd=cookieFour";

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

            assertTrue(responseText.contains("Welcome to the SameSiteAddCookieServlet!"));
            assertTrue(responseText.contains("Adding cookieFour"));
            assertTrue("The Response did not contain the following Set-Cookie header: " + expectedHeader, headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Strict attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=None and Secure attributes.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertFalse("The JSESSIONID Set-Cookie header contained the SameSite attribute and it should not have.", headerFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertFalse("The JSESSIONID Set-Cookie header contained a SameSite attribute and it should not have..", headerFound);
            Assert.assertTrue("The following Warning was not found in the logs: CWWKG0032W: Unexpected value specified for property [cookieSameSite]",
                              sameSiteServer.waitForStringInLogUsingMark("CWWKG0032W.*cookieSameSite.*InvalidValue") != null);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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

            assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertTrue("The JSESSIONID Set-Cookie header did not contain the SameSite=Lax attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
            sameSiteServer.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);
        }
    }

    /**
     * Drive a request to Servlet that creates a session.
     *
     * Configure the Server to mark the Session Cookie SameSite=Lax
     *
     * Configure the Servie via <httpSession cookieName="uniqueSessionIdCookieName"/>
     *
     * Ensure that the SET-COOKIE header contains the correct SameSite Attribute
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteSessionCookie_UniqueName() throws Exception {
        boolean headerFound = false;
        boolean splitSameSiteHeaderFound = false;

        sameSiteServer.saveServerConfiguration();

        ServerConfiguration configuration = sameSiteServer.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration.toString());

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
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=None")) {
                    splitSameSiteHeaderFound = true;
                }
            }

            LOG.info("\n" + "Response Text:");
            LOG.info("\n" + responseText);

            Assert.assertTrue(responseText.equals("Welcome to the SameSiteSessionCreationServlet!"));
            Assert.assertTrue("The uniqueSessionIdCookieName Set-Cookie header did not contain the SameSite=None attribute.", headerFound);
            assertFalse("The response contained a split SameSite Set-Cookie header and it should not have.", splitSameSiteHeaderFound);
        } finally {
            sameSiteServer.setMarkToEndOfLog();
            sameSiteServer.restoreServerConfiguration();
            LOG.info("Server configuration after it was restored: " + sameSiteServer.getServerConfiguration().toString());
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
     * TODO: Should we validate?
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
     * It is expected that the SameSite attribute is written with duplicate values.
     *
     * TODO: Currently the last SameSite attribute is used. Do we need any additional changes here
     * as it is a bit different than testSameSiteSetCookie_DuplicateSameSiteAttribute
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
     * TODO: Verify that this behavior is correct or if SameSite should be kept with an empty value. The
     * behavior here differs slightly from the testSameSiteSetCookie_EmptySameSiteValue as there will be a
     * Cookie cache so all the headers are parsed into HttpCookies. In this case the SameSite attribute will
     * be stripped since it is not valid without a value. In the other mentioned test case the SameSite
     * attribute is preserved without a value.
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
        boolean splitSameSiteHeaderFound = false;

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
                } else if (isSplitSameSiteSetCookieHeader(headerValue, "SameSite=\"\"")) {
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
