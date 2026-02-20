/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.McpResponseException;
import io.openliberty.mcp.tools.ToolManager.ToolArguments;
import io.openliberty.mcp.tools.ToolManager.ToolDefinition;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.json.bind.Jsonb;

/**
 * A handler to call a bean method asynchronously, suitable for use with {@link ToolDefinition#setAsyncHandler(java.util.function.Function)}
 */
public class AsyncBeanMethodHandler extends BeanMethodHandler<CompletionStage<ToolResponse>> {

    /**
     * @param jsonb the Jsonb to use to encode a structured response
     * @param bm the bean manager to use to look up the bean
     * @param method metadata about the method to call
     */
    public AsyncBeanMethodHandler(Jsonb jsonb, BeanManager bm, MethodMetadata method) {
        super(jsonb, bm, method);
    }

    @Override
    @FFDCIgnore({ McpResponseException.class, InvocationTargetException.class, IllegalAccessException.class, IllegalArgumentException.class, Throwable.class })
    public CompletionStage<ToolResponse> apply(ToolArguments toolArgs) {
        Object[] argsArray = constructArgsArray(toolArgs);
        CreationalContext<Object> cc = bm.createCreationalContext(null);

        try {
            Object beanInstance = bm.getReference(method.bean(), method.bean().getBeanClass(), cc);

            CompletionStage<?> methodStage;
            try {
                try {
                    // Call the tool method
                    methodStage = ((CompletionStage<?>) method.method().invoke(beanInstance, argsArray));
                } catch (Throwable t) {
                    releaseCc(cc);
                    throw t;
                }
            } catch (IllegalAccessException e) {
                throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + method.name()));
            } catch (IllegalArgumentException e) {
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
            }

            return methodStage.thenApply(result -> createSuccessfulResponse(result, toolArgs))
                              .exceptionally(throwable -> {
                                  if (throwable instanceof CompletionException) {
                                      throwable = throwable.getCause();
                                  }
                                  if (isBusinessException(throwable)) {
                                      return ToolResponses.createBusinessErrorResponse(throwable);
                                  } else {
                                      return ToolResponses.createNonBusinessErrorResponse(throwable, method.name());
                                  }
                              })
                              .whenComplete((result, throwable) -> releaseCc(cc));
        } catch (McpResponseException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (isBusinessException(t)) {
                return CompletableFuture.completedStage(ToolResponses.createBusinessErrorResponse(t));
            } else {
                return CompletableFuture.completedStage(ToolResponses.createNonBusinessErrorResponse(t, method.name()));
            }
        }
    }

}
