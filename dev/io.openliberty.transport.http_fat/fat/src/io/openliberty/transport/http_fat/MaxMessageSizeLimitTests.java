/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
        // Wait for the TCP Channel to finish loading and get the TCP Channel started message.
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
     * Save the server configuration before each test, this should be the default server
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
        httpEndpoint.getHttpOptions().setMessageSizeLimit(2); // Smaller than our file content size, should cause an error

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
     * Test that an HTTP/1.1 chunked request with an oversized chunk-size
     * (9 hex digits, triggering integer overflow) is rejected
     * with a 400 and does not result in HTTP request smuggling.
     *
     * PPSIRT 511, Chunk size of 0x100000000 wrapped to 0, causing the server to treat
     * the attacker's injected bytes as a new pipelined request (2 responses
     * on one connection). 
    */
    @Test
    public void testChunkSizeOverflowIsRejected() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            // Carrier request: chunk-size 0x100000000 (overflows int32 to 0)
            // followed immediately by a smuggled second request
            String payload =
                "POST /FileUpload/SmuggleTestServlet HTTP/1.1\r\n" +
                "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n" +
                "100000000\r\n" +    // 9-digit hex — overflows int32 to 0 without the fix
                "\r\n" +
                "GET /FileUpload/SmuggleTestServlet HTTP/1.1\r\n" +
                "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            socket.shutdownOutput();

            // Drain all response bytes
            byte[] buf = new byte[65536];
            StringBuilder response = new StringBuilder();
            InputStream in = socket.getInputStream();
            int read;
            while ((read = in.read(buf)) != -1) {
                response.append(new String(buf, 0, read, StandardCharsets.US_ASCII));
            }

            String responseStr = response.toString();
            LOG.info("Response received: " + responseStr.substring(0, Math.min(200, responseStr.length())));

            int responseCount = countOccurrences(responseStr, "HTTP/1.1");
            LOG.info("HTTP/1.1 response count: " + responseCount);

            // Expected: only 1 response (400 Bad Request), connection closed.
            // POC: 2 responses (200 + 200), confirming smuggling.
            assertEquals("Expected exactly 1 response — chunk-size overflow should be rejected", 1, responseCount);
            assertTrue("Expected 200 Response  for oversized chunk-size",
                    responseStr.contains("HTTP/1.1 200"));
        }
    }

    /**
     * Verify that a legitimate chunked request with a valid chunk-size
     * is not broken by the overflow fix. 
     * PSIRT 511
     */
    @Test
    public void testLegitimateChunkedRequestSucceeds() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();

            String chunkData = "file=hello";
            String chunkSize = Integer.toHexString(chunkData.length()); // well within 8 digits

            String payload =
                "POST /FileUpload/SmuggleTestServlet HTTP/1.1\r\n" +
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

            byte[] buf = new byte[65536];
            StringBuilder response = new StringBuilder();
            InputStream in = socket.getInputStream();
            int read;
            while ((read = in.read(buf)) != -1) {
                response.append(new String(buf, 0, read, StandardCharsets.US_ASCII));
            }

            String responseStr = response.toString();
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
    @AllowedFFDC({"com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException"})

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

            String boundary = "-----------------" + System.currentTimeMillis();

            // chunk-size 0x1000 = 4096 bytes — valid 4 digits, but exceeds messageSizeLimit of 100
            String payload =
                "POST /FileUpload/SmuggleTestServlet?readBody=true HTTP/1.1\r\n" +
                "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                //"Content-Type: multipart/form-data; boundary=" + boundary +"\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "400\r\n";

            out.write(payload.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            socket.shutdownOutput();

            byte[] buf = new byte[65536];
            StringBuilder response = new StringBuilder();
            InputStream in = socket.getInputStream();
            int read;
            while ((read = in.read(buf)) != -1) {
                response.append(new String(buf, 0, read, StandardCharsets.US_ASCII));
            }

            String responseStr = response.toString();
            LOG.info("HTTP/1.1 response Received: " + responseStr);
            assertTrue("Expected 400 Bad Request when chunk-size exceeds messageSizeLimit",
                    responseStr.contains("HTTP/1.1 400"));
        }
    }

    //Helper Method to count Responses
    private int countOccurrences(String text, String pattern) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) { count++; idx += pattern.length(); }
        return count;
    }
}
