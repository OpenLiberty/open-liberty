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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Setup B: an optional include that contains both {@code mcp-1.0} and the application with its
 * {@code <mcp path="...">} element is already present when the server starts.
 */
@RunWith(FATRunner.class)
public class DynamicMcpPathIncludePresentAtStartupTest {

    // Same content as the file that DynamicMcpPathUpdateUsingServerXMLIncludesTest drops in after startup
    private static final String INCLUDE_FILE = "server-dynamic-app.xml";

    @Server("mcp-server-dynamic-include-at-startup")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DynamicMcpConfigTestUtils.deployEar(server);
        server.copyFileToLibertyServerRoot(INCLUDE_FILE);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            server.deleteFileFromLibertyServerRoot(INCLUDE_FILE);
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testMcpPathAppliedWhenIncludeIsPresentAtStartup() throws Exception {
        DynamicMcpConfigTestUtils.assertEndpointLive(server);
    }
}
