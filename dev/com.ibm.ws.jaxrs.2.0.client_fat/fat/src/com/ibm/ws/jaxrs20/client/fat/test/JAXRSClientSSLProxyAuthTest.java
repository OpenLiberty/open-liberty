/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests are used to determine whether a JAX-RS 2.0 client can reach a
 * remote endpoint when using an authenticating proxy server.
 *
 * Note that the password used in the servlet will be different than what is
 * passed in here - this is so that the test case can check that the
 * password specified in the actual Client APIs are not logged, even when
 * tracing is enabled.
 */
@RunWith(FATRunner.class)
public class JAXRSClientSSLProxyAuthTest extends AbstractTest {

    private final static Class<?> c = JAXRSClientSSLProxyAuthTest.class;

    @Server("jaxrs20.client.ProxyAuthTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclientproxyAuth";
    private final static String target = appname + "/ClientTestServlet";

    private static int proxyPort;
    private static ClientAndServer proxy;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.client.jaxrsclientproxyauth.client",
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclientproxyauth.service");

        System.setProperty("javax.net.ssl.keyStore", "publish/servers/jaxrs20.client.ProxyAuthTest/resources/security/key.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "passw0rd");
        System.setProperty("javax.net.ssl.trustStore", "publish/servers/jaxrs20.client.ProxyAuthTest/resources/security/trust.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        ConfigurationProperties.useBouncyCastleForKeyAndCertificateGeneration(true);
        ConfigurationProperties.forwardProxyTLSX509CertificatesTrustManagerType("ANY");
        ConfigurationProperties.proxyAuthenticationUsername("jaxrsUser");
        ConfigurationProperties.proxyAuthenticationPassword("myPa$$word");
        ConfigurationProperties.proxyAuthenticationRealm("foo");
        ConfigurationProperties.attemptToProxyIfNoMatchingExpectation(true);
        
        proxyPort = Integer.getInteger("member_3.http");
        proxy = ClientAndServer.startClientAndServer(proxyPort);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        proxy.stop();
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
        Log.info(c, "preTest", "Mock Server Proxy listening on port " + proxyPort);
    }

    @After
    public void afterTest() {
        Log.info(c, "afterTest", "Proxy Server log messages: " + proxy.retrieveLogMessages(request()));
        proxy.reset();
        serverRef = null;
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpoint() throws Exception {
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

        assertTrue(proxy.retrieveLogMessages(request()).contains("received request"));
    }

    @SkipForRepeat(NO_MODIFICATION) // jaxrs-2.0 currently does not implement HTTPS-based proxy authentication
    @Test
    public void testTunnelThroughProxyToHTTPSEndpoint() throws Exception {
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

        assertTrue(proxy.retrieveLogMessages(request()).contains("received request"));
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointInvalidUsername() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser1");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        proxy.when(request().withHeader(header(not("Proxy-Authorization"))))
             .respond(response().withStatusCode(407).withHeader("Proxy-Authenticate", "Basic realm=\"foo\""));
        proxy.when(request().withHeader(header(string("Proxy-Authorization"), not("amF4cnNVc2VyOm15UGEkJHdvcmQ="))))
             .respond(response().withStatusCode(407).withHeader("Proxy-Authenticate", "Basic realm=\"foo\""));
        
        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p, 
                             "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required", // <= EE8
                             "[Proxy Error]:jakarta.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required"); // EE9

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError() throws Exception {
        //Server will respond to any request with 407 to mock a proxy authentication failure
        proxy.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p,
                             "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required", // <= EE8
                             "[Proxy Error]:jakarta.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required"); //EE9

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "1000");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_ClientBuilder", p,
                             "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ", // <= EE8
                             "[Proxy Error]:jakarta.ws.rs.ProcessingException: RESTEASY004655: Unable to invoke request"); // EE9

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }
}