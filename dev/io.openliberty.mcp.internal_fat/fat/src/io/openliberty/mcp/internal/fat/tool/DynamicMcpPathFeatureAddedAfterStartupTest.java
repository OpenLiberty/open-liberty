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

import static io.openliberty.mcp.internal.fat.tool.DynamicMcpConfigTestUtils.APP_NAME;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Setup E: server.xml contains the application and its {@code <mcp path="...">} element when
 * the server starts, but {@code mcp-1.0} is not enabled. After startup, only
 * {@code <feature>mcp-1.0</feature>} is added.
 */
@RunWith(FATRunner.class)
public class DynamicMcpPathFeatureAddedAfterStartupTest {

    @Server("mcp-server-dynamic-feature-added-later")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DynamicMcpConfigTestUtils.deployEar(server);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        // The application is started before mcp-1.0 is enabled, so its MCP classes can't be loaded
        // and the MCP path is not served until the feature is added.
        server.stopServer("SRVE0190E", // File not found: /dynamic-mcp-updated
                          "CWNEN0047W", // Resource annotations on fields ignored (MCP classes not yet available)
                          "CWNEN0049W"); // Resource annotations on methods ignored (MCP classes not yet available)
    }

    @Test
    public void testMcpPathAppliedWhenOnlyTheFeatureIsAddedAfterStartup() throws Exception {
        DynamicMcpConfigTestUtils.assertEndpointNotFound(server);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamic-mcp-server-xml-with-feature-added.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        DynamicMcpConfigTestUtils.assertEndpointLive(server);
    }
}
