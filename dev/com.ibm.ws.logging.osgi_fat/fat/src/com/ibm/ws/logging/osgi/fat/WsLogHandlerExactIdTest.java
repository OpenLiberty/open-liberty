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

        Log.info(c, "testExactIdRoutingAndFiltering", "the ending bit");
        
        // 4. Ensure that valid messages were actually received
        assertTrue("Captured count should be greater than 0", getCapturedCount() > 0);
    }
}
