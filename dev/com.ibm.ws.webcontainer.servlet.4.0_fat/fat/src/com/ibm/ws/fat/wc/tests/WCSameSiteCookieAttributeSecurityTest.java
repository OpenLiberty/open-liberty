/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * A test to check that the webAppSecurity SameSite configuration takes
 * precedence over the httpEndpoint SameSite configuration.
 *
 * Using a unique class and server here as we need to enable some additional
 * trace for SCR (logservice=all) to debug an intermittent test failure.
 *
 */
@RunWith(FATRunner.class)
public class WCSameSiteCookieAttributeSecurityTest {

    private static final Logger LOG = Logger.getLogger(WCSameSiteCookieAttributeSecurityTest.class.getName());
    private static final String APP_NAME_SAMESITE_SECURITY = "SameSiteSecurityTest";

    @Server("servlet40_sameSiteSecurity")
    public static LibertyServer sameSiteSecurityServer;

    @BeforeClass
    public static void before() throws Exception {
        // Build and deploy the application that we need for this test
        ShrinkHelper.defaultApp(sameSiteSecurityServer, APP_NAME_SAMESITE_SECURITY + ".war", "samesite.security.servlet");

        // Start the server and use the class name so we can find logs easily.
        sameSiteSecurityServer.startServer(WCSameSiteCookieAttributeSecurityTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (sameSiteSecurityServer != null && sameSiteSecurityServer.isStarted()) {
            sameSiteSecurityServer.stopServer();
        }
    }

    /**
     * Configure the server with the following:
     * <webAppSecurity sameSiteCookie="Strict"/>
     * <samesite lax="*"/> -> inside the httpEndpoint
     *
     * Drive a request to a Servlet that requires login.
     *
     * Login using the JSP.
     *
     * Verify that the LtpaToken2 cookie has a SameSite attribute of Strict as the webAppSecurity configuration
     * should take precedence over the httpEndpoint configuration.
     *
     * @throws Exception
     */
    @Test
    public void testSameSiteConfig_Lax_WebAppSecurity_Strict() throws Exception {
        boolean headerFound = false;
        String expectedResponse = "Welcome to the SameSiteSecurityServlet!";

        // CWWKS4105I: LTPA configuration is ready after x seconds
        assertNotNull("CWWKS4105I LTPA configuration message not found.",
                      sameSiteSecurityServer.waitForStringInLogUsingMark("CWWKS4105I.*"));

        String url = "http://" + sameSiteSecurityServer.getHostname() + ":" + sameSiteSecurityServer.getHttpDefaultPort() + "/" + APP_NAME_SAMESITE_SECURITY
                     + "/SameSiteSecurityServlet";
        String userName = "user1";
        String password = "user1Login";
        String location;

        HttpGet getMethod = new HttpGet(url);

        // Drive the initial request.
        LOG.info("Initial Request: url = " + getMethod.getUri().toString() + " request method = " + getMethod);
        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                LOG.info("Initial request result: " + response.getReasonPhrase());
                LOG.info("Initial request page status code: " + response.getCode());
                assertEquals("The expected status code for the initial request was not returned: ",
                             302, response.getCode());

                String content = EntityUtils.toString(response.getEntity());
                LOG.info("Initial request content: " + content);
                EntityUtils.consume(response.getEntity());

                // The initial request should result in a 302 status code so we need to
                // find where we're being redirected to and drive a request to that location
                // since we have disableRedirectHandling enabled. We should arrive at the login page.
                location = response.getHeader("location").getValue();
                LOG.info("Redirect to : " + location);
                getMethod = new HttpGet(location);
                try (final CloseableHttpResponse responseRedirect = client.execute(getMethod)) {
                    LOG.info("Form login page result: " + responseRedirect.getReasonPhrase());
                    LOG.info("Form login page status code: " + responseRedirect.getCode());
                    String contentRedirect = EntityUtils.toString(responseRedirect.getEntity());
                    LOG.info("Form login page content: " + contentRedirect);
                    EntityUtils.consume(responseRedirect.getEntity());

                    // Verify we get the form login JSP
                    assertEquals("The expected status code for the form login page was not returned: ",
                                 200, responseRedirect.getCode());
                    assertTrue("Did not find expected form login page: " + "Form Login Page",
                               contentRedirect.contains("Form Login Page"));
                }
            }

            // Perform the login now.
            LOG.info("Perform FormLogin: url=" + url +
                     " user=" + userName + " password=" + password);

            // Post method to login
            HttpPost postMethod = new HttpPost(url + "/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", userName));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps));

            try (final CloseableHttpResponse response = client.execute(postMethod)) {
                LOG.info("Post Method response code: " + response.getCode());
                assertEquals("The expected form login status code was not returned: ", 302,
                             response.getCode());

                // Verify that the LtpaToken2 cookie has SameSite=Strict
                for (Header cookieHeader : response.getHeaders("Set-Cookie")) {
                    String cookieHeaderValue = cookieHeader.getValue();
                    LOG.info("Header Name: " + cookieHeader.getName());
                    LOG.info("Header Value: " + cookieHeaderValue);

                    if (cookieHeaderValue.startsWith("LtpaToken2")) {
                        if (cookieHeaderValue.contains("SameSite=Strict")) {
                            headerFound = true;
                        }
                    }
                }

                assertTrue("The LtpaToken2 Set-Cookie header did not contain SameSite=Strict.", headerFound);

                // The Post request should result in a redirect to the servlet. Save the location Header
                // so we can drive the final authenticated request to the servlet.
                Header header = response.getFirstHeader("Location");
                location = header.getValue();
                LOG.info("Redirect location: " + location);

                EntityUtils.consume(response.getEntity());

                assertEquals("Redirect location was not the original URL: ",
                             url, location);
            }

            // Drive the request to the Servlet.
            getMethod = new HttpGet(location);

            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                LOG.info("getMethod status: " + response.getReasonPhrase());
                assertEquals("The expected status code was not returned: ",
                             200, response.getCode());

                String content = EntityUtils.toString(response.getEntity());
                LOG.info("Servlet content: " + content);

                EntityUtils.consume(response.getEntity());

                assertTrue("Response did not contain expected response: " + expectedResponse,
                           content.equals(expectedResponse));
            }
        }
    }
}
