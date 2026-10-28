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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class DualConfigurableMcpPathTest extends FATServletClient {

    @Server("mcp-server-dual-configurable")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                                    .addPackage(BasicTools.class.getPackage());

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                                    .addPackage(BasicTools.class.getPackage());

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "dualConfigurableMcpPathTests.ear")
                                          .addAsModule(war1)
                                          .addAsModule(war2);

        ShrinkHelper.exportAppToServer(server, ear, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Rule
    public McpClient war1Client = McpClient.builder(server, "/war1").path("/custom-mcp").build();

    @Rule
    public McpClient war2Client = McpClient.builder(server, "/war2").path("/custom-mcp-for-war-2").build();

    @Test
    public void testMultipleWarModulesInEarCanHaveDifferentMcpPaths() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String war1Response = war1Client.callMCP(request);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, war1Response, true);

        String war2Response = war2Client.callMCP(request);

        JSONAssert.assertEquals(expectedResponseString, war2Response, true);
    }
}
