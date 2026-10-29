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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.transport.http.downgrade.servlet.DowngradeFallbackServlet;

/**
 * Tests for protocol downgrade and fallback paths to verify consistent behavior
 * across both Legacy Transport (Channel Framework) and Netty Transport (with auto-read disabled):
 * 1. WebSocket upgrade refusal handled as standard HTTP (not upgraded to binary framing).
 * 2. Unaccepted h2c (HTTP/2 cleartext) upgrade continues as HTTP/1.1.
 * 3. TLS negotiation fallback to HTTP/1.1 over HTTPS.
 */
@RunWith(FATRunner.class)
public class DowngradeFallbackTests {

    private static final Logger LOG = Logger.getLogger(DowngradeFallbackTests.class.getName());
    private static final String APP_NAME = "downgradeApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/DowngradeFallbackServlet";
    private static final int SOCKET_TIMEOUT_MS = 10000;

    @Server("DowngradeFallback")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http.downgrade.servlet");
        server.addInstalledAppForValidation(APP_NAME);
        configurePorts();
        server.startServer();
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");
        server.waitForDefaultHTTPEndpointSSLStart();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private static void configurePorts() throws Exception {
        int httpPort = Integer.getInteger("HTTP_default", server.getHttpDefaultPort());
        if (httpPort == 0) {
            httpPort = 8010;
        }
        int httpsPort = Integer.getInteger("HTTP_default.secure", server.getHttpDefaultSecurePort());
        if (httpsPort == 0) {
            httpsPort = 8020;
        }

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

    /**
     * Test 1: WebSocket upgrade request to a standard HTTP servlet that does not upgrade.
     * The server must return standard HTTP 200 OK (not 101 Switching Protocols),
     * and the connection must remain usable for subsequent HTTP/1.1 keep-alive requests.
     */
    @Test
    public void testWebSocketUpgradeRefusedHandledAsHttp() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 1. Send WebSocket upgrade request
            String request1 = "GET " + SERVLET_PATH + "?step=ws_upgrade HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Upgrade: websocket\r\n" +
                              "Connection: keep-alive, Upgrade\r\n" +
                              "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                              "Sec-WebSocket-Version: 13\r\n" +
                              "\r\n";

            out.write(request1.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response1 = readResponse(in);
            LOG.info("Response 1 status: " + response1.getStatusLine());
            assertEquals("Expected HTTP 200 OK when WebSocket upgrade is not performed", 200, response1.getStatusCode());
            assertTrue("Expected servlet response marker in body", response1.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));
            assertTrue("Expected query string step=ws_upgrade", response1.getBody().contains("step=ws_upgrade"));

            // 2. Send follow-up HTTP/1.1 request over the same connection
            String request2 = "GET " + SERVLET_PATH + "?step=follow_up HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n";

            out.write(request2.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response2 = readResponse(in);
            LOG.info("Response 2 status: " + response2.getStatusLine());
            assertEquals("Expected HTTP 200 OK for follow-up request", 200, response2.getStatusCode());
            assertTrue("Expected servlet response marker in follow-up body", response2.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));
            assertTrue("Expected query string step=follow_up", response2.getBody().contains("step=follow_up"));
        }
    }

    /**
     * Test 2: WebSocket upgrade request returning a custom non-101 error status (403 Forbidden).
     * Verifies that error status bodies are delivered cleanly and subsequent keep-alive requests succeed.
     */
    @Test
    public void testWebSocketUpgradeRefusedWithCustomStatus() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 1. Send WebSocket upgrade request with custom 403 status
            String request1 = "GET " + SERVLET_PATH + "?status=403 HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Upgrade: websocket\r\n" +
                              "Connection: keep-alive, Upgrade\r\n" +
                              "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                              "Sec-WebSocket-Version: 13\r\n" +
                              "\r\n";

            out.write(request1.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response1 = readResponse(in);
            LOG.info("Response status for rejected upgrade: " + response1.getStatusLine());
            assertEquals("Expected HTTP 403 Forbidden", 403, response1.getStatusCode());
            assertTrue("Expected servlet response marker in body", response1.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));

            // 2. Send follow-up request over keep-alive connection
            String request2 = "GET " + SERVLET_PATH + "?step=after_403 HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n";

            out.write(request2.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response2 = readResponse(in);
            assertEquals("Expected HTTP 200 OK after custom status refusal", 200, response2.getStatusCode());
            assertTrue("Expected query string step=after_403", response2.getBody().contains("step=after_403"));
        }
    }

    /**
     * Test 3: HTTP/2 Cleartext (h2c) upgrade request on HTTP/1.1 endpoint.
     * When h2c upgrade is not accepted, connection continues processing as HTTP/1.1,
     * returning 200 OK and permitting subsequent HTTP/1.1 keep-alive requests.
     */
    @Test
    public void testH2CUpgradeNotAcceptedHandledAsHttp11() throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 1. Send h2c upgrade request
            String request1 = "GET " + SERVLET_PATH + "?step=h2c_attempt HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Connection: keep-alive, Upgrade, HTTP2-Settings\r\n" +
                              "Upgrade: h2c\r\n" +
                              "HTTP2-Settings: AAMAAABkAARAAAAAAAIAAAAA\r\n" +
                              "\r\n";

            out.write(request1.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response1 = readResponse(in);
            LOG.info("h2c attempt response: " + response1.getStatusLine());
            assertEquals("Expected HTTP 200 OK for h2c request", 200, response1.getStatusCode());
            assertTrue("Expected servlet response marker in body", response1.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));

            // 2. Send subsequent HTTP/1.1 request over the same connection
            String request2 = "GET " + SERVLET_PATH + "?step=h2c_followup HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultPort() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n";

            out.write(request2.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response2 = readResponse(in);
            assertEquals("Expected HTTP 200 OK for h2c follow-up", 200, response2.getStatusCode());
            assertTrue("Expected query string step=h2c_followup", response2.getBody().contains("step=h2c_followup"));
        }
    }

    /**
     * Test 4: TLS negotiation selecting HTTP/1.1 over HTTPS.
     * Verifies that TLS connection operates properly in HTTP/1.1 mode and supports keep-alive.
     */
    @Test
    public void testTlsNegotiationHttp11() throws Exception {
        SSLSocketFactory sslSocketFactory = createTrustAllSSLSocketFactory();
        try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(server.getHostname(), server.getHttpDefaultSecurePort())) {
            sslSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
            sslSocket.startHandshake();

            OutputStream out = sslSocket.getOutputStream();
            InputStream in = sslSocket.getInputStream();

            // 1. Send first HTTPS request
            String request1 = "GET " + SERVLET_PATH + "?step=tls_http11_1 HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "\r\n" +
                              "Connection: keep-alive\r\n" +
                              "\r\n";

            out.write(request1.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response1 = readResponse(in);
            LOG.info("TLS HTTP/1.1 response 1: " + response1.getStatusLine());
            assertEquals("Expected HTTP 200 OK over TLS", 200, response1.getStatusCode());
            assertTrue("Expected servlet marker in TLS response", response1.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));

            // 2. Send second HTTPS request over same TLS socket
            String request2 = "GET " + SERVLET_PATH + "?step=tls_http11_2 HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n";

            out.write(request2.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response2 = readResponse(in);
            LOG.info("TLS HTTP/1.1 response 2: " + response2.getStatusLine());
            assertEquals("Expected HTTP 200 OK over TLS follow-up", 200, response2.getStatusCode());
            assertTrue("Expected query string step=tls_http11_2", response2.getBody().contains("step=tls_http11_2"));
        }
    }

    /**
     * Test 5: WebSocket upgrade refusal over TLS (HTTPS).
     * Verifies that WebSocket upgrade refusal over TLS stays in HTTP/1.1 and handles keep-alive.
     */
    @Test
    public void testTlsWebSocketUpgradeRefusedHandledAsHttp() throws Exception {
        SSLSocketFactory sslSocketFactory = createTrustAllSSLSocketFactory();
        try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(server.getHostname(), server.getHttpDefaultSecurePort())) {
            sslSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
            sslSocket.startHandshake();

            OutputStream out = sslSocket.getOutputStream();
            InputStream in = sslSocket.getInputStream();

            // 1. Send WebSocket upgrade over TLS
            String request1 = "GET " + SERVLET_PATH + "?step=tls_ws_refused HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "\r\n" +
                              "Upgrade: websocket\r\n" +
                              "Connection: keep-alive, Upgrade\r\n" +
                              "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                              "Sec-WebSocket-Version: 13\r\n" +
                              "\r\n";

            out.write(request1.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response1 = readResponse(in);
            LOG.info("TLS WS refused response 1: " + response1.getStatusLine());
            assertEquals("Expected HTTP 200 OK for TLS WebSocket refusal", 200, response1.getStatusCode());
            assertTrue("Expected servlet marker in body", response1.getBody().contains(DowngradeFallbackServlet.RESPONSE_MARKER));

            // 2. Send follow-up over same TLS socket
            String request2 = "GET " + SERVLET_PATH + "?step=tls_ws_followup HTTP/1.1\r\n" +
                              "Host: " + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n";

            out.write(request2.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            ParsedHttpResponse response2 = readResponse(in);
            assertEquals("Expected HTTP 200 OK for TLS follow-up", 200, response2.getStatusCode());
            assertTrue("Expected step=tls_ws_followup", response2.getBody().contains("step=tls_ws_followup"));
        }
    }

    private static SSLSocketFactory createTrustAllSSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };
        sslContext.init(null, trustAll, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private static ParsedHttpResponse readResponse(InputStream in) throws Exception {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        int prev = -1;
        int curr;
        // Read headers until CRLF CRLF (\r\n\r\n)
        while ((curr = in.read()) != -1) {
            headerBytes.write(curr);
            byte[] bytes = headerBytes.toByteArray();
            int len = bytes.length;
            if (len >= 4 &&
                bytes[len - 4] == '\r' && bytes[len - 3] == '\n' &&
                bytes[len - 2] == '\r' && bytes[len - 1] == '\n') {
                break;
            }
        }

        String headersStr = headerBytes.toString(StandardCharsets.ISO_8859_1.name());
        String[] lines = headersStr.split("\\r?\\n");
        String statusLine = lines.length > 0 ? lines[0] : "";

        int statusCode = 0;
        String[] statusParts = statusLine.split(" ");
        if (statusParts.length >= 2) {
            try {
                statusCode = Integer.parseInt(statusParts[1]);
            } catch (NumberFormatException ignored) {}
        }

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ENGLISH),
                            line.substring(colon + 1).trim());
            }
        }

        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        String contentLengthHeader = headers.get("content-length");
        String transferEncoding = headers.get("transfer-encoding");

        if (contentLengthHeader != null) {
            int contentLength = Integer.parseInt(contentLengthHeader.trim());
            byte[] buf = new byte[1024];
            int remaining = contentLength;
            while (remaining > 0) {
                int read = in.read(buf, 0, Math.min(buf.length, remaining));
                if (read < 0) {
                    break;
                }
                bodyBytes.write(buf, 0, read);
                remaining -= read;
            }
        } else if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ENGLISH).contains("chunked")) {
            // Read chunked body
            while (true) {
                String chunkSizeLine = readLine(in);
                if (chunkSizeLine == null || chunkSizeLine.isEmpty()) {
                    continue;
                }
                int chunkSize = Integer.parseInt(chunkSizeLine.trim().split(";")[0], 16);
                if (chunkSize == 0) {
                    // Trailing CRLF
                    readLine(in);
                    break;
                }
                byte[] chunk = new byte[chunkSize];
                int readTotal = 0;
                while (readTotal < chunkSize) {
                    int read = in.read(chunk, readTotal, chunkSize - readTotal);
                    if (read < 0) break;
                    readTotal += read;
                }
                bodyBytes.write(chunk, 0, readTotal);
                // Consume trailing CRLF of chunk
                in.read();
                in.read();
            }
        } else if ("close".equalsIgnoreCase(headers.get("connection"))) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                bodyBytes.write(buf, 0, read);
            }
        }

        String body = bodyBytes.toString(StandardCharsets.ISO_8859_1.name());
        return new ParsedHttpResponse(statusLine, statusCode, headers, body);
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        int curr;
        while ((curr = in.read()) != -1) {
            if (curr == '\n') {
                break;
            }
            if (curr != '\r') {
                lineBytes.write(curr);
            }
        }
        return lineBytes.toString(StandardCharsets.ISO_8859_1.name());
    }

    private static final class ParsedHttpResponse {
        private final String statusLine;
        private final int statusCode;
        private final Map<String, String> headers;
        private final String body;

        ParsedHttpResponse(String statusLine, int statusCode, Map<String, String> headers, String body) {
            this.statusLine = statusLine;
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        String getStatusLine() {
            return statusLine;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getBody() {
            return body;
        }
    }
}
