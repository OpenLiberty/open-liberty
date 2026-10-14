/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.logging.osgi.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests verifying combined usage of exact message IDs and wildcard patterns
 * in the same routing configuration using standard Open Liberty message ID conventions.
 *
 * <p>Server is configured with {@code routerThing="CWWKF*,CWWKE0001I,CWWKS*A,CWWKZ0012E"}.
 */
@RunWith(FATRunner.class)
public class WsLogHandlerCombinedTest extends WsLogHandlerBaseTest {

    private static final Class<?> c = WsLogHandlerCombinedTest.class;

    @Server("WsLogHandlerCombinedServer")
    public static LibertyServer server;

    @Override
    protected LibertyServer getServer() {
        return server;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.installSystemBundle(BUNDLE);
        server.installSystemFeature(FEATURE);
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        server.uninstallSystemBundle(BUNDLE);
        server.uninstallSystemFeature(FEATURE);
    }

    /**
     * Verifies that the wsLogHandler-1.0 feature started and the REST endpoint is reachable.
     */
    @Test
    public void testEndpointReachable() throws Exception {
        Log.info(c, "testEndpointReachable", "Checking /wsLogHandler/ is reachable");
        int status = getStatus("/wsLogHandler/");
        assertEquals("Expected HTTP 200 from /wsLogHandler/", 200, status);
    }

    /**
     * Verifies that emitting a mix of messages matching wildcard patterns and exact IDs
     * captures all of them correctly under the combined configuration.
     */
    @Test
    public void testCombinedRoutingExactAndWildcard() throws Exception {
        Log.info(c, "testCombinedRoutingExactAndWildcard", "Testing combined wildcard and exact ID routing");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=10&messages=CWWKF0011I: Server is ready to run a smarter planet.,"
                + "CWWKE0001I: The server defaultServer has been launched.,"
                + "CWWKS1100A: Authentication audit event.,"
                + "CWWKZ0012E: Application failed to start.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWWKF*,CWWKE0001I,CWWKS*A,CWWKZ0012E");
        assertEquals("Verify should return 200 (PASSED) for combined configuration", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that an exact ID in the combined configuration (CWWKE0001I) can be
     * emitted and verified independently.
     */
    @Test
    public void testExactIdMatchedIndividuallyInCombinedConfig() throws Exception {
        Log.info(c, "testExactIdMatchedIndividuallyInCombinedConfig", "Testing exact ID capture in combined config");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWWKE0001I: The server defaultServer has been launched.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWWKE0001I");
        assertEquals("Verify should return 200 (PASSED) for exact ID CWWKE0001I", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that a level-qualified wildcard pattern in the combined configuration (CWWKS*A)
     * can be emitted and verified independently.
     */
    @Test
    public void testWildcardMatchedIndividuallyInCombinedConfig() throws Exception {
        Log.info(c, "testWildcardMatchedIndividuallyInCombinedConfig", "Testing wildcard capture in combined config");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWWKS1100A: Authentication audit event.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWWKS*A");
        assertEquals("Verify should return 200 (PASSED) for wildcard CWWKS*A", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that messages matching neither the exact IDs nor the wildcard patterns
     * in the combined configuration are excluded from routing.
     */
    @Test
    public void testUnmatchedMessagesExcludedInCombinedConfig() throws Exception {
        Log.info(c, "testUnmatchedMessagesExcludedInCombinedConfig", "Verifying unmatched messages are not routed in combined config");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWWKE0010W: Warning on kernel component.,"
                + "CWWKS4105W: LTPA configuration warning.,"
                + "CWWKZ0001I: Application sample started.,"
                + "CWWKO0219I: TCP Channel defaultHttpEndpoint has been started.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        assertEquals("Unmatched messages should not be captured", 0, getCapturedCount());
    }
}
