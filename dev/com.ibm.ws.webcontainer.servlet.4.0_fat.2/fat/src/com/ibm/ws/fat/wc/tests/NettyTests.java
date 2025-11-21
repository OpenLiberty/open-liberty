/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import  com.meterware.httpunit.WebResponse;
import com.ibm.ws.fat.wc.utils.WebUtil;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * NettyTests
 *
 */
@RunWith(FATRunner.class)
public class NettyTests {

    private static final Logger LOG = Logger.getLogger(NettyTests.class.getName());
    private static final String APP_NAME_COOKIE_TCK = "servlet_jsh_cookie_web";
    private static final String APP_NAME_COOKIE_NETTY = "NettyCookieTest";
    private static final String APP_NAME_REQEST_TCK = "servlet_jsh_httpservletrequest40_web";
    private static final String APP_NAME_REQUEST_NETTY = "NettyServletRequestTest";
    private static final String APP_NAME_READLISTENER_TCK = "servlet_jsh_readlistener_web";
    private static final String APP_NAME_READLISTENER_NETTY = "NettyReadListenerTest";
    private static final String APP_NAME_SERVERPUSH_TCK = "servlet_spec_serverpush_web";
    private static final String APP_NAME_SERVERPUSH_NETTY = "NettyServerPushTest";
    public static final String DELIMITER = "\r\n";
    public static final String ENCODING = "ISO-8859-1";

    @Server("servlet40_netty")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {

        ShrinkHelper.defaultDropinApp(server, APP_NAME_COOKIE_NETTY + ".war", "netty.cookie.servlets");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_REQUEST_NETTY + ".war", "netty.servlet.request.servlets");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_READLISTENER_NETTY + ".war", "netty.readlistener.servlets", "netty.readlistener.listeners");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_SERVERPUSH_NETTY + ".war", "netty.server.push.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(NettyTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     *
     * @throws Exception
     */
    //@Test
    public void testNettyVersion0Cookie_TCK() throws Exception {
        String url = "/" + APP_NAME_COOKIE_TCK + "/TestServlet?testname=getVersionVer0Test";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "name1=value1; Domain=" + _hostname + "; Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    /**
     *
     * @throws Exception
     */
    //@Test
    public void testNettyVersion1Cookie_TCK() throws Exception {
        String url = "/" + APP_NAME_COOKIE_TCK + "/TestServlet?testname=getVersionVer1Test";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    /**
     *
     * @throws Exception
     */
    //@Test
    public void testNettyVersion0Cookie_NETTY() throws Exception {
        String url = "/" + APP_NAME_COOKIE_NETTY + "/NettyCookieTestServlet?testname=getVersionVer0Test";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "name1=value1; Domain=" + _hostname + "; Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: \n" + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    /**
     *
     * @throws Exception
     */
    //@Test
    public void testNettyVersion1Cookie_NETTY() throws Exception {
        String url = "/" + APP_NAME_COOKIE_NETTY + "/NettyCookieTestServlet?testname=getVersionVer1Test";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: \n" + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    // @Test
    public void testNettyGetPath_TCK() throws Exception {
        String url = "/" + APP_NAME_COOKIE_TCK + "/TestServlet?testname=getPathTest";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    //@Test
    public void testNettyGetPath_NETTY() throws Exception {
        String url = "/" + APP_NAME_COOKIE_NETTY + "/NettyCookieTestServlet?testname=getPathTest";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/NettyCookieTest");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    //@Test
    public void testNettyGetDomain_TCK() throws Exception {
        String url = "/" + APP_NAME_COOKIE_TCK + "/TestServlet?testname=getDomainTest";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    //@Test
    public void testNettyGetDomain_NETTY() throws Exception {
        String url = "/" + APP_NAME_COOKIE_NETTY + "/NettyCookieTestServlet?testname=getDomainTest";
        LOG.info("url: " + url);

        String _hostname = server.getHostname();

        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("Cookie", "$Version=1; name1=value1; $Domain=" + _hostname + "; $Path=/servlet_jsh_cookie_web");

        WebResponse response = getResponse(url,
                                           headers);

        String text = response.getText();

        LOG.info("Response text: " + text);

        assertTrue("Response output did not contain: Test PASSED", text.contains("Test PASSED"));
    }

    //@Test
    public void testNettyRequestTrailerTest_TCK() throws Exception {

        Socket socket = null;
        OutputStream output;
        InputStream input;
        URL url;

        try {
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_REQEST_TCK
                          + "/TrailerTestServlet");
            LOG.info("PAN: url: " + url);

            socket = new Socket(url.getHost(), url.getPort());
            socket.setKeepAlive(true);
            output = socket.getOutputStream();

            String path = url.getPath();
            StringBuffer outputBuffer = new StringBuffer();
            outputBuffer.append("POST " + path + " HTTP/1.1" + DELIMITER);
            outputBuffer.append("Host: " + url.getHost() + DELIMITER);
            outputBuffer.append("Connection: keep-alive" + DELIMITER);
            outputBuffer.append("Content-Type: text/plain" + DELIMITER);
            outputBuffer.append("Transfer-Encoding: chunked" + DELIMITER);
            outputBuffer.append("Trailer: myTrailer, myTrailer2" + DELIMITER);
            outputBuffer.append(DELIMITER);
            outputBuffer.append("3" + DELIMITER);
            outputBuffer.append("ABC" + DELIMITER);
            outputBuffer.append("0" + DELIMITER);
            outputBuffer.append("myTrailer:foo");
            outputBuffer.append(DELIMITER);
            outputBuffer.append("myTrailer2:bar");
            outputBuffer.append(DELIMITER);
            outputBuffer.append(DELIMITER);

            byte[] outputBytes = outputBuffer.toString().getBytes(ENCODING);
            LOG.info("PAN: calling write on the socket OutputStream");
            output.write(outputBytes);

            LOG.info("PAN: calling flush on socket OutputStream");
            output.flush();

            LOG.info("PAN: getting the socket InputStream");
            input = socket.getInputStream();
            LOG.info("PAN: finished getting the socket InputStream");

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int read = 0;
            while ((read = input.read()) >= 0) {
                LOG.info("PAN: input.read >= 0, going to write: " + read);
                bytes.write(read);
                LOG.info("PAN: after write, going to read again.");
            }

            LOG.info("PAN: getting the response next");
            String response = new String(bytes.toByteArray());
            LOG.info("PAN: got the response! logging it next!");
            LOG.info(response);
            //  if (response.indexOf("isTrailerFieldsReady: true") < 0) {
            //LOG.info("isTrailerFieldsReady should be true");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.indexOf("isTrailerFieldsReady: true") < 0));
            //}

            //if (response.toLowerCase().indexOf("mytrailer=foo") < 0) {
            //LOG.info("failed to get trailer field: mytrailer=foo");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.toLowerCase().indexOf("mytrailer=foo") < 0));
            //}

            //if (response.toLowerCase().indexOf("mytrailer2=bar") < 0) {
            //LOG.info("failed to get trailer field: mytrailer=foo");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.toLowerCase().indexOf("mytrailer2=bar") < 0));
            //}
            //} catch (Exception e) {
            //    LOG.info("Caught exception: " + e.getMessage());
            //    e.printStackTrace();
            //throw new Fault("TrailerTest failed: ", e);
            LOG.info("PAN: done running test, time to cleanup");
        } finally {
            LOG.info("PAN: in finally");
            try {
                if (socket != null) {
                    LOG.info("PAN: socket not null, closing");
                    socket.close();
                }
            } catch (Exception e) {
                LOG.info("PAN: exception caught in finally");
            }
        }
    }

    //@Test
    public void testNettyRequestTrailerTest_NETTY() throws Exception {

        Socket socket = null;
        OutputStream output;
        InputStream input;
        URL url;

        try {
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_REQUEST_NETTY
                          + "/NettyServletRequestTestServlet");
            LOG.info("PAN: url: " + url);

            socket = new Socket(url.getHost(), url.getPort());
            socket.setKeepAlive(true);
            output = socket.getOutputStream();

            String path = url.getPath();
            StringBuffer outputBuffer = new StringBuffer();
            outputBuffer.append("POST " + path + " HTTP/1.1" + DELIMITER);
            outputBuffer.append("Host: " + url.getHost() + DELIMITER);
            outputBuffer.append("Connection: keep-alive" + DELIMITER);
            outputBuffer.append("Content-Type: text/plain" + DELIMITER);
            outputBuffer.append("Transfer-Encoding: chunked" + DELIMITER);
            outputBuffer.append("Trailer: myTrailer, myTrailer2" + DELIMITER);
            outputBuffer.append(DELIMITER);
            outputBuffer.append("3" + DELIMITER);
            outputBuffer.append("ABC" + DELIMITER);
            outputBuffer.append("0" + DELIMITER);
            outputBuffer.append("myTrailer:foo");
            outputBuffer.append(DELIMITER);
            outputBuffer.append("myTrailer2:bar");
            outputBuffer.append(DELIMITER);
            outputBuffer.append(DELIMITER);

            byte[] outputBytes = outputBuffer.toString().getBytes(ENCODING);
            LOG.info("PAN: calling write on the socket OutputStream");
            output.write(outputBytes);

            LOG.info("PAN: calling flush on socket OutputStream");
            output.flush();

            LOG.info("PAN: getting the socket InputStream");
            input = socket.getInputStream();
            LOG.info("PAN: finished getting the socket InputStream");

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int read = 0;
            while ((read = input.read()) >= 0) {
                LOG.info("PAN: input.read >= 0, going to write: " + read);
                bytes.write(read);
                LOG.info("PAN: after write, going to read again.");
            }

            LOG.info("PAN: getting the response next");
            String response = new String(bytes.toByteArray());
            LOG.info("PAN: got the response! logging it next!");
            LOG.info(response);
            //  if (response.indexOf("isTrailerFieldsReady: true") < 0) {
            //LOG.info("isTrailerFieldsReady should be true");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.indexOf("isTrailerFieldsReady: true") < 0));
            //}

            //if (response.toLowerCase().indexOf("mytrailer=foo") < 0) {
            //LOG.info("failed to get trailer field: mytrailer=foo");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.toLowerCase().indexOf("mytrailer=foo") < 0));
            //}

            //if (response.toLowerCase().indexOf("mytrailer2=bar") < 0) {
            //LOG.info("failed to get trailer field: mytrailer=foo");
            //throw new Fault("TrailerTest failed.");
            assertTrue(!(response.toLowerCase().indexOf("mytrailer2=bar") < 0));
            //}
            //} catch (Exception e) {
            //    LOG.info("Caught exception: " + e.getMessage());
            //    e.printStackTrace();
            //throw new Fault("TrailerTest failed: ", e);
            LOG.info("PAN: done running test, time to cleanup");
        } finally {
            LOG.info("PAN: in finally");
            try {
                if (socket != null) {
                    LOG.info("PAN: socket not null, closing");
                    socket.close();
                }
            } catch (Exception e) {
                LOG.info("PAN: exception caught in finally");
            }
        }
    }

    @Test
    public void TestNettyReadListener_nioInputTest_TCK() throws Exception {
        //int sleepInSeconds = Integer
        //                .parseInt(_props.getProperty("servlet_async_wait").trim());
        Boolean passed = true;

        String EXPECTED_RESPONSE = "=onDataAvailable|=Hello|=onDataAvailable|=World"
                                   + "|=onAllDataRead";

        BufferedReader input = null;
        BufferedWriter output = null;

        //String requestUrl = getContextRoot() + "/" + getServletName();
        URL url = null;

        try {
            //TSURL ctsURL = new TSURL();
            //url = ctsURL.getURL("http", _hostname, _port, requestUrl);
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_READLISTENER_TCK
                          + "/TestServlet");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            LOG.info("======= Connecting " + url.toExternalForm());
            conn.setRequestProperty("Content-type", "text/plain; charset=utf-8");
            conn.setChunkedStreamingMode(5);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            LOG.info("======= Header " + conn.toString());
            conn.connect();

            try {
                output = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                try {
                    String data = "Hello";
                    output.write(data);
                    output.flush();
                    Thread.sleep(4 * 1000); // PAN modified

                    data = "World";
                    output.write(data);
                    output.flush();
                    output.close();
                } catch (Exception ex) {
                    passed = false;
                    LOG.info("======= Exception sending message: " + ex.getMessage());
                }

                input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                StringBuffer message_received = new StringBuffer();

                while ((line = input.readLine()) != null) {
                    LOG.info("======= message received: " + line);
                    message_received.append(line);
                }
                passed = compareString(EXPECTED_RESPONSE,
                                       message_received.toString());
                LOG.info("PAN: EXPECTED_RESPONSE: " + EXPECTED_RESPONSE + " message_received: " + message_received.toString());

            } catch (Exception ex) {
                passed = false;
                LOG.info("Exception: " + ex.getMessage());
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (Exception ex) {
                    LOG.info("Fail to close BufferedReader" + ex.getMessage());
                }

                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (Exception ex) {
                    LOG.info("Fail to close BufferedWriter" + ex.getMessage());
                }
            }
        } catch (Exception ex3) {
            passed = false;
            LOG.info("Test" + ex3.getMessage());
        }

        if (!passed) {
            //throw new Fault("Test Failed.");
            LOG.info("PAN: TEST FAILED");
        }

        LOG.info("PAN: passed: " + passed);
        assertTrue(passed);
    }

    @Test
    public void TestNettyReadListener_nioInputTest_NETTY() throws Exception {
        //int sleepInSeconds = Integer
        //                .parseInt(_props.getProperty("servlet_async_wait").trim());
        Boolean passed = true;

        String EXPECTED_RESPONSE = "=onDataAvailable|=Hello|=onDataAvailable|=World"
                                   + "|=onAllDataRead";

        BufferedReader input = null;
        BufferedWriter output = null;

        //String requestUrl = getContextRoot() + "/" + getServletName();
        URL url = null;

        try {
            //TSURL ctsURL = new TSURL();
            //url = ctsURL.getURL("http", _hostname, _port, requestUrl);
            url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_READLISTENER_NETTY
                          + "/NettyReadListenerTestServlet");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            LOG.info("======= Connecting " + url.toExternalForm());
            conn.setRequestProperty("Content-type", "text/plain; charset=utf-8");
            conn.setChunkedStreamingMode(5);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            LOG.info("======= Header " + conn.toString());
            conn.connect();

            try {
                output = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                try {
                    String data = "Hello";
                    output.write(data);
                    output.flush();
                    Thread.sleep(4 * 1000); // PAN modified

                    data = "World";
                    output.write(data);
                    output.flush();
                    output.close();
                } catch (Exception ex) {
                    passed = false;
                    LOG.info("======= Exception sending message: " + ex.getMessage());
                }

                input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                StringBuffer message_received = new StringBuffer();

                while ((line = input.readLine()) != null) {
                    LOG.info("======= message received: " + line);
                    message_received.append(line);
                }
                passed = compareString(EXPECTED_RESPONSE,
                                       message_received.toString());
                LOG.info("PAN: EXPECTED_RESPONSE: " + EXPECTED_RESPONSE + " message_received: " + message_received.toString());

            } catch (Exception ex) {
                passed = false;
                LOG.info("Exception: " + ex.getMessage());
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (Exception ex) {
                    LOG.info("Fail to close BufferedReader" + ex.getMessage());
                }

                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (Exception ex) {
                    LOG.info("Fail to close BufferedWriter" + ex.getMessage());
                }
            }
        } catch (Exception ex3) {
            passed = false;
            LOG.info("Test" + ex3.getMessage());
        }

        if (!passed) {
            //throw new Fault("Test Failed.");
            LOG.info("PAN: TEST FAILED");
        }

        LOG.info("PAN: passed: " + passed);
        assertTrue(passed);
    }

    //@Test
    public void TestNettyServerPush_serverPushMiscTest_TCK() throws Exception { // PAN: throws Fault {
        String requestURI = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_SERVERPUSH_TCK
                            + "/TestServlet6";
        Map<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        headers.put("baz", "qux");
        List<HttpResponse<String>> responses = sendRequest(headers, null, null, requestURI);
        HttpResponse<String> pushResp = null;
        HttpRequest pushReq = null;

        for (HttpResponse<String> response : responses) {
            if (response.uri().toString().indexOf("index.html") >= 0) {
                pushResp = response;
                pushReq = response.request();
            }
        }

        if (pushResp == null) {
            //throw new Fault("can not get push response");
            LOG.info("PAN: can not get push response");
        }

        LOG.info(
                 "expected header: h1=v1, foo=v2; expected querysting: querystring=1&querystring=2");
        Map<String, List<String>> pushHeaders = pushReq.headers().map();
        LOG.info("Current push request header: " + pushHeaders);
        if (!(pushHeaders.get("h1") != null
              && pushHeaders.get("h1").get(0).equals("v1"))) {
            //throw new Fault("test fail: could not find header h1=v1");
            LOG.info("PAN: test fail: could not find header h1=v1");
        }

        if (!(pushHeaders.get("foo") != null
              && pushHeaders.get("foo").get(0).equals("v2"))) {
            //throw new Fault("test fail: could not find header foo=v2");
            LOG.info("PAN: test fail: could not find header foo=v2");
        }

        if (pushHeaders.get("baz") != null) {
            //throw new Fault("test fail");
            LOG.info("PAN: test fail");
        }

        LOG.info("Current query string of the push request is "
                 + pushReq.uri().getQuery());
        if (pushReq.uri().getQuery() == null || pushReq.uri()
                        .getQuery()
                        .indexOf("querystring=1&querystring=2") < 0) {
            //throw new Fault("test fail: could not find correct querystring \"querystring=1&querystring=2\"");
            LOG.info("PAN: test fail: could not find correct querystring \"querystring=1&querystring=2\"");
        }
    }

    //@Test
    public void TestNettyServerPush_serverPushMiscTest_NETTY() throws Exception { // PAN: throws Fault {
        String requestURI = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_SERVERPUSH_NETTY
                            + "/TestServlet6";
        Map<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        headers.put("baz", "qux");
        List<HttpResponse<String>> responses = sendRequest(headers, null, null, requestURI);
        HttpResponse<String> pushResp = null;
        HttpRequest pushReq = null;

        for (HttpResponse<String> response : responses) {
            if (response.uri().toString().indexOf("index.html") >= 0) {
                pushResp = response;
                pushReq = response.request();
            }
        }

        if (pushResp == null) {
            //throw new Fault("can not get push response");
            LOG.info("PAN: can not get push response");
        }

        LOG.info(
                 "expected header: h1=v1, foo=v2; expected querysting: querystring=1&querystring=2");
        Map<String, List<String>> pushHeaders = pushReq.headers().map();
        LOG.info("Current push request header: " + pushHeaders);
        if (!(pushHeaders.get("h1") != null
              && pushHeaders.get("h1").get(0).equals("v1"))) {
            //throw new Fault("test fail: could not find header h1=v1");
            LOG.info("PAN: test fail: could not find header h1=v1");
        }

        if (!(pushHeaders.get("foo") != null
              && pushHeaders.get("foo").get(0).equals("v2"))) {
            //throw new Fault("test fail: could not find header foo=v2");
            LOG.info("PAN: test fail: could not find header foo=v2");
        }

        if (pushHeaders.get("baz") != null) {
            //throw new Fault("test fail");
            LOG.info("PAN: test fail");
        }

        LOG.info("Current query string of the push request is "
                 + pushReq.uri().getQuery());
        if (pushReq.uri().getQuery() == null || pushReq.uri()
                        .getQuery()
                        .indexOf("querystring=1&querystring=2") < 0) {
            //throw new Fault("test fail: could not find correct querystring \"querystring=1&querystring=2\"");
            LOG.info("PAN: test fail: could not find correct querystring \"querystring=1&querystring=2\"");
        }
    }

    //@Test
    public void testNettyServerPush_serverPushSessionTest_tck() throws Exception {
        try {
            WebUtil.Response response = null;

            String requestURI = "/" + APP_NAME_SERVERPUSH_TCK + "/TestServlet3?generateSession=true";
            LOG.info("Sending request \"" + requestURI + "\"");

            //response = WebUtil.sendRequest("GET", InetAddress.getByName(server.getHostname()),
            //  server.getHttpDefaultPort(), tsurl.getRequest(requestURI), null, null);

            response = WebUtil.sendRequest("GET", InetAddress.getByName(server.getHostname()),
                                           server.getHttpDefaultPort(), "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + requestURI, null, null);

            LOG.info("The new sessionid is :" + response.content);

            System.out.println("PAN: jsessionid: " + response.content);
            System.out.println("PAN: jsessionid trimmed: " + response.content.trim());

            // Check that the page was found (no error).
            if (response.isError()) {
                LOG.info("Could not find " + requestURI);
                //throw new Fault("serverPushSessionTest failed.");
                LOG.info("serverPushSessionTest failed.");
            }

            requestURI = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME_SERVERPUSH_TCK
                         + "/TestServlet3;jsessionid=" + response.content.trim();
            LOG.info("Sending request \"" + requestURI + "\"");
            List<HttpResponse<String>> responses = sendRequest(new HashMap<>(), null,
                                                               null, requestURI);
            String responseStr = responses.get(0).body();

            LOG.info("The test result :" + responseStr);
            if (responseStr.indexOf("Test success") < 0) {
                //throw new Fault("serverPushSessionTest failed.");
                LOG.info("serverPushSessionTest failed.");
            }
        } catch (Exception e) {
            LOG.info("Caught exception: " + e.getMessage());
            e.printStackTrace();
            //throw new Fault("serverPushSessionTest failed: ", e);
            LOG.info("serverPushSessionTest failed: " + e);
        }
    }

    private WebResponse getResponse(String uri, HashMap<String, String> headers) throws Exception {
        WebConversation wc = new WebConversation();

        if (headers != null && !headers.isEmpty()) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                wc.setHeaderField(key, headers.get(key));
            }
        }

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + uri;
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);

        return response;

    }

    private boolean compareString(String expected, String actual) {
        String[] list_expected = expected.split("[|]");
        boolean found = true;
        for (int i = 0, n = list_expected.length, startIdx = 0, bodyLength = actual
                        .length(); i < n; i++) {

            String search = list_expected[i];
            if (startIdx >= bodyLength) {
                startIdx = bodyLength;
            }

            int searchIdx = actual.toLowerCase()
                            .indexOf(search.toLowerCase(),
                                     startIdx);

            LOG.info("[compareString] Scanning response for search string: '{}' starting at index " + "location: {}" + " : " + search + " : " + startIdx);
            if (searchIdx < 0) {
                found = false;
                StringBuffer sb = new StringBuffer(255);
                sb.append("[compareString] Unable to find the following search string in the server's response: '")
                                .append(search)
                                .append("' at index: ")
                                .append(startIdx)
                                .append("\n[compareString] Server's response:\n")
                                .append("-------------------------------------------\n")
                                .append(actual)
                                .append("\n-------------------------------------------\n");
                LOG.info(sb.toString());
                break;
            }

            LOG.info("[compareString] Found search string: '{}' at index '{}' in the server's response" + " : " + search + " : " + searchIdx);
            // the new searchIdx is the old index plus the lenght of the
            // search string.
            startIdx = searchIdx + search.length();
        }
        return found;
    }

    private List<HttpResponse<String>> sendRequest(Map<String, String> headers,
                                                   Authenticator auth, CookieManager cm, String requestURI) {//throws Fault {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (auth != null)
            builder.authenticator(auth);
        if (cm != null)
            builder.cookieHandler(cm);

        HttpClient client = builder.version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .executor(Executors.newFixedThreadPool(4))
                        .build();;

        List<HttpResponse<String>> responses = new ArrayList<HttpResponse<String>>();

        try {
            // GET
            HttpRequest.Builder requestBuilder = HttpRequest
                            .newBuilder(new URI(requestURI))
                            .version(HttpClient.Version.HTTP_2);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                requestBuilder.setHeader(e.getKey(), e.getValue());
            }

            HttpRequest request = requestBuilder.GET().build();

            ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<String>>> promisesMap = new ConcurrentHashMap<>();

            java.util.function.Function<HttpRequest, HttpResponse.BodyHandler<String>> promiseHandler = (HttpRequest req) -> {
                String msg = " - " + req.uri();
                LOG.info(msg);
                return HttpResponse.BodyHandlers.ofString();
            };

            CompletableFuture<HttpResponse<String>> sendAsync = client.sendAsync(
                                                                                 request, HttpResponse.BodyHandlers.ofString(),
                                                                                 HttpResponse.PushPromiseHandler.of(promiseHandler, promisesMap));

            // Original response
            HttpResponse<String> response = sendAsync.join();
            printResponse(response);
            responses.add(response);

            // Pushed responses
            for (HttpRequest key : promisesMap.keySet()) {
                CompletableFuture<HttpResponse<String>> completableFuture = promisesMap.get(key);
                response = completableFuture.get();
                printResponse(response);
                responses.add(response);
            }
        } catch (Exception e) {
            //throw new Fault("Test fail", e);
            LOG.info("PAN: test fail: exception: " + e);
        }
        return responses;
    }

    private void printResponse(HttpResponse<String> response) {
        LOG.info("ResponseURI:     " + response.uri());
        LOG.info("ResponseBody:     " + response.body());
        LOG.info("HTTP-Version: " + response.version());
        LOG.info("Statuscode:   " + response.statusCode());
        LOG.info("Header:");
        response.headers()
                        .map()
                        .forEach(
                                 (header, values) -> LOG.info("  " + header + " = " + values.stream()
                                                 .map(String::trim)
                                                 .reduce(String::concat)
                                                 .orElse("hallo")));
    }

}
