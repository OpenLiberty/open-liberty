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
 * FAT tests verifying wildcard message ID routing (both prefix and level-qualified)
 * using standard Open Liberty message ID conventions.
 *
 * <p>Server is configured with {@code routerThing="CWWKF*,CWWKE*W,CWWKS*A,CWWKZ*E"}.
 */
@RunWith(FATRunner.class)
public class WsLogHandlerWildcardTest extends WsLogHandlerBaseTest {

    private static final Class<?> c = WsLogHandlerWildcardTest.class;

    @Server("WsLogHandlerWildcardServer")
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
     * Verifies that the prefix wildcard pattern CWWKF* captures all messages starting with CWWKF.
     */
    @Test
    public void testWildcardPrefixPatternCapture() throws Exception {
        Log.info(c, "testWildcardPrefixPatternCapture", "Testing CWWKF* prefix wildcard capture");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=10&messages=CWWKF0011I: Server is ready to run a smarter planet.,"
                + "CWWKF0012I: The server installed the following features: [wsLogHandler-1.0].,"
                + "CWWKF0001A: Audit event recorded on feature manager.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWWKF*");
        assertEquals("Verify should return 200 (PASSED) for CWWKF* wildcard", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that level-qualified wildcard patterns (CWWKE*W, CWWKS*A, CWWKZ*E)
     * correctly capture matching messages with their corresponding log levels.
     */
    @Test
    public void testWildcardLevelQualifiedPatternsCapture() throws Exception {
        Log.info(c, "testWildcardLevelQualifiedPatternsCapture", "Testing level-qualified wildcard patterns");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=10&messages=CWWKE0010W: Warning on kernel component.,"
                + "CWWKS1100A: Authentication audit event.,"
                + "CWWKZ0002E: An exception occurred while starting the application.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWWKE*W,CWWKS*A,CWWKZ*E");
        assertEquals("Verify should return 200 (PASSED) for level-qualified wildcards", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that messages matching a prefix but NOT the required level are NOT routed.
     * (e.g. CWWKE0001I is INFO while CWWKE*W is configured; CWWKZ0001I is INFO while CWWKZ*E is configured).
     */
    @Test
    public void testWildcardLevelMismatchNotCaptured() throws Exception {
        Log.info(c, "testWildcardLevelMismatchNotCaptured", "Verifying messages with mismatched level are not routed");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWWKE0001I: The server defaultServer has been launched.,"
                + "CWWKS4105W: LTPA configuration warning.,"
                + "CWWKZ0001I: Application sample started.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        assertEquals("Messages with non-matching level should not be captured", 0, getCapturedCount());
    }

    /**
     * Verifies that messages with unconfigured prefixes (e.g. CWWKO*, CWWKT*) are NOT routed.
     */
    @Test
    public void testUnmatchedWildcardPrefixNotCaptured() throws Exception {
        Log.info(c, "testUnmatchedWildcardPrefixNotCaptured", "Verifying unconfigured prefixes are not routed");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWWKO0219I: TCP Channel defaultHttpEndpoint has been started.,"
                + "CWWKT0016I: Web application available.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        assertEquals("Unconfigured prefix messages should not be captured", 0, getCapturedCount());
    }
}
