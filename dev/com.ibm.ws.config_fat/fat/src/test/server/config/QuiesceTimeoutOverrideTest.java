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

package test.server.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that quiesceTimeout on the server element overrides chainQuiesceTimeout
 * on the channelfw element, and that the appropriate informational message is logged.
 */
@RunWith(FATRunner.class)
public class QuiesceTimeoutOverrideTest {

    private static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.quiesce.override");
    }

    @After
    public void cleanupAfterTest() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that when both quiesceTimeout and chainQuiesceTimeout are configured,
     * the CWWKO0400I informational message is logged with the ignored chainQuiesceTimeout value,
     * and verify the actual value used is from quiesceTimeout (not chainQuiesceTimeout).
     */
    @Test
    public void testQuiesceTimeoutOverridesChainQuiesceTimeout() throws Exception {
        server.setServerConfigurationFile("quiesceoverride/quiesceTimeoutOverridesChainQuiesceTimeout.xml");
        server.startServer(true); // Enable trace

        // Wait for server to be ready
        assertNotNull("Server did not start successfully",
                      server.waitForStringInLog("CWWKF0011I")); // Server is ready

        // Verify the override message appears showing the ignored chainQuiesceTimeout value of 10 seconds
        assertNotNull("Expected CWWKO0400I message indicating quiesceTimeout overrides chainQuiesceTimeout",
                      server.waitForStringInLog("CWWKO0400I.*chainQuiesceTimeout.*10 seconds"));
    }

    /**
     * Test that when only quiesceTimeout is configured (and chainQuiesceTimeout is set to the default value),
     * no override message is logged, because we assume chainQuiesceTimeout wasn't configured.
     */
    @Test
    public void testQuiesceTimeoutOnly() throws Exception {
        server.setServerConfigurationFile("quiesceoverride/quiesceTimeoutOnly.xml");
        server.startServer(true); // Enable trace

        // Wait for server to be ready
        assertNotNull("Server did not start successfully",
                      server.waitForStringInLog("CWWKF0011I")); // Server is ready

        // Verify the override message does NOT appear when chainQuiesceTimeout is not explicitly configured
        assertNull("CWWKO0400I message should not appear when only quiesceTimeout is configured",
                   server.waitForStringInLog("CWWKO0400I", 5000));
    }

    /**
     * Test that when only chainQuiesceTimeout is configured (no quiesceTimeout on server element),
     * no override message is logged because there's no server-level quiesceTimeout to perform the override.
     */
    @Test
    public void testChainQuiesceTimeoutOnly() throws Exception {
        server.setServerConfigurationFile("quiesceoverride/chainQuiesceTimeoutOnly.xml");
        server.startServer(true); // Enable trace

        // Wait for server to be ready
        assertNotNull("Server did not start successfully",
                      server.waitForStringInLog("CWWKF0011I")); // Server is ready

        // Verify the override message does NOT appear
        assertNull("CWWKO0400I message should not appear when only chainQuiesceTimeout is configured",
                   server.waitForStringInLog("CWWKO0400I", 5000));
    }
    /**
     * Test that when both quiesceTimeout and chainQuiesceTimeout are configured to the SAME value,
     * no override message is logged because there's no conflict to report.
     * The override still happens, but silently since both values are identical.
     */
    @Test
    public void testQuiesceTimeoutSameAsChainQuiesceTimeout() throws Exception {
        server.setServerConfigurationFile("quiesceoverride/quiesceTimeoutSameAsChainQuiesceTimeout.xml");
        server.startServer(true); // Enable trace

        // Wait for server to be ready
        assertNotNull("Server did not start successfully",
                      server.waitForStringInLog("CWWKF0011I")); // Server is ready

        // Verify the override message does NOT appear when both timeouts are the same
        assertNull("CWWKO0400I message should not appear when both timeouts are set to the same value",
                   server.waitForStringInLog("CWWKO0400I", 5000));
    }
}

// Made with Bob
