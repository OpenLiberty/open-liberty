/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Setup H: like setup C, but a single server.xml update adds {@code mcp-1.0} and three applications at once: two
 * declared with {@code <enterpriseApplication>} and one with a plain {@code <application>}. Each application has its
 * own {@code <mcp path>} and {@code <info name>}, and each must be served at its own path with its own serverInfo
 * name.
 */
@RunWith(FATRunner.class)
public class DynamicMcpPathMultipleAppsAddedAfterStartupTest {

    private static final String APP_ONE = "dynamicMcpMultiAppOne";
    private static final String APP_TWO = "dynamicMcpMultiAppTwo";
    private static final String APP_THREE = "dynamicMcpMultiAppThree";

    @Server("mcp-server-dynamic-multiple-apps")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DynamicMcpConfigTestUtils.deployEar(server, APP_ONE);
        DynamicMcpConfigTestUtils.deployEar(server, APP_TWO);
        DynamicMcpConfigTestUtils.deployEar(server, APP_THREE);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("SRVE0190E"); // File not found: the mcp paths before the update
    }

    @Test
    public void testEachAppServedAtItsOwnPathWhenFeatureAndAppsAreAddedTogether() throws Exception {
        DynamicMcpConfigTestUtils.assertEndpointNotFound(server, APP_ONE, "/mcp-one");
        DynamicMcpConfigTestUtils.assertEndpointNotFound(server, APP_TWO, "/mcp-two");
        DynamicMcpConfigTestUtils.assertEndpointNotFound(server, APP_THREE, "/mcp-three");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamic-mcp-multiple-apps-with-feature.xml");
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_ONE, APP_TWO, APP_THREE));

        DynamicMcpConfigTestUtils.assertEndpointLive(server, APP_ONE, "/mcp-one", "multi-app-one");
        DynamicMcpConfigTestUtils.assertEndpointLive(server, APP_TWO, "/mcp-two", "multi-app-two");
        DynamicMcpConfigTestUtils.assertEndpointLive(server, APP_THREE, "/mcp-three", "multi-app-three");
    }
}
