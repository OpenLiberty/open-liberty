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
 * Setup G: like setup C, but the application is declared with a plain {@code <application>} element, so it does
 * not use {@code ibm:extends}. The mcp configuration is then matched to the application by the application's own
 * service PID. The server starts without {@code mcp-1.0} and without the application. After startup, server.xml
 * itself is replaced with a version that adds {@code mcp-1.0} and the application with its
 * {@code <mcp path="...">} element.
 */
@RunWith(FATRunner.class)
public class DynamicMcpPathPlainApplicationAddedAfterStartupTest {

    @Server("mcp-server-dynamic-plain-application")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DynamicMcpConfigTestUtils.deployEar(server);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("SRVE0190E"); // File not found: /dynamic-mcp-updated
    }

    @Test
    public void testMcpPathAppliedToPlainApplicationAddedAfterStartup() throws Exception {
        DynamicMcpConfigTestUtils.assertEndpointNotFound(server);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamic-mcp-plain-application-with-feature-and-app.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        DynamicMcpConfigTestUtils.assertEndpointLive(server);
    }
}
