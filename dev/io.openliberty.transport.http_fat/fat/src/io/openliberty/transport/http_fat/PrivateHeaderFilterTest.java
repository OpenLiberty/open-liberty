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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for the WAS private header ($WS*) filter
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PrivateHeaderFilterTest {

    private static final Logger LOG = Logger.getLogger(PrivateHeaderFilterTest.class.getName());

    private static final String APP_NAME     = "privateHeaderApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/PrivateHeaderServlet";
    /** Port value that differs from the real server port, used to verify header filtering */
    private static final String ALT_PORT     = "19999";
    /** An alternate remote address sent via $WSRA, distinct from any real loopback address */
    private static final String ALT_REMOTE_ADDR = "10.11.12.13";
    private static final int    SOCKET_TIMEOUT = 5000;

    private static final String DEFAULT_CONFIG          = "privateHeader-default.xml";
    private static final String TRUST_SENSITIVE_CONFIG  = "privateHeader-trustSensitive.xml";
    private static final String DESENSITIZE_CONFIG      = "privateHeader-desensitize.xml";

    @Server("PrivateHeaderFilter")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http.privatehdr.servlet");
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

    private void reconfigure(String configFile) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(configFile);
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a $WSSP header sent from a source that is not listed in
     * trustedSensitiveHeaderOrigin is ignored by the server.
     * HttpServletRequest.getServerPort() must return the actual listening
     * port, not the value carried in the header.
     */
    @Test
    public void testSensitiveHeaderIgnoredFromUnrecognisedSource() throws Exception {
        String response = sendRawGet(SERVLET_PATH, "$WSSP: " + ALT_PORT);
        LOG.info("Response:\n" + response);

        String serverPort = extractResponseField(response, "SERVER_PORT");
        assertFalse("SERVER_PORT must not equal the value from the $WSSP header (" + ALT_PORT + ")",
                ALT_PORT.equals(serverPort));
        assertEquals("SERVER_PORT should equal the real server port",
                String.valueOf(server.getHttpDefaultPort()), serverPort);
    }

    /**
     * Verifies that the Location header in a 302 context-root trailing-slash
     * redirect uses the real server port, not a port value supplied via $WSSP
     * from a source that is not listed in trustedSensitiveHeaderOrigin.
     */
    @Test
    public void testRedirectLocationReflectsRealPort() throws Exception {
        String contextRoot = "/" + APP_NAME;
        String response = sendRawGet(contextRoot, "$WSSP: " + ALT_PORT);
        LOG.info("Redirect response:\n" + response);

        assertTrue("Expected a 302 redirect from context-root request",
                response.contains("302"));

        String location = extractHeader(response, "Location");
        assertFalse("Location header must not contain the port value from the $WSSP header ("
                    + ALT_PORT + "). Got: " + location,
                location != null && location.contains(":" + ALT_PORT));
    }

    /**
     * Baseline check: a normal request carrying no $WS* headers must be
     * handled correctly — the real server port and server name are reported, and
     * REQUEST_URL is well-formed.
     */
    @Test
    public void testCleanRequestUnaffected() throws Exception {
        String response = sendRawGet(SERVLET_PATH);
        LOG.info("Clean response:\n" + response);

        assertTrue("Expected 200 OK for a normal request", response.contains("200"));

        String serverPort = extractResponseField(response, "SERVER_PORT");
        assertEquals("SERVER_PORT should equal the real server port",
                String.valueOf(server.getHttpDefaultPort()), serverPort);

        String requestUrl = extractResponseField(response, "REQUEST_URL");
        assertFalse("REQUEST_URL must not be empty", requestUrl == null || requestUrl.isEmpty());
        assertTrue("REQUEST_URL should contain the real server port",
                requestUrl.contains(":" + server.getHttpDefaultPort()));
    }

    /**
     * Verifies that non-sensitive $WS* headers pass through in the default
     * configuration (no trustedHeaderOrigin restriction set).
     * The request must complete successfully and SERVER_PORT must reflect the real port.
     */
    @Test
    public void testNonSensitivePrivateHeaderNotStrippedByDefault() throws Exception {
        String response = sendRawGet(SERVLET_PATH, "$WSSN: testServerName");
        LOG.info("Non-sensitive header response:\n" + response);

        assertTrue("Request with a non-sensitive $WS* header should succeed (200 OK)",
                response.contains("200"));
        String serverPort = extractResponseField(response, "SERVER_PORT");
        assertEquals("SERVER_PORT should equal the real server port",
                String.valueOf(server.getHttpDefaultPort()), serverPort);
    }

    
    /**
     * Verifies that a $WSRA header (remote address override) sent from a
     * source that is not listed in trustedSensitiveHeaderOrigin is stripped.
     * HttpServletRequest.getRemoteAddr() must return the real socket address,
     * not the value carried in the header.
     */
    @Test
    public void testSensitiveWsraStrippedByDefault() throws Exception {
        String response = sendRawGet(SERVLET_PATH, "$WSRA: " + ALT_REMOTE_ADDR);
        LOG.info("$WSRA stripped response:\n" + response);

        String remoteAddr = extractResponseField(response, "REMOTE_ADDR");
        assertFalse("REMOTE_ADDR must not equal the alternate $WSRA value",
                ALT_REMOTE_ADDR.equals(remoteAddr));
        // HDR_WSRA must be null — the header was removed before the servlet saw it
        assertEquals("$WSRA header must not be visible to the servlet",
                "null", extractResponseField(response, "HDR_WSRA"));
    }

    /**
     * Verifies the 80/443 carve-out: a $WSSP: 443 header is permitted from
     * any source even without trustedSensitiveHeaderOrigin configured
     */
    @Test
    public void testDefaultHttpsPortWsspAllowed() throws Exception {
        String response = sendRawGet(SERVLET_PATH, "$WSSP: 443");
        LOG.info("$WSSP:443 response:\n" + response);

        assertTrue("Expected 200 OK", response.contains("200"));
        assertEquals("$WSSP: 443 should be honoured via the standard-port allowance",
                "443", extractResponseField(response, "SERVER_PORT"));
    }

    /**
     * Verifies the 80/443 carve-out for the default HTTP port.
     */
    @Test
    public void testDefaultHttpPortWsspAllowed() throws Exception {
        String response = sendRawGet(SERVLET_PATH, "$WSSP: 80");
        LOG.info("$WSSP:80 response:\n" + response);

        assertTrue("Expected 200 OK", response.contains("200"));
        assertEquals("$WSSP: 80 should be honoured via the standard-port allowance",
                "80", extractResponseField(response, "SERVER_PORT"));
    }

    /**
     * Verifies that $WSRA is honoured when trustedSensitiveHeaderOrigin="*"
     * is configured. HttpServletRequest.getRemoteAddr() must return the value
     * supplied in the header, not the real socket address.
     */
    @Test
    public void testSensitiveWsraHonouredWhenTrustedOriginAll() throws Exception {
        reconfigure(TRUST_SENSITIVE_CONFIG);
    
        String response = sendRawGet(SERVLET_PATH, "$WSRA: " + ALT_REMOTE_ADDR);
        LOG.info("$WSRA trusted response:\n" + response);

        assertEquals("$WSRA must be honoured when trustedSensitiveHeaderOrigin=*",
                ALT_REMOTE_ADDR, extractResponseField(response, "REMOTE_ADDR"));
        assertEquals("$WSRA header must be visible to the servlet",
                ALT_REMOTE_ADDR, extractResponseField(response, "HDR_WSRA"));

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sends a raw HTTP/1.1 GET request, optionally appending extra header lines.
     * Returns the full raw response as a string.
     */
    private static String sendRawGet(String path, String... extraHeaders) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("GET ").append(path).append(" HTTP/1.1\r\n");
        sb.append("Host: ").append(server.getHostname())
          .append(":").append(server.getHttpDefaultPort()).append("\r\n");
        sb.append("Connection: close\r\n");
        for (String header : extraHeaders) {
            sb.append(header).append("\r\n");
        }
        sb.append("\r\n");

        byte[] requestBytes = sb.toString().getBytes(StandardCharsets.ISO_8859_1);

        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(requestBytes);
            out.flush();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (true) {
                try {
                    int n = in.read(buf);
                    if (n < 0) break;
                    responseBytes.write(buf, 0, n);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            return responseBytes.toString(StandardCharsets.ISO_8859_1.name());
        }
    }

    /**
     * Extracts a KEY=value field from the servlet's plain-text response body.
     * Returns null if not found.
     */
    private static String extractResponseField(String response, String key) {
        for (String line : response.split("\\r?\\n")) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return null;
    }

    /**
     * Extracts a named HTTP response header value (case-insensitive on the header name).
     * Returns null if not present.
     */
    private static String extractHeader(String response, String headerName) {
        for (String line : response.split("\\r?\\n")) {
            if (line.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                return line.substring(headerName.length() + 1).trim();
            }
        }
        return null;
    }
}
