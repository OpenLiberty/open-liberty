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
 * Setup D: {@code mcp-1.0}, the application and its {@code <mcp path="...">} element are all
 * present inline in server.xml when the server starts.
 */
@RunWith(FATRunner.class)
public class DynamicMcpPathInlineAtStartupTest {

    @Server("mcp-server-dynamic-inline")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DynamicMcpConfigTestUtils.deployEar(server);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMcpPathAppliedWhenConfiguredInlineAtStartup() throws Exception {
        DynamicMcpConfigTestUtils.assertEndpointLive(server);
    }
}
