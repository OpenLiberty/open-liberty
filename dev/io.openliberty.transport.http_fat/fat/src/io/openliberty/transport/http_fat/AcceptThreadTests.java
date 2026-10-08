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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for the tcpOptions acceptThread and waitToAccept configuration options.
 *
 * acceptThread=true causes each endpoint to use its own dedicated accept event loop thread
 * rather than sharing one across all endpoints.
 *
 * waitToAccept=true delays accepting connections until the server is fully started,
 * and implicitly forces acceptThread=true.
 *
 * Trace sources:
 *  - Channelfw path: TCPChannelConfiguration.outputConfigToTrace() logs "acceptThread: <value>"
 *    and "waitToAccept: <value>" at startup (trace group TCPChannel=all).
 *  - Netty path: TCPUtils.createBootstrap() logs "acceptThread: true - using dedicated..."
 *    or "acceptThread: false - using shared..." and "waitToAccept: true/false - ..."
 *    every time a bootstrap is created (trace group io.openliberty.netty*=all).
 *
 * Both trace groups are enabled in bootstrap.properties so the same assertions work
 * on either transport path.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcceptThreadTests {

    private static final Logger LOG = Logger.getLogger(AcceptThreadTests.class.getName());

    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";

    // These strings appear in trace for BOTH channelfw (outputConfigToTrace) and
    // Netty (TCPUtils.createBootstrap debug lines) — the substring match is sufficient.
    private static final String TRACE_ACCEPT_THREAD_TRUE  = "acceptThread: true";
    private static final String TRACE_ACCEPT_THREAD_FALSE = "acceptThread: false";
    private static final String TRACE_WAIT_TO_ACCEPT_TRUE  = "waitToAccept: true";
    private static final String TRACE_WAIT_TO_ACCEPT_FALSE = "waitToAccept: false";

    // Netty-only trace strings (from TCPUtils.createBootstrap)
    private static final String NETTY_DEDICATED = "using dedicated accept EventLoopGroup";
    private static final String NETTY_SHARED    = "using shared accept EventLoopGroup";

    @Server("AcceptThread")
    public static LibertyServer server;

    private static boolean runningNetty = false;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer(AcceptThreadTests.class.getSimpleName() + ".log");

        // Detect transport: the TCP channel started message contains the Netty class name
        // when running on the Netty transport.
        String tcpMsg = server.waitForDefaultHTTPEndpointStart();
        runningNetty = tcpMsg != null && tcpMsg.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Save the baseline server configuration before each test.
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    /**
     * Restore to the saved baseline after each test so options set in one test
     * do not bleed into the next.
     */
    @After
    public void afterTest() throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    // -----------------------------------------------------------------------
    // Default value tests
    // -----------------------------------------------------------------------

    /**
     * Verifies that both acceptThread and waitToAccept default to false.
     * Resets log marks to 0 before searching so the startup trace lines are
     * visible regardless of which test ran first in the suite.
     */
    @Test
    public void testAcceptThreadAndWaitToAcceptDefaults() throws Exception {
        LOG.info("Verifying default acceptThread=false and waitToAccept=false in trace");
        server.resetLogMarks();
        assertNotNull("Default 'acceptThread: false' not found in trace",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_FALSE));
        assertNotNull("Default 'waitToAccept: false' not found in trace",
                      server.waitForStringInTraceUsingMark(TRACE_WAIT_TO_ACCEPT_FALSE));
    }

    // -----------------------------------------------------------------------
    // acceptThread=true
    // -----------------------------------------------------------------------

    /**
     * Sets acceptThread=true via dynamic config update and verifies:
     *   1. The value is reflected in trace after the update.
     *   2. On Netty: a dedicated event loop group is used (not the shared one).
     *   3. The endpoint is still reachable after the config change.
     */
    @Test
    public void testAcceptThreadTrue() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        HttpEndpoint endpoint = config.getHttpEndpoints().getById("defaultHttpEndpoint");
        endpoint.getTcpOptions().setAcceptThread(true);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        // The Netty TCPUtils bootstrap trace fires after the config update
        // and is anchored to the mark. Channelfw re-creates TCPChannelConfiguration
        // on each endpoint restart, so outputConfigToTrace also fires after the mark.
        assertNotNull("'acceptThread: true' not found in trace after config update",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_TRUE));

        if (runningNetty) {
            assertNotNull("Netty did not log dedicated accept group for acceptThread=true",
                          server.waitForStringInTraceUsingMark(NETTY_DEDICATED));
        }

        assertEndpointReachable();
    }

    /**
     * Dynamically switches acceptThread from false (default) to true and back to
     * false, verifying the trace reflects both transitions and the endpoint stays
     * reachable throughout.
     */
    @Test
    public void testAcceptThreadToggle() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        HttpEndpoint endpoint = config.getHttpEndpoints().getById("defaultHttpEndpoint");

        // --- Enable dedicated thread ---
        endpoint.getTcpOptions().setAcceptThread(true);
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNotNull("'acceptThread: true' not found after enabling",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_TRUE));
        assertEndpointReachable();

        // --- Disable, back to shared thread ---
        endpoint.getTcpOptions().setAcceptThread(false);
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNotNull("'acceptThread: false' not found after disabling",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_FALSE));

        if (runningNetty) {
            assertNotNull("Netty did not log shared accept group after disabling acceptThread",
                          server.waitForStringInTraceUsingMark(NETTY_SHARED));
        }
        assertEndpointReachable();
    }

    // -----------------------------------------------------------------------
    // waitToAccept=true — config validation
    // -----------------------------------------------------------------------

    /**
     * Sets waitToAccept=true and verifies:
     *   1. waitToAccept: true is in trace after the update.
     *   2. acceptThread is coerced to true (waitToAccept implies acceptThread).
     *   3. The endpoint is reachable — confirming the accept gate was lifted.
     */
    @Test
    public void testWaitToAcceptTrue() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        HttpEndpoint endpoint = config.getHttpEndpoints().getById("defaultHttpEndpoint");
        endpoint.getTcpOptions().setWaitToAccept(true);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNotNull("'waitToAccept: true' not found in trace after config update",
                      server.waitForStringInTraceUsingMark(TRACE_WAIT_TO_ACCEPT_TRUE));
        // waitToAccept forces acceptThread=true
        assertNotNull("'acceptThread: true' not found despite waitToAccept=true",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_TRUE));
        // Server must still accept connections after the startup gate is lifted.
        assertEndpointReachable();
    }

    /**
     * Sets both acceptThread=true and waitToAccept=true explicitly, verifying both
     * flags appear in trace and the endpoint remains reachable.
     */
    @Test
    public void testAcceptThreadAndWaitToAcceptCombined() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        HttpEndpoint endpoint = config.getHttpEndpoints().getById("defaultHttpEndpoint");
        endpoint.getTcpOptions().setAcceptThread(true);
        endpoint.getTcpOptions().setWaitToAccept(true);

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNotNull("'acceptThread: true' not found in combined test",
                      server.waitForStringInTraceUsingMark(TRACE_ACCEPT_THREAD_TRUE));
        assertNotNull("'waitToAccept: true' not found in combined test",
                      server.waitForStringInTraceUsingMark(TRACE_WAIT_TO_ACCEPT_TRUE));

        if (runningNetty) {
            assertNotNull("Netty did not log dedicated accept group in combined test",
                          server.waitForStringInTraceUsingMark(NETTY_DEDICATED));
        }

        assertEndpointReachable();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Sends a GET to the server root and asserts an HTTP response is received,
     * confirming the endpoint is accepting connections.
     */
    private void assertEndpointReachable() throws Exception {
        server.waitForDefaultHTTPEndpointStart();
        URL url = HttpUtils.createURL(server, "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try {
            int responseCode = conn.getResponseCode();
            assertTrue("Endpoint not reachable, unexpected HTTP response code: " + responseCode,
                       responseCode > 0);
            LOG.info("Endpoint reachable, HTTP response code: " + responseCode);
        } finally {
            conn.disconnect();
        }
    }
}
