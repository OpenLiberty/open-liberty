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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.encoderToolApp.EncoderTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher;

@RunWith(FATRunner.class)
public class EncoderTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "encoderTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0019E"));
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(EncoderTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testFallbackBuiltInJsonTextContentEncoder() throws Exception {
        assertThat(client.callTool("testDefaultEncoderResponse", Map.of("name", "Manchester")),
                   ToolResponseMatcher.withTextContent("{\"country\":\"England\",\"isCapital\":false,\"name\":\"Manchester\",\"population\":8000}"));
    }

    @Test
    public void testContentEncoder() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testContentEncoder"),
                   ToolResponseMatcher.withTextContent("{\"age\":32,\"fistName\":\"Jon\",\"lastName\":\"Encoded by PersonContentEncoder\"}"));
    }

    @Test
    public void testContentEncoderListEncoding() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testContentEncoderEncodingAList"),
                   ToolResponseMatcher.withTextContent("{\"age\":32,\"fistName\":\"Jon\",\"lastName\":\"Encoded by PersonContentEncoder\"}"));
    }

    @Test
    public void testEncoderPriority() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testEncoderPriority"),
                   ToolResponseMatcher.withTextContent("{\"hello\":\"Hello from HigerPriorityEncoder\"}"));
    }

    @Test
    public void testToolResponseEncoderSuccess() throws Exception {
        assertThat(client.callTool("testToolResponseEncoder", Map.of("isSuccessful", true)),
                   ToolResponseMatcher.withTextContent("[{\"country\":\"England\",\"isCapital\":true,\"name\":\"London\",\"population\":18000},"
                                                       + "{\"country\":\"England\",\"isCapital\":false,\"name\":\"Machester\",\"population\":8000}]"));
    }

    @Test
    public void testToolResponseEncoderErrorResponse() throws Exception {
        assertThat(client.callTool("testToolResponseEncoder", Map.of("isSuccessful", false)),
                   ToolResponseMatcher.withErrorText("Database Query failed with error: Some SQL execution error"));
    }

    @Test
    public void testDependentBeanEncoder() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testDependantBeanEncoder"),
                   ToolResponseMatcher.withTextContent("{\"hello\":\"Hello from DependantBeanEncoder\"}"));
    }

    @Test
    public void testRequestScopedBeanEncoder() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testRequestScopedBeanEncoder"),
                   ToolResponseMatcher.withTextContent("{\"hello\":\"Hello from RequestScopedBeanEncoder\"}"));
    }

    @Test
    public void testSessionScopedBeanEncoder() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testSessionScopedBeanEncoder"),
                   ToolResponseMatcher.withTextContent("{\"hello\":\"Hello from SessionScopedBeanEncoder\"}"));
    }

    @Test
    public void testSingletonBeanEncoderWillNOTBeDiscovered() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testSingletonBeanEncoder"),
                   ToolResponseMatcher.withTextContent("{\"hello\":\"Hello\"}"));
    }

    @Test
    public void testToolResponseEncoderOverContentEncoderSuccess() throws Exception {
        assertThat(client.callTool("testToolResponseEncoderPriorityOverContentEncoder", Map.of("isSuccessful", true)),
                   ToolResponseMatcher.withTextContent("{\"content\":\"Encoded by RestResponseToolResponseEncoder\",\"isSuccessfull\":true,\"statusCode\":200}"));
    }

    /**
     * Test that @Priority annotation is NOT inherited from superclass.
     * This verifies the fix where priority is now captured from ProcessManagedBean.getAnnotatedBeanClass()
     * instead of walking the superclass hierarchy at runtime.
     *
     *
     * The encoder with highest priority (BaseEncoderWithPriority) should be used.
     */
    @Test
    public void testPriorityNotInheritedFromSuperclass() throws Exception {
        // Should use BaseEncoderWithPriority (priority 200), not SubclassEncoderNoPriority
        // which would incorrectly inherit priority 200 with the old buggy implementation
        assertThat(client.callTool("testPriorityNotInherited"),
                   ToolResponseMatcher.withTextContent("{\"message\":\"Encoded by BaseEncoderWithPriority (priority 50)\"}"));
    }

    @Test
    public void testCdiBasePriorityHighest() throws Exception {
        // The CDI base encoder has @Priority(400), while the subclass has @Priority(50),
        // so the base encoder should be selected.
        assertThat(client.callTool("testCdiBasePriorityHighest"),
                   ToolResponseMatcher.withTextContent("{\"message\":\"Encoded by CdiBaseEncoderWithHighestPriority (priority 400)\"}"));
    }

    /**
     * Tests that a ToolResponseEncoder registered for interface IShape is selected
     * when the tool return type is a concrete class (Shape) that directly implements IShape.
     * The encoder output reflects the runtime class name of the returned instance.
     */
    @Test
    public void testGetTypeExactMatch() throws Exception {
        assertThat(client.callTool("testGetTypeExactMatch"),
                   ToolResponseMatcher.withTextContent("encoded by ShapeEncoder: Shape"));
    }

    /**
     * Tests that a ToolResponseEncoder registered for interface IShape is selected
     * when the tool return type is a different implementing record (Circle).
     * This validates the getType().isInstance(result) dispatch semantics.
     * The encoder output reflects the concrete runtime class name (Circle).
     */
    @Test
    public void testGetTypeSubtypeMatch() throws Exception {
        assertThat(client.callTool("testGetTypeSubtypeMatch"),
                   ToolResponseMatcher.withTextContent("encoded by ShapeEncoder: Circle"));
    }

    /**
     * Tests that when a ContentEncoder throws an exception, CWMCM0019E is logged
     * (not the generic CWMCM0010E "tool method threw unexpected exception").
     */
    @Test
    public void testThrowingContentEncoderLogsCorrectError() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        client.callTool("testThrowingContentEncoder");

        assertNotNull("Expected CWMCM0019E to be logged when ContentEncoder throws",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0019E.*ThrowingContentEncoder.*testThrowingContentEncoder"));

        assertTrue("Expected CWMCM0010E NOT to be logged for an encoder exception",
                   serverLifecycle.getServer().findStringsInLogsUsingMark("CWMCM0010E.*testThrowingContentEncoder",
                                                                          serverLifecycle.getServer().getDefaultLogFile())
                                  .isEmpty());
    }

    /**
     * Tests that when a ToolResponseEncoder throws an exception, CWMCM0019E is logged
     * (not the generic CWMCM0010E "tool method threw unexpected exception").
     */
    @Test
    public void testThrowingToolResponseEncoderLogsCorrectError() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        client.callTool("testThrowingToolResponseEncoder");

        assertNotNull("Expected CWMCM0019E to be logged when ToolResponseEncoder throws",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0019E.*ThrowingToolResponseEncoder.*testThrowingToolResponseEncoder"));

        assertTrue("Expected CWMCM0010E NOT to be logged for an encoder exception",
                   serverLifecycle.getServer().findStringsInLogsUsingMark("CWMCM0010E.*testThrowingToolResponseEncoder",
                                                                          serverLifecycle.getServer().getDefaultLogFile())
                                  .isEmpty());
    }

    /**
     * Tests that when an async tool's ContentEncoder throws, CWMCM0019E is logged on the async path.
     */
    @Test
    public void testAsyncThrowingContentEncoderLogsCorrectError() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        client.callTool("testAsyncThrowingContentEncoder");

        assertNotNull("Expected CWMCM0019E to be logged when async ContentEncoder throws",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0019E.*ThrowingContentEncoder.*testAsyncThrowingContentEncoder"));

        assertTrue("Expected CWMCM0010E NOT to be logged for an async encoder exception",
                   serverLifecycle.getServer().findStringsInLogsUsingMark("CWMCM0010E.*testAsyncThrowingContentEncoder",
                                                                          serverLifecycle.getServer().getDefaultLogFile())
                                  .isEmpty());
    }

    /**
     * Tests that when an async tool's ToolResponseEncoder throws, CWMCM0019E is logged on the async path.
     */
    @Test
    public void testAsyncThrowingToolResponseEncoderLogsCorrectError() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        client.callTool("testAsyncThrowingToolResponseEncoder");

        assertNotNull("Expected CWMCM0019E to be logged when async ToolResponseEncoder throws",
                      serverLifecycle.getServer().waitForStringInLogUsingMark("CWMCM0019E.*ThrowingToolResponseEncoder.*testAsyncThrowingToolResponseEncoder"));

        assertTrue("Expected CWMCM0010E NOT to be logged for an async encoder exception",
                   serverLifecycle.getServer().findStringsInLogsUsingMark("CWMCM0010E.*testAsyncThrowingToolResponseEncoder",
                                                                          serverLifecycle.getServer().getDefaultLogFile())
                                  .isEmpty());
    }
}