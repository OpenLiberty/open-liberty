/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import org.mcpjava.server.Icon.Theme;
import org.mcpjava.server.Role;

/**
 * Jsonb Adapters for Enum types in the MCP Java API
 */
public class EnumAdapters {

    /** Adapter for {@link Role} */
    public static final EnumAdapter<Role> ROLE_ADAPTER = new RoleAdapter();
    /** Adapter for {@link Theme} */
    public static final EnumAdapter<Theme> THEME_ADAPTER = new ThemeAdapter();

    public static final class RoleAdapter extends EnumAdapter<Role> {

        @Override
        protected String stringify(Role value) {
            return value.name().toLowerCase();
        }

        @Override
        protected Class<Role> getEnumClass() {
            return Role.class;
        }
    }

    public static final class ThemeAdapter extends EnumAdapter<Theme> {

        @Override
        protected String stringify(Theme value) {
            return value.name().toLowerCase();
        }

        @Override
        protected Class<Theme> getEnumClass() {
            return Theme.class;
        }
    }

}
