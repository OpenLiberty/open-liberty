/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.securityApps;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test app with no annotation at class level
 * We expect that method level security annotations will override the class one
 *
 */
@ApplicationScoped
public class AsyncNoClassAnnotationTools {

    @PermitAll
    @Tool(name = "noClassAnnotationAsync_echoPermitAll", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> noClassAnnotationAsync_echoPermitAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @DenyAll
    @Tool(name = "noClassAnnotationAsync_echoDenyAll", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> noClassAnnotationAsync_echoDenyAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @RolesAllowed("Admins")
    @Tool(name = "noClassAnnotationAsync_echoAdminAllowed", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> noClassAnnotationAsync_echoAdminAllowed(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @Tool(name = "noClassAnnotationAsync_echoNoSecurityAnnotationExists", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> noClassAnnotationAsync_echoNoSecurityAnnotationExists(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }
}
