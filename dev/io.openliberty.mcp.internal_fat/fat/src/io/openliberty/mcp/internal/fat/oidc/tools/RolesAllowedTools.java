/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.oidc.tools;

import io.openliberty.mcp.annotations.Tool;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolesAllowedTools {

    @RolesAllowed("mcp-admin")
    @Tool(name = "adminTool", title = "Admin Tool", description = "A tool that can only be used by admins")
    public String adminTool() {
        return "Hello you handsome admin!";
    }

    @RolesAllowed("mcp-user")
    @Tool(name = "userTool", title = "User Tool", description = "A tool that can only be used by mcp users and admins")
    public String userTool() {
        return "Hello you basic user";
    }

}
