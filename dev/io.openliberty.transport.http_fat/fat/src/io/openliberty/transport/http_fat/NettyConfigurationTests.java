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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that Netty configuration options and server quiesce timeout are correctly read,
 * and that runtime config changes log the expected warning.
 */
@RunWith(FATRunner.class)
public class NettyConfigurationTests {

    private static final Logger LOG = Logger.getLogger(NettyConfigurationTests.class.getName());

    @Server("NettyConfigServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log");
        server.waitForDefaultHTTPEndpointStart();
    }

    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void afterTest() throws Exception {
        if (server.isStarted()) {
            server.setMarkToEndOfLog();
            server.setTraceMarkToEndOfDefaultTrace();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(null);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKO0040W", "CWWKG0111W");
        }
    }

    /**
     * Verifies default Netty config values and default quiesce timeout are logged at startup,
     * then verifies custom values (including server quiesceTimeout) are picked up after a restart
     * with netty-custom-server.xml.
     */
    @Test
    public void testNettyConfigurationPickedUp() throws Exception {
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
        assertNotNull("Default quiesce timeout 30000ms not found in Netty trace",
                      server.waitForStringInTrace(".*Default Chain Quiesce Timeout: 30000.*"));

        // Restart with custom config and verify all values changed.
        server.stopServer("CWWKO0040W", "CWWKG0111W");
        server.setServerConfigurationFile("netty-custom-server.xml");
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log", false);
        server.waitForDefaultHTTPEndpointStart();

        assertNotNull("Custom scalerMinThreads=2 not found",
                      server.waitForStringInTrace(".*scalerMinThreads=2.*"));
        assertNotNull("Custom scalerMaxThreads=8 not found",
                      server.waitForStringInTrace(".*scalerMaxThreads=8.*"));
        assertNotNull("Custom scalerWindowSize=2000 not found",
                      server.waitForStringInTrace(".*scalerWindowSize=2000.*"));
        assertNotNull("Custom scalerDownThreshold=0.20 not found",
                      server.waitForStringInTrace(".*scalerDownThreshold=0\\.2.*"));
        assertNotNull("Custom scalerUpThreshold=0.80 not found",
                      server.waitForStringInTrace(".*scalerUpThreshold=0\\.8.*"));
        assertNotNull("Custom scalerDownStep=2 not found",
                      server.waitForStringInTrace(".*scalerDownStep=2.*"));
        assertNotNull("Custom scalerUpStep=2 not found",
                      server.waitForStringInTrace(".*scalerUpStep=2.*"));
        assertNotNull("Custom scalerCycles=5 not found",
                      server.waitForStringInTrace(".*scalerCycles=5.*"));
        assertNotNull("Custom scalerMetricsWindowSize=10000 not found",
                      server.waitForStringInTrace(".*scalerMetricsWindowSize=10000.*"));
        assertNotNull("Custom useNativeIO=false not found",
                      server.waitForStringInTrace(".*useNativeIO=false.*"));
        assertNotNull("Custom quiesce timeout 90000ms not found in Netty trace",
                      server.waitForStringInTrace(".*Default Chain Quiesce Timeout: 90000.*"));
    }

    /**
     * Verifies that when a quiesce timeout less than the 30-second minimum is configured
     * on the <server> element (e.g. quiesceTimeout="15s"), Netty relies on the minimum of 30s (30000ms).
     */
    @Test
    public void testNettyQuiesceTimeoutLessThanMinimum() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO0040W", "CWWKG0111W");
        }
        server.setServerConfigurationFile("netty-quiesce-min-server.xml");
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log", false);
        server.waitForDefaultHTTPEndpointStart();

        assertNotNull("Minimum quiesce timeout 30000ms not found in Netty trace",
                      server.waitForStringInTrace(".*Default Chain Quiesce Timeout: 30000.*"));
    }

    /**
     * Verifies that when both <server quiesceTimeout> and <channelfw chainQuiesceTimeout>
     * are configured, the <server> quiesceTimeout takes precedence for Netty and CWWKO0400I is logged.
     */
    @Test
    public void testNettyQuiesceTimeoutOverridesChannelfw() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO0040W", "CWWKG0111W");
        }
        server.setServerConfigurationFile("netty-quiesce-override-server.xml");
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log", false);
        server.waitForDefaultHTTPEndpointStart();

        assertNotNull("Override quiesce timeout 90000ms not found in Netty trace",
                      server.waitForStringInTrace(".*Default Chain Quiesce Timeout: 90000.*"));
        assertNotNull("CWWKO0400I override message not found in log",
                      server.waitForStringInLog("CWWKO0400I"));
    }

    /**
     * Verifies that a live config update adding a <netty> element logs CWWKO0040W,
     * warning that dynamic Netty config changes require a restart.
     */
    @Test
    public void testNettyDynamicConfigWarningLogged() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO0040W", "CWWKG0111W");
        }
        server.setServerConfigurationFile("netty-default-server.xml");
        server.startServer(NettyConfigurationTests.class.getSimpleName() + ".log", false);
        server.waitForDefaultHTTPEndpointStart();
        server.saveServerConfiguration();

        // Switching to netty-custom-server.xml adds a <netty> element — a real change
        // from the baseline server.xml — triggering @Modified on NettyFrameworkImpl.
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("netty-custom-server.xml");

        assertNotNull("CWWKG0017I not found after config update",
                      server.waitForStringInLogUsingMark("CWWKG0017I"));
        assertNotNull("CWWKO0040W not found — dynamic Netty config warning missing",
                      server.waitForStringInLogUsingMark("CWWKO0040W"));
    }

}
