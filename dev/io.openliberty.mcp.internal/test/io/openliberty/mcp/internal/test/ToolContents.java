/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.meta.MetaKey;

public class ToolContents {

    @Test
    public void testTextContentConstructorAndFields() {
        TextContent content = new TextContent("Hello world");

        assertEquals("Hello world", content.text());
        assertNull(content._meta());
        assertEquals(TextContent.Type.TEXT, content.type());
        assertSame(content, content.asText());
    }

    @Test
    public void testTextContentWithMeta() {
        Map<MetaKey, Object> meta = Map.of();
        TextContent content = new TextContent("Text with meta", meta);

        assertEquals("Text with meta", content.text());
        assertEquals(meta, content._meta());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTextContentNullTextThrowsException() {
        new TextContent(null);
    }
}
