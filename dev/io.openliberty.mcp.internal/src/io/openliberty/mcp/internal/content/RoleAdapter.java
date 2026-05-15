/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.content;

import org.mcpjava.server.Role;

import jakarta.json.bind.adapter.JsonbAdapter;

public class RoleAdapter implements JsonbAdapter<Role, String> {

    @Override
    public Role adaptFromJson(String string) throws Exception {
        return Role.valueOf(string.toUpperCase());
    }

    @Override
    public String adaptToJson(Role role) throws Exception {
        return role.name().toLowerCase();
    }
}