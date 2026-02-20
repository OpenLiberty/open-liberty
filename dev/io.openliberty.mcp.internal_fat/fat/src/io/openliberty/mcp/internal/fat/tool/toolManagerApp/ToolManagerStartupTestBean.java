/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.toolManagerApp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolManager.ToolAnnotations;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 *
 */
@ApplicationScoped
public class ToolManagerStartupTestBean {

    @Inject
    ToolManager toolManager;

    @SuppressWarnings("unused")
    private void createCreatedTool(@Observes Startup startup) {
        // For ToolManagerTestServlet.testCreatedToolInfo
        toolManager.newTool("startup-tool")
                   .setHandler(a -> ToolResponse.success("OK"))
                   .register();
    }

    @SuppressWarnings("unused")
    private void removeMethodTool(@Observes Startup startup) {
        // Remove a tool that was defined by annotating a method
        toolManager.removeTool("methodToolToBeRemoved");
    }

    @Tool
    private String methodTool() {
        // This tool will not be removed
        return "methodTool";
    }

    @Tool
    private String methodToolToBeRemoved() {
        // This tool will be removed by the observer method above
        return "methodToolToBeRemoved";
    }

    @SuppressWarnings("unused")
    private void createToolListTools(@Observes Startup startup) {
        // Create tools to test tools/list response
        toolManager.newTool("tool-with-args")
                   .addArgument("stringArg", "string argument", false, String.class)
                   .addArgument("intArg", "integer argument", true, Integer.class)
                   .addArgument("pojoArg", "POJO argument", false, PojoInput.class)
                   .generateOutputSchema(PojoOutput.class)
                   .setTitle("Tool With Args")
                   .setDescription("Test tool with arguments")
                   .setAnnotations(new ToolAnnotations("Anno Title", true, false, false, false))
                   .setHandler(a -> ToolResponse.success("OK"))
                   .register();

        JsonObject inputSchema = Json.createObjectBuilder()
                                     .add("type", "object")
                                     .add("properties", Json.createObjectBuilder()
                                                            .add("foo", Json.createObjectBuilder()
                                                                            .add("type", "string"))
                                                            .add("bar", Json.createObjectBuilder()
                                                                            .add("type", "integer")))
                                     .add("required", Json.createArrayBuilder()
                                                          .add("foo"))
                                     .build();
        JsonObject outputSchema = Json.createObjectBuilder()
                                      .add("type", "object")
                                      .add("properties", Json.createObjectBuilder()
                                                             .add("baz", Json.createObjectBuilder()
                                                                             .add("type", "string"))
                                                             .add("qux", Json.createObjectBuilder()
                                                                             .add("type", "array")
                                                                             .add("items", Json.createObjectBuilder()
                                                                                               .add("type", "boolean"))))
                                      .add("required", Json.createArrayBuilder()
                                                           .add("baz")
                                                           .add("qux"))
                                      .build();

        toolManager.newTool("tool-with-manual-schema")
                   .setInputSchema(inputSchema)
                   .setOutputSchema(outputSchema)
                   .setAsyncHandler(a -> CompletableFuture.completedStage(ToolResponse.success("OK")))
                   .register();
    }

    private static record PojoInput(String foo, int bar) {};

    private static record PojoOutput(String baz, List<Boolean> qux) {}

    @SuppressWarnings("unused")
    private void createSyncTestTool(@Observes Startup startup) {
        toolManager.newTool("sync-test-tool")
                   .addArgument("action", null, true, String.class)
                   .setHandler(a -> {
                       String action = (String) a.args().get("action");
                       return switch (action) {
                           case "success" -> ToolResponse.success("OK");
                           case "error" -> ToolResponse.error("Error");
                           case "exception" -> throw new RuntimeException("Test Exception");
                           default -> throw new RuntimeException("Unknown action");
                       };
                   })
                   .register();
    }

    @SuppressWarnings("unused")
    private void createAsyncTestTool(@Observes Startup startup) {
        toolManager.newTool("async-test-tool")
                   .addArgument("action", null, true, String.class)
                   .setAsyncHandler(a -> {
                       String action = (String) a.args().get("action");
                       return switch (action) {
                           case "success" -> CompletableFuture.completedFuture(ToolResponse.success("OK"));
                           case "error" -> throw new ToolCallException("Error");
                           case "async-error" -> CompletableFuture.completedFuture(ToolResponse.error("Error"));
                           case "exception" -> throw new RuntimeException("Test Exception");
                           case "async-exception" -> CompletableFuture.failedFuture(new RuntimeException("Test Async Exception"));
                           default -> throw new RuntimeException("Unknown action");
                       };
                   })
                   .register();
    }

}
