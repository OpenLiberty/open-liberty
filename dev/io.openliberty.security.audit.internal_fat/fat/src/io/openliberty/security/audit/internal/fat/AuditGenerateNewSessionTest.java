/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.audit.internal.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

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

    /** Config snippet that sets generateNewSession=false */
    private static final String SERVER_XML_NO_NEW_SESSION = "generateNewSession_false.xml";

    private static final int CONN_TIMEOUT = 10;
    private static final String SESSION_ENDPOINT = "/session";

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
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Default behaviour: {@code generateNewSession=true}.
     * The audit subsystem is allowed to create a new session, so a JSESSIONID
     * Set-Cookie header MAY appear. We simply verify the endpoint is reachable
     * and the server is stable — we do not assert on cookie presence because
     * the legacy behaviour is preserved deliberately.
     */
    @Test
    public void testDefaultGenerateNewSession_endpointReachable() throws Exception {
        Log.info(c, "testDefaultGenerateNewSession_endpointReachable",
                "Calling REST endpoint with generateNewSession=true (default)");

        URL url = new URL(restEndpointUrl());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        try {
            assertEquals("REST endpoint should return HTTP 200", HttpURLConnection.HTTP_OK,
                    con.getResponseCode());
        } finally {
            con.disconnect();
        }
        Log.info(c, "testDefaultGenerateNewSession_endpointReachable", "PASSED");
    }

    /**
     * Fix verification: {@code generateNewSession=false}.
     * The audit subsystem must NOT create a new HTTP session. No JSESSIONID
     * Set-Cookie header should appear in the response from a session-free REST
     * endpoint.
     */
    @Test
    public void testGenerateNewSessionFalse_noJSessionIdCookie() throws Exception {
        Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                "Switching server config to generateNewSession=false");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SERVER_XML_NO_NEW_SESSION);
        // Wait for config update to take effect
        server.waitForConfigUpdateInLogUsingMark(null);

        try {
            Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                    "Calling REST endpoint — expecting no JSESSIONID Set-Cookie header");

            String jsessionCookie = getSetCookieHeader();

            assertNull("With generateNewSession=false the audit service must not create a new HTTP session. "
                    + "Unexpected JSESSIONID Set-Cookie header found: " + jsessionCookie,
                    jsessionCookie);

            Log.info(c, "testGenerateNewSessionFalse_noJSessionIdCookie",
                    "PASSED — no JSESSIONID cookie returned");
        } finally {
            // Restore the original server configuration for subsequent tests
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

    /**
     * Application-created session with {@code generateNewSession=false}.
     *
     * <p>When the application itself calls {@code getSession(true)}, a session IS
     * created — that is intentional application behaviour and must be preserved.
     * The audit service must then record that session ID (not null) even though
     * {@code generateNewSession=false} prevents the audit code from creating
     * sessions on its own.
     *
     * <p>Asserts:
     * <ol>
     *   <li>The response contains a {@code Set-Cookie: JSESSIONID} header —
     *       proving the application-created session reached the client.</li>
     *   <li>The audit log contains the same session ID in the audit record —
     *       proving the audit service read the existing session rather than
     *       recording null.</li>
     * </ol>
     */
    @Test
    public void testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId() throws Exception {
        Log.info(c, "testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId",
                "Switching server config to generateNewSession=false");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SERVER_XML_NO_NEW_SESSION);
        server.waitForConfigUpdateInLogUsingMark(null);

        try {
            URL url = new URL(sessionEndpointUrl());
            HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
            String sessionIdFromResponse = null;
            try {
                assertEquals("Session endpoint should return HTTP 200",
                        HttpURLConnection.HTTP_OK, con.getResponseCode());

                // The response body is the session ID returned by the application
                java.io.InputStream is = con.getInputStream();
                sessionIdFromResponse = new java.util.Scanner(is).useDelimiter("\\A").next().trim();
                Log.info(c, "testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId",
                        "Session ID from response body: " + sessionIdFromResponse);

                // Assert 1: JSESSIONID cookie must be present — the application created it
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
                        "JSESSIONID cookie found: " + jsessionCookie);
            } finally {
                con.disconnect();
            }

            // Assert 2: the audit log must contain the session ID — not null
            assertNotNull("Session ID from application must not be null or empty", sessionIdFromResponse);
            assertNotNull("Audit log must contain the application-created session ID: " + sessionIdFromResponse,
                    server.waitForStringInLog(sessionIdFromResponse));

            Log.info(c, "testGenerateNewSessionFalse_appCreatesSession_auditRecordsSessionId",
                    "PASSED — JSESSIONID cookie present and audit log contains session ID");
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }
}
