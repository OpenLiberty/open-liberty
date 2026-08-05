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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.mcp.internal.ToolValidation;

public class ToolValidationTest {
    @Test
    public void testUnicodeMatcher() {
        assertFalse(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("test_é").matches());
        assertFalse(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("test_ᴀ").matches());
        assertFalse(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("test_ぁ").matches());
        assertFalse(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("test_𝟏").matches());
        assertFalse(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("test—1").matches());
        assertTrue(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("hello").matches());
        assertTrue(ToolValidation.TOOL_NAME_CHARACTER_PATTERN.matcher("heLLo2").matches());
    }

    @Test
    public void testIsValidMetaPrefix() {
        assertTrue(ToolValidation.isValidMetaPrefix("a/"));
        assertTrue(ToolValidation.isValidMetaPrefix("a.b/"));
        assertTrue(ToolValidation.isValidMetaPrefix("org.example.foo.bar/"));
        assertTrue(ToolValidation.isValidMetaPrefix("a1/"));
        assertTrue(ToolValidation.isValidMetaPrefix("a1-3/"));

        assertFalse(ToolValidation.isValidMetaPrefix("1a/"));
        assertFalse(ToolValidation.isValidMetaPrefix("a..b/"));
        assertFalse(ToolValidation.isValidMetaPrefix("/"));
        assertFalse(ToolValidation.isValidMetaPrefix("a1_3/"));
        assertFalse(ToolValidation.isValidMetaPrefix("a1-/"));
        assertFalse(ToolValidation.isValidMetaPrefix("a1-.b/"));
        assertFalse(ToolValidation.isValidMetaPrefix("aᴀa/"));
        assertFalse(ToolValidation.isValidMetaPrefix("a𝟏a/"));
        assertFalse(ToolValidation.isValidMetaPrefix("org.example.foo.bar"));
        assertFalse(ToolValidation.isValidMetaPrefix(null));
    }

    @Test
    public void isValidMetaName() {
        assertTrue(ToolValidation.isValidMetaName("a"));
        assertTrue(ToolValidation.isValidMetaName("a..b"));
        assertTrue(ToolValidation.isValidMetaName("org.example.foo.bar"));
        assertTrue(ToolValidation.isValidMetaName("a1"));
        assertTrue(ToolValidation.isValidMetaName("a1-3"));
        assertTrue(ToolValidation.isValidMetaName("1a"));
        assertTrue(ToolValidation.isValidMetaName("a1_3"));
        assertTrue(ToolValidation.isValidMetaName("a1-.b"));
        assertTrue(ToolValidation.isValidMetaName("")); // Blank name permitted by spec

        assertFalse(ToolValidation.isValidMetaName("a/"));
        assertFalse(ToolValidation.isValidMetaName("/a"));
        assertFalse(ToolValidation.isValidMetaName("a1-"));
        assertFalse(ToolValidation.isValidMetaName("aᴀa"));
        assertFalse(ToolValidation.isValidMetaName("a𝟏a"));
        assertFalse(ToolValidation.isValidMetaName(null));
    }

    @Test
    public void isValidMetaKey() {
        // The prefix is optional, so all meta name tests are still valid
        assertTrue(ToolValidation.isValidMetaKey("a"));
        assertTrue(ToolValidation.isValidMetaKey("org.example.foo.bar"));
        assertTrue(ToolValidation.isValidMetaKey("a1"));
        assertTrue(ToolValidation.isValidMetaKey("a1-3"));
        assertTrue(ToolValidation.isValidMetaKey("1a"));
        assertTrue(ToolValidation.isValidMetaKey("a1_3"));
        assertTrue(ToolValidation.isValidMetaKey("a1-.b"));
        assertTrue(ToolValidation.isValidMetaKey(""));

        assertFalse(ToolValidation.isValidMetaKey("a1-"));
        assertFalse(ToolValidation.isValidMetaKey("aᴀa"));
        assertFalse(ToolValidation.isValidMetaKey("a𝟏a"));
        assertFalse(ToolValidation.isValidMetaKey(null));

        // Valid prefix + name
        assertTrue(ToolValidation.isValidMetaKey("a/a"));
        assertTrue(ToolValidation.isValidMetaKey("org.example.foo.bar/a"));
        assertTrue(ToolValidation.isValidMetaKey("org.example.foo.bar/org.example.foo.bar"));
        assertTrue(ToolValidation.isValidMetaKey("org.example.foo-bar/a"));
        assertTrue(ToolValidation.isValidMetaKey("a1/a_b"));
        assertTrue(ToolValidation.isValidMetaKey("a1.b2/a..b"));

        // Invalid prefix
        assertFalse(ToolValidation.isValidMetaKey("1a/a"));
        assertFalse(ToolValidation.isValidMetaKey("a..b/a"));
        assertFalse(ToolValidation.isValidMetaKey("/a"));
        assertFalse(ToolValidation.isValidMetaKey("1_3/a"));

        // Invalid name
        assertFalse(ToolValidation.isValidMetaKey("a.b.c/"));
        assertFalse(ToolValidation.isValidMetaKey("a.b.c/a/b"));
        assertFalse(ToolValidation.isValidMetaKey("a.b.c/a-"));
        assertFalse(ToolValidation.isValidMetaKey("a.b.c/a:b"));
    }

}
