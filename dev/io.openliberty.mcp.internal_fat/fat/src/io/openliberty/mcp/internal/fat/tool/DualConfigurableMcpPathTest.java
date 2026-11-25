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
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
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
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;

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
        ToolResponse war1Response = war1Client.callTool("echo", Map.of("input", "Hello"));
        assertThat(war1Response, toolResponse().withTextContent("Hello").withNoStructuredContent());

        ToolResponse war2Response = war2Client.callTool("echo", Map.of("input", "Hello"));
        assertThat(war2Response, toolResponse().withTextContent("Hello").withNoStructuredContent());
    }
}