/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.security.oidcclient.wellknown.common.MetadataResponse;
import io.openliberty.security.oidcclient.wellknown.common.OAuthProtectedResourceMetadataHandlerBase;
import io.openliberty.security.oidcclient.wellknown.common.ProtectedResourceMetadataResolver;

/**
 * Unit tests for {@link OAuthProtectedResourceMetadataHandlerBase}.
 */
public class OAuthProtectedResourceMetadataServletTest {

    private static final String METADATA_JSON = "{\"resource\":\"https://example.com/mcp\",\"authorization_servers\":[\"https://example.com/as\"]}";

    @Test
    public void returnsNotFoundWhenResolverReturnsNull() {
        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                return null;
            }
        });

        MetadataResponse response = handler.handle("/unknown");

        assertFalse(response.isFound());
        assertNull(response.getContentType());
        assertNull(response.getCharacterEncoding());
        assertNull(response.getBody());
    }

    @Test
    public void returnsMetadataJsonWhenResolverReturnsMetadata() {
        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("/mcp");

        assertTrue(response.isFound());
        assertEquals("application/json", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void normalizesMissingLeadingSlashBeforeLookup() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("mcp");

        assertEquals("/mcp", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void handlesNullPathInfoAsRootPath() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle(null);

        assertEquals("/", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void handlesEmptyPathInfoAsRootPath() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("");

        assertEquals("/", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void handlesSlashPathInfoAsRootPath() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("/");

        assertEquals("/", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void preservesNestedProtectedResourcePath() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("/app/api/v1");

        assertEquals("/app/api/v1", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }

    @Test
    public void preservesTrailingSlashInProtectedResourcePath() {
        final String[] resolvedPath = new String[1];

        OAuthProtectedResourceMetadataHandlerBase handler = new OAuthProtectedResourceMetadataHandlerBase(new ProtectedResourceMetadataResolver() {
            @Override
            public String resolveMetadataJson(String protectedResourcePath) {
                resolvedPath[0] = protectedResourcePath;
                return METADATA_JSON;
            }
        });

        MetadataResponse response = handler.handle("/mcp/");

        assertEquals("/mcp/", resolvedPath[0]);
        assertTrue(response.isFound());
        assertEquals(METADATA_JSON, response.getBody());
    }
}