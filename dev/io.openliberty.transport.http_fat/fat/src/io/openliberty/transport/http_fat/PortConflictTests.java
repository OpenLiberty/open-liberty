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

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
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

/**
 * Verifies that an httpEndpoint bound to a specific host is rejected when another httpEndpoint has
 * already bound the same port on the wildcard address.
 *
 * <pre>
 * &lt;httpEndpoint id="HttpEndpoint1" host="*"             httpPort="9085" httpsPort="9445"/&gt;
 * &lt;httpEndpoint id="HttpEndpoint2" host="my.host.name"   httpPort="9085" httpsPort="9445"/&gt;
 * </pre>
 *
 * The test runs in two steps so the bind order is deterministic. The server starts with only
 * HttpEndpoint1, which binds host * on the port. HttpEndpoint2 is then added with a configuration
 * update, binds the same port on a specific address, and must fail with CWWKO0221E rather than
 * silently succeeding.
 *
 * Note: Linux rejects this bind whether or not SO_REUSEADDR is set, so on Linux the test cannot tell the
 * fixed behavior from the broken one. On macOS, BSD and Windows, setting SO_REUSEADDR lets the second bind
 * succeed, so the test only fails without the fix on those platforms.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PortConflictTests {

    private static final String CLASS_NAME = PortConflictTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final String WILDCARD_ENDPOINT = "HttpEndpoint1";
    private static final String SPECIFIC_HOST_ENDPOINT = "HttpEndpoint2";

    @Server("PortConflict")
    public static LibertyServer server;

    /** Set by a test that changes the server configuration, so {@link #restoreConfiguration()} knows to undo it. */
    private boolean configurationChanged = false;

    @BeforeClass
    public static void setup() throws Exception {
        // Step 1: start with only HttpEndpoint1 so it is guaranteed to bind the port first.
        server.startServer(PortConflictTests.class.getSimpleName() + ".log");
    }

    /**
     * Undo any configuration change a test made, so tests do not depend on each other's leftover state
     * or on the order they run in.
     */
    @After
    public void restoreConfiguration() throws Exception {
        if (configurationChanged) {
            configurationChanged = false;
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // CWWKO0221E: TCP Channel HttpEndpoint2 initialization did not succeed.
            // The socket bind did not succeed for host <host name> and port <port>. The port might already be in use.
            server.stopServer("CWWKO0221E");
        }
    }

    /**
     * HttpEndpoint1 must bind host * on the port. HttpEndpoint2, added afterwards for the same port on a
     * specific host, must be rejected with CWWKO0221E. HttpEndpoint1 must stay up and keep accepting connections.
     */
    @Test
    public void testSpecificHostRejectedWhenWildcardBound() throws Exception {
        String httpPort = String.valueOf(server.getHttpDefaultPort());

        List<String> wildcardStarted = server.findStringsInLogs("CWWKO0219I:.* " + WILDCARD_ENDPOINT + " .*port " + httpPort);
        assertTrue("HttpEndpoint1 should have started on host * and port " + httpPort, !wildcardStarted.isEmpty());

        int bindErrorsBefore = server.findStringsInLogs("CWWKO0221E:.* " + WILDCARD_ENDPOINT + " ").size();

        // Step 2: add HttpEndpoint2 on the same ports. Use this machine's host name, since a fixed host name
        // in server.xml would only work on one machine.
        InetAddress localHost = InetAddress.getLocalHost();
        String localHostName = localHost.getHostName();
        LOG.info("Adding " + SPECIFIC_HOST_ENDPOINT + " on host " + localHostName + " (address " + localHost.getHostAddress()
                 + ", loopback=" + localHost.isLoopbackAddress() + ") and port " + httpPort);

        server.saveServerConfiguration();
        configurationChanged = true;

        ServerConfiguration configuration = server.getServerConfiguration();
        HttpEndpoint specificHostEndpoint = new HttpEndpoint();
        specificHostEndpoint.setId(SPECIFIC_HOST_ENDPOINT);
        specificHostEndpoint.setHost(localHostName);
        specificHostEndpoint.setHttpPort(httpPort);
        specificHostEndpoint.setHttpsPort(String.valueOf(server.getHttpDefaultSecurePort()));
        configuration.getHttpEndpoints().add(specificHostEndpoint);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        assertNotNull("The configuration update adding " + SPECIFIC_HOST_ENDPOINT + " did not complete",
                      server.waitForConfigUpdateInLogUsingMark(null));

        assertNotNull("HttpEndpoint2 should have been rejected with a bind error because port " + httpPort + " is already bound on host *",
                      server.waitForStringInLogUsingMark("CWWKO0221E:.* " + SPECIFIC_HOST_ENDPOINT + " .*port " + httpPort));

        List<String> startedMessages = server.findStringsInLogs("CWWKO0219I:.* " + SPECIFIC_HOST_ENDPOINT + " ");
        assertTrue("HttpEndpoint2 must not report that it started, but found: " + startedMessages, startedMessages.isEmpty());

        // The wildcard endpoint must be unaffected by the rejected endpoint.
        try (Socket socket = new Socket("localhost", server.getHttpDefaultPort())) {
            assertTrue("HttpEndpoint1 should still accept connections", socket.isConnected());
        }

        List<String> bindErrors = server.findStringsInLogs("CWWKO0221E:.* " + WILDCARD_ENDPOINT + " ");
        assertEquals("HttpEndpoint1 must not report a new bind error: " + bindErrors, bindErrorsBefore, bindErrors.size());
    }
}
