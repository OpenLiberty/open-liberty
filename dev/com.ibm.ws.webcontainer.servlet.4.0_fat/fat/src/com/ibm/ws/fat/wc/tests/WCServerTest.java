/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * All Servlet 4.0 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServerTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());
    private static final String SERVLET_40_APP_JAR_NAME = "TestServlet40";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        JavaArchive testServlet40Jar = ShrinkWrap.create(JavaArchive.class, SERVLET_40_APP_JAR_NAME + ".jar");
        ShrinkHelper.addDirectory(testServlet40Jar, "test-applications/" + SERVLET_40_APP_JAR_NAME + ".jar" + "/resources");
        testServlet40Jar.addPackage("testservlet40.jar.servlets");

        WebArchive testServlet40War = ShrinkWrap.create(WebArchive.class, SERVLET_40_APP_JAR_NAME + ".war");
        testServlet40War.addAsLibrary(testServlet40Jar);
        testServlet40War.addPackage("testservlet40.servlets");
        testServlet40War.addPackage("testservlet40.listeners");

        ShrinkHelper.exportDropinAppToServer(server, testServlet40War);

        server.startServer(WCServerTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
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
     * Request a simple servlet.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleServlet() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + SERVLET_40_APP_JAR_NAME + "/SimpleTestServlet", "Hello World");
    }

    /**
     * Simple test to a servlet then read the header to ensure we are using
     * Servlet 4.0. This test looks for the X-Powered-By header specifically.
     *
     * This test is skipped for EE9_FEATURES repeat because for servlet-5.0 + the
     * X-Powered-By header is going to be disabled by default.
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testServletXPoweredByHeader() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/MyServlet";
        String expectedResponse = "Hello World";
        String expectedHeaderValue = "Servlet/4.0";

        // verify the X-Powered-By Response header is present by default and equals Servlet/4.0
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                // Validate that the X-Powered-By header was not present in the response
                Header header = response.getHeader("X-Powered-By");
                assertTrue("The X-Powered-By header did not contain the expected header value: " + expectedHeaderValue + " but was: " + header.getValue(),
                           header.getValue().equals(expectedHeaderValue));
            }
        }
    }

    /**
     * Simple test to a servlet then read the header to ensure the X-Powered-By
     * header is disabled on Servlet-5.0
     *
     * This test is skipped for NO_MODIFICATION(servlet-4.0 feature).
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testServletXPoweredByHeader_Servlet50_Default() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/MyServlet";
        String expectedResponse = "Hello World";

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                // Validate that the X-Powered-By header was not present in the response
                Header header = response.getHeader("X-Powered-By");
                assertTrue("The X-Powered-By header was found when it should not have been.", header == null);
            }
        }
    }

    /**
     * Simple test to a servlet then read the header to ensure that when the
     * X-Powered-By header is enabled it contains Servlet/5.0.
     *
     * This test is skipped for NO_MODIFICATION(servlet-4.0 feature).
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testServletXPoweredByHeader_Servlet50_Enabled() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/MyServlet";
        String expectedResponse = "Hello World";
        String expectedHeaderValue = "Servlet/5.0";

        // Save the current server configuration
        server.saveServerConfiguration();

        // Enable the X-PoweredByHeader
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getWebContainer().setDisablexpoweredby(false);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);

        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(SERVLET_40_APP_JAR_NAME));
        LOG.info("Updated server configuration: " + server.getServerConfiguration());

        // verify the X-Powered-By Response header is present and equals Servlet/5.0
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                // Validate that the X-Powered-By header was not present in the response
                Header header = response.getHeader("X-Powered-By");
                assertTrue("The X-Powered-By header did not contain the expected header value: " + expectedHeaderValue + " but was: " + header.getValue(),
                           header.getValue().equals(expectedHeaderValue));
            }
        } finally {
            // Restore the original server configuration
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(SERVLET_40_APP_JAR_NAME));
            LOG.info("Restored server configuration: " + server.getServerConfiguration());
        }
    }

    /**
     * Verifies that the ServletContext.getMajorVersion() returns 4 and
     * ServletContext.getMinorVersion() returns 0 for Servlet 4.0.
     *
     * @throws Exception
     */

    @Test
    public void testServletContextMajorMinorVersion() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME;
        String majorVersionExpectedResult = "majorVersion: 4";
        if (JakartaEE9Action.isActive()) {
            majorVersionExpectedResult = "majorVersion: 5";
        }
        HttpUtils.findStringInReadyUrl(server, "/" + SERVLET_40_APP_JAR_NAME + "/MyServlet?TestMajorMinorVersion=true", majorVersionExpectedResult);

        HttpUtils.findStringInReadyUrl(server, "/" + SERVLET_40_APP_JAR_NAME + "/MyServlet?TestMajorMinorVersion=true", "minorVersion: 0");
    }

    /**
     * Test the setSessionTimeout and getSessionTimeout methods from the servlet
     * context API.
     *
     * @throws Exception
     */
    @Test
    public void testServletContextSetAndGetSessionTimeout() throws Exception {
        // The first url.
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/SessionTimeoutServlet?TestSessionTimeout=new";
        LOG.info("url: " + url);

        // First request will get a new session and will verify that the
        // getSessionTimeout method returns the correct timeout.
        String[] expectedResponseStrings = new String[] { "Session Timeout: 1", "Session object: # HttpSessionImpl #",
                                                          "max inactive interval : 60", "valid session : true",
                                                          "new session : true" };
        HttpGet getMethod = new HttpGet(url);
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                for (String expectedResponse : expectedResponseStrings) {
                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                }
            }

            // Second request will verify that the session is still the same, so no
            // new session has been created.
            // The second url.
            url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/SessionTimeoutServlet?TestSessionTimeout=current";
            LOG.info("url: " + url);
            expectedResponseStrings = new String[] { "Session object: # HttpSessionImpl #", "max inactive interval : 60",
                                                     "valid session : true", "new session : false" };
            getMethod = new HttpGet(url);
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                for (String expectedResponse : expectedResponseStrings) {
                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                }
            }

            Thread.sleep(70000); // Wait 70 seconds or 1 minute and 10 seconds

            // Third request will verify if session was invalidated after 70 seconds
            // or 1 minute and 10 seconds.
            // The third url.
            url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SERVLET_40_APP_JAR_NAME + "/SessionTimeoutServlet?TestSessionTimeout=current";
            LOG.info("url: " + url);
            expectedResponseStrings = new String[] { "Session object: null", "Session Invalidated" };
            getMethod = new HttpGet(url);
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                for (String expectedResponse : expectedResponseStrings) {
                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                }
            }
        }
    }
}