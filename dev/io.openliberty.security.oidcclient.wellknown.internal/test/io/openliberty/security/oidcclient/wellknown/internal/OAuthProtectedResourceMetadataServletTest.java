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

import org.junit.Test;

/**
 * Unit tests for path normalization in {@link OAuthProtectedResourceMetadataServlet}.
 */
public class OAuthProtectedResourceMetadataServletTest {

    private final OAuthProtectedResourceMetadataServlet servlet = new OAuthProtectedResourceMetadataServlet();

    @Test
    public void toProtectedResourcePath_nullIsNormalizedToRoot() {
        assertEquals("/", servlet.toProtectedResourcePath(null));
    }

    @Test
    public void toProtectedResourcePath_emptyIsNormalizedToRoot() {
        assertEquals("/", servlet.toProtectedResourcePath(""));
    }

    @Test
    public void toProtectedResourcePath_slashIsNormalizedToRoot() {
        assertEquals("/", servlet.toProtectedResourcePath("/"));
    }

    @Test
    public void toProtectedResourcePath_leadingSlashIsPreserved() {
        assertEquals("/myApp/protected", servlet.toProtectedResourcePath("/myApp/protected"));
    }

    @Test
    public void toProtectedResourcePath_missingLeadingSlashIsPrefixed() {
        assertEquals("/mcp", servlet.toProtectedResourcePath("mcp"));
    }

    @Test
    public void toProtectedResourcePath_nestedPathIsPreserved() {
        assertEquals("/app/api/v1", servlet.toProtectedResourcePath("/app/api/v1"));
    }

    @Test
    public void toProtectedResourcePath_trailingSlashIsPreserved() {
        assertEquals("/mcp/", servlet.toProtectedResourcePath("/mcp/"));
    }
}
