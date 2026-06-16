/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.telemetry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.tools.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class McpMetricBean {
    @Tool(name = "basicTool", title = "Basic Tool", description = "Simple tool to test telemetry")
    public String basicTool() {
        return "Hello from this basic tool";
    }

    @Tool(name = "advancedTool", title = "Advanced Tool", description = "Advanced tool to test telemetry")
    public String advancedTool() {
        return "Hello from this advanced tool";
    }

    @Tool(description = "Tool that waits for a while before finishing")
    @WrapBusinessError(InterruptedException.class)
    public String waitingTool(@ToolArg int waitMs) throws InterruptedException {
        Thread.sleep(waitMs);
        return "OK";
    }

    @Tool(name = "businessErrorTool", title = "Business Error Tool", description = "Sync tool that throws ToolCallException for testing error metrics")
    public String businessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("bad-value".equals(input)) {
            throw new ToolCallException("Invalid business input: " + input);
        }
        return "Success: " + input;
    }

    @Tool(name = "nonBusinessErrorTool", title = "Non-Business Error Tool", description = "Sync tool that throws generic exception for testing error metrics")
    public String nonBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input)) {
            throw new RuntimeException("Unexpected error occurred");
        }
        return "Success: " + input;
    }

    @Tool(name = "asyncBusinessErrorTool", title = "Async Business Error Tool", description = "Async tool that throws ToolCallException for testing async error metrics")
    public CompletionStage<String> asyncBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("bad-value".equals(input)) {
            throw new ToolCallException("Async invalid business input: " + input);
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

    @Tool(name = "asyncNonBusinessErrorTool", title = "Async Non-Business Error Tool", description = "Async tool that throws generic exception for testing async error metrics")
    public CompletionStage<String> asyncNonBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input)) {
            throw new RuntimeException("Async unexpected error occurred");
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

    @Tool(name = "asyncFailedStageTool", title = "Async Failed Stage Tool", description = "Async tool that returns failed CompletionStage for testing async error metrics")
    public CompletionStage<String> asyncFailedStageTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input)) {
            return CompletableFuture.failedStage(new ToolCallException("Async failed stage error"));
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

}
