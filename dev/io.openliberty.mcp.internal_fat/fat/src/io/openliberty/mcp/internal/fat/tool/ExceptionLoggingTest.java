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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.exceptionToolApp.ExceptionToolApp;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ExceptionLoggingTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/exceptionLoggingTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "exceptionLoggingTest.war")
                                   .addPackage(ExceptionToolApp.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/exceptionLoggingTest/mcp"

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0010E" // general exception
        );
    }

    @Test
    public void testComChildConcreteClass() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "exceptionTool"
                          }
                        }
                        """;
        client.callMCP(request);
        List<String> expectedErrorHeaders = List.of("Caused by: java.lang.RuntimeException: Root Exception", "Caused by: java.lang.Exception: Exception at level 0",
                                                    "Caused by: java.lang.Exception: Exception at level 1", "Caused by: java.lang.Exception: Exception at level 2",
                                                    "CWMCM0010E: The exceptionTool tool method threw an unexpected exception. The exception is java.lang.Exception: Exception at level 3");

        for (String exc : expectedErrorHeaders) {
            assertTrue("Expected header not found: " + exc, !server.findStringsInLogs(exc).isEmpty());
        }

    }
}