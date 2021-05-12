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
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests are used to determine whether a JAX-RS 2.0 client can reach a
 * remote enpoint when using an authenticating proxy server.
 *
 * Note that the password used in the servlet will be different than what is
 * passed in here - this is so that the test case can check that the
 * password specified in the actual Client APIs are not logged, even when
 * tracing is enabled.
 */
@SkipForRepeat("EE9_FEATURES") // Continue to skip this test for EE9 as proxy authority (properties com.ibm.ws.jaxrs.client.proxy.*) is not supported yet
@RunWith(FATRunner.class)
public class JAXRSClientSSLProxyAuthTest extends AbstractTest {

    private final static Class<?> c = JAXRSClientSSLProxyAuthTest.class;

    @Server("jaxrs20.client.ProxyAuthTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclientproxyAuth";
    private final static String target = appname + "/ClientTestServlet";

    private static int proxyPort;
    private static int mockPort;
    private static ClientAndServer proxy;
    private static ClientAndServer mock;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclientproxyauth.client",
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclientproxyauth.service");

        System.setProperty("javax.net.ssl.keyStore", "publish/servers/jaxrs20.client.ProxyAuthTest/resources/security/key.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "passw0rd");
        System.setProperty("javax.net.ssl.trustStore", "publish/servers/jaxrs20.client.ProxyAuthTest/resources/security/trust.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        proxyPort = Integer.getInteger("member_3.http");
        proxy = ClientAndServer.startClientAndServer(proxyPort);
        
        mockPort = Integer.getInteger("member_4.http");
        mock = ClientAndServer.startClientAndServer(mockPort);

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
        mock.stop();
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
        serverRef = null;

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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

    @Test
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_ClientBuilder() throws Exception {
        //Server will respond to any request with 407 to mock a proxy authentication failure
        mock.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_ClientBuilder", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

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
        p.put("timeout", "1000");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_ClientBuilder", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

    @Test
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_Client() throws Exception {
        //Server will respond to any request with 407 to mock a proxy authentication failure
        mock.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Client", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointTimeout_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "1000");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_Client", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

    @Test
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word
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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    @Test
    public void testProxyReturnsAuthError_WebTarget() throws Exception {
        //Server will respond to any request with 407 to mock a proxy authentication failure
        mock.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_WebTarget", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
    }

    @Test
    public void testTunnelThroughProxyToHTTPEndpointTimeout_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "1000");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_WebTarget", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

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

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(false)); //jaxrsUser:myPa$$word
    }

    //@Test
    public void testTunnelThroughProxyToHTTPSEndpoint_Builder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("secPort", "" + server.getHttpDefaultSecurePort());

        this.runTestOnServer(target, "testProxyToHTTPS_Builder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyOm15UGEkJHdvcmQ=").withSecure(true)); //jaxrsUser:myPa$$word
    }

    //@Test
    public void testTunnelThroughProxyToHTTPEndpointInvalidUsername_Builder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser1");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Builder", p, "[Basic Resource]:helloRochester");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));

        proxy.verify(request().withHeader("Proxy-Authorization", "Basic amF4cnNVc2VyMTpteVBhJCR3b3Jk").withSecure(false)); //jaxrsUser1:myPa$$word
    }

    //@Test // TODO: Enable after fixing Open-Liberty issue 227
    public void testProxyReturnsAuthError_Builder() throws Exception {
        //Server will respond to any request with 407 to mock a proxy authentication failure
        mock.when(request()).respond(response().withStatusCode(407));

        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + mockPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");

        this.runTestOnServer(target, "testProxyToHTTP_Builder", p, "[Proxy Error]:javax.ws.rs.ClientErrorException: HTTP 407 Proxy Authentication Required");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }

    //@Test // TODO: decide should timeouts be allowed to be configured in the Invocation.Builder?
    // if so, we need to implement that functionality, then uncomment this test
    public void testTunnelThroughProxyToHTTPEndpointTimeout_Builder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "helloRochester");
        p.put("proxyhost", "localhost");
        p.put("proxyport", "" + proxyPort);
        p.put("proxytype", "HTTP");
        p.put("proxyusername", "jaxrsUser");
        p.put("proxypassword", "USE_PASSWORD_FROM_SERVLET");
        p.put("timeout", "1000");

        this.runTestOnServer(target, "testProxyToHTTPTimeout_Builder", p, "[Proxy Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: ");

        assertEquals("Proxy Password value printed in trace output", 0, server.findStringsInLogsAndTraceUsingMark("myPa\\$\\$word").size());
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs/trace.log"));
    }
}
