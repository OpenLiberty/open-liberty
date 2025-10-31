/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.noparamtool.NoParamTools;

@RunWith(FATRunner.class)
public class NoParamNameTest extends FATServletClient {
    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ExpectedAppFailureValidator.deployAppToAssertFailure(server, "ExpectedNoParamNameFailureTest", NoParamTools.class.getPackage());
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(ExpectedAppFailureValidator.APP_START_FAILED_CODE);
    }

    @Test
    public void testNoParamNameToolArg() throws Exception {
        String expectedErrorHeader = "Missing arguments found in MCP Tool:";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.noparamtool.NoParamTools.missingToolArgNameTool");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Missing arguments found in MCP Tool: ", expectedErrorHeader, expectedErrorList, server);
    }
}
