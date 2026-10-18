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

    /** Set to true when a test has saved the server configuration and it must be restored. */
    private static boolean configSaved;

    @After
    public void restoreConfig() throws Exception {
        if (configSaved) {
            server.restoreServerConfiguration();
            configSaved = false;
        }
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

    /**
     * Verifies that a dynamic {@code routerThing} update is picked up correctly for wildcard patterns:
     * <ul>
     *   <li>Patterns removed from {@code routerThing} no longer route their messages.</li>
     *   <li>A pattern narrowed by including part of the number range ({@code CWLFB*W} →
     *       {@code CWLFB02*W}) only routes messages whose ID starts with the longer prefix;
     *       messages that matched the broader pattern but not the narrower one are excluded.</li>
     *   <li>Patterns retained across the update continue to route their messages.</li>
     *   <li>New patterns added by the update start routing their messages.</li>
     * </ul>
     *
     * <p>Initial {@code routerThing}: {@code CWLFA*,CWLFB*W,CWLFC*A,CWLFD*E}.<br>
     * Updated {@code routerThing}: {@code CWLFB02*W,CWLFC*A,CWLFG*,CWLFH*I}
     * (removed {@code CWLFA*} and {@code CWLFD*E}; narrowed {@code CWLFB*W} to
     * {@code CWLFB02*W}; retained {@code CWLFC*A}; added {@code CWLFG*} and {@code CWLFH*I}).
     */
    @Test
    public void testDynamicUpdateChangesWildcardPatterns() throws Exception {
        Log.info(c, "testDynamicUpdateChangesWildcardPatterns", "Applying dynamic wildcard routerThing update and verifying routing changes");

        // 1. Save server.xml so @After can restore it.
        server.saveServerConfiguration();
        configSaved = true;

        // 2. Apply the updated server.xml and wait for the config change to be processed.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("wsLogHandler/server-wildcardUpdate.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        // 3. Clear any messages captured before the update.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        // 4. Emit messages covering every relevant category.
        //    Unconfigured-prefix messages (CWLFP*, CWLFQ*, CWLFR*, CWLFS*) are interspersed
        //    throughout the batch to confirm the router ignores them regardless of position.
        String emitBody = "count=5&messages="
                // --- Removed pattern: CWLFA* ---
                // Previously matched CWLFA*; must NOT be routed after the update.
                + "CWLFA0001I: Test component alpha — removed prefix pattern must not route.,"
                // Unconfigured: CWLFP prefix was never in routerThing; must NOT be routed.
                + "CWLFP0001I: Test component papa — unconfigured prefix must not route.,"

                // --- Narrowed pattern: CWLFB*W -> CWLFB02*W ---
                // CWLFB0005W starts with CWLFB00, not CWLFB02; does NOT match CWLFB02*W; must NOT be routed.
                + "CWLFB0005W: Test component beta 00xx — broader prefix no longer matches.,"
                // CWLFB0050W starts with CWLFB00; also does NOT match CWLFB02*W; must NOT be routed.
                + "CWLFB0050W: Test component beta 00xx — another out-of-range number; must not route.,"
                // CWLFB0221W starts with CWLFB02; matches CWLFB02*W; must be routed.
                + "CWLFB0221W: Test component beta 02xx — narrowed prefix matches.,"
                // CWLFB0229W also starts with CWLFB02; matches CWLFB02*W; must be routed.
                + "CWLFB0229W: Test component beta 02xx — another narrowed-prefix match.,"
                // Unconfigured: CWLFQ prefix was never in routerThing; must NOT be routed.
                + "CWLFQ0003W: Test component quebec — unconfigured prefix must not route.,"

                // --- Retained pattern: CWLFC*A ---
                // Unchanged; must still be routed.
                + "CWLFC0010A: Test component charlie — retained audit pattern must route.,"
                // Unconfigured: CWLFR prefix was never in routerThing; must NOT be routed.
                + "CWLFR0002A: Test component romeo — unconfigured prefix must not route.,"

                // --- Removed pattern: CWLFD*E ---
                // Previously matched CWLFD*E; must NOT be routed after the update.
                + "CWLFD0003E: Test component delta — removed level-qualified pattern must not route.,"

                // --- New pattern: CWLFG* ---
                // Not in original routerThing; must be routed after the update (any level).
                + "CWLFG0001I: Test component golf — new plain prefix pattern must route.,"
                + "CWLFG0002W: Test component golf — new plain prefix any level must route.,"
                // Unconfigured: CWLFS prefix was never in routerThing; must NOT be routed.
                + "CWLFS0005I: Test component sierra — unconfigured prefix must not route.,"

                // --- New pattern: CWLFH*I ---
                // INFO level matches CWLFH*I; must be routed after the update.
                + "CWLFH0001I: Test component hotel — new INFO-only pattern must route.,"
                // WARNING level does NOT match CWLFH*I; must NOT be routed.
                + "CWLFH0001W: Test component hotel — WARNING does not match INFO-only pattern.";
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));
        
        // 5. Verify only the updated patterns' messages were captured.
        //    routerThing mirrors the updated server.xml value exactly.
        assertEquals("Verify should return 200 (PASSED) — only post-update patterns must be captured",
                     200, getStatus("/wsLogHandler/verify?routerThing=CWLFB02*W,CWLFC*A,CWLFG*,CWLFH*I"));
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);

        // 6. Assert that the removed, narrowed-out, and unconfigured messages were NOT captured.
        assertEquals("verifyAbsent should return 200 (PASSED) — removed, narrowed-out and unconfigured messages must not be routed",
                     200, getStatus("/wsLogHandler/verifyAbsent?patterns="
                             + "CWLFA*,"        // removed plain prefix
                             + "CWLFB0005W,"    // CWLFB*W narrowed; 00xx no longer matches
                             + "CWLFB0050W,"    // CWLFB*W narrowed; 00xx no longer matches
                             + "CWLFD*,"        // removed level-qualified ERROR
                             + "CWLFH0001W,"    // CWLFH*I is INFO-only; WARNING excluded
                             + "CWLFP*,"        // unconfigured prefix
                             + "CWLFQ*,"        // unconfigured prefix
                             + "CWLFR*,"        // unconfigured prefix
                             + "CWLFS*"));      // unconfigured prefix
    }
}
