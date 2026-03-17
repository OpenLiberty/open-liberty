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
 * Test app with @RolesAllowed at class level
 * We expect that method level security annotations will override the class one
 */
@ApplicationScoped
@RolesAllowed("Admins")
public class AsyncAdminsRoleTools {

    @DenyAll
    @Tool(name = "rolesAllowedAsyncClass_Admins_echoDenyAll", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> rolesAllowedAsyncClass_Admins_echoDenyAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @RolesAllowed("Admins")
    @Tool(name = "rolesAllowedAsyncClass_Admins_echoAdminAllowed", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> rolesAllowedAsyncClass_Admins_echoAdminAllowed(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @PermitAll
    @Tool(name = "rolesAllowedAsyncClass_Admins_echoPermitAll", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> rolesAllowedAsyncClass_Admins_echoPermitAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }

    @Tool(name = "rolesAllowedAsyncClass_Admins_echoNoSecurityAnnotationExists", title = "Echoes the input", description = "Returns the input unchanged")
    public CompletionStage<String> rolesAllowedAsyncClass_Admins_echoNoSecurityAnnotationExists(@ToolArg(name = "input", description = "input to echo") String input) {
        return CompletableFuture.completedStage(input);
    }
}
