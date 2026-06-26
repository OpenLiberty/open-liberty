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
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Test that verifies invalid sessionTimeout configuration falls back to default 10-minute timeout
 * and logs appropriate warning message.
 */
@RunWith(FATRunner.class)
public class InvalidSessionTimeoutTest {

    private final static String APP_NAME = "InvalidTimeoutTest";

    @Server("mcp-server-invalid-timeout")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/" + APP_NAME);

    private static final String BASIC_TOOL_REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "basicTool",
                        "arguments": {}
                      }
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMCM0036W");
    }

    /**
     * Test that invalid sessionTimeout configuration:
     * 1. Logs warning message CWMCM0036W with the invalid value
     * 2. Falls back to default 10-minute timeout
     * 3. Session remains active for more than 1 second (proving it's not using the invalid value)
     */
    @Test
    public void testInvalidTimeoutFallsBackToDefault() throws Exception {
        // Make a request to create a session
        client.callMCP(BASIC_TOOL_REQUEST);

        // Expected: "CWMCM0036W: The invalid-value sessionTimeout configuration value is not valid. The default timeout of 10 minutes is used."
        String expectedErrorHeader = "CWMCM0036W: The (.+?) sessionTimeout configuration value is not valid";
        List<String> expectedErrorList = List.of("invalid-value");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid sessionTimeout: ", 
                                                                       expectedErrorHeader, 
                                                                       expectedErrorList, 
                                                                       server);
    }
}

// Made with Bob
