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

import static io.openliberty.mcp.internal.fat.utils.TestConstants.POSITIVE_TIMEOUT;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.messaging.Cancellation.OperationCancellationException;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

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

    @Tool(name = "asyncObjectToolWithoutStructuredContent", title = "Async Object Tool No Schema", description = "Returns a city object but no structuredContent")
    public CompletionStage<City> asyncObjectToolWithoutStructuredContent() {
        return executor.supplyAsync(() -> new City("Leeds", "England", 7000, false));
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
        toolStatus.signalStarted(latchName);
        return executor.supplyAsync(() -> {
            long startTime = System.nanoTime();
            while ((System.nanoTime() - startTime) < POSITIVE_TIMEOUT.toNanos()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
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

    /*******************************************************************************
     * Test encoding a completion stage with a given content encoder
     *******************************************************************************/

    public record Person(String fistName, String lastName, int age) {}

    @ApplicationScoped
    public static class PersonContentEncoder implements ContentEncoder<Person> {

        private final Jsonb jsonb = JsonbBuilder.create();

        @Override
        public boolean supports(Class<?> runtimeType) {
            return Person.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(Person person) {
            Person encodedPerson = new Person(person.fistName, "Encoded by PersonContentEncoder", person.age);
            return new TextContent(jsonb.toJson(encodedPerson));
        }
    }

    @Tool(name = "testContentEncoderCompletionStage", description = "tests that a Person object is encoded to content correctly by the PersonContentEncoder")
    public CompletionStage<Person> testContentEncoderCompletionStage() {
        return executor.supplyAsync(() -> {
            return new Person("Jon", "Doe", 32);
        });
    }

    /*******************************************************************************
     * Test encoding CompletionStage that wraps a list of objects with a given
     * content encoder
     *******************************************************************************/

    @Tool(name = "testContentEncoderEncodingACompletionStageContainingAList",
          description = "tests that a Person object is encoded to content correctly by the PersonContentEncoder")
    public CompletionStage<List<Person>> testContentEncoderEncodingACompletionStageContainingAList() {
        return executor.supplyAsync(() -> {
            return List.of(new Person("Jon", "Doe", 32), new Person("Jane", "Doe", 22));
        });
    }

}