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
 * sets {@code Connection: close} on the outgoing response and then lets Netty's
 * parent handler close the socket. The handler never modifies the request.
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
    private static final String UPGRADE_SERVLET_PATH = "/" + APP_NAME + "/UpgradeServlet";

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

    /** HTTP/2 client connection preface (RFC 7540 section 3.5). */
    private static final String H2_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";

    /** An empty HTTP/2 SETTINGS frame: length 0, type 4, no flags, stream 0. */
    private static final String H2_EMPTY_SETTINGS = "\0\0\0\4\0\0\0\0\0";

    /** A SETTINGS acknowledgement: length 0, type 4, ACK flag, stream 0. */
    private static final String H2_SETTINGS_ACK = "\0\0\0\4\1\0\0\0\0";

    /**
     * Base64url of a valid SETTINGS payload (MAX_CONCURRENT_STREAMS=100,
     * INITIAL_WINDOW_SIZE=1 GiB, ENABLE_PUSH=0), for the HTTP2-Settings header.
     */
    private static final String H2_SETTINGS_HEADER = "AAMAAABkAARAAAAAAAIAAAAA";

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
     * {@code maxKeepAliveRequests=0} is accepted by the configuration and, as in
     * the channel framework, behaves like {@code 1}: the first response must
     * carry {@code Connection: close} and the server must close the socket.
     */
    @Test
    public void maxKeepAliveRequestsZero_closesAfterFirstResponse() throws Exception {
        setKeepAliveOptions(null, 0);

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1);
            RawResponse response = connection.readResponse();
            LOG.info("maxKeepAliveRequests=0 response:\n" + response);

            assertEquals("The request itself should still succeed", 200, response.statusCode);
            assertTrue("maxKeepAliveRequests=0 must add Connection: close to the first response",
                       response.isConnectionClose());
            assertTrue("The server must close the connection after the first response",
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
    // Pipelined requests
    // ---------------------------------------------------------------------

    /**
     * Control for the pipelining tests. With the default configuration, three
     * requests sent in one write must be answered in order, none of the responses
     * may ask for the connection to be closed, and the socket must stay open.
     *
     * <p>Without this test, the "request not answered" checks in the tests below
     * would also pass if pipelined requests were never answered at all.
     */
    @Test
    public void defaultConfig_pipelinedRequests_allAnsweredInOrder() throws Exception {
        try (RawConnection connection = new RawConnection()) {
            connection.sendPipelinedGets(3);

            for (int i = 1; i <= 3; i++) {
                RawResponse response = connection.readResponse();
                LOG.info("defaultConfig pipelined response " + i + ":\n" + response);

                assertEquals("Pipelined request " + i + " should succeed", 200, response.statusCode);
                assertTrue("Responses must arrive in request order, expected req=" + i + " but got: " + response.body.trim(),
                           response.body.contains("req=" + i));
                assertFalse("Response " + i + " must not ask to close the connection with default config",
                            response.isConnectionClose());
            }
            assertFalse("The server must leave the connection open with default config",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
        }
    }

    /**
     * With {@code keepAliveEnabled=false}, two requests sent in one write must
     * produce one response, carrying {@code Connection: close}. The server then
     * closes the socket without answering the second request.
     *
     * <p>This is how the channel framework behaves: it reads a pipelined request
     * only after the previous exchange has finished and the connection is still
     * persistent. Netty must match.
     */
    @Test
    public void keepAliveDisabled_pipelinedRequests_secondNotAnswered() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            connection.sendPipelinedGets(2);

            RawResponse first = connection.readResponse();
            LOG.info("keepAliveDisabled pipelined response 1:\n" + first);
            assertEquals("Pipelined request 1 should succeed", 200, first.statusCode);
            assertTrue("Response 1 must be for request 1: " + first.body.trim(), first.body.contains("req=1"));
            assertTrue("keepAliveEnabled=false must add Connection: close to response 1", first.isConnectionClose());

            assertTrue("The server must close the connection after response 1 without answering pipelined request 2",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /**
     * With {@code maxKeepAliveRequests=2}, three requests sent in one write must
     * produce two responses. Response 1 leaves the connection open, response 2
     * carries {@code Connection: close}, and the server then closes the socket
     * without answering request 3.
     *
     * <p>This checks the request counter against the response that is being
     * written. If the counter counted every pipelined request as soon as it
     * arrived, response 1 would already see the limit as reached and be closed
     * early, so response 2 would never be sent.
     */
    @Test
    public void maxKeepAliveRequests_pipelinedRequests_closesOnNthAndDropsTheRest() throws Exception {
        setKeepAliveOptions(null, 2);

        try (RawConnection connection = new RawConnection()) {
            connection.sendPipelinedGets(3);

            RawResponse first = connection.readResponse();
            LOG.info("maxKeepAliveRequests pipelined response 1:\n" + first);
            assertEquals("Pipelined request 1 should succeed", 200, first.statusCode);
            assertTrue("Response 1 must be for request 1: " + first.body.trim(), first.body.contains("req=1"));
            assertFalse("Response 1 of 2 must not close the connection", first.isConnectionClose());

            RawResponse second = connection.readResponse();
            LOG.info("maxKeepAliveRequests pipelined response 2:\n" + second);
            assertEquals("Pipelined request 2 should succeed", 200, second.statusCode);
            assertTrue("Response 2 must be for request 2: " + second.body.trim(), second.body.contains("req=2"));
            assertTrue("Response 2 of 2 must carry Connection: close", second.isConnectionClose());

            assertTrue("The server must close the connection after response 2 without answering pipelined request 3",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    // ---------------------------------------------------------------------
    // Servlet 3.1 HttpUpgradeHandler upgrades
    // ---------------------------------------------------------------------

    /**
     * With {@code keepAliveEnabled=false}, a Servlet 3.1 upgrade must still
     * succeed and the socket must stay open.
     *
     * <p>Keep-alive describes how a connection is reused for further HTTP
     * requests. After {@code 101 Switching Protocols} the connection no longer
     * carries HTTP; it belongs to the upgraded protocol (here, an echo handler).
     * Closing it because "keep-alive is disabled" would break every servlet
     * upgrade on a server that disables keep-alive. The legacy channel framework
     * exempts upgraded connections, so Netty must behave the same way.
     *
     * <p>The test proves the connection is alive by sending bytes after the
     * {@code 101} and expecting the same bytes echoed back, then confirming the
     * server has not closed the socket.
     */
    @Test
    public void keepAliveDisabled_servletUpgrade_staysOpen() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            assertUpgradeSucceedsAndStaysOpen(connection, false);
        }
    }

    /**
     * With {@code maxKeepAliveRequests=1} the upgrade request is itself request
     * number 1, the last one allowed on the connection. The upgrade must still
     * succeed and the socket must stay open, for the same reason as above: the
     * request limit applies to HTTP requests, and the connection has stopped
     * carrying HTTP.
     */
    @Test
    public void maxKeepAliveRequestsOne_servletUpgrade_staysOpen() throws Exception {
        setKeepAliveOptions(null, 1);

        try (RawConnection connection = new RawConnection()) {
            assertUpgradeSucceedsAndStaysOpen(connection, false);
        }
    }

    /**
     * Same as {@link #keepAliveDisabled_servletUpgrade_staysOpen()} but the
     * upgrade handler uses plain blocking reads and never sets a
     * {@code ReadListener}.
     *
     * <p>Setting a {@code ReadListener} marks the connection as a listener-based
     * upgrade, and the server's close logic handles those separately from
     * handlers that never set one. This test covers the listener-less path.
     * The handler stays in {@code init} reading until the client disconnects.
     * If it returned instead, the server would close the socket on both
     * transports, which says nothing about keep-alive.
     */
    @Test
    public void keepAliveDisabled_blockingServletUpgrade_staysOpen() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            assertUpgradeSucceedsAndStaysOpen(connection, true);
        }
    }

    /**
     * Same as {@link #maxKeepAliveRequestsOne_servletUpgrade_staysOpen()} but
     * with the blocking, listener-less upgrade handler. See
     * {@link #keepAliveDisabled_blockingServletUpgrade_staysOpen()} for why this
     * path is tested separately.
     */
    @Test
    public void maxKeepAliveRequestsOne_blockingServletUpgrade_staysOpen() throws Exception {
        setKeepAliveOptions(null, 1);

        try (RawConnection connection = new RawConnection()) {
            assertUpgradeSucceedsAndStaysOpen(connection, true);
        }
    }

    // ---------------------------------------------------------------------
    // h2c (HTTP/2 enabled on the cleartext endpoint)
    // ---------------------------------------------------------------------

    /**
     * On an h2c endpoint a plain HTTP/1.1 request that does not upgrade takes a
     * separate path: the pipeline starts without the keep-alive handler and
     * installs it only after it sees the request is not HTTP/2. This checks that
     * path still enforces {@code keepAliveEnabled=false}.
     */
    @Test
    public void h2cEndpoint_http1Request_keepAliveDisabled_closes() throws Exception {
        setEndpointOptions(false, null, "http/2");

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1);
            RawResponse response = connection.readResponse();
            LOG.info("h2c endpoint, HTTP/1.1 response:\n" + response);

            assertEquals("The request itself should still succeed", 200, response.statusCode);
            assertTrue("keepAliveEnabled=false must add Connection: close on an h2c endpoint",
                       response.isConnectionClose());
            assertTrue("The server must close the connection after the response",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /** Same path as above for {@code maxKeepAliveRequests=2}. */
    @Test
    public void h2cEndpoint_http1Request_maxKeepAliveRequests_closesOnNthResponse() throws Exception {
        setEndpointOptions(null, 2, "http/2");

        try (RawConnection connection = new RawConnection()) {
            connection.sendGet(1);
            RawResponse first = connection.readResponse();
            LOG.info("h2c endpoint, response 1:\n" + first);
            assertEquals("Request 1 should succeed", 200, first.statusCode);
            assertFalse("Response 1 of 2 must not close the connection", first.isConnectionClose());
            assertFalse("The connection must still be open after request 1 of 2",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));

            connection.sendGet(2);
            RawResponse second = connection.readResponse();
            LOG.info("h2c endpoint, response 2:\n" + second);
            assertEquals("Request 2 should succeed", 200, second.statusCode);
            assertTrue("Response 2 of 2 must carry Connection: close", second.isConnectionClose());
            assertTrue("The server must close the connection after the limit is reached",
                       connection.isClosedByServer(RESPONSE_TIMEOUT_MS));
        }
    }

    /**
     * Control for the two h2c upgrade tests below. With the default keep-alive
     * configuration, an {@code Upgrade: h2c} request must be accepted and
     * answered over HTTP/2. This proves the raw HTTP/2 exchange in
     * {@link #assertH2cUpgradeStaysOpen} works at all.
     */
    @Test
    public void h2cEndpoint_h2cUpgrade_defaultConfig_staysOpen() throws Exception {
        // keepAliveEnabled=true is the default. It is set explicitly so the config update
        // carries an httpOptions change like the other tests, which the wait below relies on.
        setEndpointOptions(true, null, "http/2");

        try (RawConnection connection = new RawConnection()) {
            assertH2cUpgradeStaysOpen(connection);
        }
    }

    /**
     * An h2c upgrade is HTTP/2 from then on, and keep-alive is an HTTP/1
     * concept. With both options set to their most aggressive values
     * ({@code keepAliveEnabled=false}, {@code maxKeepAliveRequests=1}) the
     * upgrade must still succeed and the connection must stay open.
     */
    @Test
    public void h2cEndpoint_h2cUpgrade_keepAliveOptionsSet_staysOpen() throws Exception {
        setEndpointOptions(false, 1, "http/2");

        try (RawConnection connection = new RawConnection()) {
            assertH2cUpgradeStaysOpen(connection);
        }
    }

    /**
     * Same as above for an h2c connection that starts as HTTP/2 with prior
     * knowledge: the client sends the HTTP/2 preface instead of an HTTP/1
     * request, so the keep-alive handler is never involved.
     */
    @Test
    public void h2cEndpoint_priorKnowledge_keepAliveOptionsSet_staysOpen() throws Exception {
        setEndpointOptions(false, 1, "http/2");

        try (RawConnection connection = new RawConnection()) {
            connection.writeRaw(H2_PREFACE + H2_EMPTY_SETTINGS);
            H2Frame frame = connection.readH2Frame();
            LOG.info("prior knowledge, first server " + frame);

            assertEquals("The server must answer the HTTP/2 preface with a SETTINGS frame",
                         H2Frame.TYPE_SETTINGS, frame.type);
            assertFalse("The server must not close an HTTP/2 connection",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
        }
    }

    /**
     * Sends an h2c upgrade request, expects {@code 101} with {@code Upgrade: h2c},
     * completes the HTTP/2 handshake, and expects the response to the upgrade
     * request on stream 1. Then checks that the server neither sent GOAWAY nor
     * closed the socket.
     */
    private void assertH2cUpgradeStaysOpen(RawConnection connection) throws IOException {
        connection.sendH2cUpgradeRequest();
        RawResponse response = connection.readResponseHead();
        LOG.info("h2c upgrade response:\n" + response);

        assertEquals("The h2c upgrade must be accepted with 101 Switching Protocols",
                     101, response.statusCode);
        assertEquals("The 101 must name the h2c protocol", "h2c", response.headers.get("upgrade"));

        connection.writeRaw(H2_PREFACE + H2_EMPTY_SETTINGS);

        // The response to the upgrade request arrives as HEADERS on stream 1.
        boolean sawResponse = false;
        for (int i = 0; i < 10 && !sawResponse; i++) {
            H2Frame frame = connection.readH2Frame();
            LOG.info("h2c upgrade, server " + frame);
            assertTrue("The server must not send GOAWAY: " + frame, frame.type != H2Frame.TYPE_GOAWAY);
            if (frame.isSettingsNeedingAck()) {
                // A compliant client acknowledges the server's SETTINGS. Without the ACK the
                // channel framework never sent the stream 1 response (the read timed out).
                connection.writeRaw(H2_SETTINGS_ACK);
            }
            sawResponse = frame.type == H2Frame.TYPE_HEADERS && frame.streamId == 1;
        }
        assertTrue("The server must answer the upgrade request on HTTP/2 stream 1", sawResponse);
        assertFalse("The server must not close an HTTP/2 connection",
                    connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
    }

    /**
     * Control for {@link #keepAliveDisabled_servletUpgrade_earlyBytesDelivered()}:
     * with the default keep-alive configuration, bytes sent in the same write as
     * the upgrade request must reach the upgraded handler.
     */
    @Test
    public void defaultConfig_servletUpgrade_earlyBytesDelivered() throws Exception {
        try (RawConnection connection = new RawConnection()) {
            assertEarlyBytesDelivered(connection);
        }
    }

    /**
     * With {@code keepAliveEnabled=false} the handler leaves the request's
     * {@code Connection: Upgrade} header alone. The dispatcher reads that header
     * to recognise an upgrade request and to hold back bytes that arrive with it.
     * This checks that bytes sent in the same write as the upgrade request are
     * still delivered to the upgraded handler.
     */
    @Test
    public void keepAliveDisabled_servletUpgrade_earlyBytesDelivered() throws Exception {
        setKeepAliveOptions(false, null);

        try (RawConnection connection = new RawConnection()) {
            assertEarlyBytesDelivered(connection);
        }
    }

    /** Same as above for {@code maxKeepAliveRequests=1}, where the upgrade is request 1. */
    @Test
    public void maxKeepAliveRequestsOne_servletUpgrade_earlyBytesDelivered() throws Exception {
        setKeepAliveOptions(null, 1);

        try (RawConnection connection = new RawConnection()) {
            assertEarlyBytesDelivered(connection);
        }
    }

    /**
     * Sends the upgrade request with {@code EARLY} appended in the same write,
     * expects {@code 101}, then expects the echo handler to return {@code EARLY}.
     */
    private void assertEarlyBytesDelivered(RawConnection connection) throws IOException {
        String early = "EARLY";
        connection.sendUpgradeRequest(false, early);
        RawResponse response = connection.readResponseHead();
        LOG.info("upgrade response:\n" + response);

        assertEquals("The servlet upgrade must be accepted with 101 Switching Protocols",
                     101, response.statusCode);
        assertEquals("Bytes sent with the upgrade request must reach the upgraded handler",
                     early, connection.readRaw(early.length()));
        assertFalse("The server must not close an upgraded connection",
                    connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
    }

    /**
     * Sends a servlet upgrade request, expects {@code 101 Switching Protocols}
     * with {@code Connection: Upgrade}, then checks the upgraded connection is
     * alive by echoing bytes through it and confirming the server did not close
     * the socket.
     *
     * @param blocking true to use the blocking, listener-less upgrade handler
     */
    private void assertUpgradeSucceedsAndStaysOpen(RawConnection connection, boolean blocking) throws IOException {
        connection.sendUpgradeRequest(blocking);
        RawResponse response = connection.readResponseHead();
        LOG.info("upgrade response:\n" + response);

        assertEquals("The servlet upgrade must be accepted with 101 Switching Protocols",
                     101, response.statusCode);
        // The Connection header is a token list. The channel framework sends
        // "Upgrade, Close" here when keep-alive is disabled, and still hands the
        // socket to the upgraded protocol, so only require the Upgrade token.
        // Whether the socket really stays usable is checked by the echo below.
        assertTrue("The 101 must keep the Upgrade token in Connection: " + response.headers.get("connection"),
                   response.hasConnectionToken("upgrade"));

        String payload = "PING";
        connection.writeRaw(payload);
        assertEquals("The upgraded connection must echo what is written to it",
                     payload, connection.readRaw(payload.length()));
        assertFalse("The server must not close an upgraded connection",
                    connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
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
        setEndpointOptions(keepAliveEnabled, maxRequests, null);
    }

    /**
     * Same as {@link #setKeepAliveOptions(Boolean, Integer)} and optionally sets
     * {@code protocolVersion} on the endpoint, for example {@code http/2} to
     * enable h2c on the cleartext port.
     *
     * @param protocolVersion value for {@code protocolVersion}, or null to leave it unset
     */
    private void setEndpointOptions(Boolean keepAliveEnabled, Integer maxRequests, String protocolVersion) throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        HttpEndpoint endpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        if (protocolVersion != null) {
            endpoint.setProtocolVersion(protocolVersion);
        }
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

    /** One HTTP/2 frame header. The payload is read and discarded. */
    private static class H2Frame {
        static final int TYPE_HEADERS = 0x1;
        static final int TYPE_SETTINGS = 0x4;
        static final int TYPE_GOAWAY = 0x7;
        static final int FLAG_ACK = 0x1;

        final int type;
        final int flags;
        final int streamId;
        final int length;

        H2Frame(int type, int flags, int streamId, int length) {
            this.type = type;
            this.flags = flags;
            this.streamId = streamId;
            this.length = length;
        }

        /** True for a SETTINGS frame that is not itself an acknowledgement. */
        boolean isSettingsNeedingAck() {
            return type == TYPE_SETTINGS && (flags & FLAG_ACK) == 0;
        }

        @Override
        public String toString() {
            return "frame type=" + type + " flags=" + flags + " stream=" + streamId + " length=" + length;
        }
    }

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

        /** True when the comma-separated {@code Connection} header contains the token (case-insensitive). */
        boolean hasConnectionToken(String token) {
            String connection = headers.get("connection");
            if (connection == null) {
                return false;
            }
            for (String part : connection.split(",")) {
                if (part.trim().equalsIgnoreCase(token)) {
                    return true;
                }
            }
            return false;
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
         * Sends {@code count} GET requests to the test servlet in a single write,
         * without waiting for any response (HTTP/1.1 pipelining). The requests are
         * numbered 1 to {@code count}, and the servlet echoes the number back.
         */
        void sendPipelinedGets(int count) throws IOException {
            StringBuilder requests = new StringBuilder();
            for (int i = 1; i <= count; i++) {
                requests.append("GET ").append(SERVLET_PATH).append("?req=").append(i).append(" HTTP/1.1\r\n");
                requests.append("Host: ").append(server.getHostname()).append(':').append(server.getHttpDefaultPort()).append("\r\n");
                requests.append("\r\n");
            }
            out.write(requests.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /**
         * Sends a GET to the upgrade servlet asking for a protocol upgrade. The
         * servlet answers {@code 101} and hands the connection to an echo handler.
         *
         * @param blocking true to select the blocking, listener-less handler with {@code ?mode=blocking}
         */
        void sendUpgradeRequest(boolean blocking) throws IOException {
            sendUpgradeRequest(blocking, "");
        }

        /**
         * Same as {@link #sendUpgradeRequest(boolean)} but appends {@code earlyBytes}
         * in the same write, so they can arrive in the same TCP segment as the request.
         */
        void sendUpgradeRequest(boolean blocking, String earlyBytes) throws IOException {
            String request = "GET " + UPGRADE_SERVLET_PATH + (blocking ? "?mode=blocking" : "") + " HTTP/1.1\r\n"
                             + "Host: " + server.getHostname() + ':' + server.getHttpDefaultPort() + "\r\n"
                             + "Upgrade: KeepAliveEcho\r\n"
                             + "Connection: Upgrade\r\n"
                             + "\r\n"
                             + earlyBytes;
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /**
         * Sends an h2c upgrade request: {@code Upgrade: h2c} plus the
         * {@code HTTP2-Settings} header, as defined by RFC 7540 section 3.2.
         */
        void sendH2cUpgradeRequest() throws IOException {
            String request = "GET " + SERVLET_PATH + "?req=1 HTTP/1.1\r\n"
                             + "Host: " + server.getHostname() + ':' + server.getHttpDefaultPort() + "\r\n"
                             + "Connection: Upgrade, HTTP2-Settings\r\n"
                             + "Upgrade: h2c\r\n"
                             + "HTTP2-Settings: " + H2_SETTINGS_HEADER + "\r\n"
                             + "\r\n";
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /**
         * Reads one HTTP/2 frame and discards its payload. Fails the test if the
         * server closed the connection before a whole frame header arrived.
         */
        H2Frame readH2Frame() throws IOException {
            String header = readRaw(9);
            assertEquals("The server closed the connection before sending an HTTP/2 frame", 9, header.length());
            int length = (header.charAt(0) << 16) | (header.charAt(1) << 8) | header.charAt(2);
            int type = header.charAt(3);
            int flags = header.charAt(4);
            int streamId = ((header.charAt(5) & 0x7f) << 24) | (header.charAt(6) << 16)
                           | (header.charAt(7) << 8) | header.charAt(8);
            readRaw(length);
            return new H2Frame(type, flags, streamId, length);
        }

        /** Writes raw bytes on the connection, for use after an upgrade. */
        void writeRaw(String data) throws IOException {
            out.write(data.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /** Reads exactly {@code length} raw bytes, for use after an upgrade. */
        String readRaw(int length) throws IOException {
            byte[] data = new byte[length];
            int read = 0;
            while (read < length) {
                int count = in.read(data, read, length - read);
                if (count < 0) {
                    break;
                }
                read += count;
            }
            return new String(data, 0, read, StandardCharsets.ISO_8859_1);
        }

        /**
         * Reads only the status line and headers. Used directly for a
         * {@code 101 Switching Protocols}, which has no body and no
         * Content-Length.
         */
        RawResponse readResponseHead() throws IOException {
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
            return response;
        }

        /**
         * Reads exactly one response: the status line, the headers, and a body of
         * {@code Content-Length} bytes. Fails the test if the response has no
         * Content-Length, since the servlet is expected to always set one.
         */
        RawResponse readResponse() throws IOException {
            RawResponse response = readResponseHead();

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
