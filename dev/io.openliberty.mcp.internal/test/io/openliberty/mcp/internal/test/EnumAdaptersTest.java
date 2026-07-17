/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static io.openliberty.mcp.internal.EnumAdapters.ROLE_ADAPTER;
import static io.openliberty.mcp.internal.EnumAdapters.THEME_ADAPTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mcpjava.server.Icon.Theme;
import org.mcpjava.server.Role;

public class EnumAdaptersTest {

    @Test
    public void testRoleAdapter() {
        assertEquals("user", ROLE_ADAPTER.adaptToJson(Role.USER));
        assertEquals("assistant", ROLE_ADAPTER.adaptToJson(Role.ASSISTANT));
        assertEquals(Role.USER, ROLE_ADAPTER.adaptFromJson("user"));
        assertEquals(Role.ASSISTANT, ROLE_ADAPTER.adaptFromJson("assistant"));
        assertNull(ROLE_ADAPTER.adaptFromJson("foo"));
        assertNull(ROLE_ADAPTER.adaptFromJson(""));
    }

    @Test
    public void testThemeAdapter() {
        assertEquals("light", THEME_ADAPTER.adaptToJson(Theme.LIGHT));
        assertEquals("dark", THEME_ADAPTER.adaptToJson(Theme.DARK));
        assertEquals(Theme.LIGHT, THEME_ADAPTER.adaptFromJson("light"));
        assertEquals(Theme.DARK, THEME_ADAPTER.adaptFromJson("dark"));
        assertNull(THEME_ADAPTER.adaptFromJson("foo"));
        assertNull(THEME_ADAPTER.adaptFromJson(""));
    }
}
