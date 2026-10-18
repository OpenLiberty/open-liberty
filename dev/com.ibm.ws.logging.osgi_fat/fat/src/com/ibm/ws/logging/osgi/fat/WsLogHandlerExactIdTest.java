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
 * FAT tests verifying exact message ID routing (the existing/old behavior)
 * using synthetic message IDs following the standard Liberty syntax
 * (CW prefix, alphabetic component code, 0-10 numeric range, and log level letter).
 *
 * <p>Server is configured with {@code routerThing="CWXYZ0001I,CWTRC0005W,CWEVT0010E"}.
 */
@RunWith(FATRunner.class)
public class WsLogHandlerExactIdTest extends WsLogHandlerBaseTest {

    private static final Class<?> c = WsLogHandlerExactIdTest.class;

    @Server("WsLogHandlerExactServer")
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
     * Verifies that configured exact IDs are captured while unexpected/unconfigured IDs
     * are filtered out by the router in a single end-to-end emission and verification cycle.
     */
    @Test
    public void testExactIdRoutingAndFiltering() throws Exception {
        Log.info(c, "testExactIdRoutingAndFiltering", "Emitting valid exact IDs and unexpected IDs, then verifying");

        // 1. Clear any pre-existing messages
        int clearStatus = getStatus("/wsLogHandler/clear");
        assertEquals("Clear should return 200", 200, clearStatus);

        // 2. Emit a batch containing both configured exact IDs and unexpected/unconfigured IDs
        String emitBody = "count=10&messages="
                + "CWXYZ0001I: System component initialized successfully.,"
                + "CWTRC0005W: Potential bottleneck detected in subsystem.,"
                + "CWEVT0010E: Failed to process inbound data event.,"
                + "CWABC0002I: Secondary service registered (unexpected).,"
                + "CWDEF0007W: Memory buffer nearing target capacity (unexpected).";
        int emitStatus = postForm("/wsLogHandler/emit/text", emitBody);
        assertEquals("Emit should return 200", 200, emitStatus);

        // 3. Verify against the configured exact IDs.
        //    Returns HTTP 200 (PASSED) if all captured messages match the configured IDs
        //    and no unexpected messages were erroneously captured.
        int verifyStatus = getStatus("/wsLogHandler/verify?routerThing=CWXYZ0001I,CWTRC0005W,CWEVT0010E");
        assertEquals("Verify should return 200 (PASSED) — valid messages captured and unexpected messages excluded",
                     200, verifyStatus);

        // 4. Ensure that valid messages were actually received
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);

        // 5. Assert that the unconfigured IDs were NOT routed to the handler.
        //    CWABC0002I and CWDEF0007W are not in routerThing, so the router must have
        //    filtered them out — verifyAbsent returns 200 only if neither appears in the store.
        int absentStatus = getStatus("/wsLogHandler/verifyAbsent?patterns=CWABC0002I,CWDEF0007W");
        assertEquals("verifyAbsent should return 200 (PASSED) — unconfigured IDs must not have been captured",
                     200, absentStatus);
    }

    /**
     * Verifies that a dynamic {@code routerThing} update is picked up at runtime:
     * <ul>
     *   <li>IDs removed from {@code routerThing} are no longer routed after the update.</li>
     *   <li>IDs added to {@code routerThing} start being routed after the update.</li>
     *   <li>IDs retained across the update continue to be routed.</li>
     * </ul>
     *
     * <p>Initial {@code routerThing}: {@code CWXYZ0001I,CWTRC0005W,CWEVT0010E}.<br>
     * Updated {@code routerThing}: {@code CWTRC0005W,CWUPD0001I,CWUPD0002E}
     * (removed {@code CWXYZ0001I} and {@code CWEVT0010E}; retained {@code CWTRC0005W};
     * added {@code CWUPD0001I} and {@code CWUPD0002E}).
     */
    @Test
    public void testDynamicUpdateChangesRoutedIds() throws Exception {
        Log.info(c, "testDynamicUpdateChangesRoutedIds", "Applying dynamic routerThing update and verifying routing changes");

        // 1. Save server.xml so @After can restore it.
        server.saveServerConfiguration();
        configSaved = true;

        // 2. Apply the updated server.xml (CWXYZ0001I and CWEVT0010E removed; CWUPD0001I and CWUPD0002E added).
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("wsLogHandler/server-exactUpdate.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        // 3. Clear any messages captured before the update.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        // 4. Emit all five IDs: the two removed ones, the retained one, and the two new ones.
        String emitBody = "count=5&messages="
                + "CWXYZ0001I: Old ID that was removed from routerThing.,"   // removed — must NOT be routed
                + "CWEVT0010E: Old ID that was removed from routerThing.,"   // removed — must NOT be routed
                + "CWTRC0005W: Retained ID that is still in routerThing.,"   // retained — must be routed
                + "CWUPD0001I: New ID added to routerThing by the update.,"  // added — must be routed
                + "CWUPD0002E: New ID added to routerThing by the update.";  // added — must be routed
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));

        // 5. Verify that only the updated set of IDs was routed.
        assertEquals("Verify should return 200 (PASSED) — only the updated routerThing IDs must be captured",
                     200, getStatus("/wsLogHandler/verify?routerThing=CWTRC0005W,CWUPD0001I,CWUPD0002E"));
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }

    /**
     * Verifies the special case where {@code routerThing="*"} is configured, which routes
     * ALL log messages across any component prefix and log level to the handler.
     */
    @Test
    public void testRouteAllStarCapturesAllMessages() throws Exception {
        Log.info(c, "testRouteAllStarCapturesAllMessages", "Applying routerThing=\"*\" update and verifying all messages are captured");

        final int emitCount = 10;
        
        // 1. Save server.xml so @After can restore it.
        server.saveServerConfiguration();
        configSaved = true;

        // 2. Apply the updated server.xml with routerThing="*".
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("wsLogHandler/server-exactAllUpdate.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        // 3. Clear any messages captured before the update.
        assertEquals("Clear should return 200", 200, getStatus("/wsLogHandler/clear"));

        // 4. Emit a diverse batch of messages with different component prefixes and severity levels.
        String emitBody = "count="+emitCount+"&messages="
                + "CWAAA0001I: Test component alpha info message.,"
                + "CWBBB0002W: Test component beta warning message.,"
                + "CWCCC0003E: Test component charlie error message.,"
                + "CWDDD0004A: Test component delta audit message.,"
                + "CWXYZ9999I: Test component custom info message.";
        assertEquals("Emit should return 200", 200, postForm("/wsLogHandler/emit/text", emitBody));        

        
        
        // 5. Verify that there are emitCount messages and more
        assertTrue("Captured count should be greater than 0", getCapturedCount() > emitCount);

        // 6. Also verify that each emitted ID was captured.
        assertEquals("Verify should return 200 (PASSED) for all emitted IDs",
                     200, getStatus("/wsLogHandler/verify?routerThing=CWAAA0001I,CWBBB0002W,CWCCC0003E,CWDDD0004A,CWXYZ9999I"));
    }
}
