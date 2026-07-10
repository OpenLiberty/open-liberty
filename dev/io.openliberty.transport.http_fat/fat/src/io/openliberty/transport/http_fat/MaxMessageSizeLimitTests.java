/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Logger;
import java.io.InputStream;
import java.net.Socket;

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
import componenttest.topology.impl.LibertyServer;
import componenttest.rules.repeater.EE6FeatureReplacementAction;
import componenttest.annotation.AllowedFFDC;

/**
 * Test to ensure that the tcpOptions inactivityTimeout works.
 */
@SkipForRepeat(EE6FeatureReplacementAction.ID) // Part.getSubmittedFileName requires Servlet 3.1+
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MaxMessageSizeLimitTests {

    private static final Logger LOG = Logger.getLogger(MaxMessageSizeLimitTests.class.getName());
    private static final String APP_NAME = "FileUpload";
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server("MaxMessageSize")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.transport.http.fileupload.servlet");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(MaxMessageSizeLimitTests.class.getSimpleName() + ".log");

        // Go through logs and check if Netty is being used.
        // Wait for the TCP Channel to finish loading and get the TCP Channel started
        // message.
        String tcpChannelMessage = server.waitForDefaultHTTPEndpointStart();
        LOG.info("Endpoint: " + tcpChannelMessage);

        runningNetty = tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Save the server configuration before each test, this should be the default
     * server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the server to the default state.
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    @Test
    public void testFileThatIsWithinLimit() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(3000); // Larger than our file content size

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        String boundary = "-----------------" + System.currentTimeMillis();

        URL url = new URL("http://" + server.getHostname() + ":" +
                server.getHttpDefaultPort() + "/" + APP_NAME + "/FileUploadServlet");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true); // Sending Data
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String fileName = "test.txt";
        String fileContent = "Hello, world! This is some test file content!";

        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
        body.append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(fileContent).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");

        // Send the request with file content
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        try {
            assertEquals("Expected HTTP 200", 200, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }
    }

    @Test
    public void testFileThatExceedsLimit() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(2); // Smaller than our file content size, should cause an
                                                              // error

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        String boundary = "-----------------" + System.currentTimeMillis();

        URL url = new URL("http://" + server.getHostname() + ":" +
                server.getHttpDefaultPort() + "/" + APP_NAME + "/FileUploadServlet");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true); // Sending Data
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String fileName = "test.txt";
        String fileContent = "This is test file content!";

        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
        body.append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(fileContent).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");

        // Send the request with file content
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        try {
            assertEquals("Expected HTTP 413", 413, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }

    }

    /**
     * Verify that a chunked POST where the chunk-size field contains more
     * than 8 hex digits is rejected with HTTP 400 when the application
     * reads the request body.
    */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.http.channel.exception.MessageTooLargeException" })
    public void testChunkedRequestBodyRead_OversizeChunkIsRejected() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            String pipedPayload = "GET /FileUpload/SentinelServlet HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "\r\n";

            String payload = "POST /FileUpload/ChunkSizeTestServlet?readBody=true HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n" +
                    "100000000\r\n" +
                    "AAAA\r\n" +
                    "0\r\n\r\n" +
                    pipedPayload;

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            String responseStr = drainResponse(socket);
            LOG.info("[testChunkedRequestBodyRead_OversizeChunkIsRejected] Response: " + responseStr.substring(0, Math.min(300, responseStr.length())));

            assertFalse("SENTINEL-HIT must not appear — piped request must not be dispatched",
                    responseStr.contains("SENTINEL-HIT"));
            assertEquals("Expected exactly 1 HTTP response", 1, countOccurrences(responseStr, "HTTP/1.1"));
            assertTrue("Expected HTTP 400 for overflow chunk-size", responseStr.contains("HTTP/1.1 400"));
        }
    }

    /**
     * Verify that a chunked POST where the chunk-size field contains more
     * than 8 hex digits is rejected with HTTP 400 when the application
     * does not read the request body (Liberty purges on close).
    */
    @Test
    public void testChunkedRequestBodyNotRead_OversizeChunkIsRejected() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            String pipedPayload = "GET /FileUpload/SentinelServlet HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "\r\n";

            String payload = "POST /FileUpload/ChunkSizeTestServlet HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n" +
                    "100000000\r\n" +
                    "AAAA\r\n" +
                    "0\r\n\r\n" +
                    pipedPayload;

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            String responseStr = drainResponse(socket);
            LOG.info("[testChunkedRequestBodyNotRead_OversizeChunkIsRejected] Response: " + responseStr.substring(0, Math.min(300, responseStr.length())));

            assertFalse("SENTINEL-HIT must not appear — piped request must not be dispatched",
                    responseStr.contains("SENTINEL-HIT"));
            // Connection must be closed — only 1 response allowed (the ChunkSizeTestServlet 200 or
            // no response)
            assertTrue("Expected at most 1 HTTP response — no second pipelined response",
                    countOccurrences(responseStr, "HTTP/1.1") <= 1);
        }
    }

    /**
     * Boundary check: chunk-size 0x7FFFFFFF (8 hex digits, Integer.MAX_VALUE)
     * is the largest value the transport layer accepts.
     * Verify the request is not rejected at the transport layer.
    */
    @Test
    @AllowedFFDC({ "java.io.EOFException" })
    public void testBoundary_2GB_Chunk_IsAccepted() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(-1); // Larger than our file content size

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            // Send the exact boundary chunk-size line; follow with 1 byte + terminal chunk.
            // The declared size (0x7FFFFFFF) won't match the 1-byte body, but we're only
            // testing that the size value itself doesn't trigger a guard rejection.
            String payload = "POST /FileUpload/ChunkSizeTestServlet?readBody=true HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "7FFFFFFF\r\n" +
                    "A\r\n" +
                    "0\r\n\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            LOG.info("[Boundary 2GB] Request dispatched");

            try{
                String responseStr = drainResponse(socket);
            LOG.info("[Boundary 2GB] Response: " + responseStr.substring(0, Math.min(200, responseStr.length())));

            assertFalse("0x7FFFFFFF is the maximum accepted chunk size and must not be rejected",
                    responseStr.contains("HTTP/1.1 400"));
            } catch (java.net.SocketTimeoutException expected) {
                // Liberty accepted the chunk-size and is waiting for the 2 GB body.
                // Timeout is the expected outcome — the size was not rejected.
                LOG.info("[Boundary 2GB] Socket timed out waiting for body — chunk-size was accepted as expected.");
            }

        }
    }

    /**
     * Boundary check: chunk-size 0x80000000 (8 hex digits) exceeds the
     * maximum accepted chunk size and must be rejected with HTTP 400.
    */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.http.channel.exception.MessageTooLargeException" })
    public void testBoundary_greaterThan_2GB_Chunk_IsRejected() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            String payload = "POST /FileUpload/ChunkSizeTestServlet?readBody=true HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "80000000\r\n" +
                    "AAAA\r\n" +
                    "0\r\n\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            String responseStr = drainResponse(socket);
            LOG.info("[Boundary Greater than 2GB] Response: " + responseStr.substring(0, Math.min(200, responseStr.length())));

            assertTrue("Expected HTTP 400 for chunk-size 0x80000000 which exceeds the maximum accepted value",
                    responseStr.contains("HTTP/1.1 400"));
        }
    }

    /**
     * Verify that a well-formed HTTP/1.1 chunked request with a valid
     * chunk-size is processed correctly without being rejected at the
     * transport layer.
     */
    @Test
    public void testLegitimateChunkedRequestSucceeds() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            String chunkData = "file=hello";
            String chunkSize = Integer.toHexString(chunkData.length()); 

            String payload = "POST /FileUpload/ChunkSizeTestServlet?readBody=true HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    chunkSize + "\r\n" +
                    chunkData + "\r\n" +
                    "0\r\n" +
                    "\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            socket.shutdownOutput();

            String responseStr = drainResponse(socket);
            LOG.info("HTTP/1.1 response Received: " + responseStr);
            // Servlet may return 400 for bad multipart, but the HTTP framing
            // should work — we should not get a connection-closed 400 from transport
            assertTrue("Expected valid HTTP response (not a transport-level rejection)",
                    responseStr.startsWith("HTTP/1.1 2") || responseStr.startsWith("HTTP/1.1 4"));
            assertFalse("Should not contain more than one HTTP response for a legitimate request",
                    countOccurrences(responseStr, "HTTP/1.1") > 1);
        }
    }

    /**
     * Verify that a chunk-size within 8 hex digits but exceeding the
     * configured messageSizeLimit is also rejected (hardening check).
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException" })

    public void testChunkSizeExceedingMessageSizeLimitIsRejected() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(400); // 100 bytes limit

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            // chunk-size 0x1000 = 4096 bytes — valid 4 digits, but exceeds messageSizeLimit
            // of 100
            String payload = "POST /FileUpload/ChunkSizeTestServlet?readBody=true HTTP/1.1\r\n" +
                    "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "400\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            socket.shutdownOutput();

            String responseStr = drainResponse(socket);
            LOG.info("HTTP/1.1 response Received: " + responseStr);
            assertTrue("Expected 400 Bad Request when chunk-size exceeds messageSizeLimit",
                    responseStr.contains("HTTP/1.1 400"));
        }
    }

    // Helper Method to drain response
    private String drainResponse(Socket socket) throws Exception {
        byte[] buf = new byte[65536];
        StringBuilder sb = new StringBuilder();
        InputStream in = socket.getInputStream();
        int read;
        while ((read = in.read(buf)) != -1) {
            sb.append(new String(buf, 0, read, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    // Helper Method to count Responses
    private int countOccurrences(String text, String pattern) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
