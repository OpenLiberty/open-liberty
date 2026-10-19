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

import java.util.ArrayList;
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
 * FAT tests verifying that invalid wildcard message ID patterns are rejected by the
 * router and reported with warning CWWKE0710W.
 *
 * <p>The valid syntax is {@code <prefix>*[<level>]}: a non-empty prefix, exactly one
 * {@code *} as the last or second-to-last character, and an optional level letter
 * (I, A, W or E).
 *
 * <p>Each test starts the server it needs (see {@link #startServer(LibertyServer)}) and
 * {@link #stopServer()} stops it afterwards, because each scenario needs a different
 * {@code routerThing} and that is only read when the server starts:
 * <ul>
 *   <li>{@code WsLogHandlerInvalidWildcardServer} - only invalid patterns, see {@link #INVALID_PATTERNS}.</li>
 *   <li>{@code WsLogHandlerMixedWildcardServer} - valid and invalid patterns together.</li>
 * </ul>
 *
 * <p>CWWKE0710W is only emitted on beta builds; both servers' {@code jvm.options}
 * set {@code -Dcom.ibm.ws.beta.edition=true}.
 */
@RunWith(FATRunner.class)
public class WsLogHandlerInvalidWildcardTest extends WsLogHandlerBaseTest {

    private static final Class<?> c = WsLogHandlerInvalidWildcardTest.class;

    /**
     * Invalid wildcard patterns. Must match the routerThing value in
     * WsLogHandlerInvalidWildcardServer/server.xml.
     */
    private static final String[] INVALID_PATTERNS = {
        // Invalid log level
        "CWWF*K", "CWWKF*X", "CWWKF*1", "CWWKF*i",
        // Multiple asterisks
        "CWK**", "CWK**I", "CW*L*I", "CW*K*", "**",
        // Asterisk misplaced: followed by more than one character
        "CW*1I", "CWWKF*EFJ",
        // No prefix before the asterisk
        "*I"
    };

    /**
     * Valid patterns in the mixed server. Must match the routerThing value in
     * WsLogHandlerMixedWildcardServer/server.xml.
     */
    private static final String GOOD_PATTERNS = "CWLFA*,CWLFB*W,CWLFC0001I";

    /**
     * Invalid patterns in the mixed server. Must match the routerThing value in
     * WsLogHandlerMixedWildcardServer/server.xml.
     */
    private static final String[] BAD_PATTERNS = { "CWLFD*K", "CWLFE**" };

    /**
     * Valid patterns in WsLogHandlerWildcardServer/server.xml, kept by the dynamic update.
     */
    private static final String WILDCARD_SERVER_PATTERNS = "CWLFA*,CWLFB*W,CWLFC*A,CWLFD*E";

    /**
     * Invalid patterns added by the dynamic update. Must match the routerThing value in
     * publish/files/wsLogHandler/server-wildcardBadUpdate.xml.
     */
    private static final String[] DYNAMIC_BAD_PATTERNS = { "CWLFX*K", "CWLFY**" };

    /** Replacement server.xml, relative to the autoFVT test files directory (publish/files). */
    private static final String BAD_UPDATE_SERVER_XML = "wsLogHandler/server-wildcardBadUpdate.xml";

    @Server("WsLogHandlerInvalidWildcardServer")
    public static LibertyServer invalidServer;

    @Server("WsLogHandlerMixedWildcardServer")
    public static LibertyServer mixedServer;

    @Server("WsLogHandlerWildcardServer")
    public static LibertyServer wildcardServer;

    /** Set when a test replaced the server.xml of {@link #wildcardServer} and it must be restored. */
    private static boolean wildcardConfigSaved;

    /** The server started by the currently running test; used by the HTTP helpers. */
    private static LibertyServer activeServer;

    @Override
    protected LibertyServer getServer() {
        return activeServer;
    }

    /**
     * Installs the test bundle and feature once. No server is started here: each test
     * starts the server it needs.
     */
    @BeforeClass
    public static void installBundleAndFeature() throws Exception {
        invalidServer.installSystemBundle(BUNDLE);
        invalidServer.installSystemFeature(FEATURE);
    }

    @AfterClass
    public static void uninstallBundleAndFeature() throws Exception {
        invalidServer.uninstallSystemBundle(BUNDLE);
        invalidServer.uninstallSystemFeature(FEATURE);
    }

    /**
     * Stops whichever server the test started. The uninstall in {@link #uninstallBundleAndFeature()}
     * requires that no server is running.
     */
    @After
    public void stopServer() throws Exception {
        if (activeServer != null && activeServer.isStarted()) {
            // CWWKE0710W is the expected warning for the invalid patterns.
            activeServer.stopServer("CWWKE0710W");
        }
        activeServer = null;

        // WsLogHandlerWildcardServer is shared with WsLogHandlerWildcardTest, so put its server.xml back.
        if (wildcardConfigSaved) {
            wildcardServer.restoreServerConfiguration();
            wildcardConfigSaved = false;
        }
    }

    /**
     * Starts the given server and waits for it to be ready.
     */
    private static void startServer(LibertyServer server) throws Exception {
        activeServer = server;
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");
    }

    /**
     * Builds the regex for the warning naming exactly this pattern. It is anchored on the
     * text after the pattern so that, for example, the warning for {@code CWK**I} cannot
     * satisfy the check for {@code CWK**}.
     */
    private static String warningRegex(String pattern) {
        return "CWWKE0710W: The wildcard message ID for " + pattern.replace("*", "\\*") + " is invalid";
    }

    /**
     * Verifies that every invalid pattern in {@link #INVALID_PATTERNS} causes its own
     * CWWKE0710W warning in messages.log.
     *
     * <p>Every pattern is checked before asserting, so a failure lists all missing warnings.
     */
    @Test
    public void testInvalidWildcardWarnings() throws Exception {
        startServer(invalidServer);

        List<String> missing = new ArrayList<String>();

        for (String pattern : INVALID_PATTERNS) {
            List<String> lines = invalidServer.findStringsInLogs(warningRegex(pattern));
            Log.info(c, "testInvalidWildcardWarnings", pattern + " -> " + lines.size() + " warning(s)");
            if (lines.size() != 1) {
                missing.add(pattern + " (found " + lines.size() + ")");
            }
        }

        assertTrue("Expected exactly one CWWKE0710W warning for each invalid pattern; problems: " + missing,
                   missing.isEmpty());
    }

    /**
     * Verifies that when valid and invalid patterns are configured together:
     * <ol>
     *   <li>a CWWKE0710W warning is written for each invalid pattern, and none for the valid ones;</li>
     *   <li>messages matching the valid patterns are routed to the handler;</li>
     *   <li>messages that the invalid patterns would have matched, had they been accepted, are not routed.</li>
     * </ol>
     */
    @Test
    public void testValidPatternsRouteWhenInvalidPatternsPresent() throws Exception {
        startServer(mixedServer);

        // 1. Warnings: one per invalid pattern, none naming a valid pattern.
        List<String> problems = new ArrayList<String>();

        for (String pattern : BAD_PATTERNS) {
            List<String> lines = mixedServer.findStringsInLogs(warningRegex(pattern));
            Log.info(c, "testValidPatternsRouteWhenInvalidPatternsPresent", pattern + " -> " + lines.size() + " warning(s)");
            if (lines.size() != 1) {
                problems.add("invalid pattern " + pattern + " (found " + lines.size() + " warnings, expected 1)");
            }
        }

        assertTrue("Unexpected CWWKE0710W warnings: " + problems, problems.isEmpty());

        // 2. Emit messages that should be routed, and messages that should not.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        String emitBody = "count=10&messages="
                // Routed: match CWLFA*, CWLFB*W and the exact ID CWLFC0001I
                + "CWLFA0001I: Test component alpha started successfully.,"
                + "CWLFB0005W: Test component beta potential bottleneck detected.,"
                + "CWLFC0001I: Test component charlie exact ID message.,"
                // Not routed: right prefix but wrong level / different exact ID
                + "CWLFB0001I: Test component beta info does not match the warning-only pattern.,"
                + "CWLFC0002I: Test component charlie other ID does not match the exact ID.,"
                // Not routed: would match CWLFD*K / CWLFE** if those invalid patterns had been accepted
                + "CWLFD0001E: Test component delta must not route because its pattern is invalid.,"
                + "CWLFE0001I: Test component echo must not route because its pattern is invalid.";
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));

        // 3. Only messages matching the valid patterns were captured.
        assertEquals("Verify should return 200 (PASSED) - only messages matching the valid patterns were routed",
                     200, getStatus("/wsLogHandler/verify?routerThing=" + GOOD_PATTERNS));
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);

        // 4. None of the filtered messages were captured.
        assertEquals("verifyAbsent should return 200 (PASSED) - messages for invalid patterns and non-matching IDs must not be routed",
                     200, getStatus("/wsLogHandler/verifyAbsent?patterns=CWLFD*,CWLFE*,CWLFB*I,CWLFC0002I"));
    }

    /**
     * Verifies that invalid patterns added by a dynamic configuration update are handled the
     * same way as at startup. {@code WsLogHandlerWildcardServer} is started with valid patterns
     * only, then its server.xml is replaced with one that keeps those patterns and adds two
     * invalid ones ({@link #DYNAMIC_BAD_PATTERNS}). Checks that:
     * <ol>
     *   <li>no CWWKE0710W warning is written before the update;</li>
     *   <li>exactly one CWWKE0710W warning is written for each added invalid pattern;</li>
     *   <li>messages matching the pre-existing valid patterns are still routed;</li>
     *   <li>messages that the invalid patterns would have matched are not routed.</li>
     * </ol>
     */
    @Test
    public void testInvalidPatternsAddedByDynamicUpdate() throws Exception {
        startServer(wildcardServer);

        // 1. The initial configuration only has valid patterns.
        assertTrue("No CWWKE0710W warning expected before the update",
                   wildcardServer.findStringsInLogs("CWWKE0710W").isEmpty());

        // 2. Replace server.xml and wait for the update to finish and for each warning to be written.
        wildcardServer.saveServerConfiguration();
        wildcardConfigSaved = true;

        String[] warningRegexes = new String[DYNAMIC_BAD_PATTERNS.length];
        for (int i = 0; i < DYNAMIC_BAD_PATTERNS.length; i++) {
            warningRegexes[i] = warningRegex(DYNAMIC_BAD_PATTERNS[i]);
        }

        wildcardServer.setMarkToEndOfLog();
        wildcardServer.setServerConfigurationFile(BAD_UPDATE_SERVER_XML);
        wildcardServer.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet(), warningRegexes);

        // 3. Exactly one warning per added invalid pattern, and nothing else.
        List<String> problems = new ArrayList<String>();
        for (String pattern : DYNAMIC_BAD_PATTERNS) {
            List<String> lines = wildcardServer.findStringsInLogs(warningRegex(pattern));
            Log.info(c, "testInvalidPatternsAddedByDynamicUpdate", pattern + " -> " + lines.size() + " warning(s)");
            if (lines.size() != 1) {
                problems.add(pattern + " (found " + lines.size() + ")");
            }
        }
        int totalWarnings = wildcardServer.findStringsInLogs("CWWKE0710W").size();
        if (totalWarnings != DYNAMIC_BAD_PATTERNS.length) {
            problems.add("total CWWKE0710W warnings was " + totalWarnings + ", expected " + DYNAMIC_BAD_PATTERNS.length);
        }
        assertTrue("Expected exactly one CWWKE0710W warning for each pattern added by the update; problems: " + problems,
                   problems.isEmpty());

        // 4. Emit messages and check routing after the update.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        String emitBody = "count=10&messages="
                // Routed: match CWLFA*, CWLFB*W, CWLFC*A and CWLFD*E
                + "CWLFA0001I: Test component alpha started successfully.,"
                + "CWLFB0005W: Test component beta potential bottleneck detected.,"
                + "CWLFC0010A: Test component charlie audit event recorded.,"
                + "CWLFD0003E: Test component delta failed to initialize subsystem.,"
                // Not routed: would match CWLFX*K / CWLFY** if those invalid patterns had been accepted
                + "CWLFX0001E: Test component x-ray must not route because its pattern is invalid.,"
                + "CWLFY0001I: Test component yankee must not route because its pattern is invalid.";
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));

        assertEquals("Verify should return 200 (PASSED) - messages matching the valid patterns must still be routed",
                     200, getStatus("/wsLogHandler/verify?routerThing=" + WILDCARD_SERVER_PATTERNS));
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
        assertEquals("verifyAbsent should return 200 (PASSED) - messages for invalid patterns must not be routed",
                     200, getStatus("/wsLogHandler/verifyAbsent?patterns=CWLFX*,CWLFY*"));
    }
}
