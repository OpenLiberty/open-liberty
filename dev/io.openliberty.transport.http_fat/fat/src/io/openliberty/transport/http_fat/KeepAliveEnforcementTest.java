/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for the {@code keepAliveEnabled} and {@code maxKeepAliveRequests}
 * httpOptions attributes.
 *
 * <p>Both attributes are enforced by {@code LibertyHttpKeepAliveHandler}, which
 * marks the inbound request {@code Connection: close} so that the response
 * carries {@code Connection: close} and the server then closes the socket.
 *
 * <p>Every test talks to the server over a single raw {@link Socket}, because
 * the behavior under test is what happens to a <em>persistent</em> connection:
 * does it stay open, or does the server close it, and after which request?
 * A higher-level client such as HttpURLConnection would hide that by silently
 * opening a new connection.
 *
 * <p>The test servlet always sets a {@code Content-Length}, so each response can
 * be read off the socket without parsing chunked encoding.
 *
 * <p>The tests that send a spoofed {@code x-http2-stream-id} header guard against
 * a bypass: an earlier version of the handler treated that header as proof of
 * HTTP/2 and skipped enforcement. A client controls its own request headers, so
 * the header must have no effect on an HTTP/1.1 connection.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class KeepAliveEnforcementTest {

    private static final Logger LOG = Logger.getLogger(KeepAliveEnforcementTest.class.getName());

    private static final String APP_NAME = "keepAliveApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/KeepAliveServlet";

    /** The header Netty's HTTP/2-to-HTTP/1 adapter adds. A client can also send it. */
    private static final String SPOOFED_STREAM_ID_HEADER = "x-http2-stream-id: 3";

    /** How long to wait for a response or for the server to close the socket. */
    private static final int RESPONSE_TIMEOUT_MS = 10000;

    /**
     * How long to wait before concluding that the server has left the socket
     * open. Shorter than {@link #RESPONSE_TIMEOUT_MS} because a connection that
     * stays open always waits out the full time.
     */
    private static final int STAYS_OPEN_TIMEOUT_MS = 1500;

    @Server("KeepAlive")
    public static LibertyServer server;

    /** True when the current test changed the server configuration and needs it restored. */
    private boolean configChanged;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http.keepalive.servlet");
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /** Save the default configuration so each test can change it and put it back. */
    @Before
    public void saveConfiguration() throws Exception {
        configChanged = false;
        server.saveServerConfiguration();
    }

    /** Restore the default configuration, but only if the test changed it. */
    @After
    public void restoreConfiguration() throws Exception {
        if (configChanged) {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    /**
     * Control test. With the default configuration (keep-alive enabled, no
     * request limit) several requests on one socket must all succeed, none of
     * the responses may ask for the connection to be closed, and the server
     * must leave the socket open.
     *
     * <p>Without this test the other tests would still pass if the handler
     * closed every connection, since they only prove that connections close.
     */
    @Test
    public void defaultConfig_connectionStaysOpen() throws Exception {
        try (RawConnection connection = new RawConnection()) {
            for (int i = 1; i <= 3; i++) {
                connection.sendGet(i);
                RawResponse response = connection.readResponse();
                LOG.info("defaultConfig response " + i + ":\n" + response);

                assertEquals("Request " + i + " should succeed", 200, response.statusCode);
                assertFalse("Request " + i + " must not ask to close the connection with default config",
                            response.isConnectionClose());
                assertTrue("Response " + i + " should echo its request number", response.body.contains("req=" + i));
            }
            assertFalse("The server must leave the connection open with default config",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
        }
    }

    /**
     * With {@code keepAliveEnabled=false} the very first response must carry
     * {@code Connection: close} and the server must then close the socket.
     */
    @Test
    public void keepAliveDisabled_closesAfterFirstResponse() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1);
            RawResponse response = connection.readResponse();
            LOG.info("keepAliveDisabled response:\n" + response);

            assertEquals("The request itself should still succeed", 200, response.statusCode);
            assertTrue("keepAliveEnabled=false must add Connection: close to the response",
                       response.isConnectionClose());
            assertTrue("The server must close the connection after the response",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /**
     * With {@code maxKeepAliveRequests=3}, responses 1 and 2 must leave the
     * connection open and response 3 must carry {@code Connection: close}
     * followed by the server closing the socket.
     */
    @Test
    public void maxKeepAliveRequests_closesOnNthResponse() throws Exception {
        setKeepAliveOptions(null, 3);

        try (RawConnection connection = new RawConnection()) {
            for (int i = 1; i <= 2; i++) {
                connection.sendGet(i);
                RawResponse response = connection.readResponse();
                LOG.info("maxKeepAliveRequests response " + i + ":\n" + response);

                assertEquals("Request " + i + " should succeed", 200, response.statusCode);
                assertFalse("Response " + i + " of 3 must not close the connection", response.isConnectionClose());
            }
            assertFalse("The connection must still be open after request 2 of 3",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));

            connection.sendGet(3);
            RawResponse third = connection.readResponse();
            LOG.info("maxKeepAliveRequests response 3:\n" + third);

            assertEquals("Request 3 should succeed", 200, third.statusCode);
            assertTrue("Response 3 of 3 must carry Connection: close", third.isConnectionClose());
            assertTrue("The server must close the connection after the limit is reached",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /**
     * Regression test for the header-spoofing bypass, against
     * {@code keepAliveEnabled=false}. The request sends
     * {@code x-http2-stream-id} on an HTTP/1.1 connection. The connection is
     * HTTP/1.1, so the header must be ignored and the connection must still be
     * closed.
     */
    @Test
    public void spoofedStreamIdHeader_keepAliveDisabled_stillCloses() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1, SPOOFED_STREAM_ID_HEADER);
            RawResponse response = connection.readResponse();
            LOG.info("spoofed keepAliveDisabled response:\n" + response);

            assertEquals("The request itself should still succeed", 200, response.statusCode);
            assertTrue("A spoofed x-http2-stream-id header must not bypass keepAliveEnabled=false",
                       response.isConnectionClose());
            assertTrue("The server must still close the connection",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /**
     * Regression test for the header-spoofing bypass, against
     * {@code maxKeepAliveRequests}. Every request carries the spoofed header, yet
     * the limit of 2 must still be counted and enforced on the second request.
     */
    @Test
    public void spoofedStreamIdHeader_maxRequests_stillCloses() throws Exception {
        setKeepAliveOptions(null, 2);

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1, SPOOFED_STREAM_ID_HEADER);
            RawResponse first = connection.readResponse();
            LOG.info("spoofed maxRequests response 1:\n" + first);
            assertEquals("Request 1 should succeed", 200, first.statusCode);
            assertFalse("Response 1 of 2 must not close the connection", first.isConnectionClose());

            connection.sendGet(2, SPOOFED_STREAM_ID_HEADER);
            RawResponse second = connection.readResponse();
            LOG.info("spoofed maxRequests response 2:\n" + second);
            assertEquals("Request 2 should succeed", 200, second.statusCode);
            assertTrue("A spoofed x-http2-stream-id header must not stop the request limit being enforced",
                       second.isConnectionClose());
            assertTrue("The server must close the connection once the limit is reached",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    // ---------------------------------------------------------------------
    // Configuration helper
    // ---------------------------------------------------------------------

    /**
     * Sets the keep-alive httpOptions on the default endpoint and waits for the
     * server to apply them. A null argument leaves that attribute unset so the
     * default applies.
     *
     * @param keepAliveEnabled value for {@code keepAliveEnabled}, or null for the default
     * @param maxRequests      value for {@code maxKeepAliveRequests}, or null for the default
     */
    private void setKeepAliveOptions(Boolean keepAliveEnabled, Integer maxRequests) throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        HttpEndpoint endpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        if (keepAliveEnabled != null) {
            endpoint.getHttpOptions().setKeepAliveEnabled(keepAliveEnabled);
        }
        if (maxRequests != null) {
            endpoint.getHttpOptions().setMaxKeepAliveRequests(maxRequests);
        }
        LOG.info("Updating server configuration: " + endpoint);

        configChanged = true;
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false,
                                                 "CWWKT0016I:.*" + APP_NAME + ".*");
    }

    // ---------------------------------------------------------------------
    // Raw HTTP/1.1 client
    // ---------------------------------------------------------------------

    /** A parsed HTTP response: status code, lower-cased header names, and body. */
    private static class RawResponse {
        final int statusCode;
        final Map<String, String> headers = new HashMap<>();
        String body = "";

        RawResponse(int statusCode) {
            this.statusCode = statusCode;
        }

        /** True when the response carries {@code Connection: close}. */
        boolean isConnectionClose() {
            String connection = headers.get("connection");
            return connection != null && connection.trim().equalsIgnoreCase("close");
        }

        @Override
        public String toString() {
            return "status=" + statusCode + " headers=" + headers + " body=" + body.trim();
        }
    }

    /**
     * One persistent TCP connection to the server. The same input stream is
     * reused for every response, so bytes read ahead for one response are never
     * lost before the next is parsed.
     */
    private static class RawConnection implements AutoCloseable {
        private final Socket socket;
        private final OutputStream out;
        private final InputStream in;

        RawConnection() throws IOException {
            socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
            out = socket.getOutputStream();
            in = new BufferedInputStream(socket.getInputStream());
        }

        /**
         * Sends an HTTP/1.1 GET to the test servlet. No {@code Connection} header
         * is sent, so the request itself asks for a persistent connection.
         *
         * @param requestNumber echoed back by the servlet as {@code req=N}
         * @param extraHeaders  additional raw header lines, for example a spoofed header
         */
        void sendGet(int requestNumber, String... extraHeaders) throws IOException {
            StringBuilder request = new StringBuilder();
            request.append("GET ").append(SERVLET_PATH).append("?req=").append(requestNumber).append(" HTTP/1.1\r\n");
            request.append("Host: ").append(server.getHostname()).append(':').append(server.getHttpDefaultPort()).append("\r\n");
            for (String header : extraHeaders) {
                request.append(header).append("\r\n");
            }
            request.append("\r\n");
            out.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /**
         * Reads exactly one response: the status line, the headers, and a body of
         * {@code Content-Length} bytes. Fails the test if the response has no
         * Content-Length, since the servlet is expected to always set one.
         */
        RawResponse readResponse() throws IOException {
            String statusLine = readLine();
            assertNotNull("Connection closed before a status line was received", statusLine);

            // Status line looks like: HTTP/1.1 200 OK
            String[] parts = statusLine.split(" ", 3);
            RawResponse response = new RawResponse(Integer.parseInt(parts[1]));

            String line;
            while ((line = readLine()) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    response.headers.put(line.substring(0, colon).trim().toLowerCase(), line.substring(colon + 1).trim());
                }
            }

            String contentLength = response.headers.get("content-length");
            assertNotNull("Response must have a Content-Length so it can be read off a shared socket: " + response,
                          contentLength);
            byte[] body = new byte[Integer.parseInt(contentLength)];
            int read = 0;
            while (read < body.length) {
                int count = in.read(body, read, body.length - read);
                if (count < 0) {
                    break;
                }
                read += count;
            }
            response.body = new String(body, 0, read, StandardCharsets.ISO_8859_1);
            return response;
        }

        /**
         * Checks whether the server has closed this connection.
         *
         * <p>A read that returns -1 (end of stream) means the server closed it
         * cleanly. A read that times out means the server left it open. Any other
         * IOException, such as a reset, also means the connection is gone.
         *
         * @param timeoutMs how long to wait for the close before deciding the connection is still open
         */
        boolean isClosedByServer(int timeoutMs) {
            try {
                socket.setSoTimeout(timeoutMs);
                return in.read() < 0;
            } catch (SocketTimeoutException e) {
                return false;
            } catch (IOException e) {
                return true;
            }
        }

        /** Reads one CRLF-terminated line, without the line ending. Returns null at end of stream. */
        private String readLine() throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) >= 0) {
                if (b == '\n') {
                    break;
                }
                if (b != '\r') {
                    line.write(b);
                }
            }
            if (b < 0 && line.size() == 0) {
                return null;
            }
            return line.toString(StandardCharsets.ISO_8859_1.name());
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
