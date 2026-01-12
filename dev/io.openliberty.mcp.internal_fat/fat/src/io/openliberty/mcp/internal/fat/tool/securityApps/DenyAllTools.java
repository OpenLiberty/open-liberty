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

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test app with @DenyAll at class level
 * We expect that method level security annotations will override the class one
 *
 */
@ApplicationScoped
@DenyAll
public class DenyAllTools {

    @DenyAll
    @Tool(name = "denyAllClass_echoDenyAll", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoDenyAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @RolesAllowed("Admins")
    @Tool(name = "denyAllClass_echoAdminAllowed", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoAdminAllowed(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @RolesAllowed("TestUsers")
    @Tool(name = "denyAllClass_echoTestUserAllowed", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoTestUserAllowed(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @RolesAllowed({ "Admins", "TestUsers" })
    @Tool(name = "denyAllClass_echoTwoRolesAllowed", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoTwoRolesAllowed(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @PermitAll
    @Tool(name = "denyAllClass_echoPermitAll", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoPermitAll(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @Tool(name = "denyAllClass_echoNoSecurityAnnotationExists", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoNoSecurityAnnotationExists(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @RolesAllowed({ "RoleDoesNotExist" })
    @Tool(name = "denyAllClass_echoRoleDoesNotExist", title = "Echoes the input", description = "Returns the input unchanged")
    public String denyAllClass_echoRoleDoesNotExist(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }
}
