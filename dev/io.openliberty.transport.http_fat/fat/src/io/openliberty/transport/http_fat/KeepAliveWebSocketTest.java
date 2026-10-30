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
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE6FeatureReplacementAction;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests that a WebSocket connection survives {@code keepAliveEnabled=false}
 * and {@code maxKeepAliveRequests=1}.
 *
 * <p>A WebSocket handshake is an HTTP/1.1 request, so {@code LibertyHttpKeepAliveHandler}
 * sees it. With those options the handler sets {@code Connection: close} on the
 * outgoing response, but it leaves a {@code 101 Switching Protocols} alone and never
 * modifies the request. The WebSocket handshake rejects a request whose
 * {@code Connection} header no longer contains {@code Upgrade}, so a handler that
 * rewrote the request would make the handshake fail with a {@code 400}. These
 * tests complete a raw WebSocket handshake, expect the {@code 101}, send a text
 * frame, and expect the echo back on the same socket.
 *
 * <p>Two orderings are covered, because the handler sees the request before the
 * upgrade is committed:
 * <ul>
 * <li>the first frame is sent after the {@code 101} has been read</li>
 * <li>the first frame is sent in the same write as the handshake request</li>
 * </ul>
 *
 * <p>The two "first frame with the handshake" tests run on the Netty transport only. On the
 * Channel Framework (CFW) transport the client times out waiting for the first echo.
 * CFW does not use {@code LibertyHttpKeepAliveHandler}, so the failure is not in the
 * code under test. A CFW run with trace enabled for {@code com.ibm.ws.tcpchannel},
 * {@code com.ibm.ws.http.channel}, {@code com.ibm.ws.http.dispatcher} and
 * {@code com.ibm.ws.wsoc} showed:
 * <ul>
 * <li>When the handshake is sent alone, the server's first TCP read is 179 bytes. The two
 * frames then arrive as separate reads of 11 and 12 bytes, and those tests pass.</li>
 * <li>When the first frame is sent in the same write as the handshake, the first TCP read
 * is 190 bytes (179 + the 11 byte frame).</li>
 * <li>After the HTTP channel parses the headers and the upgrade runs,
 * {@code WsocConnLink.startReading} starts a new TCP read. No {@code processRead} or
 * {@code WsocReadCallback.complete} follows, and the next read result is {@code -1}
 * when the client gives up.</li>
 * </ul>
 * In the source, {@code WsocUpgradeHandler.init} calls {@code SessionImpl.signalAppOnOpen()}
 * with no leftover buffer, so {@code startReading} is used. Only the client-side
 * {@code ClientConnector} passes leftover bytes to {@code processDataThenStartRead}.
 * The trace does not dump the HTTP buffer after the parse, so that the 11 bytes are still
 * held there is inferred from the read sizes, not observed.
 *
 * <p>Netty passes these tests. They stay as coverage for the new handler on Netty, and are
 * skipped on CFW.
 *
 * <p>This is a separate class and server from {@link KeepAliveEnforcementTest} because
 * it needs a {@code websocket-*} feature, which the repeat actions replace per EE level.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(EE6FeatureReplacementAction.ID) // websocket-1.1 is not an EE6 feature
public class KeepAliveWebSocketTest {

    private static final Logger LOG = Logger.getLogger(KeepAliveWebSocketTest.class.getName());

    /** Present in the "TCP Channel started" message only when the Netty transport is used. */
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";

    private static boolean runningNetty = false;

    private static final String APP_NAME = "keepAliveWebSocketApp";
    private static final String ENDPOINT_PATH = "/" + APP_NAME + "/echo";

    /** The sample key from RFC 6455 section 1.3 and the accept value it must produce. */
    private static final String WS_KEY = "dGhlIHNhbXBsZSBub25jZQ==";
    private static final String WS_ACCEPT = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";

    /** Fixed masking key. Client-to-server frames must be masked (RFC 6455 section 5.3). */
    private static final byte[] MASK = { 0x12, 0x34, 0x56, 0x78 };

    private static final int RESPONSE_TIMEOUT_MS = 10000;

    /** How long to wait before concluding that the server has left the socket open. */
    private static final int STAYS_OPEN_TIMEOUT_MS = 1500;

    @Server("KeepAliveWebSocket")
    public static LibertyServer server;

    /** True when the current test changed the server configuration and needs it restored. */
    private boolean configChanged;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http.keepalive.websocket");
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");

        // The TCP channel started message names the transport in use.
        String tcpChannelMessage = server.waitForDefaultHTTPEndpointStart();
        LOG.info("Endpoint: " + tcpChannelMessage);
        runningNetty = tcpChannelMessage != null && tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Before
    public void saveConfiguration() throws Exception {
        configChanged = false;
        server.saveServerConfiguration();
    }

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

    /** Control test. With the default configuration the WebSocket must work and stay open. */
    @Test
    public void defaultConfig_webSocketStaysOpen() throws Exception {
        assertWebSocketStaysOpen(false);
    }

    /** {@code keepAliveEnabled=false}: the WebSocket must still open, echo, and stay open. */
    @Test
    public void keepAliveDisabled_webSocketStaysOpen() throws Exception {
        setKeepAliveOptions(false, null);
        assertWebSocketStaysOpen(false);
    }

    /** {@code maxKeepAliveRequests=1}: the handshake is request 1, and the WebSocket must stay open. */
    @Test
    public void maxKeepAliveRequestsOne_webSocketStaysOpen() throws Exception {
        setKeepAliveOptions(null, 1);
        assertWebSocketStaysOpen(false);
    }

    /**
     * {@code keepAliveEnabled=false} with the first frame in the same write as the handshake.
     * Netty only. See the class Javadoc for why CFW is skipped.
     */
    @Test
    public void keepAliveDisabled_firstFrameWithHandshake_webSocketStaysOpen() throws Exception {
        assumeNettyForEarlyFrame();
        setKeepAliveOptions(false, null);
        assertWebSocketStaysOpen(true);
    }

    /**
     * {@code maxKeepAliveRequests=1} with the first frame in the same write as the handshake.
     * Netty only. See the class Javadoc for why CFW is skipped.
     */
    @Test
    public void maxKeepAliveRequestsOne_firstFrameWithHandshake_webSocketStaysOpen() throws Exception {
        assumeNettyForEarlyFrame();
        setKeepAliveOptions(null, 1);
        assertWebSocketStaysOpen(true);
    }

    /**
     * Skips the calling test on the Channel Framework transport, where a WebSocket frame
     * that arrives in the same TCP read as the handshake is never processed. See the class
     * Javadoc for the trace evidence.
     */
    private static void assumeNettyForEarlyFrame() {
        if (!runningNetty) {
            LOG.info("Skipping: the Channel Framework transport does not process a WebSocket frame "
                     + "that arrives in the same read as the handshake");
        }
        assumeTrue(runningNetty);
    }

    /**
     * Completes a WebSocket handshake, echoes two messages, and checks the server
     * leaves the socket open.
     *
     * @param firstFrameWithHandshake true to send the first frame in the same write as the handshake
     */
    private void assertWebSocketStaysOpen(boolean firstFrameWithHandshake) throws Exception {
        try (RawConnection connection = new RawConnection()) {
            if (firstFrameWithHandshake) {
                connection.sendHandshake(maskedTextFrame("first"));
            } else {
                connection.sendHandshake("");
            }

            RawResponse response = connection.readResponseHead();
            LOG.info("WebSocket handshake response: " + response);
            assertEquals("The handshake must be answered with 101 Switching Protocols", 101, response.statusCode);
            assertEquals("The handshake must return the RFC 6455 accept key", WS_ACCEPT,
                         response.headers.get("sec-websocket-accept"));

            if (!firstFrameWithHandshake) {
                connection.writeRaw(maskedTextFrame("first"));
            }
            assertEquals("The first message must be echoed", "first", connection.readTextFrame());

            // A second round trip proves the connection is still usable, not just that one echo got out.
            connection.writeRaw(maskedTextFrame("second"));
            assertEquals("The second message must be echoed", "second", connection.readTextFrame());

            assertFalse("The server must leave the WebSocket connection open",
                        connection.isClosedByServer(STAYS_OPEN_TIMEOUT_MS));
        }
    }

    /** Builds one masked, unfragmented text frame (payload must be shorter than 126 bytes). */
    private static String maskedTextFrame(String text) {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x81); // FIN + text opcode
        frame.write(0x80 | payload.length); // MASK bit + length
        frame.write(MASK, 0, MASK.length);
        for (int i = 0; i < payload.length; i++) {
            frame.write(payload[i] ^ MASK[i % 4]);
        }
        return new String(frame.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    // ---------------------------------------------------------------------
    // Configuration helper
    // ---------------------------------------------------------------------

    /**
     * Sets the keep-alive httpOptions on the default endpoint and waits for the
     * server to apply them. A null argument leaves that attribute unset.
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
    // Raw WebSocket client
    // ---------------------------------------------------------------------

    /** A parsed HTTP response head: status code and lower-cased header names. */
    private static class RawResponse {
        final int statusCode;
        final Map<String, String> headers = new HashMap<>();

        RawResponse(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public String toString() {
            return "status=" + statusCode + " headers=" + headers;
        }
    }

    /** One TCP connection to the server, used for the handshake and then for WebSocket frames. */
    private static class RawConnection implements AutoCloseable {
        private final Socket socket;
        private final OutputStream out;
        private final DataInputStream in;

        RawConnection() throws IOException {
            socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
            out = socket.getOutputStream();
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        }

        /**
         * Sends a WebSocket handshake request, followed by {@code earlyBytes} in the
         * same write so they can arrive in the same TCP segment.
         */
        void sendHandshake(String earlyBytes) throws IOException {
            String request = "GET " + ENDPOINT_PATH + " HTTP/1.1\r\n"
                             + "Host: " + server.getHostname() + ':' + server.getHttpDefaultPort() + "\r\n"
                             + "Upgrade: websocket\r\n"
                             + "Connection: Upgrade\r\n"
                             + "Sec-WebSocket-Key: " + WS_KEY + "\r\n"
                             + "Sec-WebSocket-Version: 13\r\n"
                             + "\r\n"
                             + earlyBytes;
            writeRaw(request);
        }

        void writeRaw(String data) throws IOException {
            out.write(data.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        }

        /** Reads the status line and headers. A 101 has no body. */
        RawResponse readResponseHead() throws IOException {
            String statusLine = readLine();
            assertNotNull("Connection closed before a status line was received", statusLine);

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

        /** Reads one unmasked server-to-client text frame (payload shorter than 126 bytes). */
        String readTextFrame() throws IOException {
            int first = in.readUnsignedByte();
            int second = in.readUnsignedByte();
            assertEquals("Expected an unfragmented text frame", 0x81, first);
            assertEquals("Server frames must not be masked", 0, second & 0x80);
            int length = second & 0x7f;
            byte[] payload = new byte[length];
            in.readFully(payload);
            return new String(payload, StandardCharsets.UTF_8);
        }

        /**
         * Checks whether the server has closed this connection. A read that returns -1
         * means the server closed it, a timeout means it is still open.
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
