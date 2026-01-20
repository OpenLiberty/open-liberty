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

import io.openliberty.mcp.internal.McpCdiExtension;

public class McpCdiExtensionTest {
    @Test
    public void testUnicodeMatcher() {
        assertFalse(McpCdiExtension.getRegexMatcher().matcher("test_é").matches());
        assertFalse(McpCdiExtension.getRegexMatcher().matcher("test_ᴀ").matches());
        assertFalse(McpCdiExtension.getRegexMatcher().matcher("test_ぁ").matches());
        assertFalse(McpCdiExtension.getRegexMatcher().matcher("test_𝟏").matches());
        assertFalse(McpCdiExtension.getRegexMatcher().matcher("test—1").matches());
        assertTrue(McpCdiExtension.getRegexMatcher().matcher("hello").matches());
        assertTrue(McpCdiExtension.getRegexMatcher().matcher("heLLo2").matches());

    }

}
