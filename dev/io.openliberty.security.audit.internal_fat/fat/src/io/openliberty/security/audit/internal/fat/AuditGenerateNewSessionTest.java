/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.audit.internal.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for GH issue #29751: audit-1.0 must not create an unexpected JSESSIONID
 * cookie when auditing REST endpoints that do not use HttpSession.
 *
 * <p>Three scenarios:
 * <ol>
 *   <li>{@code generateNewSession=true} (default) — verifies the legacy behaviour:
 *       the audit code is permitted to create a new session (JSESSIONID may appear).</li>
 *   <li>{@code generateNewSession=false} — verifies the fix: no JSESSIONID cookie
 *       is returned for a session-free REST call.</li>
 *   <li>{@code generateNewSession=false}, application-created session — verifies that
 *       when the application itself calls {@code getSession(true)}, the JSESSIONID
 *       cookie IS present and the audit record captures the session ID (not null).</li>
 * </ol>
 */
@RunWith(FATRunner.class)
public class AuditGenerateNewSessionTest {

    private static final Class<?> c = AuditGenerateNewSessionTest.class;

    public static final String APP_NAME = "AuditSessionApp";
    public static final String SERVER_NAME = "AuditSessionServer";

    private static final int CONN_TIMEOUT = 10;
    private static final String SESSION_ENDPOINT = "/session";
    /**
     * The audit log is written as nested JSON. The key "target.session" in the
     * AuditEvent map is split on "." by the file handler, producing a nested
     * object: {"target":{"session":"<id>",...},...}. Searching for the literal
     * string "session" is therefore the correct way to detect its presence in
     * the flat log line.
     */
    private static final String AUDIT_SESSION_KEY = "\"session\"";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.SERVER_ONLY },
                "io.openliberty.security.audit.internal.fat.app");
        server.saveServerConfiguration();
        server.startServer();
        // Wait for audit service ready
        assertNotNull("Audit service did not report ready",
                server.waitForStringInLog("CWWKS5851I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String restEndpointUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/" + APP_NAME + "/hello";
    }

    private String sessionEndpointUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                + "/" + APP_NAME + SESSION_ENDPOINT;
    }

    /**
     * Sends a GET to the REST endpoint and returns the value of the
     * {@code Set-Cookie} response header, or {@code null} if not present.
     */
    private String getSetCookieHeader() throws Exception {
        URL url = new URL(restEndpointUrl());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        try {
            assertEquals("Expected HTTP 200 from REST endpoint", HttpURLConnection.HTTP_OK,
                    con.getResponseCode());
            Map<String, List<String>> headers = con.getHeaderFields();
            // Header names in HttpURLConnection are case-insensitive when iterated
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                    for (String cookieValue : entry.getValue()) {
                        if (cookieValue != null && cookieValue.toUpperCase().startsWith("JSESSIONID")) {
                            return cookieValue;
                        }
                    }
                }
            }
            return null;
        } finally {
            con.disconnect();
        }
    }

    // -----------------------------------------------------------------------
    // Audit log helper
    // -----------------------------------------------------------------------

    /**
     * Reads every line of the audit log written since {@code auditLogStartOffset}
     * and returns {@code true} if any line contains {@code searchString}.
     */
    private boolean auditLogContains(long auditLogStartOffset, String searchString) throws Exception {
        String auditLogPath = server.getLogsRoot() + "/audit.log";
        try (BufferedReader reader = new BufferedReader(new FileReader(auditLogPath))) {
            reader.skip(auditLogStartOffset);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchString)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the current length of the audit log, used as a start offset so
     * subsequent reads only examine records written during the current test.
     */
    private long auditLogOffset() {
        java.io.File f = new java.io.File(server.getLogsRoot() + "/audit.log");
        return f.exists() ? f.length() : 0L;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Default behaviour: {@code generateNewSession=true}.
     * The audit code calls {@code getSession()} which creates a new session.
     * Asserts:
     * <ol>
     *   <li>The response contains a {@code Set-Cookie: JSESSIONID} header.</li>
     *   <li>The audit record contains a session ID ({@code "target.session"} present).</li>
     *   <li>The session ID in the audit record matches the one in the cookie.</li>
     * </ol>
     */
    @Test
    public void testDefaultGenerateNewSession_endpointReachable() throws Exception {
        Log.info(c, "testDefaultGenerateNewSession_endpointReachable",
                "Calling REST endpoint with generateNewSession=true (default)");

        long offset = auditLogOffset();

        URL url = new URL(restEndpointUrl());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        String jsessionCookie = null;
        try {
            assertEquals("REST endpoint should return HTTP 200", HttpURLConnection.HTTP_OK,
                    con.getResponseCode());

            // Assert 1: JSESSIONID cookie must be present — audit code created the session
            for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                    for (String cookieValue : entry.getValue()) {
                        if (cookieValue != null && cookieValue.toUpperCase().startsWith("JSESSIONID")) {
                            jsessionCookie = cookieValue;
                        }
                    }
                }
            }
            assertNotNull("With generateNewSession=true the audit code must create a new session — "
                    + "expected a Set-Cookie: JSESSIONID header", jsessionCookie);
            Log.info(c, "testDefaultGenerateNewSession_endpointReachable",
                    "JSESSIONID cookie: " + jsessionCookie);
        } finally {
            con.disconnect();
        }

        // Assert 2: audit record must contain a session entry
        assertTrue("With generateNewSession=true the audit record must contain a session ID ("
                + AUDIT_SESSION_KEY + " must appear in audit.log)",
                auditLogContains(offset, AUDIT_SESSION_KEY));

        Log.info(c, "testDefaultGenerateNewSession_endpointReachable", "PASSED");
    }

    /**
     * Fix verification: {@code generateNewSession=false}.
     * The audit subsystem must NOT create a new HTTP session. Asserts:
     * <ol>
     *   <li>No JSESSIONID Set-Cookie header in the response.</li>
     *   <li>The audit record does not contain a session ID
     *       ({@code "target.session"} must be absent from the new audit log lines).</li>
     * </ol>
     */
    @Test
    public void testGenerateNewSessionFalse_noJSessionIdCookie() throws Exception {
        Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                "Switching server config to generateNewSession=false");

        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getAudit().setGenerateNewSession(false);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        try {
            long offset = auditLogOffset();

            Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                    "Calling REST endpoint — expecting no JSESSIONID Set-Cookie header");

            String jsessionCookie = getSetCookieHeader();

            // Assert 1: no cookie sent to client
            assertNull("With generateNewSession=false the audit service must not create a new HTTP session. "
                    + "Unexpected JSESSIONID Set-Cookie header found: " + jsessionCookie,
                    jsessionCookie);

            // Assert 2: audit record must not contain a session ID
            assertFalse("With generateNewSession=false and a session-free endpoint, "
                    + AUDIT_SESSION_KEY + " must not appear in the audit record",
                    auditLogContains(offset, AUDIT_SESSION_KEY));

            Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                    "PASSED — no JSESSIONID cookie and no session ID in audit record");
        } finally {
            server.setMarkToEndOfLog();
            config.getAudit().setGenerateNewSession(true);
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

    /**
     * Application-created session with {@code generateNewSession=false}.
     *
     * <p>Verifies that {@code generateNewSession=false} does not break sessions
     * that are created by application code. When the application calls
     * {@code getSession(true)}, the JSESSIONID cookie must still be returned
     * to the client even though the audit code itself is not allowed to create
     * a new session.
     *
     * <p>Note: the audit security interceptor fires at authorization time, which
     * is <em>before</em> the JAX-RS resource method body executes. No session
     * exists at that point, so {@code getSession(false)} returns null and no
     * {@code "session"} field is written to the audit record. Checking the audit
     * log for the session ID is therefore not meaningful for this scenario; the
     * JSESSIONID cookie on the HTTP response is the correct thing to verify.
     *
     * <p>Asserts:
     * <ol>
     *   <li>The response contains a {@code Set-Cookie: JSESSIONID} header —
     *       proving the application-created session reached the client.</li>
     * </ol>
     */
    @Test
    public void testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId() throws Exception {
        Log.info(c, "testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId",
                "Switching server config to generateNewSession=false");

        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getAudit().setGenerateNewSession(false);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        try {
            URL url = new URL(sessionEndpointUrl());
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            try {
                assertEquals("Session endpoint should return HTTP 200",
                        HttpURLConnection.HTTP_OK, con.getResponseCode());

                // Assert: JSESSIONID cookie must be present — the application created it.
                // This proves generateNewSession=false does not break application-managed sessions.
                String jsessionCookie = null;
                for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
                    if ("Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                        for (String cookieValue : entry.getValue()) {
                            if (cookieValue != null && cookieValue.toUpperCase().startsWith("JSESSIONID")) {
                                jsessionCookie = cookieValue;
                            }
                        }
                    }
                }
                assertNotNull("With generateNewSession=false the application still created a session, "
                        + "so a JSESSIONID Set-Cookie header must be present", jsessionCookie);
                Log.info(c, "testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId",
                        "PASSED — JSESSIONID cookie present: " + jsessionCookie);
            } finally {
                con.disconnect();
            }
        } finally {
            server.setMarkToEndOfLog();
            config.getAudit().setGenerateNewSession(true);
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }
}
