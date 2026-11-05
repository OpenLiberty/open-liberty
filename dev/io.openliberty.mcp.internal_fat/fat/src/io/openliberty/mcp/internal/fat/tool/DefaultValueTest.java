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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

@RunWith(FATRunner.class)
public class DefaultValueTest extends FATServletClient {
    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "defaultValueTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        serverLifecycle.getServer().addIgnoredErrors(List.of(
                                                             "CWMCM0034W", // Converter implementation without type parameter
                                                             "CWMCM0035W" // Converter implementation returns a generic type
        ));
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(DefaultValueApp.class.getPackage());
        serverLifecycle.deployDropinApp(war);

        assertStartupMessages();
    }

    private static void assertStartupMessages() {
        assertNotNull("Expected CWMCM0034W for InvalidNoParameterTypeCustomConverter to be logged at deploy",
                      serverLifecycle.getServer()
                                     .waitForStringInLogUsingMark(Pattern.quote("CWMCM0034W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidNoParameterTypeCustomConverter DefaultValueConverter implementation must specify a type parameter.")));
        assertNotNull("Expected CWMCM0035W for InvalidGenericConverter to be logged at deploy",
                      serverLifecycle.getServer()
                                     .waitForStringInLogUsingMark(Pattern.quote("CWMCM0035W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidGenericConverter DefaultValueConverter implementation converts to a generic type parameter instead of a concrete type.")));
        assertNotNull("Expected CWMCM0008I message with full MCP URL to appear in the log",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0008I: The MCP server endpoint: .*/defaultValueTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testToolCallWithToolArgStringDefaultValue() throws Exception {
        assertThat(client.callTool("testToolArgStringDefaultValue", Map.of()),
                   toolResponse().withTextContent("Jupiter")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolCallWithToolArgIntDefaultValue() throws Exception {
        assertThat(client.callTool("testToolArgIntDefaultValue", Map.of()),
                   toolResponse().withTextContent("2025")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValue() throws Exception {
        // Only look at log entries produced by this tool call
        serverLifecycle.getServer().setMarkToEndOfLog();
        assertThat(client.callTool("testToolArgCustomTypeDefaultValue", Map.of()),
                   toolResponse().withTextContent("{\"country\":\"England\",\"isCapital\":false,\"name\":\"Manchester\",\"population\":8000}")
                                 .withNoStructuredContent());
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark(Pattern.quote("[PriorityCityConverter] City converter with HIGHER priority used")));
        assertNull(serverLifecycle.getServer().waitForStringInLogUsingMark(Pattern.quote("[CityConverter] City converter with LOWER priority used"),
                                                                           TestConstants.NEGATIVE_TIMEOUT_MS));
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValueAndInheritedConverter() throws Exception {
        assertThat(client.callTool("testToolArgCustomTypeDefaultValueWithInheritedConverter", Map.of()),
                   toolResponse().withTextContent("{\"number\":5,\"street\":\"London Road\"}")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolCallWithTwoToolArgsWithOneDefaultValue() throws Exception {
        assertThat(client.callTool("testMultipleToolArgsOneDefaultValue", Map.of("year", "2000")),
                   toolResponse().withTextContent("Planet Jupiter was created in the year 2000")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolCallWithDependentBeanCustomConverter() throws Exception {
        // Only look at log entries produced by this tool call
        serverLifecycle.getServer().setMarkToEndOfLog();
        assertThat(client.callTool("testDependentBeanCustomConverter", Map.of()),
                   toolResponse().withTextContent("{\"age\":25,\"name\":\"Joe\"}")
                                 .withNoStructuredContent());
        assertNull(serverLifecycle.getServer().waitForStringInLogUsingMark(Pattern.quote("[PersonConverterDependentBean] PreDestroy called"), TestConstants.NEGATIVE_TIMEOUT_MS));
    }
}