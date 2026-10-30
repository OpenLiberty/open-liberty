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

    private static final String DEFAULT_CONFIG               = "privateHeader-default.xml";
    private static final String TRUST_SENSITIVE_CONFIG       = "privateHeader-trustSensitive.xml";
    private static final String ALLOWED_SERVER_NAMES_CONFIG  = "privateHeader-allowedServerNames.xml";
    private static final String ALLOWED_AND_SENSITIVE_CONFIG = "privateHeader-allowedAndSensitive.xml";
    private static final String TRUSTED_FALSE_CONFIG         = "privateHeader-trustedFalse.xml";
    private static final String VHOST_ALLOWED_CONFIG         = "privateHeader-vhost-allowed.xml";
    private static final String VHOST_UNLISTED_CONFIG        = "privateHeader-vhost-unlisted.xml";
    private static final String VHOST_SENSITIVE_CONFIG       = "privateHeader-vhost-sensitive.xml";
    private static final String VHOST_APP_NAME               = "vhostApp";
    private static final String VHOST_SERVLET_PATH           = "/vhostApp/VhostServlet";

    @Server("PrivateHeaderFilter")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http.privatehdr.servlet");
        server.addInstalledAppForValidation(APP_NAME);
        // Build the vhost WAR and export it so it is available when vhost configs reference it.
        // DISABLE_VALIDATION prevents ShrinkHelper from adding vhostApp to the startup
        // verification list — the default server.xml has no <application location="vhostApp.war"/>
        // so the server would never start it and validation would time out.
        // Only configs that explicitly include vhostApp.war will deploy it; each vhost test
        // waits for CWWKT0016I after reconfigure() before sending a request.
        ShrinkHelper.defaultApp(server, VHOST_APP_NAME,
                new ShrinkHelper.DeployOptions[] { ShrinkHelper.DeployOptions.DISABLE_VALIDATION },
                "io.openliberty.transport.http.vhost.servlet");
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

    /**
     * Variant of {@link #reconfigure} for config files that deploy both
     * {@code privateHeaderApp} and {@code vhostApp}. Uses
     * {@code waitForConfigUpdateInLogUsingMark} with both app names so the
     * test does not proceed until Liberty has confirmed both applications are
     * live — the standard Liberty FAT mechanism for app-startup gating.
     */
    private void reconfigureWithVhost(String configFile) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(configFile);
        server.waitForStringInLogUsingMark("CWWKG0017I", 30000);
        server.waitForConfigUpdateInLogUsingMark(
                new java.util.HashSet<>(java.util.Arrays.asList(APP_NAME, VHOST_APP_NAME)));
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
        assertEquals("$WSSN should be present as a non-sensitive header",
                "testServerName", extractResponseField(response, "HDR_WSSN"));
        // When $WSSN is not in allowedServerNames and the peer is not sensitive-trusted,
        // the dispatcher takes the "unapproved" path: it still returns the $WSSN value
        // as the API server name but marks the response to carry Cache-Control: no-store
        // so that caches do not store a response whose server-name came from an
        // unverified source.
        assertEquals("SERVER_NAME should equal the unapproved $WSSN value",
                "testServerName", extractResponseField(response, "SERVER_NAME"));
        assertEquals("Cache-Control must be no-store when $WSSN is used from an unapproved source",
                "no-store", extractHeader(response, "Cache-Control"));
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
     * Verifies that case variants of $WSRA (e.g. $wsra, $Wsra)
     * are stripped from an untrusted source, not passed through to the servlet.
     * Netty's header map is case-insensitive, so a lowercase name that is not matched
     * by the filter would still be readable as  HDR_$WSRA downstream.
     */
    @Test
    public void testSensitiveWsraStrippedRegardlessOfCase() throws Exception {
        for (String wireName : new String[] { "$wsra", "$Wsra", "$WSRA" }) {
            String response = sendRawGet(SERVLET_PATH, wireName + ": " + ALT_REMOTE_ADDR);
            LOG.info("Case variant [" + wireName + "] response:\n" + response);

            assertFalse("REMOTE_ADDR must not equal the override value for wire name [" + wireName + "]",
                    ALT_REMOTE_ADDR.equals(extractResponseField(response, "REMOTE_ADDR")));
            assertEquals("$WSRA must not be visible to the servlet for wire name [" + wireName + "]",
                    "null", extractResponseField(response, "HDR_WSRA"));
        }
    }

    /**
     * Verifies that case variants of  $WSSP (e.g. $wssp $Wssp)
     * are stripped from an untrusted source, not passed through to the servlet.
     */
    @Test
    public void testWsspStrippedRegardlessOfCase() throws Exception {
        for (String wireName : new String[] { "$wssp", "$Wssp", "$WSSP" }) {
            String response = sendRawGet(SERVLET_PATH, wireName + ": " + ALT_PORT);
            LOG.info("Case variant [" + wireName + "] response:\n" + response);

            String serverPort = extractResponseField(response, "SERVER_PORT");
            assertFalse("SERVER_PORT must not equal ALT_PORT for wire name [" + wireName + "]",
                    ALT_PORT.equals(serverPort));
            assertEquals("SERVER_PORT should equal the real server port for wire name [" + wireName + "]",
                    String.valueOf(server.getHttpDefaultPort()), serverPort);
        }
    }

    /**
     * Verifies that $WSRA is honoured when trustedSensitiveHeaderOrigin="*"
     * is configured. HttpServletRequest.getRemoteAddr() must return the value
     * supplied in the header, not the real socket address.
     */
    @Test
    public void testSensitiveWsraHonouredWhenTrustedOriginAll() throws Exception {
        server.saveServerConfiguration(); 
        try {
            reconfigure(TRUST_SENSITIVE_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSRA: " + ALT_REMOTE_ADDR);
            LOG.info("$WSRA trusted response:\n" + response);

            assertEquals("$WSRA must be honoured when trustedSensitiveHeaderOrigin=*",
                    ALT_REMOTE_ADDR, extractResponseField(response, "REMOTE_ADDR"));
            assertEquals("$WSRA header must be visible to the servlet",
                    ALT_REMOTE_ADDR, extractResponseField(response, "HDR_WSRA"));

        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        }

    }

    // -------------------------------------------------------------------------
    // Tests — allowedServerNames and Cache-Control: no-store behaviour
    // -------------------------------------------------------------------------

    /**
     * Verifies that a {@code $WSSN} value explicitly listed in
     * {@code httpDispatcher allowedServerNames} is treated as an approved name.
     * {@code HttpServletRequest.getServerName()} must return the value from the
     * header, {@code getRequestURL()} must contain it, and the response must not
     * carry {@code Cache-Control: no-store} because the name came from a
     * pre-approved list.
     */
    @Test
    public void testWssnFromAllowedList() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(ALLOWED_SERVER_NAMES_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: testServerName");
            LOG.info("testWssnFromAllowedList response:\n" + response);

            assertEquals("SERVER_NAME should equal the listed $WSSN value",
                    "testServerName", extractResponseField(response, "SERVER_NAME"));
            String requestUrl = extractResponseField(response, "REQUEST_URL");
            assertTrue("REQUEST_URL should contain the approved $WSSN value",
                    requestUrl != null && requestUrl.contains("testServerName"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store for an approved $WSSN",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that a {@code $WSSN} value that is <em>not</em> listed in
     * {@code allowedServerNames} (and where the peer is not sensitive-trusted)
     * triggers the unapproved path: the dispatcher still returns the {@code $WSSN}
     * value as the API server name so that application routing can proceed, but
     * it also sets {@code Cache-Control: no-store} on the response to prevent
     * caches from storing a response whose server-name came from an unverified source.
     */
    @Test
    public void testWssnNotInAllowedList() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(ALLOWED_SERVER_NAMES_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: unlisted.other.com");
            LOG.info("testWssnNotInAllowedList response:\n" + response);

            assertEquals("SERVER_NAME should equal the unapproved $WSSN value",
                    "unlisted.other.com", extractResponseField(response, "SERVER_NAME"));
            assertEquals("Cache-Control must be no-store when $WSSN is not in allowedServerNames",
                    "no-store", extractHeader(response, "Cache-Control"));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that a {@code $WSSN} value matching a wildcard entry in
     * {@code allowedServerNames} (e.g. {@code *.example.test}) is treated as
     * an approved name. {@code HttpServletRequest.getServerName()} must return
     * the matched value and the response must not carry {@code Cache-Control: no-store}.
     */
    @Test
    public void testWssnFromWildcardInAllowedList() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(ALLOWED_SERVER_NAMES_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: sub.example.test");
            LOG.info("testWssnFromWildcardInAllowedList response:\n" + response);

            assertEquals("SERVER_NAME should equal the wildcard-matched $WSSN value",
                    "sub.example.test", extractResponseField(response, "SERVER_NAME"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store for a wildcard-approved $WSSN",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that a {@code $WSSN} header from a peer that is trusted for sensitive
     * headers ({@code trustedSensitiveHeaderOrigin="*"}) is unconditionally approved,
     * regardless of whether the value is listed in {@code allowedServerNames}.
     * {@code HttpServletRequest.getServerName()} must return the value and the
     * response must not carry {@code Cache-Control: no-store}.
     */
    @Test
    public void testWssnSensitiveTrustedPeer() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(TRUST_SENSITIVE_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: sensitivetrusted.example.test");
            LOG.info("testWssnSensitiveTrustedPeer response:\n" + response);

            assertEquals("SERVER_NAME should equal the $WSSN value for a sensitive-trusted peer",
                    "sensitivetrusted.example.test", extractResponseField(response, "SERVER_NAME"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store when $WSSN is approved via sensitive trust",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that when both {@code allowedServerNames} and
     * {@code trustedSensitiveHeaderOrigin="*"} are configured, a {@code $WSSN}
     * value matching the allowed list is approved via the sensitive-trust path
     * (which takes precedence). The response must not carry {@code Cache-Control: no-store}.
     */
    @Test
    public void testWssnAllowedAndSensitiveTrusted() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(ALLOWED_AND_SENSITIVE_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: listed.example.test");
            LOG.info("testWssnAllowedAndSensitiveTrusted response:\n" + response);

            assertEquals("SERVER_NAME should equal the listed $WSSN value",
                    "listed.example.test", extractResponseField(response, "SERVER_NAME"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store when $WSSN is in allowedServerNames and the peer is sensitive-trusted",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Directly exercises the {@code Cache-Control: no-store} response hook.
     * When {@code $WSSN} is not listed in {@code allowedServerNames} and the
     * peer is not sensitive-trusted, the dispatcher writes
     * {@code Cache-Control: no-store} at response marshal time and still
     * exposes the {@code $WSSN} value as the API server name.
     */
    @Test
    public void testWssnNoStoreWhenUnapproved() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(ALLOWED_SERVER_NAMES_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: unapproved.other.com");
            LOG.info("testWssnNoStoreWhenUnapproved response:\n" + response);

            assertEquals("Cache-Control must be no-store for an unapproved $WSSN",
                    "no-store", extractHeader(response, "Cache-Control"));
            assertEquals("SERVER_NAME should equal the unapproved $WSSN value",
                    "unapproved.other.com", extractResponseField(response, "SERVER_NAME"));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that {@code webContainer trusted="false"} causes all WAS private
     * headers to be stripped regardless of {@code trustedHeaderOrigin} settings.
     * {@code HttpServletRequest.getServerName()} must return the Host-derived name
     * (not the injected {@code $WSSN} value) and no {@code Cache-Control: no-store}
     * must be added, because the header is discarded before the dispatcher logic runs.
     */
    @Test
    public void testWssnIgnoredWhenTrustedFalse() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigure(TRUSTED_FALSE_CONFIG);

            String response = sendRawGet(SERVLET_PATH, "$WSSN: injected.example.test");
            LOG.info("testWssnIgnoredWhenTrustedFalse response:\n" + response);

            assertFalse("SERVER_NAME must not equal the injected $WSSN value when webContainer trusted=false",
                    "injected.example.test".equals(extractResponseField(response, "SERVER_NAME")));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store when $WSSN is stripped due to trusted=false",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies that a {@code $WSSN} value that is syntactically invalid as a
     * host name is silently ignored. The value {@code "bad_name"} is valid on
     * the HTTP wire (no control characters or spaces) but contains an underscore,
     * which is not a legal DNS label character and therefore fails the
     * well-formedness check. The dispatcher must fall back to the Host-derived
     * server name and must not add {@code Cache-Control: no-store}.
     */
    @Test
    public void testWssnMalformed() throws Exception {
        // Default server config — no reconfigure needed.
        String response = sendRawGet(SERVLET_PATH, "$WSSN: bad_name");
        LOG.info("testWssnMalformed response:\n" + response);

        assertFalse("SERVER_NAME must not equal the malformed $WSSN value",
                "bad_name".equals(extractResponseField(response, "SERVER_NAME")));
        String cacheControl = extractHeader(response, "Cache-Control");
        assertFalse("Cache-Control must not be no-store when $WSSN fails the well-formedness check",
                "no-store".equals(cacheControl));
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

    /**
     * Sends a raw HTTP/1.1 GET request using a caller-supplied {@code Host:} header
     * value instead of the default {@code server.getHostname():server.getHttpDefaultPort()}.
     * This is required for virtual-host routing tests where the Host header must carry
     * a back-end name (e.g. {@code internal-gw.example.test}) that differs from the
     * actual server address.
     * <p>
     * A separate helper is needed because {@link #sendRawGet} always emits its own
     * {@code Host:} line before the extra headers, which would produce two {@code Host:}
     * lines — forbidden by HTTP/1.1 — if the caller also passed one as an extra header.
     *
     * @param path         request path (e.g. {@code /vhostApp/VhostServlet})
     * @param hostHeader   the full value for the {@code Host:} header line
     *                     (e.g. {@code "internal-gw.example.test:9080"})
     * @param extraHeaders additional header lines to append (e.g. {@code "$WSSN: public.example.test"})
     */
    private static String sendRawGetWithHost(String path, String hostHeader,
                                             String... extraHeaders) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("GET ").append(path).append(" HTTP/1.1\r\n");
        sb.append("Host: ").append(hostHeader).append("\r\n");
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

    // -------------------------------------------------------------------------
    // Tests — virtual-host routing via $WSSN
    // -------------------------------------------------------------------------

    /**
     * Verifies the core regression scenario: the IHS plug-in sends a back-end
     * {@code Host} header ({@code internal-gw.example.test}) together with
     * {@code $WSSN: public.example.test}. With {@code public.example.test} listed
     * in {@code allowedServerNames}, the dispatcher uses it as the routing key,
     * which matches the {@code publicVhost} alias, and the virtual-host application
     * ({@code vhostApp}) is served.
     * <p>
     * Before the fix, the routing key would be the Host value and the request would
     * miss the virtual host entirely.
     */
    @Test
    public void testVhostRoutingWithAllowedServerNames() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigureWithVhost(VHOST_ALLOWED_CONFIG);

            String hostHeader = "internal-gw.example.test:" + server.getHttpDefaultPort();
            String response = sendRawGetWithHost(VHOST_SERVLET_PATH, hostHeader,
                    "$WSSN: public.example.test");
            LOG.info("testVhostRoutingWithAllowedServerNames response:\n" + response);

            assertTrue("Expected 200 OK — vhostApp should be served",
                    response.contains("200"));
            assertEquals("APP_ID must be vhost — vhostApp must have answered the request",
                    "vhost", extractResponseField(response, "APP_ID"));
            assertEquals("SERVER_NAME should equal the approved $WSSN value",
                    "public.example.test", extractResponseField(response, "SERVER_NAME"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store for an approved $WSSN",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies fail-closed behaviour: when {@code $WSSN} is not listed in
     * {@code allowedServerNames} and the peer is not sensitive-trusted, the
     * routing key stays as the Host header value ({@code internal-gw.example.test}).
     * That key does not match the {@code publicVhost} alias, so the virtual-host
     * application is not served — the presence of the alias alone is not enough
     * to trust the {@code $WSSN} value.
     */
    @Test
    public void testVhostRoutingFailClosedWhenNotListed() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigureWithVhost(VHOST_UNLISTED_CONFIG);

            String hostHeader = "internal-gw.example.test:" + server.getHttpDefaultPort();
            String response = sendRawGetWithHost(VHOST_SERVLET_PATH, hostHeader,
                    "$WSSN: public.example.test");
            LOG.info("testVhostRoutingFailClosedWhenNotListed response:\n" + response);

            // The routing key is the Host value, which has no matching virtual host.
            // Liberty returns a 404 or dispatcher error page; either way APP_ID=vhost
            // must not appear in the body.
            assertFalse("vhostApp must not be served when $WSSN is not in allowedServerNames",
                    "vhost".equals(extractResponseField(response, "APP_ID")));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }

    /**
     * Verifies the sensitive-trusted routing path end-to-end: with
     * {@code trustedSensitiveHeaderOrigin="*"}, any well-formed {@code $WSSN}
     * is approved without needing an {@code allowedServerNames} entry.
     * The routing key becomes {@code public.example.test:<port>}, matching
     * the {@code publicVhost} alias, and {@code vhostApp} is served without
     * {@code Cache-Control: no-store} because the name is approved.
     */
    @Test
    public void testVhostRoutingWithSensitiveTrustedPeer() throws Exception {
        server.saveServerConfiguration();
        try {
            reconfigureWithVhost(VHOST_SENSITIVE_CONFIG);

            String hostHeader = "internal-gw.example.test:" + server.getHttpDefaultPort();
            String response = sendRawGetWithHost(VHOST_SERVLET_PATH, hostHeader,
                    "$WSSN: public.example.test");
            LOG.info("testVhostRoutingWithSensitiveTrustedPeer response:\n" + response);

            assertTrue("Expected 200 OK — vhostApp should be served via sensitive trust",
                    response.contains("200"));
            assertEquals("APP_ID must be vhost — vhostApp must have answered the request",
                    "vhost", extractResponseField(response, "APP_ID"));
            assertEquals("SERVER_NAME should equal the $WSSN value approved via sensitive trust",
                    "public.example.test", extractResponseField(response, "SERVER_NAME"));
            String cacheControl = extractHeader(response, "Cache-Control");
            assertFalse("Cache-Control must not be no-store when $WSSN is approved via sensitive trust",
                    "no-store".equals(cacheControl));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }
}
