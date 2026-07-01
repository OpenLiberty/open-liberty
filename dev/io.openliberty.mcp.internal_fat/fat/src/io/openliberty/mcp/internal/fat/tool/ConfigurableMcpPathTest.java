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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@CheckpointTest(alwaysRun = true)
@RunWith(FATRunner.class)
public class ConfigurableMcpPathTest extends FATServletClient {

    @Server("mcp-server-configurable")
    public static LibertyServer server;

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                                                                      .setConsoleLogName(ConfigurableMcpPathTest.class.getSimpleName() + ".log")
                                                                      .setServerSetup(ConfigurableMcpPathTest::initialSetup)
                                                                      .setServerStart(ConfigurableMcpPathTest::testSetup)
                                                                      .setServerTearDown(ConfigurableMcpPathTest::testTearDown)
                                                                      .setCheckpointPhase(CheckpointPhase.AFTER_APP_START)
                                                                      .setPostCheckpointLambda((srv) -> {
                                                                          assertNotNull("'CWWKZ0001I: Application configurableMcpPathTests started' message not found during restore",
                                                                                        srv.waitForStringInLogUsingMark("CWWKZ0001I: Application configurableMcpPathTests started",
                                                                                                                        0));
                                                                      });

    @Rule
    public McpClient client = new McpClient(server, "/configurableMcpPathTests", "/custom-mcp");

    private static final String PATH = "/custom-mcp";

    public static LibertyServer initialSetup(CheckpointRule.ServerMode mode) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "configurableMcpPathTests.war")
                                   .addPackage(BasicTools.class.getPackage());
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);

        return server;
    }

    public static void testSetup(CheckpointRule.ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();
        assertNotNull("Web application was not available during " + mode + " server launch",
                      server.waitForStringInLogUsingMark("CWWKT0016I:.*configurableMcpPathTests"));
    }

    public static void testTearDown(CheckpointRule.ServerMode mode, LibertyServer server) throws Exception {
        server.stopServer();
    }

    @Test
    public void testCustomEndpointPath() throws Exception {
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

        String response = client.callMCPCustomized(request, PATH, 200);

        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}
