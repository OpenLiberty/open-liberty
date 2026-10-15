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
 * using synthetic message IDs following the standard Liberty syntax
 * (CW prefix, alphabetic component code, 0-10 numeric range, and log level letter).
 *
 * <p>Server is configured with {@code routerThing="CWLFA*,CWLFB*W,CWLFC*A,CWLFD*E"}.
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
     * Verifies that the prefix wildcard (CWLFA*) and the level-qualified wildcards
     * (CWLFB*W, CWLFC*A, CWLFD*E) configured in server.xml all capture their matching messages.
     */
    @Test
    public void testWildcardPatternsCapture() throws Exception {
        Log.info(c, "testWildcardPatternsCapture", "Testing prefix and level-qualified wildcard capture");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=10&messages=CWLFA0001I: Test component alpha initialized successfully.,"
                + "CWLFA0002I: Test component alpha registered configuration.,"
                + "CWLFA0003A: Test component alpha audit checkpoint recorded.,"
                + "CWLFB0005W: Test component beta potential bottleneck detected.,"
                + "CWLFC0010A: Test component charlie audit event recorded.,"
                + "CWLFD0003E: Test component delta failed to initialize subsystem.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        // Same value as routerThing in WsLogHandlerWildcardServer/server.xml
        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWLFA*,CWLFB*W,CWLFC*A,CWLFD*E");
        assertEquals("Verify should return 200 (PASSED) for the server.xml wildcard patterns", 200, verifyStatus);
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies that messages matching a prefix but NOT the required level are NOT routed.
     * (e.g. CWLFB0001I is INFO while CWLFB*W is configured; CWLFD0001I is INFO while CWLFD*E is configured).
     */
    @Test
    public void testWildcardLevelMismatchNotCaptured() throws Exception {
        Log.info(c, "testWildcardLevelMismatchNotCaptured", "Verifying messages with mismatched level are not routed");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWLFB0001I: Test component beta info message.,"
                + "CWLFC0007W: Test component charlie warning does not match audit pattern.,"
                + "CWLFD0001I: Test component delta info message.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int absentStatus = getStatus("/wsLogHandler/verifyAbsent?patterns=CWLFB0001I,CWLFC0007W,CWLFD0001I");
        assertEquals("verifyAbsent should return 200 (PASSED) — messages with non-matching level must not be captured",
                     200, absentStatus);
    }

    /**
     * Verifies that messages with unconfigured prefixes (e.g. CWLFX*, CWLFY*) are NOT routed.
     */
    @Test
    public void testUnmatchedWildcardPrefixNotCaptured() throws Exception {
        Log.info(c, "testUnmatchedWildcardPrefixNotCaptured", "Verifying unconfigured prefixes are not routed");

        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        String emitBody = "count=5&messages=CWLFX0001I: Test component xray started successfully.,"
                + "CWLFY0002W: Test component yankee configuration warning.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int absentStatus = getStatus("/wsLogHandler/verifyAbsent?patterns=CWLFX*,CWLFY*");
        assertEquals("verifyAbsent should return 200 (PASSED) — unconfigured prefix messages must not be captured",
                     200, absentStatus);
    }
}
