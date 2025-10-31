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
import io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest;

/**
 *
 */
@RunWith(FATRunner.class)
public class DeploymentProblemTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ExpectedAppFailureValidator.deployAppToAssertFailure(server, "ExpectedAppFailureTest", DuplicateToolErrorTest.class.getPackage());
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(ExpectedAppFailureValidator.APP_START_FAILED_CODE); // will be extended for translations
    }

    @Test
    public void testDuplicateToolDeploymentError() throws Exception {
        String expectedErrorHeader = "More than one MCP tool has the same name:";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.bob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.duplicateBob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.duplicateBob",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.duplicateEcho",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest.echo",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.duplicateEcho",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateToolErrorTest2.echo");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Duplicate Tool: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testBlankToolArg() throws Exception {
        String expectedErrorHeader = "Blank arguments found in MCP Tool:";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlank",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlankVariant");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Blank Tool Arg: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testDuplicatesToolArgs() throws Exception {
        String expectedErrorHeader = "Duplicate arguments found in MCP Tool:";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParam.*arg",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParamVariant.*arg");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Duplicate Tool Arg: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testDuplicateSpecialArgsTestCase() throws Exception {
        String expectedErrorHeader = "Only 1 instance is allowed, of type: ";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateSpecialArgsErrorTest.duplicateCancellation");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Duplicate Special Args: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testInvalidSpecialArgsTestCase() throws Exception {
        String expectedErrorHeader = "Special argument type not supported: ";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidSpecialArgsErrorTest.invalidSpecialArgumentTool");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid Special Args: ", expectedErrorHeader, expectedErrorList, server);
    }
}
