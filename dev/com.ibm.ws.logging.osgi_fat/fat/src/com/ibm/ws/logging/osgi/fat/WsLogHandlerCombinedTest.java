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

import java.util.Collections;
import java.util.List;

import org.junit.After;
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
 * in the same routing configuration using synthetic message IDs following the
 * standard Liberty syntax (CW prefix, alphabetic component code, numeric suffix,
 * and log level letter).
 *
 * <p>Server is configured with {@code routerThing="CWLFA*,CWLFB0001I,CWLFC*A,CWLFD0003E"}.
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

    /** Set to true when a test has saved the server configuration and it must be restored. */
    private static boolean configSaved;

    @After
    public void restoreConfig() throws Exception {
        if (configSaved) {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            configSaved = false;
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server == null) {
            return;
        }
        try {
            if (server.isStarted()) {
                server.stopServer("CWWKE0710W");
            }
        } finally {
            server.uninstallSystemBundle(BUNDLE);
            server.uninstallSystemFeature(FEATURE);
        }
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

        String emitBody = "count=10&messages=CWLFA0001I: Test component alpha initialized successfully.,"
                + "CWLFB0001I: Test component beta started successfully.,"
                + "CWLFC0010A: Test component charlie audit event recorded.,"
                + "CWLFD0003E: Test component delta failed to initialize subsystem.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWLFA*,CWLFB0001I,CWLFC*A,CWLFD0003E");
        assertEquals("Verify should return 200 (PASSED) for combined configuration", 200, verifyStatus);
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

        String emitBody = "count=5&messages=CWLFB0005W: Test component beta warning does not match exact ID.,"
                + "CWLFC0007W: Test component charlie warning does not match audit pattern.,"
                + "CWLFD0001I: Test component delta info does not match exact error ID.,"
                + "CWLFX0001I: Test component xray unconfigured prefix.";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        assertEquals("Unmatched messages should not be captured", 0, getCapturedCount());
    }

    /**
     * Verifies that a dynamic {@code routerThing} update combining exact IDs and wildcard
     * patterns is applied correctly at runtime, and that malformed wildcard patterns included
     * in the update are rejected with a {@code CWWKE0710W} warning while leaving valid
     * routing intact.
     *
     * <p>Initial {@code routerThing}: {@code CWLFA*,CWLFB0001I,CWLFC*A,CWLFD0003E}.<br>
     * Updated {@code routerThing}: {@code CWLFC*A,CWLFD0003E,CWLFE*W,CWLFM0007I,CWLFN**,CWLFO*Z}
     * <ul>
     *   <li>Removed: {@code CWLFA*} (wildcard), {@code CWLFB0001I} (exact)</li>
     *   <li>Retained: {@code CWLFC*A} (wildcard), {@code CWLFD0003E} (exact)</li>
     *   <li>Added valid: {@code CWLFE*W} (wildcard), {@code CWLFM0007I} (exact)</li>
     *   <li>Added invalid: {@code CWLFN**} (multiple asterisks), {@code CWLFO*Z} (bad level letter)</li>
     * </ul>
     *
     * <p>The two invalid patterns must each produce exactly one {@code CWWKE0710W} warning.
     * Their prefixes ({@code CWLFN}, {@code CWLFO}) must not appear in the captured message store.
     *
     * <p>Note: the {@code /verify} and {@code /verifyAbsent} endpoints accept only syntactically
     * valid patterns. The invalid patterns {@code CWLFN**} and {@code CWLFO*Z} are therefore
     * tested via their {@code CWWKE0710W} warning and through {@code verifyAbsent} using safe
     * prefix wildcards ({@code CWLFN*} and {@code CWLFO*}) rather than the malformed strings.
     */
    @Test
    public void testDynamicUpdateCombinedConfig() throws Exception {
        Log.info(c, "testDynamicUpdateCombinedConfig",
                 "Applying combined dynamic update with valid, removed, and invalid patterns");

        // 1. Save server.xml so @After can restore it cleanly.
        server.saveServerConfiguration();
        configSaved = true;

        // 2. Apply the updated server.xml and wait for the config change to settle.
        //    We also wait for a CWWKE0710W warning for each of the two invalid patterns.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("wsLogHandler/server-combinedUpdate.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet(),
                "CWWKE0710W: The wildcard message ID for CWLFN\\*\\* is invalid",
                "CWWKE0710W: The wildcard message ID for CWLFO\\*Z is invalid");

        // 3. Verify exactly one CWWKE0710W warning was emitted for each invalid pattern,
        //    and that no warning was emitted for any of the valid patterns.
        List<String> warningsNN = server.findStringsInLogs(
                "CWWKE0710W: The wildcard message ID for CWLFN\\*\\* is invalid");
        List<String> warningsOZ = server.findStringsInLogs(
                "CWWKE0710W: The wildcard message ID for CWLFO\\*Z is invalid");
        assertEquals("Expected exactly one CWWKE0710W for CWLFN**", 1, warningsNN.size());
        assertEquals("Expected exactly one CWWKE0710W for CWLFO*Z", 1, warningsOZ.size());

        // No warnings for any of the valid patterns.
        for (String validPattern : new String[] { "CWLFC*A", "CWLFD0003E", "CWLFE*W", "CWLFM0007I" }) {
            List<String> validWarnings = server.findStringsInLogs(
                    "CWWKE0710W.*" + validPattern.replace("*", "\\*"));
            assertTrue("No CWWKE0710W warning expected for valid pattern " + validPattern,
                       validWarnings.isEmpty());
        }

        // 4. Clear the captured store so pre-update early messages don't pollute the assertion.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        // 5. Emit messages covering every category.
        //    Completely unconfigured prefixes (CWLFP*, CWLFQ*) are interspersed to confirm
        //    the router ignores them regardless of their position in the batch.
        String emitBody = "count=30&messages="
                // --- Removed wildcard: CWLFA* ---
                // Previously matched CWLFA*; must NOT be routed after the update.
                + "CWLFA0001I: Test component alpha — removed wildcard prefix must not route.,"
                + "CWLFA0002W: Test component alpha — another removed wildcard message must not route.,"

                // --- Removed exact ID: CWLFB0001I ---
                // Previously matched as exact ID; must NOT be routed after the update.
                + "CWLFB0001I: Test component beta — removed exact ID must not route.,"
                // Unconfigured (interspersed): CWLFP prefix was never in routerThing.
                + "CWLFP0001I: Test component papa — unconfigured prefix must not route.,"

                // --- Retained wildcard: CWLFC*A ---
                // Unchanged; must still be routed.
                + "CWLFC0010A: Test component charlie — retained audit wildcard must route.,"
                + "CWLFC0003A: Test component charlie — second retained audit message must route.,"

                // --- Retained exact ID: CWLFD0003E ---
                // Unchanged; must still be routed.
                + "CWLFD0003E: Test component delta — retained exact ID must route.,"
                // Unconfigured (interspersed): CWLFQ prefix was never in routerThing.
                + "CWLFQ0002E: Test component quebec — unconfigured prefix must not route.,"

                // --- New wildcard: CWLFE*W ---
                // Not in original routerThing; WARNING-level messages must be routed after the update.
                + "CWLFE0001W: Test component echo — new WARNING wildcard must route.,"
                + "CWLFE0007W: Test component echo — second new WARNING wildcard message must route.,"
                // CWLFE0001I is INFO; does NOT match CWLFE*W; must NOT be routed.
                + "CWLFE0001I: Test component echo — INFO does not match the WARNING-only wildcard.,"

                // --- New exact ID: CWLFM0007I ---
                // Not in original routerThing; must be routed after the update.
                + "CWLFM0007I: Test component mike — new exact ID must route.,"
                // CWLFM0001I is a different exact ID under the same prefix; must NOT be routed.
                + "CWLFM0001I: Test component mike — different exact ID under same prefix must not route.,"

                // --- Invalid pattern targets: CWLFN** and CWLFO*Z ---
                // The router rejected both patterns; neither prefix should route anything.
                + "CWLFN0001I: Test component november — rejected invalid pattern must not route.,"
                + "CWLFO0001I: Test component oscar — rejected invalid pattern must not route.";
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));

        // 6. Verify that only messages matching the four valid post-update patterns were captured.
        //    The malformed patterns CWLFN** and CWLFO*Z are intentionally omitted from this
        //    call — the endpoint assumes all supplied patterns are syntactically valid.
        assertEquals("Verify should return 200 (PASSED) — only the four valid post-update patterns must be captured",
                     200, getStatus("/wsLogHandler/verify?routerThing=CWLFC*A,CWLFD0003E,CWLFE*W,CWLFM0007I"));
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);

        // 7. Assert that all excluded messages were NOT captured.
        //    Invalid patterns are represented here by safe syntactic equivalents (CWLFN*, CWLFO*)
        //    rather than the malformed strings themselves.
        assertEquals("verifyAbsent should return 200 (PASSED) — removed, level-mismatched, and unconfigured messages must not be routed",
                     200, getStatus("/wsLogHandler/verifyAbsent?patterns="
                             + "CWLFA*,"        // removed wildcard prefix
                             + "CWLFB0001I,"    // removed exact ID
                             + "CWLFE0001I,"    // CWLFE*W is WARNING-only; INFO excluded
                             + "CWLFM0001I,"    // different exact ID; only CWLFM0007I is configured
                             + "CWLFN*,"        // safe proxy for rejected CWLFN** pattern
                             + "CWLFO*,"        // safe proxy for rejected CWLFO*Z pattern
                             + "CWLFP*,"        // unconfigured prefix
                             + "CWLFQ*"));      // unconfigured prefix
    }
}
