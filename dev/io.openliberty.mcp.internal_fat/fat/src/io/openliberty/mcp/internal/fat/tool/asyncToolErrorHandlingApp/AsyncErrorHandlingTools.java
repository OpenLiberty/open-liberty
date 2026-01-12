/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.asyncToolErrorHandlingApp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.tools.ToolCallException;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AsyncErrorHandlingTools {

    @Resource
    private ManagedScheduledExecutorService executor;

    @Tool
    @WrapBusinessError(BusinessException.class)
    public CompletionStage<String> asyncErrorTool(@ToolArg String exception, @ToolArg String failureMechanism) throws Exception {
        Supplier<? extends Exception> exceptionSupplier = switch (exception) {
            case "BusinessException" -> () -> new BusinessException("BusinessException");
            case "NonBusinessException" -> () -> new NonBusinessException("NonBusinessException");
            case "SpecificBusinessException" -> () -> new SpecificBusinessException("SpecificBusinessException");
            case "ToolCallException" -> () -> new ToolCallException("ToolCallException");
            default -> throw new ToolCallException("Invalid exception type: " + exception);
        };

        CompletionStage<String> result = switch (failureMechanism) {
            case "THROWN" -> throw exceptionSupplier.get();
            case "FAILED" -> CompletableFuture.failedStage(exceptionSupplier.get());
            case "FAILED_DELAY" -> delayedCS().thenCompose(ex -> CompletableFuture.failedStage(exceptionSupplier.get()));
            case "FAILED_MULTISTAGE" -> CompletableFuture.<String> failedStage(exceptionSupplier.get())
                                                         .thenApply(Function.identity())
                                                         .thenApply(Function.identity())
                                                         .thenApply(Function.identity());
            default -> throw new ToolCallException("Invalid failure mechanism: " + failureMechanism);
        };
        return result;
    }

    /**
     * Create a CompletionStage which completes after a delay.
     * <p>
     * Uses {@link ManagedScheduledExecutorService} to delay completion.
     *
     * @return CompletionStage which will complete in the future
     */
    private CompletionStage<String> delayedCS() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        executor.schedule(() -> cf.complete("Delayed result"), 500, TimeUnit.MILLISECONDS);
        return cf;
    }
}
