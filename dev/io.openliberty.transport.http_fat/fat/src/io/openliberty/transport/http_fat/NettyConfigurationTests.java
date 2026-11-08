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

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to ensure that the netty configuration options are read.
 * Note that this bucket doesn't verify the properties actually work.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NettyConfigurationTests {

    private static final String CLASS_NAME = NettyConfigurationTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Server("NettyConfigServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Verifies that the default netty configuration values are picked up when no explicit config is provided.
     * @throws Exception
     */
    @Test
    public void testNettyConfigurationPickedUp() throws Exception {
        LOG.info("Testing default netty configuration values");

        // Verify default values are picked up
        assertNotNull("Default scalerMinThreads=1 not found",
                      server.waitForStringInTrace(".*scalerMinThreads=1.*"));
        assertNotNull("Default scalerMaxThreads=4 not found",
                      server.waitForStringInTrace(".*scalerMaxThreads=4.*"));
        assertNotNull("Default scalerWindowSize=1500 not found",
                      server.waitForStringInTrace(".*scalerWindowSize=1500.*"));
        assertNotNull("Default scalerDownThreshold=0.15 not found",
                      server.waitForStringInTrace(".*scalerDownThreshold=0\\.15.*"));
        assertNotNull("Default scalerUpThreshold=0.85 not found",
                      server.waitForStringInTrace(".*scalerUpThreshold=0\\.85.*"));
        assertNotNull("Default scalerDownStep=1 not found",
                      server.waitForStringInTrace(".*scalerDownStep=1.*"));
        assertNotNull("Default scalerUpStep=1 not found",
                      server.waitForStringInTrace(".*scalerUpStep=1.*"));
        assertNotNull("Default scalerCycles=3 not found",
                      server.waitForStringInTrace(".*scalerCycles=3.*"));
        assertNotNull("Default scalerMetricsWindowSize=0 not found",
                      server.waitForStringInTrace(".*scalerMetricsWindowSize=0.*"));
        
        LOG.info("Default netty configuration values verified successfully");
        
        LOG.info("Stopping server to apply updated configuration");
        server.stopServer();
        
        // Update configuration file
        server.setServerConfigurationFile("netty-custom-server.xml");
        
        // Restart
        LOG.info("Restarting server with updated netty configuration");
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log", false);
        
        // Verify custom configuration values (all different from defaults)
        LOG.info("Verifying custom netty configuration values");
        assertNotNull("Custom scalerMinThreads=2 not found (default is 1)",
                      server.waitForStringInTrace(".*scalerMinThreads=2.*"));
        assertNotNull("Custom scalerMaxThreads=8 not found (default is 4)",
                      server.waitForStringInTrace(".*scalerMaxThreads=8.*"));
        assertNotNull("Custom scalerWindowSize=2000 not found (default is 1500)",
                      server.waitForStringInTrace(".*scalerWindowSize=2000.*"));
        assertNotNull("Custom scalerDownThreshold=0.20 not found (default is 0.15)",
                      server.waitForStringInTrace(".*scalerDownThreshold=0\\.2.*"));
        assertNotNull("Custom scalerUpThreshold=0.80 not found (default is 0.85)",
                      server.waitForStringInTrace(".*scalerUpThreshold=0\\.8.*"));
        assertNotNull("Custom scalerDownStep=2 not found (default is 1)",
                      server.waitForStringInTrace(".*scalerDownStep=2.*"));
        assertNotNull("Custom scalerUpStep=2 not found (default is 1)",
                      server.waitForStringInTrace(".*scalerUpStep=2.*"));
        assertNotNull("Custom scalerCycles=5 not found (default is 3)",
                      server.waitForStringInTrace(".*scalerCycles=5.*"));
        assertNotNull("Custom scalerMetricsWindowSize=10000 not found (default is 0)",
                      server.waitForStringInTrace(".*scalerMetricsWindowSize=10000.*"));
        assertNotNull("Custom useNativeIO=false not found",
                      server.waitForStringInTrace(".*useNativeIO=false.*"));
        
        LOG.info("Custom configuration verified successfully - all values differ from the defaults!");
    }

}
