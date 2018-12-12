/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Class to test the X-Forwarded-* and Forwarded headers
 * with the <remoteIp> configuration in the server.xml
 */
@RunWith(FATRunner.class)
public class HttpXForwardedAndForwardedHeaderTests {

    private static final Class<?> ME = HttpXForwardedAndForwardedHeaderTests.class;
    public static final String APP_NAME = "EndpointInformation";

    @Server("FATServer")
    public static LibertyServer server;

    // Since we have tracing enabled give server longer timeout to start up.
    private static final long SERVER_START_TIMEOUT = 30 * 1000;

    // Timeout used for searching a string in log files
    private static final long SERVER_LOG_SEARCH_TIMEOUT = 5 * 1000;

    private final int httpDefaultPort = server.getHttpDefaultPort();
    private HttpClient client;

    @BeforeClass
    public static void setupOnlyOnce() throws Exception {
        // Create a WebArchive that will have the file name 'EndpointInformation.war' once it's written to a file
        // Include the 'com.ibm.ws.transport.http.servlets' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transport.http.servlets");
    }

    @Before
    public void setup() throws Exception {
        RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(InetAddress.getByName("127.0.0.1")).build();
        client = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler()).setDefaultRequestConfig(requestConfig).build();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer();
    }

    /**
     * Test trusted Forwarded for parameters against the default remoteIp configuration.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testTrustedForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test multiple Forwarded headers with for parameters against the default remoteIp configuration.
     *
     * All the IP addresses in this test are considered trusted.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleTrustedForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testMultipleTrustedForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299, for=192.168.0.101, for=192.168.2.322"),
                                        new BasicHeader("Forwarded", "for=172.17.045.122"),
                                        new BasicHeader("Forwarded", "for=172.31.255.188, for=169.254.234.322")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the Forwarded for parameters against the default remoteIp configuration.
     *
     * Note that there is an IP that is not considered as trusted, as a result the
     * remote client information should not be verified.
     *
     * @throws Exception
     */
    @Test
    public void testUntrustedForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testUntrustedForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // IP 192.18.40.130 is invalid in the default remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.18.40.130,for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test multiple Forwarded headers with for parameters against the default remoteIp configuration.
     *
     * Note that there is an IP that is not considered as trusted, as a result the
     * remote client information should not be verified.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleUntrustedForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testMultipleUntrustedForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // IP 192.18.40.130 is invalid in the default remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299, for=192.168.0.101, for=192.168.2.322"),
                                        new BasicHeader("Forwarded", "for=172.17.045.122"),
                                        new BasicHeader("Forwarded", "for=192.18.40.130, for=169.254.234.322")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test trusted Forwarded for parameters but with a for misspelled parameter against the default remoteIp configuration.
     *
     * The remote client address should not be verified because of the typo.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForMisspelledWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForMisspelledWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a typo in one of the for parameters. Instead of "for", it says "far".
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,far=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                     + "host=example.com;"
                                                                     + "proto=https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Host", !response.contains("Remote Host: example.com"));
        assertTrue("Response does contain expected Scheme", !response.contains("Scheme: https"));
        assertTrue("Response does contain expected isSecure", !response.contains("isSecure: true"));
    }

    /**
     * Test trusted Forwarded for parameters but with host misspelled parameter against the default remoteIp configuration.
     *
     * The remote client address should not be verified because of the typo.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedHostMisspelledWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedHostMisspelledWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a typo in one of the for parameters. Instead of "host", it says "hst".
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                     + "hst=example.com;"
                                                                     + "proto=https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Host", !response.contains("Remote Host: example.com"));
        assertTrue("Response does contain expected Scheme", !response.contains("Scheme: https"));
        assertTrue("Response does contain expected isSecure", !response.contains("isSecure: true"));
    }

    /**
     * Test trusted Forwarded for parameter with spaces against the default remoteIp configuration.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForWithSpacesWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedWithSpacesWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for = 192.100.0.299 , for = 192.168.0.101 , for = 192.168.2.322 , for = 172.31.255.188; "
                                                                     + "host=example.com; "
                                                                     + "proto=https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: example.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: true"));
    }

    /**
     * Test the forwarded header with the for parameter and with an "unknown" proxy
     * identifier against the default remoteIp configuration.
     *
     * Note that the "unknown" identifier is not allowed in the default remoteIp config.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedUnknownProxyForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testUntrustedUnknownProxyForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is an "unknown" identifier in the list
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=unknown,for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the forwarded header with the for parameter and with an obfuscated
     * identifier against the default remoteIp configuration.
     *
     * The remote client address should not be verified since
     * obfuscated node indentifiers are not allow by default.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedObfuscatedProxyForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testUntrustedObfuscatedProxyForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a "_SEV754KISEK" obfuscated identifier in the list
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=172.17.045.122,for=_SEV754KISEK")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test trusted Forwarded for parameters with an invalid remote client port.
     *
     * Since the port validation should fail, the remote client will not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndInvalidRemoteClientPortWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForAndInvalidRemoteClientPortWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the port of remote client IP address 192.100.0.299 is invalid as it contains letters
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:as23\",for=192.168.0.101,for=192.168.2.322,for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: as23"));
    }

    /**
     * Test trusted Forwarded for parameters with obfuscated remote client port.
     *
     * Since the port validation should fail, the remote client will not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndObfuscatedRemoteClientPortWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForAndObfuscatedRemoteClientPortWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the port of remote client IP address 192.100.0.299 is obfuscated
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:_re65\",for=192.168.0.101,for=192.168.2.322,for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: _re65"));
    }

    /**
     * Test trusted Forwarded for parameters with an invalid port in a proxy server.
     *
     * Ports in proxy servers don't matter, as a result, if the port of the proxy is wrong
     * but everything else is correct, verification of the remote client information is expected.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndInvalidProxyPortWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForAndInvalidProxyPortWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the port of proxy IP address 192.168.2.322 is invalid as it contains letters
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=192.168.0.101,for=\"192.168.2.322:as23\",for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
    }

    /**
     * Test trusted forwarded for parameters with an obfuscated port in a proxy server.
     *
     * Ports in proxy servers don't matter, as a result, if the port of the proxy is wrong
     * but everything else is correct, verification of the remote client information is expected.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndObfuscatedProxyPortWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForAndObfuscatedProxyPortWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the port of proxy IP address 192.168.2.322 is obfuscated
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=192.168.0.101,for=\"192.168.2.322:_as23\",for=172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
    }

    /**
     * Test trusted Forwarded for parameters against the default remoteIp configuration
     * in multiple requests for the same connection.
     *
     * The remote client address should be verified per requests. Note that
     * the forwarded information in the first and second request should be different
     * although the same connection is used for both requests.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForInMultipleRequestsWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForInMultipleRequestsWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> firstRequestHeaderList = new ArrayList<BasicHeader>();
        firstRequestHeaderList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188"),
                                                    new BasicHeader("Connection", "keep-alive")));

        List<BasicHeader> secondRequestHeaderList = new ArrayList<BasicHeader>();
        secondRequestHeaderList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=175.54.34.034,for=172.16.34.1,for=169.254.234.438,for=127.323.258.923")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, firstRequestHeaderList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "First response: " + response);

        assertTrue("First response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("First response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));

        httpResponse = execute(APP_NAME, servletName, secondRequestHeaderList, testName);
        response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Second response: " + response);

        assertTrue("Second response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Second response does not contain the expected Remote Address", response.contains("Remote Address: 175.54.34.034"));
    }

    /**
     * Test trusted X-Forwarded-For header against the default remoteIp configuration.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testTrustedXForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322, 172.31.255.188")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test multiple X-Forwarded-For header against the default remoteIp configuration.
     *
     * All the IP addresses in this test are considered trusted.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleTrustedXForwardedForDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testMultipleTrustedXForwardedForDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322"),
                                        new BasicHeader("X-Forwarded-For", "172.17.045.122"),
                                        new BasicHeader("X-Forwarded-For", "172.31.255.188, 169.254.234.322")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the X-Forwarded-For header against the default remoteIp configuration.
     *
     * Note that there is an IP that is not considered as trusted, as a result the
     * remote client information should not be verified.
     *
     * @throws Exception
     */
    @Test
    public void testUntrustedXForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testUntrustedXForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // IP 192.18.40.130 is invalid in the default remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322, 192.18.40.130")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test multiple X-Forwarded-For headers against the default remoteIp configuration.
     *
     * Note that there is an IP that is not considered as trusted, as a result the
     * remote client information should not be verified.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleUntrustedXForwardedForDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testMultipleUntrustedXForwardedForDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // IP 192.18.40.130 is invalid in the default remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322"),
                                        new BasicHeader("X-Forwarded-For", "192.18.40.130"),
                                        new BasicHeader("X-Forwarded-For", "172.31.255.188, 169.254.234.322")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test trusted X-Forwarded-For headers but with X-Forwarded-For misspelled header against the default remoteIp configuration.
     *
     * The remote client address should not be verified because of the typo.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedForMisspelledWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedForMisspelledWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a typo in one of the headers. Instead of "X-Forwarded-For", it says "X-Forwarded-Far".
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-Far", "192.100.0.299,192.168.0.101,192.168.2.322, 172.31.255.188"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Host", !response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does contain expected Scheme", !response.contains("Scheme: https"));
        assertTrue("Response does contain expected isSecure", !response.contains("isSecure: true"));
    }

    /**
     * Test trusted X-Forwarded-For headers but with X-Forwarded-Host misspelled header against the default remoteIp configuration.
     *
     * The remote client address should be verified even if there is a typo in the X-Forwarded-Host header name.
     * The HTTP Channel should just ignore the wrong header name.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedHostMisspelledWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedHostMisspelledWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a typo in one of the headers. Instead of "X-Forwarded-Host", it says "XForwarded-Host".
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299,192.168.0.101,192.168.2.322, 172.31.255.188"),
                                        new BasicHeader("XForwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Host", !response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: true"));
    }

    /**
     * Test trusted X-Forwarded-For header with spaces against the default remoteIp configuration.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedForWithSpacesWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedForWithSpacesWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a typo in one of the headers. Instead of "X-Forwarded-Host", it says "XForwarded-Host".
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299 , 192.168.0.101 , 192.168.2.322 , 172.31.255.188"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: true"));
    }

    /**
     * Test the combination of the X-Forwarded-For and Forwarded for headers with trusted IP addresses.
     *
     * Note that we first set X-Forwarded-For and then Forwarded for.
     *
     * When both headers are set, it should only take into account the Forwarded header
     * over the X-Forwarded header.
     *
     * The remote client address should be verified using the Forwarded header.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedForAndForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedForAndForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "127.18.2.101, 169.254.234.322, 10.31.255.255"),
                                        new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188"),
                                        new BasicHeader("X-Forwarded-For", "127.18.2.102, 169.254.234.323, 10.31.255.256"),
                                        new BasicHeader("Forwarded", "for=127.18.2.102,for=169.254.234.322")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the combination of the Forwarded for and X-Forwarded-For headers with trusted IP addresses.
     *
     * Note that we first set Forwarded for and then X-Forwarded-For.
     *
     * When both headers are set, it should only take into account the Forwarded header
     * over the X-Forwarded header.
     *
     * The remote client address should be verified using the Forwarded header.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndXForwardedForWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedForwardedForAndXForwardedForWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188"),
                                        new BasicHeader("X-Forwarded-For", "127.18.2.101, 169.254.234.322, 10.31.255.255"),
                                        new BasicHeader("Forwarded", "for=127.18.2.102,for=169.254.234.322"),
                                        new BasicHeader("X-Forwarded-For", "127.18.2.102, 169.254.234.323, 10.31.255.256")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test trusted X-Forwarded-For header with an invalid X-Forwarded-Port header value.
     *
     * Since the port validation should fail, the remote client will not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedForAndInvalidRemoteClientPortWithDefaultRemoteIpConfig() throws Exception {
        String variation = "defaultRemoteIP";
        String testName = "testTrustedXForwardedForAndInvalidRemoteClientPortWithDefaultRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the port of remote client IP address 192.100.0.299 is invalid as it contains letters
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299,192.168.0.101,192.168.2.322,172.31.255.188"),
                                        new BasicHeader("X-Forwarded-Port", "as23")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: as23"));
    }

    /**
     * Test trusted Forwarded for parameters with ports.
     *
     * The remote client address and remote port should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testTrustedForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"[2018:ac5:deba::23af]:8888\",for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 2018:ac5:deba::23af"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8888"));
    }

    /**
     * Test untrusted Forwarded for parameters with ports.
     *
     * The remote client address and remote port should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testUntrustedForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that IPv6 [2002:db8:ab:1234::ad32] is invalid in the remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=\"[2001:db9:cafe::17]:943\",for=\"[2002:db8:ab:1234::ad32]:12344\",for=192.168.123.123")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 2001:db9:cafe::17"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 943"));
    }

    /**
     * Test trusted X-Forwarded-For header with the X-Forwarded-Port header.
     *
     * The remote client address and remote port should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testTrustedXForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "2018:ac5:deba::23af,192.168.123.123,2001:db9:cafe::17,2001:db8:ab:1234::ad32"),
                                        new BasicHeader("X-Forwarded-Port", "8888")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 2018:ac5:deba::23af"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8888"));
    }

    /**
     * Test untrusted X-Forwarded-For header with the X-Forwarded-Port header.
     *
     * The remote client address and remote port should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedXForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testUntrustedXForwardedForAndPortWithIPv4AndIPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that IPv6 [2002:db8:ab:1234::ad32] is invalid in the remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "2001:db9:cafe::17,2002:db8:ab:1234::ad32,192.168.123.123"),
                                        new BasicHeader("X-Forwarded-Port", "943")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 2001:db9:cafe::17"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 943"));
    }

    /**
     * Test the Forwarded for parameter with a wrong proxy IPv4 address.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithProxyIPv4Error() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithProxyIPv4Error";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy information 192.168.1ra.123 is wrong. It is not actually an IPv4 address.
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"192.168.1ra.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 8989"));
    }

    /**
     * Test the Forwarded for parameter with a wrong proxy IPv6 address.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithProxyIPv6Error() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithProxyIPv6Error";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy information [2018:dtr9:erdco::17] is wrong. It is not actually an IPv6 address.
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"192.168.123.123:12344\",for=\"[2018:dtr9:erdco::17]:943\",for=\"[2001:db8:ab:1234::ad32]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 8989"));
    }

    /**
     * Test the Forwarded for parameter with a proxy IPv6 address missing the closing bracket.
     *
     * The remote client address should not be verified because of the missing bracket.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithProxyIPv6MissingClosingBracketError() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithProxyIPv6MissingClosingBracketError";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy IPv6 address [2001:db8:ab:1234::ad32 is missing the closing bracket
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"192.168.123.123:12344\",for=\"[2001:db8:ab:1234::ad32\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 8989"));
    }

    /**
     * Test the Forwarded for parameter with a proxy IPv6 address missing the opening bracket.
     *
     * The remote client address should not be verified because of the missing bracket.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithProxyIPv6MissingOpeningBracketError() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithProxyIPv6MissingOpeningBracketError";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy IPv6 address 2001:db8:ab:1234::ad32] is missing the opening bracket
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"2001:db8:ab:1234::ad32]\",for=\"192.168.123.123:12344\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does contain the expected Remote Port", !response.contains("Remote Port: 8989"));
    }

    /**
     * Test the Forwarded for parameter with an unknown remote client information.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithUnknownRemoteClient() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithUnknownRemoteClient";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the remote client information is unknown
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=unknown,for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: unknown"));
    }

    /**
     * Test the Forwarded for parameter with an obfuscated remote client information.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithObfuscatedRemoteClient() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedForWithObfuscatedRemoteClient";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the remote client information is unknown
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=_SEV754KISEK,for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: _SEV754KISEK"));
    }

    /**
     * Test the X-Forwarded-For header with a wrong proxy IPv4 address.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedForWithProxyIPv4Error() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedForWithProxyIPv4Error";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy information 192.168.1ra.123 is wrong. It is not actually an IPv4 address.
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.1ra.123, 2001:db9:cafe::17, 2001:db8:ab:1234::ad32")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the X-Forwarded-For header with a wrong proxy IPv6 address.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedForWithProxyIPv6Error() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedForWithProxyIPv6Error";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy information 2018:dtr9:erdco::17 is wrong. It is not actually an IPv6 address.
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.123.123, 2018:dtr9:erdco::17, 2001:db8:ab:1234::ad32")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the X-Forwarded-For header with a proxy IPv6 address including a closing bracket.
     *
     * The remote client address should not be verified because IPv6 addresses in X-Forwarded-For don't have brackets.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedForWithProxyIPv6IncludingClosingBracketError() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedForWithProxyIPv6IncludingClosingBracketError";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the proxy IPv6 address 2001:db8:ab:1234::ad32] is including a closing bracket. It is not actually an IPv6 address.
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.123.123, 2001:db8:ab:1234::ad32]")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the X-Forwarded-For header with an unknown remote client.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedForWithUnknownRemoteClient() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedForWithUnknownRemoteClient";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the remote client information is unknown
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "unknown, 192.168.123.123, 2001:db9:cafe::17, 2001:db8:ab:1234::ad32")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: unknown"));
    }

    /**
     * Test the X-Forwarded-For header with an obfuscated remote client.
     *
     * The remote client address should not be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedForWithObfuscatedRemoteClient() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedForWithObfuscatedRemoteClient";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that the remote client information is obfuscated
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "_SEV754KISEK, 192.168.123.123, 2001:db9:cafe::17, 2001:db8:ab:1234::ad32")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does contain the expected Remote Address", !response.contains("Remote Address: _SEV754KISEK"));
    }

    /**
     * Test the forwarded header with the for parameter and with an "unknown"
     * identifier against a custom remoteIp configuration that allows "unknown".
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithTrustedUnknownRemoteIpConfig() throws Exception {
        String variation = "unknownAndObfuscatedIndetifierRemoteIP";
        String testName = "testForwardedForWithTrustedUnknownRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is an "unknown" identifier in the list
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=unknown,for=127.31.255.188,for=\"[2001:db8:ab:1234::aB34]\"")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test the forwarded header with the for parameter and with an obfuscated
     * identifier against a custom remoteIp configuration that allows obfuscated.
     *
     * The remote client address should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedForWithTrustedObfuscatedRemoteIpConfig() throws Exception {
        String variation = "unknownAndObfuscatedIndetifierRemoteIP";
        String testName = "testForwardedForWithTrustedObfuscatedRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that there is a "_SEV754KISEK" obfuscated identifier in the list
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=127.31.255.188,for=_SEV754KISEK")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
    }

    /**
     * Test all the parameters of the Forwarded header. All the IP addresses in this case
     * are considered trusted.
     *
     * Note that even though the Forwarded "by" parameter is provided, it really should not be used.
     *
     * The remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testAllForwardedParametersAndPortWithIPv4IPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testAllForwardedParametersAndPortWithIPv4IPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\";"
                                                             + "by=169.254.478.368;"
                                                             + "proto=https;"
                                                             + "host=clienttest.com")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: true"));
    }

    /**
     * Test all the parameters of the Forwarded header. All the IP addresses in this case
     * are considered trusted.
     *
     * Note that even though the Forwarded "by" parameter is provided, it really should not be used.
     *
     * Also test that both proto and host parameters can be overwritten if they are sent
     * in a second Forwarded header.
     *
     * The remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testForwardedProtoAndHostOverwriteWithIPv4IPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testForwardedProtoAndHostOverwriteWithIPv4IPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"192.100.0.299:8989\",for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\";"
                                                             + "by=169.254.478.368;"
                                                             + "proto=https;"
                                                             + "host=clienttest.com"),
                                new BasicHeader("Forwarded", "proto=http;"
                                                             + "host=example.com")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: example.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: http"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: false"));
    }

    /**
     * Test all the X-Forwarded-* headers.
     *
     * Note that even though the X-Forwarded-By parameter is provided, it really should not be used.
     *
     * The remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testAllXForwardedParametersAndPortWithIPv4IPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testAllXForwardedParametersAndPortWithIPv4IPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.123.123, 2001:db9:cafe::17, 2001:db8:ab:1234::ad32"),
                                        new BasicHeader("X-Forwarded-By", "169.254.478.368"),
                                        new BasicHeader("X-Forwarded-Port", "8989"),
                                        new BasicHeader("X-Forwarded-Proto", "https"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: true"));
    }

    /**
     * Test all X-Forwarded-* headers.
     *
     * Note that even though the X-Forwarded-By parameter is provided, it really should not be used.
     *
     * Also test that both X-Forwarded-Proto and X-Forwarded-Host headers can be overwritten if they are sent
     * in a second X-Forwarded header.
     *
     * The remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testXForwardedProtoAndHostOverwriteWithIPv4IPv6RemoteIpConfig() throws Exception {
        String variation = "IPv4IPv6RemoteIP";
        String testName = "testXForwardedProtoAndHostOverwriteWithIPv4IPv6RemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.123.123, 2001:db9:cafe::17, 2001:db8:ab:1234::ad32"),
                                        new BasicHeader("X-Forwarded-By", "169.254.478.368"),
                                        new BasicHeader("X-Forwarded-Port", "8989"),
                                        new BasicHeader("X-Forwarded-Proto", "https"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "http"),
                                        new BasicHeader("X-Forwarded-Host", "example.com")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: example.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: http"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: false"));
    }

    /**
     * Test trusted Forwarded for parameter along with the host and proto parameters to be logged in the
     * access log file.
     *
     * The remote client information should be verified and logged in the access log.
     *
     * @throws Exception
     */
    @Test
    public void testTrustedForwardedWithAccessLogRemoteIpConfig() throws Exception {
        String variation = "accessLogRemoteIP";
        String testName = "testTrustedForwardedWithAccessLogRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                     + "host=clienttest.com;"
                                                                     + "proto=http")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));

        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, http";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test untrusted Forwarded for parameter along with the host and proto parameters and check if it
     * is logged in the access log file.
     *
     * The remote client information should not be verified, as a result, it should not be logged
     * in the access log file.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedForwardedWithAccessLogRemoteIpConfig() throws Exception {
        String variation = "accessLogRemoteIP";
        String testName = "testUntrustedForwardedWithAccessLogRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that IP address 126.45.52.625 is not trusted in the accessLog remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=126.45.52.625,for=172.31.255.188;"
                                                                     + "host=clienttest.com;"
                                                                     + "proto=http")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));

        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, http";

        // There should not be a match so fail if there is one.
        assertNull("The following string was found in the access log: " + stringToSearchFor,
                   server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test trusted X-Forwarded-For header along with X-Forwarded-Host and X-Forwarded-Proto headers to be logged in the
     * access log file.
     *
     * The remote client information should be verified and logged in the access log.
     *
     * @throws Exception
     */
    @Test
    public void testTrustedXForwardedWithAccessLogRemoteIpConfig() throws Exception {
        String variation = "accessLogRemoteIP";
        String testName = "testTrustedXForwardedWithAccessLogRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322, 172.31.255.188"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));

        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, https";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test untrusted X-Forwarded-For header along with X-Forwarded-Host and X-Forwarded-Proto headers and check if it
     * is logged in the access log file.
     *
     * The remote client information should not be verified, as a result, it should not be logged
     * in the access log file.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUntrustedXForwardedWithAccessLogRemoteIpConfig() throws Exception {
        String variation = "accessLogRemoteIP";
        String testName = "testUntrustedXForwardedWithAccessLogRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        // Note that IP address 126.45.52.625 is not trusted in the accessLog remoteIp configuration
        headerList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299, 192.168.0.101, 192.168.2.322, 126.45.52.625"),
                                        new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                        new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));

        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, https";

        // There should not be a match so fail if there is one.
        assertNull("The following string was found in the access log: " + stringToSearchFor,
                   server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test trusted Forwarded parameters against the remoteIp configuration
     * that has the access log attribute disabled, but the access log is configured.
     *
     * The remote client address should be verified but the access log file should not contain
     * any information from the forwarded header that was verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedWithDisableAccessLogRemoteIpConfig() throws Exception {
        String variation = "disableAccessLogRemoteIPConfig";
        String testName = "testTrustedForwardedWithDisableAccessLogRemoteIpConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                     + "host=clienttest.com;"
                                                                     + "proto=http")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));

        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, http";

        // There should not be a match so fail if there is one.
        assertNull("The following string was found in the access log: " + stringToSearchFor,
                   server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test trusted Forwarded parameters against multiple server configurations on the same server and without restarting it.
     *
     * Drive two requests (one per server configuration), and in both cases,
     * the remote client information should be verified.
     *
     * Also, in both cases, check the access log file. In the first case the NCSA access log is enabled, and in the second case
     * the NCSA access log is disabled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedForwardedWithMultipleServerConfigurations() throws Exception {
        String variation1 = "accessLogRemoteIP";
        String variation2 = "IPv4IPv6RemoteIP";
        String testName = "testTrustedForwardedWithMultipleServerConfigurations";
        String servletName = "EndpointInformationServlet";

        // Start the server with the accessLogRemoteIP configuration
        startServer(variation1);

        // First request
        List<BasicHeader> firstRequestHeaderList = new ArrayList<BasicHeader>();
        firstRequestHeaderList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                                 + "host=clienttest.com;"
                                                                                 + "proto=http")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, firstRequestHeaderList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "First response: " + response);

        assertTrue("First response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("First response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("First response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("First response does not contain expected Scheme", response.contains("Scheme: http"));
        assertTrue("First response does not contain expected isSecure", response.contains("isSecure: false"));

        // Since the first configuration has the access log enabled, check that the correct remote client information is logged.
        RemoteFile accessLog = server.getFileFromLibertyServerRoot("logs/http_access.log");

        String stringToSearchFor = "192.100.0.299, clienttest.com, http";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));

        // Update the server configuration on the fly (without restart) to use the IPv4IPv6RemoteIP configuration
        server.reconfigureServer("remoteIPConfig/" + variation2 + "-server.xml", "CWWKT0016I:.*EndpointInformation.*");

        // Second request
        List<BasicHeader> secondRequestHeaderList = new ArrayList<BasicHeader>();
        secondRequestHeaderList.addAll(Arrays
                        .asList(new BasicHeader("Forwarded", "for=\"135.369.47.888:8989\",for=\"192.168.123.123:12344\",for=\"[2001:db9:cafe::17]:943\",for=\"[2001:db8:ab:1234::ad32]\";"
                                                             + "host=example.com;"
                                                             + "proto=https")));

        httpResponse = execute(APP_NAME, servletName, secondRequestHeaderList, testName);
        response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Second response: " + response);

        assertTrue("Second response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Second response does not contain the expected Remote Address", response.contains("Remote Address: 135.369.47.888"));
        assertTrue("Second response does not contain the expected Remote Port", response.contains("Remote Port: 8989"));
        assertTrue("Second response does not contain the expected Remote Host", response.contains("Remote Host: example.com"));
        assertTrue("Second response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("Second response does not contain expected isSecure", response.contains("isSecure: true"));

        // Update the string to search for
        stringToSearchFor = "135.369.47.888, example.com, https";

        // Since the IPv4IPv6RemoteIP configuration doesn't have the useRemoteIpInAccessLog set to true,
        // then there should not be a match so fail if there is one.
        assertNull("The following string was found in the access log: " + stringToSearchFor,
                   server.waitForStringInLog(stringToSearchFor, SERVER_LOG_SEARCH_TIMEOUT, accessLog));
    }

    /**
     * Test trusted X-Forwarded-* headers against multiple server configurations on the same server and without restarting it.
     *
     * Drive two requests (one per server configuration), and in both cases,
     * the remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrustedXForwardedWithMultipleServerConfigurations() throws Exception {
        String variation1 = "unknownAndObfuscatedIndetifierRemoteIP";
        String variation2 = "defaultRemoteIP";
        String testName = "testTrustedXForwardedWithMultipleServerConfigurations";
        String servletName = "EndpointInformationServlet";

        // Start the server with the unknownAndObfuscatedIndetifierRemoteIP configuration
        startServer(variation1);

        // First request
        List<BasicHeader> firstRequestHeaderList = new ArrayList<BasicHeader>();
        firstRequestHeaderList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "192.100.0.299,192.168.0.101,127.31.255.188,2001:db8:ab:1234::aB34"),
                                                    new BasicHeader("X-Forwarded-Host", "example.com"),
                                                    new BasicHeader("X-Forwarded-Proto", "https")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, firstRequestHeaderList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "First response: " + response);

        assertTrue("First response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("First response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("First response does not contain the expected Remote Host", response.contains("Remote Host: example.com"));
        assertTrue("First response does not contain expected Scheme", response.contains("Scheme: https"));
        assertTrue("First response does not contain expected isSecure", response.contains("isSecure: true"));

        // Update the server configuration on the fly (without restart) to use the defaultRemoteIP configuration
        server.reconfigureServer("remoteIPConfig/" + variation2 + "-server.xml", "CWWKT0016I:.*EndpointInformation.*");

        // Second request
        List<BasicHeader> secondRequestHeaderList = new ArrayList<BasicHeader>();
        secondRequestHeaderList.addAll(Arrays.asList(new BasicHeader("X-Forwarded-For", "199.512.256.999,192.168.0.101,192.168.2.322,172.31.255.188"),
                                                     new BasicHeader("X-Forwarded-Port", "7845"),
                                                     new BasicHeader("X-Forwarded-Host", "clienttest.com"),
                                                     new BasicHeader("X-Forwarded-Proto", "http")));

        httpResponse = execute(APP_NAME, servletName, secondRequestHeaderList, testName);
        response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Second response: " + response);

        assertTrue("Second response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Second response does not contain the expected Remote Address", response.contains("Remote Address: 199.512.256.999"));
        assertTrue("Second response does not contain the expected Remote Port", response.contains("Remote Port: 7845"));
        assertTrue("Second response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("Second response does not contain expected Scheme", response.contains("Scheme: http"));
        assertTrue("Second response does not contain expected isSecure", response.contains("isSecure: false"));
    }

    /**
     * Test trusted Forwarded parameters using the remoteIpRef and remoteIp default configuration.
     *
     * The remote client information should be verified.
     *
     * @throws Exception
     */
    @Test
    public void testTrustedForwardedWithDefaultRemoteIpRefConfig() throws Exception {
        String variation = "defaultRemoteIPRef";
        String testName = "testTrustedForwardedWithDefaultRemoteIpRefConfig";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        List<BasicHeader> headerList = new ArrayList<BasicHeader>();
        headerList.addAll(Arrays.asList(new BasicHeader("Forwarded", "for=192.100.0.299,for=192.168.0.101,for=192.168.2.322,for=172.31.255.188;"
                                                                     + "host=clienttest.com;"
                                                                     + "proto=http")));

        HttpResponse httpResponse = execute(APP_NAME, servletName, headerList, testName);
        String response = getResponseAsString(httpResponse);

        Log.info(ME, testName, "Response: " + response);

        assertTrue("Response does not contain Endpoint Information Servlet Test message", response.contains("Endpoint Information Servlet Test"));
        assertTrue("Response does not contain the expected Remote Address", response.contains("Remote Address: 192.100.0.299"));
        assertTrue("Response does not contain the expected Remote Host", response.contains("Remote Host: clienttest.com"));
        assertTrue("Response does not contain expected Scheme", response.contains("Scheme: http"));
        assertTrue("Response does not contain expected isSecure", response.contains("isSecure: false"));
    }

    /**
     * Private method to start a server.
     *
     * Please look at publish/files/remoteIPConfig directory for the different server names.
     *
     * @param variation The name of the server that needs to be appended to "-server.xml"
     * @throws Exception
     */
    private void startServer(String variation) throws Exception {
        server.setServerConfigurationFile("remoteIPConfig/" + variation + "-server.xml");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer(variation + ".log");
    }

    /**
     * Private method to execute/drive an HTTP request and obtain an HTTP response
     *
     * @param app The app name or context root
     * @param path The specific path to drive the request
     * @param headerList A list of headers to be added in the request
     * @param variation The name of the server for logging purposes
     * @return The HTTP response for the request
     * @throws Exception
     */
    private HttpResponse execute(String app, String path, List<BasicHeader> headerList, String variation) throws Exception {

        String urlString = "http://" + server.getHostname() + ":" + httpDefaultPort + "/" + app + "/" + path;
        URI uri = URI.create(urlString);
        Log.info(ME, variation, "Execute request to " + uri);

        HttpGet request = new HttpGet(uri);

        Log.info(ME, variation, "Header list: " + headerList.toString());

        for (BasicHeader header : headerList) {
            request.addHeader(header.getName(), header.getValue());
        }

        HttpResponse response = client.execute(request);
        Log.info(ME, variation, "Returned: " + response.getStatusLine());
        return response;
    }

    /**
     * Get the HTTP response as a String
     *
     * @param response
     * @return A String that contains the response
     * @throws IOException
     */
    private String getResponseAsString(HttpResponse response) throws IOException {

        final HttpEntity entity = response.getEntity();

        assertNotNull("No response found", entity);

        return EntityUtils.toString(entity);
    }
}
