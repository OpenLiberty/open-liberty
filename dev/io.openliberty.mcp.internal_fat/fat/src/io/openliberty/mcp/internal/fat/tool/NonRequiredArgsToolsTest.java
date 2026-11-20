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

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.nonRequiredArgsApp.NonRequiredArgsTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class NonRequiredArgsToolsTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "nonRequiredArgsToolsTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(NonRequiredArgsTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testNonRequiredArgFalseMissingArgs() throws Exception {
        assertThat(client.callTool("toolArgRequiredFalse", Map.of()),
                   toolResponse().withTextContent("Hello toolArgRequiredFalse")
                                 .withNoStructuredContent());
    }

    @Test
    public void testNonRequiredArgFalseProvidedArgs() throws Exception {
        assertThat(client.callTool("toolArgRequiredFalse", Map.of("input", "Awesome")),
                   toolResponse().withTextContent("Hello from: Awesome")
                                 .withNoStructuredContent());
    }

    @Test
    public void testDefaultStringArgsProvided() throws Exception {
        assertThat(client.callTool("toolArgDefaultValueSetString", Map.of("input", "Awesome")),
                   toolResponse().withTextContent("Hello from: Awesome")
                                 .withNoStructuredContent());
    }

    @Test
    public void testDefaultStringArgsNotProvided() throws Exception {
        assertThat(client.callTool("toolArgDefaultValueSetString", Map.of()),
                   toolResponse().withTextContent("Hello from: The Default Value")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolArgWithOptionalValue() throws Exception {
        assertThat(client.callTool("toolArgWithOptionalValue", Map.of()),
                   toolResponse().withTextContent("Hello World")
                                 .withNoStructuredContent());
    }

    @Test
    public void testToolArgWithOptionalValueArgumentsPresent() throws Exception {
        assertThat(client.callTool("toolArgWithOptionalValue", Map.of("input", "Awesome")),
                   toolResponse().withTextContent("Hello from: Awesome")
                                 .withNoStructuredContent());
    }
}