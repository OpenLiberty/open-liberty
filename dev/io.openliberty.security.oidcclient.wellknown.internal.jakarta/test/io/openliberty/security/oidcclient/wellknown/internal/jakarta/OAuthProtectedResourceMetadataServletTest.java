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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

public class OAuthProtectedResourceMetadataServletTest {

    @Test
    public void returnsNotFoundWhenProtectedResourceIsUnknown() {
        OAuthProtectedResourceMetadataHandler handler = new OAuthProtectedResourceMetadataHandler();

        OAuthProtectedResourceMetadataHandler.MetadataResponse response = handler.handle("/unknown");

        assertThat(response.isFound(), is(false));
        assertThat(response.getContentType(), is(nullValue()));
        assertThat(response.getCharacterEncoding(), is(nullValue()));
        assertThat(response.getBody(), is(nullValue()));
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

        assertThat(response.isFound(), is(true));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getCharacterEncoding(), is("UTF-8"));
        assertThat(response.getBody(), is("{\"resource\":\"https://example.com/mcp\",\"authorization_servers\":[\"https://example.com/as\"]}"));
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

        assertThat(resolvedPath[0], is("/mcp"));
        assertThat(response.isFound(), is(true));
        assertThat(response.getBody(), is("{}"));
    }
}

// Made with Bob