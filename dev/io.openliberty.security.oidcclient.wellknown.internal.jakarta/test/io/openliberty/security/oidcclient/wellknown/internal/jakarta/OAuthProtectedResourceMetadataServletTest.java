/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.internal.jakarta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OAuthProtectedResourceMetadataServletTest {

    @Test
    public void returnsNotFoundWhenProtectedResourceIsUnknown() {
        OAuthProtectedResourceMetadataHandler handler = new OAuthProtectedResourceMetadataHandler();

        OAuthProtectedResourceMetadataHandler.MetadataResponse response = handler.handle("/unknown");

        assertFalse(response.isFound());
        assertNull(response.getContentType());
        assertNull(response.getCharacterEncoding());
        assertNull(response.getBody());
    }

    @Test
    public void returnsMetadataJsonForKnownEnabledProtectedResource() {
        OAuthProtectedResourceMetadataHandler handler = new OAuthProtectedResourceMetadataHandler() {
            @Override
            protected String resolveMetadataJson(String protectedResourcePath) {
                return "{\"resource\":\"https://example.com/mcp\",\"authorization_servers\":[\"https://example.com/as\"]}";
            }
        };

        OAuthProtectedResourceMetadataHandler.MetadataResponse response = handler.handle("/mcp");

        assertTrue(response.isFound());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("{\"resource\":\"https://example.com/mcp\",\"authorization_servers\":[\"https://example.com/as\"]}", response.getBody());
    }

    @Test
    public void normalizesMissingLeadingSlashBeforeLookup() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandler handler = new OAuthProtectedResourceMetadataHandler() {
            @Override
            protected String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return "{}";
            }
        };

        OAuthProtectedResourceMetadataHandler.MetadataResponse response = handler.handle("mcp");

        assertEquals("/mcp", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals("{}", response.getBody());
    }
}