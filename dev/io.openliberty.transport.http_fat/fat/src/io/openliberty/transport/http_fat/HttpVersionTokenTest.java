/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that the HTTP channel correctly handles malformed request-line
 * version tokens containing unexpected characters after the minor version
 * digit, and that HTTP/1.0 connection semantics are applied correctly.
 */
@RunWith(FATRunner.class)
public class HttpVersionTokenTest {

    private static final Logger LOG = Logger.getLogger(HttpVersionTokenTest.class.getName());
    private static final String APP_NAME = "obsFoldApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/ObsFoldEchoServlet";
    private static final String HOST_HEADER_NAME = "obsfold1.example.test";
    private static final int SOCKET_TIMEOUT_MS = 2000;

    @Server("ObsoleteLineFolding")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http_fat.obsfold.servlets");
        server.addInstalledAppForValidation(APP_NAME);
        configurePorts();
        server.startServer();
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");
        String tcpChannelMessage = server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint");
        LOG.info("HTTP transport: " + tcpChannelMessage);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // -----------------------------------------------------------------------
    // HTTP version token handling Test
    // -----------------------------------------------------------------------

    /**
     * A request-line with version token is "HTTP/1.0 1" — a space followed
     * by an extra digit after the minor version — must result in exactly one
     * response.  The trailing bytes after the first request must not be
     * dispatched as a second request.
     */
    @Test
    public void malformedVersionTokenWithTrailingDigitYieldsOneResponse() throws Exception {
        String secondRequestMarker = "req=second-v10trail1";
        String payload = "GET " + SERVLET_PATH + " HTTP/1.0 1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "\r\n" +
                         "GET " + SERVLET_PATH + "?req=second-v10trail1 HTTP/1.1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "Connection: close\r\n" +
                         "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for malformedVersionTokenWithTrailingDigitYieldsOneResponse:\n" + response);

        List<String> statusLines = statusLines(response);
        assertEquals(
            "Expected exactly one HTTP response for a malformed version token. " +
            "Full response:\n" + response,
            1, statusLines.size());
        // verify the single response is a rejection, not a successful dispatch
        assertSingleStatusContains(response, "400");
        assertFalse(
            "The second request must not have been dispatched. " +
            "Full response:\n" + response,
            response.contains(secondRequestMarker));
    }

    /**
     * Variant with a different trailing digit — confirms the behaviour is
     * not specific to any one character value.
     */
    @Test
    public void malformedVersionTokenWithOtherTrailingDigitYieldsOneResponse() throws Exception {
        String secondRequestMarker = "req=second-v10trail2";
        String payload = "GET " + SERVLET_PATH + " HTTP/1.0 2\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "\r\n" +
                         "GET " + SERVLET_PATH + "?req=second-v10trail2 HTTP/1.1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "Connection: close\r\n" +
                         "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for malformedVersionTokenWithOtherTrailingDigitYieldsOneResponse:\n" + response);

        List<String> statusLines = statusLines(response);
        assertEquals(
            "Expected exactly one HTTP response for a malformed version token. " +
            "Full response:\n" + response,
            1, statusLines.size());
        // verify the single response is a rejection, not a successful dispatch
        assertSingleStatusContains(response, "400");
    
        assertFalse(
            "The second request must not have been dispatched. " +
            "Full response:\n" + response,
            response.contains(secondRequestMarker));
        
    }

    /**
     * Control: a canonical HTTP/1.0 request must continue to work normally
     * and return exactly one response.
     */
    @Test
    public void canonicalHttp10RequestYieldsOneResponse() throws Exception {
        String payload = "GET " + SERVLET_PATH + " HTTP/1.0\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for canonicalHttp10RequestYieldsOneResponse:\n" + response);

        List<String> statusLines = statusLines(response);
        assertEquals(
            "Canonical HTTP/1.0 request must produce exactly one response. " +
            "Full response:\n" + response,
            1, statusLines.size());
    }

    /**
     * Variant with a unkonwn minor version on the HTTP token.
     * The unkown version should be responded with 505 
     */
    @Test
    public void unsupportedMinorVersionReturns505AndNoSecondResponse() throws Exception {
        String secondRequestMarker = "req=second-v12";
        String payload = "GET " + SERVLET_PATH + " HTTP/1.2\r\n" +
                        "Host: " + hostHeader() + "\r\n" +
                        "\r\n" +
                        "GET " + SERVLET_PATH + "?req=second-v12 HTTP/1.1\r\n" +
                        "Host: " + hostHeader() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for unsupportedMinorVersionReturns505AndNoSecondResponse:\n" + response);

        assertSingleStatusContains(response, "505");
        assertFalse("Unsupported version must not be reflected in response.\n" + response,
                    response.contains("HTTP/1.2"));
        assertFalse("Second request must not have been dispatched.\n" + response,
                    response.contains(secondRequestMarker));
    }

    /**
     * Variant with a unkonwn major version on the HTTP token.
     * The unkown version should be responded with 505 
     */
    @Test
    public void unsupportedMajorVersionReturns505() throws Exception {
        String payload = "GET " + SERVLET_PATH + " HTTP/9.0\r\n" +
                        "Host: " + hostHeader() + "\r\n" +
                        "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for unsupportedMajorVersionReturns505:\n" + response);

        assertSingleStatusContains(response, "505");
        assertFalse(response.contains("HTTP/9.0"));
    }

    /**
     * Variant with a malformed version on the HTTP token.
     * The malformed token should be rejected with a 400
     */
    @Test
    public void malformedMultiDigitMinorVersionReturns400() throws Exception {
        String payload = "GET " + SERVLET_PATH + " HTTP/1.10\r\n" +
                        "Host: " + hostHeader() + "\r\n" +
                        "\r\n";

        String response = sendRawRequest(payload);
        LOG.info("Response for malformedMultiDigitMinorVersionReturns400:\n" + response);

        assertSingleStatusContains(response, "400");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String sendRawRequest(String request) throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            socket.shutdownOutput();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    int read = in.read(buffer);
                    if (read < 0) break;
                    response.write(buffer, 0, read);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            return response.toString(StandardCharsets.ISO_8859_1.name());
        }
    }

    private static String hostHeader() {
        return HOST_HEADER_NAME + ":" + server.getHttpDefaultPort();
    }

    private static List<String> statusLines(String response) {
        List<String> result = new ArrayList<>();
        for (String line : response.split("\\r?\\n")) {
            if (line.startsWith("HTTP/")) {
                result.add(line);
            }
        }
        return result;
    }

    private static void assertSingleStatusContains(String response, String code) {
        String statusLine = onlyStatusLine(response);
        assertTrue("Expected status containing " + code + " but got: " + statusLine,
        statusLine.contains(" " + code + " ") || statusLine.contains(" " + code + "\r")
                || statusLine.endsWith(" " + code));
    }

    private static String onlyStatusLine(String response) {
        List<String> statusLines = statusLines(response);
        assertEquals("Expected exactly one HTTP response.\n" + response, 1, statusLines.size());
        return statusLines.get(0);
    }

    private static void configurePorts() throws Exception {
        int httpPort = Integer.getInteger("HTTP_default", server.getHttpDefaultPort());
        if (httpPort == 0) httpPort = 8010;
        int httpsPort = Integer.getInteger("HTTP_default.secure", server.getHttpDefaultSecurePort());
        if (httpsPort == 0) httpsPort = 8020;
        server.setHttpDefaultPort(httpPort);
        server.setHttpDefaultSecurePort(httpsPort);

        RemoteFile bootstrapPropertiesFile = server.getServerBootstrapPropertiesFile();
        Properties props = new Properties();
        if (bootstrapPropertiesFile.exists()) {
            try (InputStream in = bootstrapPropertiesFile.openForReading()) {
                props.load(in);
            }
        }
        props.setProperty("bvt.prop.HTTP_default", String.valueOf(httpPort));
        props.setProperty("bvt.prop.HTTP_default.secure", String.valueOf(httpsPort));
        try (OutputStream out = bootstrapPropertiesFile.openForWriting(false)) {
            props.store(out, "");
        }
    }

}