/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.noparamtool.NoParamTools;

@RunWith(FATRunner.class)
public class NoParamNameTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "ExpectedNoParamNameFailureTest";

    @BeforeClass
    public static void setup() throws Exception {
        ExpectedAppFailureValidator.deployAppToAssertFailureOnRunningServer(serverLifecycle.getServer(), APP_NAME, NoParamTools.class.getPackage());
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(Arrays.asList(
            ExpectedAppFailureValidator.APP_START_FAILED_CODE,
            "CWMCM0003E", // Tools not found
            "CWMCM0005E"  // MCP server has one or more validation errors
        ));
        ExpectedAppFailureValidator.undeployFailedDropinApp(serverLifecycle.getServer(), APP_NAME);
    }

    @Test
    public void testNoParamNameToolArg() throws Exception {
        String expectedErrorHeader = "CWMCM0003E: The (.+?) MCP tool method has one or more arguments without a name specified.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.noparamtool.NoParamTools.missingToolArgNameTool");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Missing arguments found in MCP Tool: ", expectedErrorHeader, expectedErrorList, serverLifecycle.getServer());
    }

    @Test
    public void missingToolArgAnnotation() throws Exception {
        String expectedErrorHeader = "CWMCM0003E: The (.+?) MCP tool method has one or more arguments without a name specified.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.noparamtool.NoParamTools.missingToolArgAnnotation");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Missing arguments found in MCP Tool: ", expectedErrorHeader, expectedErrorList, serverLifecycle.getServer());
    }

    /**
     * When a tool method has multiple arguments all without names, CWMCM0003E must be logged
     * exactly once (not once per missing argument), and CWMCM0002E must not appear at all.
     */
    @Test
    public void testMultipleUnnamedArgsReportedOnce() throws Exception {
        String methodName = "io.openliberty.mcp.internal.fat.noparamtool.NoParamTools.multipleUnnamedArgs";
        String missingNamePattern = "CWMCM0003E.*" + methodName;

        List<String> occurrences = serverLifecycle.getServer().findStringsInLogs(missingNamePattern);
        assertEquals("CWMCM0003E should be logged exactly once for " + methodName + " but was logged " + occurrences.size() + " times",
                     1, occurrences.size());

        String duplicateArgPattern = "CWMCM0002E.*" + methodName;
        List<String> duplicateOccurrences = serverLifecycle.getServer().findStringsInLogs(duplicateArgPattern);
        assertTrue("CWMCM0002E should not be logged for " + methodName + " (missing names are not real duplicates) but was found: " + duplicateOccurrences,
                   duplicateOccurrences.isEmpty());
    }
}
