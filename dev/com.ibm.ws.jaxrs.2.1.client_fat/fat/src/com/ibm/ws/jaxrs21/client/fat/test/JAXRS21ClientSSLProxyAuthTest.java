/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests are used to determine whether a JAX-RS 2.0 client can reach a remote endpoint when using an authenticating
 * proxy server.
 *
 * Note that the password used in the servlet will be different than what is passed in here - this is so that the test
 * case can check that the password specified in the actual Client APIs are not logged, even when tracing is enabled.
 */
@RunWith(FATRunner.class)
public class JAXRS21ClientSSLProxyAuthTest extends JAXRS21AbstractTest {
    private final static Class<?> c = JAXRS21ClientSSLProxyAuthTest.class;

    @Server("jaxrs21.client.JAXRS21ProxyAuthTest")
    public static LibertyServer server;

    private static final String clientproxyauthwar = "jaxrs21clientproxyAuth";

    private final static String target = "jaxrs21clientproxyAuth/JAXRS21ClientTestServlet";

    private static int proxyPort;

    private static int mockServerPort;

    private static ClientAndServer proxyClient;

    private static ClientAndServer mockServerClient;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, clientproxyauthwar,
                                      "com.ibm.ws.jaxrs21.client.jaxrs21clientproxyauth.client",
                                      "com.ibm.ws.jaxrs21.client.jaxrs21clientproxyauth.service");

        Log.info(c, "setup", "Setting system properties");
        System.setProperty("javax.net.ssl.keyStore", "publish/servers/jaxrs21.client.JAXRS21ProxyAuthTest/resources/security/key.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "passw0rd");
        System.setProperty("javax.net.ssl.trustStore", "publish/servers/jaxrs21.client.JAXRS21ProxyAuthTest/resources/security/trust.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        ConfigurationProperties.useBouncyCastleForKeyAndCertificateGeneration(true);
        ConfigurationProperties.forwardProxyTLSX509CertificatesTrustManagerType("ANY");
        ConfigurationProperties.proxyAuthenticationUsername("jaxrsUser");
        ConfigurationProperties.proxyAuthenticationPassword("myPa$$word");
        ConfigurationProperties.proxyAuthenticationRealm("foo");
        ConfigurationProperties.attemptToProxyIfNoMatchingExpectation(true);

        Log.info(c, "setup", "Starting mock server proxy server");
        proxyPort = Integer.getInteger("member_1.http");
        proxyClient = startClientAndServer(proxyPort);

        Log.info(c, "setup", "Starting mock server backend server");
        mockServerPort = Integer.getInteger("member_2.http");
        mockServerClient = startClientAndServer(mockServerPort);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            Log.info(c, "setup", "Starting Liberty server");
            server.startServer(true);
        } catch (Exception e) {
            Log.error(c, "setup", e, "Exception while starting server");
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        proxyClient.stop();
        mockServerClient.stop();
        server.stopServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
        Log.info(c, "preTest", "Mock Server Proxy listening on port " + proxyPort);
    }

    @After
    public void afterTest() {
        serverRef = null;
        mockServerClient.reset();
    }

    /////////////////////////////////////////////////////////////////////////
    // Test when configuring the proxy options using the ClientBuilder object

    @Test
    public void testTunnelThroughProxyToHTTPEndpoint_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

//    @Test TODO: intermittent test bug "Socket output is already shutdown"
    public void testTunnelThroughProxyToHTTPSEndpoint_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("secPort", "" + server.getHttpDefaultSecurePort());

        this.runTestOnServer(target, "testProxyToHTTPS_ClientBuilder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word

    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointInvalidUsername_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser1");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_ClientBuilder() throws Exception {
        // Server will respond to any request with 407 to mock a proxy authentication failure
        mockServerClient.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockServerPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required",
                                                                         "[Proxy Error]:jakarta.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointTimeout_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "100");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_ClientBuilder", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ",
                                                                                "[Proxy Error]:jakarta.ws.rs.ProcessingException: RESTEASY004655");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    //////////////////////////////////////////////////////////////////
    // Test when configuring the proxy options using the Client object
    @Test
    public void testTunnelThroughProxyToHTTPEndpoint_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Client", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

 // TODO: https://github.com/OpenLiberty/open-liberty/issues/18849
    @Test
    @MinimumJavaLevel(javaLevel = 11)
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES"}) // RESTEasy only supports properties set on ClientBuilder
    public void testTunnelThroughProxyToHTTPSEndpoint_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("secPort", "" + server.getHttpDefaultSecurePort());

        this.runTestOnServer(target, "testProxyToHTTPS_Client", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word

    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointInvalidUsername_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser1");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Client", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_Client() throws Exception {
        // Server will respond to any request with 407 to mock a proxy authentication failure
        mockServerClient.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockServerPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Client", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required",
                                                                  "[Proxy Error]:jakarta.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES"}) // RESTEasy only supports properties set on ClientBuilder
    public void testTunnelThroughProxyToHTTPEndpointTimeout_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "100");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_Client", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ",
                                                                         "[Proxy Error]:jakarta.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    /////////////////////////////////////////////////////////////////////
    // Test when configuring the proxy options using the WebTarget object
    @Test
    public void testTunnelThroughProxyToHTTPEndpoint_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_WebTarget", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

    // TODO: https://github.com/OpenLiberty/open-liberty/issues/18849
    @Test
    @MinimumJavaLevel(javaLevel = 11)
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES"}) // RESTEasy only supports properties set on ClientBuilder
    public void testTunnelThroughProxyToHTTPSEndpoint_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("secPort", "" + server.getHttpDefaultSecurePort());

        this.runTestOnServer(target, "testProxyToHTTPS_WebTarget", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word

    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointInvalidUsername_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser1");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_WebTarget", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_WebTarget() throws Exception {
        // Server will respond to any request with 407 to mock a proxy authentication failure
        mockServerClient.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockServerPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_WebTarget", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required",
                                                                     "[Proxy Error]:jakarta.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
    }

    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES"}) // RESTEasy only supports properties set on ClientBuilder
    public void testTunnelThroughProxyToHTTPEndpointTimeout_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "100");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_WebTarget", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ",
                                                                            "[Proxy Error]:jakarta.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    //////////////////////////////////////////////////////////////////////////////
    // Test when configuring the proxy options using the Invocation.Builder object
    @Test
    public void testTunnelThroughProxyToHTTPEndpoint_Builder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Builder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        // TODO Enable after MockServer fix has been delivered
        // proxyClient.verify(request().withHeader("Proxy-Authorization", "Basic
        // amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }
}
