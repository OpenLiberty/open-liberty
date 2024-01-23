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
import org.apache.hc.core5.http.Header;
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
 * Test HTTP response sendRedirect methods:
 *
 * sendRedirect(String location)
 * sendRedirect(String location, int status_code)
 * sendRedirect(String location, boolean clearBuffer)
 * sendRedirect(String location, int status_code, boolean clearBuffer)
 *
 * These tests will just verify the response status code, Location header, and response's body without follow up with the redirect.
 *
 * Location is a fix location "https://github.com/OpenLiberty"
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61ResponseSendRedirectTest {

    private static final Logger LOG = Logger.getLogger(Servlet61ResponseSendRedirectTest.class.getName());
    private static final String TEST_APP_NAME = "ResponseSendRedirectTest";
    private static final String SERVLET_MAPPING = "TestResponseSendRedirect";
    private static final String REDIRECT_LOCATION = "https://github.com/OpenLiberty";
    private static final String RESP_DEFAULT_HYPER_TEXT_URL_BODY = "&lt;html&gt;&lt;body&gt;&lt;p&gt;Redirecting to &lt;a href=&quot;https://github.com/OpenLiberty&quot;&gt;&lt;/a&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;";
    private static final String RESP_CUSTOM_TEXT_BODY = "Send Redirect Custom Message Text";

    @Server("servlet61_ResponseSendRedirectTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_ResponseSendRedirectTest.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "sendredirect.servlets");
        server.startServer(Servlet61ResponseSendRedirectTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for test.");
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
     * Test sendRedirect(String REDIRECT_LOCATION)
     *
     * Test sendRedirect 302 (default) to REDIRECT_LOCATION with RESP_DEFAULT_HYPER_TEXT_URL_BODY message.
     */
    @Test
    public void test_sendRedirect() throws Exception {

        LOG.info("====== <test_sendRedirect> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testSendRedirect");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = EntityUtils.toString(response.getEntity());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [302]", sc == 302);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }

                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_DEFAULT_HYPER_TEXT_URL_BODY));
            }
        }
    }

    /**
     * Test sendRedirect(String REDIRECT_LOCATION, int status_code)
     *
     * Test sendRedirect 301 to REDIRECT_LOCATION with RESP_DEFAULT_HYPER_TEXT_URL_BODY message.
     */
    @Test
    public void test_sendRedirect_301() throws Exception {

        LOG.info("====== <test_sendRedirect_301> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testSendRedirect_301");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = EntityUtils.toString(response.getEntity());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [301]", sc == 301);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }

                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_DEFAULT_HYPER_TEXT_URL_BODY));
            }
        }
    }

    /**
     * Test sendRedirect(String REDIRECT_LOCATION, int status_code)
     *
     * Test sendRedirect 303 to REDIRECT_LOCATION with RESP_DEFAULT_HYPER_TEXT_URL_BODY message.
     */
    @Test
    public void test_sendRedirect_303() throws Exception {

        LOG.info("====== <test_sendRedirect_303> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testSendRedirect_303");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = EntityUtils.toString(response.getEntity());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [303]", sc == 303);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }

                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_DEFAULT_HYPER_TEXT_URL_BODY));
            }
        }
    }

    /**
     * Test sendRedirect(String Location) but use PrinterWriter first
     *
     * This test the server ability to switch between OutputStream and Writer to adapt to the application's action
     * Expecting 302 redirect to REDIRECT_LOCATION with RESP_DEFAULT_HYPER_TEXT_URL_BODY message.
     */
    @Test
    public void test_sendRedirect_writer() throws Exception {

        LOG.info("====== <test_sendRedirect_writer> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "testSendRedirect_writer");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = EntityUtils.toString(response.getEntity());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [302]", sc == 302);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }

                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_DEFAULT_HYPER_TEXT_URL_BODY));
            }
        }
    }

    /**
     * Test sendRedirect(String REDIRECT_LOCATION, boolean clearBuffer)
     *
     * Test sendRedirect 302 (default) with a clearBuffer = false to retains the application's body RESP_CUSTOM_TEXT_BODY that will be included in the redirect
     */

    @Test
    public void test_sendRedirect_clearBuffer_false() throws Exception {

        LOG.info("====== <test_sendRedirect_clearBuffer_false> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testSendRedirect_clearBuffer_false");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = (EntityUtils.toString(response.getEntity()).trim());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [302]", sc == 302);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }
                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_CUSTOM_TEXT_BODY));
            }
        }
    }

    /**
     * sendRedirect(String REDIRECT_LOCATION, int status_code, boolean clearBuffer)
     *
     * Test sendRedirect 301 with a clearBuffer = false to retains the application's body RESP_CUSTOM_TEXT_BODY that will be included in the redirect
     */
    @Test
    public void test_sendRedirect_301_clearBuffer_false() throws Exception {

        LOG.info("====== <test_sendRedirect_301_clearBuffer_false> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testSendRedirect_301_clearBuffer_false");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = (EntityUtils.toString(response.getEntity()).trim());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [301]", sc == 301);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }
                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_CUSTOM_TEXT_BODY));
            }
        }
    }

    /**
     * sendRedirect(String REDIRECT_LOCATION, int status_code, boolean clearBuffer)
     *
     * Test sendRedirect 303 with a clearBuffer = true to replace the application's body with the default RESP_DEFAULT_HYPER_TEXT_URL_BODY
     *
     * @throws Exception
     */
    @Test
    public void test_sendRedirect_303_clearBuffer_true() throws Exception {

        LOG.info("====== <test_sendRedirect_303_clearBuffer_true> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/" + SERVLET_MAPPING;
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testSendRedirect_303_clearBuffer_true");

        try (final CloseableHttpClient client = HttpClientBuilder.create().disableRedirectHandling().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int sc = response.getCode();
                String headerValue = null;
                Header[] headers = response.getHeaders();
                String responseText = (EntityUtils.toString(response.getEntity()).trim());

                LOG.info("\n" + "Response Status: [" + sc + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("The response code is [" + sc + "] ; expecting [303]", sc == 303);

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                    if (header.getName().equals("Location")) {
                        headerValue = header.getValue();
                        LOG.info("[Location] header; value [" + headerValue + "] ; Expecting value [" + REDIRECT_LOCATION + "]");
                        assertTrue("Expected Location header not found ", headerValue.equals(REDIRECT_LOCATION));
                        break;
                    }
                }
                assertTrue("Expecting body not found ", responseText.equalsIgnoreCase(RESP_DEFAULT_HYPER_TEXT_URL_BODY));
            }
        }
    }
}
