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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        server.stopServer(ExpectedAppFailureValidator.APP_START_FAILED_CODE,
                          "CWMCM0001E", // Blank arguments
                          "CWMCM0002E", // Duplicate arguments
                          "CWMCM0004E", // Duplicate toold
                          "CWMCM0005E", // There are one or more MCP validation errors.
                          "CWMCM0006E", // Duplicate special arguments.
                          "CWMCM0007E", // Invalid Special arguments.
                          "CWMCM0017E", //  Default value has no type converter.
                          "CWMCM0018E", //  Arguments contain generics.
                          "CWMCM0020E", //  Invalid default value for argument type.
                          "CWMCM0023E", // Invalid tool name length
                          "CWMCM0024E", // Invalid tool name character
                          "CWMCM0025E" //  Invalid return type.
        );
    }

    @Test
    public void testDuplicateToolDeploymentError() throws Exception {
        String expectedErrorHeader = "CWMCM0004E: There are multiple MCP tool methods named (.+?). The methods are (.+?).";
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
        String expectedErrorHeader = "CWMCM0001E: The (.+?) MCP tool method has one or more arguments with blank names";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlank",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.argNameisBlankVariant");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Blank Tool Arg: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testDuplicatesToolArgs() throws Exception {
        String expectedErrorHeader = "CWMCM0002E: The (.+?) MCP tool method has more than one argument named (.+?)";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParam.*arg",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.duplicateParamVariant.*arg");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Duplicate Tool Arg: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testDuplicateSpecialArgsTestCase() throws Exception {
        String expectedErrorHeader = "The (.+?) MCP Tool has more than one parameter with the (.+?) type. Use only one (.+?) parameter in each Tool method.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.DuplicateSpecialArgsErrorTest.duplicateCancellation");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Duplicate Special Args: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testInvalidSpecialArgsTestCase() throws Exception {
        String expectedErrorHeader = "The (.+?) MCP Tool has a parameter of type (.+?) which is not a recognized special argument type and does not have a `@ToolArg` annotation.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidSpecialArgsErrorTest.invalidSpecialArgumentTool");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid Special Args: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testGenericArgsTestCase() throws Exception {
        String expectedErrorHeader = "The (.+?) argument of the (.+?) MCP tool method contains unsupported components such as TypeVariable, Wildcard, GenericArrayType.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.addGenericToGenericArray");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Generic Args: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testInvalidToolNamesWithinvalidCharctersTestCase() throws Exception {

        for (Map.Entry<String, String> entry : Map.of("invalidTool1", "invalid tool", "invalidTool2", "invalidtool2!", "invalidTool3", "invalid,tool3").entrySet()) {
            String qualifiedName = "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest." + entry.getKey();
            String expectedErrorHeader = Pattern.quote("The " + entry.getValue() + " tool name for the " + qualifiedName
                                                       + " MCP tool method is not valid. Tool names must contain only ASCII letters, digits, underscores, or hyphens.");
            List<String> expectedErrorList = List.of(qualifiedName);
            ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid tool name: ", expectedErrorHeader, expectedErrorList, server);
        }
    }

    @Test
    public void testInvalidToolNamesWithinvalidLengthZeroTestCase() throws Exception {
        String expectedErrorHeader = Pattern.quote("The  tool name for the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest.invalidTool4 MCP tool method is not valid. Tool names must be between 1 and 128 characters in length, inclusive.");
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest.invalidTool4");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid tool name: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testInvalidToolNamesWithinvalidLengthAndCharactersTestCase() throws Exception {
        String expectedLengthErrorHeader = Pattern.quote("The openlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenlibertyopenlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenliberty tool name for the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest.invalidTool5 MCP tool method is not valid. Tool names must be between 1 and 128 characters in length, inclusive.");
        String expectedCharacterErrorHeader = Pattern.quote("The openlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenlibertyopenlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenliberty tool name for the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest.invalidTool5 MCP tool method is not valid. Tool names must contain only ASCII letters, digits, underscores, or hyphens.");
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolNameTest.invalidTool5");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid tool name: ", expectedLengthErrorHeader, expectedErrorList, server);
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid tool name: ", expectedCharacterErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testToolArgDefaultValueWithoutTypeConverter() throws Exception {
        String expectedErrorHeader = Pattern.quote("CWMCM0017E: The city argument of the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.testToolArgDefaultValueWithoutTypeConverter MCP tool method does not have a converter to change its default value into an object of type class io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest$City.");
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.testToolArgDefaultValueWithoutTypeConverter");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("ToolArg DefaultValue Without Converter: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testToolArgInvalidDefaultValueForType() throws Exception {
        String expectedErrorHeader = "CWMCM0020E: The default value of the year argument of the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.testToolArgInvalidNumberDefaultValue MCP tool cannot be converted to the int type. The value is TwentyTwentyFive. The error is java.lang.NumberFormatException: For input string: \"TwentyTwentyFive\"";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.testToolArgInvalidNumberDefaultValue");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("ToolArg Invalid DefaultValue for Argument Type: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testToolArgWithOptionalValueAndDefaultValue() throws Exception {
        String expectedErrorHeader = "CWMCM0017E: The input argument of the io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.toolArgWithOptionalValueAndDefaultValueSet MCP tool method does not have a converter to change its default value into an object of type java.util.Optional<java.lang.String>.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.ToolArgValidationTest.toolArgWithOptionalValueAndDefaultValueSet");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("ToolArg No Converter for DefaultValue: ", expectedErrorHeader, expectedErrorList, server);
    }

    @Test
    public void testInvalidListReturn() throws Exception {
        String expectedErrorHeader = "CWMCM0025E: The (.+?) return type of the (.+?) MCP tool method must be an object.";
        List<String> expectedErrorList = List.of("io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolReturnsTest.asyncListObjectTool",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolReturnsTest.testArrayResponse",
                                                 "io.openliberty.mcp.internal.fat.tool.deploymentErrorApps.InvalidToolReturnsTest.testListStringResponse");
        ExpectedAppFailureValidator.findAndAssertExpectedErrorsInLogs("Invalid return type: ", expectedErrorHeader, expectedErrorList, server);
    }
}
