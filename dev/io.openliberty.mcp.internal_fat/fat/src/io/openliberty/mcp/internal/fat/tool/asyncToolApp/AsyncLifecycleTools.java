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

import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class AsyncLifecycleTools {
    private static final Logger LOG = Logger.getLogger(AsyncLifecycleTools.class.getName());
    private String id;

    @Resource
    ManagedExecutorService executor;

    @Inject
    ToolStatus toolStatus;

    @PostConstruct
    void init() {
        id = "AsyncLifecycleTools#" + System.identityHashCode(this);
        LOG.info("[LIFECYCLE] @PostConstruct AsyncLifecycleTools fired (" + id + ")");
    }

    @PreDestroy
    void destroy() {
        LOG.info("[LIFECYCLE] @PreDestroy AsyncLifecycleTools (" + id + ")");
    }

    @Tool(name = "asyncLifecycleEcho", title = "Async Echo", description = "Echoes input asynchronously")
    public CompletionStage<String> asyncLifecycleEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        LOG.info("[LOGGED] AsyncLifecycleTools.asyncLifecycleEcho Tool logged");
        if (input.equals("throw error")) {
            LOG.info("[LOGGED] AsyncLifecycleTools.asyncLifecycleEcho Tool throwing error");
            throw new RuntimeException("Method call caused runtime exception. This is expected if the input was 'throw error'");
        }
        return executor.supplyAsync(() -> {
            return input + ": (async)";
        });
    }
}