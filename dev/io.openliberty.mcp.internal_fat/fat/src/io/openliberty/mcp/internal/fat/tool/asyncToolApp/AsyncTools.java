/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.asyncToolApp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.messaging.Cancellation.OperationCancellationException;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 */
@ApplicationScoped
public class AsyncTools {
    private static final Logger LOG = Logger.getLogger(AsyncTools.class.getName());

    @Resource
    ManagedExecutorService executor;

    @Inject
    ToolStatus toolStatus;

    @Tool(name = "asyncEcho", title = "Async Echo", description = "Echoes input asynchronously")
    public CompletionStage<String> asyncEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        if (input.equals("throw error")) {
            throw new RuntimeException("Method call caused runtime exception. This is expected if the input was 'throw error'");
        }
        return CompletableFuture.completedStage(input + ": (async)");
    }

    @Tool(name = "asyncDelayedEcho", title = "Async Echo", description = "Echoes input asynchronously")
    public CompletionStage<String> asyncDelayedEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        return executor.supplyAsync(() -> {
            try {
                Thread.sleep(1_000); //simulate long running tool
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }).thenAccept((result) -> {
            if (input.equals("throw error")) {
                throw new RuntimeException("Method call caused runtime exception. This is expected if the input was 'throw error'");
            }
        }).thenApply((result) -> {
            return input + ": (async)";
        });
    }

    @Tool(name = "asyncCancellationTool", title = "Async Cancellable tool", description = "A tool that waits to be cancelled, called in async mode")
    public CompletionStage<String> asyncCancellationTool(Cancellation cancellation,
                                                         @ToolArg(name = "latchName", description = "name of countdown latch to use for test") String latchName) {
        LOG.info("[asyncCancellationTool] Starting");
        toolStatus.setRunning(latchName);
        return executor.supplyAsync(() -> {
            int counter = 0;
            while (counter++ < 20) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
                LOG.info("[asyncCancellationTool] Checking if tool is cancelled");
                if (cancellation.check().isRequested()) {
                    LOG.info("[asyncCancellationTool] tool is cancelled");
                    throw new OperationCancellationException();
                }
            }
            LOG.info("[asyncCancellationTool] the tool was not cancelled");
            return "If this String is returned, then the tool was not cancelled";
        });
    }

    public record City(String name, String country, int population, boolean isCapital) {};

    @Tool(name = "asyncListObjectTool", title = "Async asyncListObjectTool", description = "A tool to return a list of cities asynchronously", structuredContent = true)
    public CompletionStage<List<City>> asyncListObjectTool() {
        return executor.supplyAsync(() -> {
            City city1 = new City("Paris", "France", 8000, true);
            City city2 = new City("Manchester", "England", 15000, false);
            return List.of(city1, city2);
        });
    }

    @Tool(name = "asyncObjectTool", title = "Async asyncObjectTool", description = "A tool to return an object of cities asynchronously", structuredContent = true)
    public CompletionStage<City> asyncObjectTool(@ToolArg(name = "name", description = "name of your city") String name) {
        return executor.supplyAsync(() -> {
            return new City(name, "England", 8000, false);
        });
    }

    @Tool(name = "asyncToolThatNeverCompletes", title = "Async Echo", description = "Echoes input asynchronously")
    public CompletionStage<String> asyncToolThatNeverCompletes(@ToolArg(name = "input", description = "input to echo") String input) {

        CompletionStage<String> neverReturns = new CompletableFuture<>();

        return neverReturns.thenApply(result -> {
            return input + ": (async) : Will Never be returned";
        });
    }

}