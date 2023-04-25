/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.microprofile.openapi.internal.common.OpenAPIEndpointManager.EndpointId;

public class OpenAPIEndpointManagerTest {

    private static final String DEFAULT_OPENAPI_ENDPOINT = "/openapi";

    @Test
    public void resolvePathValidPath() {
        assertEquals(OpenAPIEndpointManager.normalizePath("/path"), "/path");
    }

    @Test
    public void resolvePathAddStartingSlash() {
        assertEquals(OpenAPIEndpointManager.normalizePath("path"), "/path");
    }

    @Test
    public void resolvePathRemoveTraillingSlash() {
        assertEquals(OpenAPIEndpointManager.normalizePath("/path/"), "/path");
    }

    @Test
    public void resolvePathReplaceMultiSlash() {
        assertEquals(OpenAPIEndpointManager.normalizePath("/path//path2////path3"), "/path/path2/path3");
    }

    @Test
    public void resolvePathSingleSlash() {
        assertEquals(OpenAPIEndpointManager.normalizePath("/"), "/");
    }

    @Test
    public void resolvePathEmptyPath() {
        assertEquals(OpenAPIEndpointManager.normalizePath(""), "/");
    }

    @Test
    public void validatePathSlashPath() {
        assertTrue(OpenAPIEndpointManager.validatePath("/", EndpointId.UI));
    }

    @Test
    public void validatePathSimpleValidPath() {
        assertTrue(OpenAPIEndpointManager.validatePath("/foo/bar", EndpointId.UI));
    }

    @Test
    public void validatePathOpenAPIPath() {
        //UI Path becomes `/openapi`, while Document path is registered as `/docs`
        assertTrue(OpenAPIEndpointManager.validatePath(DEFAULT_OPENAPI_ENDPOINT, EndpointId.UI));
    }

    @Test
    public void validatePathDotPathStart() {
        assertFalse(OpenAPIEndpointManager.validatePath("/.", EndpointId.UI));
    }

    @Test
    public void validatePathDoubleDotPathStart() {
        assertFalse(OpenAPIEndpointManager.validatePath("/..", EndpointId.UI));
    }

    @Test
    public void validatePathInvalidDotSegments() {
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/./bar", EndpointId.DOCUMENT));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/../bar", EndpointId.DOCUMENT));
    }

    @Test
    public void validatePathMultiDotSegments() {
        assertTrue(OpenAPIEndpointManager.validatePath("/foo/..../bar", EndpointId.DOCUMENT));
    }

    @Test
    public void validatePathEncodedCharacter() {
        assertFalse(OpenAPIEndpointManager.validatePath("/foo%3ebar", EndpointId.DOCUMENT));
    }

    @Test
    public void validatePathValidNonAlphaCharacters() {
        assertTrue(OpenAPIEndpointManager.validatePath("/foo.bar", EndpointId.UI));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo..bar", EndpointId.UI));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo-bar", EndpointId.UI));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo_bar", EndpointId.UI));
        assertTrue(OpenAPIEndpointManager.validatePath("/f00/bar", EndpointId.UI));
    }

    @Test
    public void validatePathInvalidCharacters() {
        assertFalse(OpenAPIEndpointManager.validatePath("/foo\\bar", EndpointId.DOCUMENT));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/bar?query=param", EndpointId.DOCUMENT));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo#bar", EndpointId.DOCUMENT));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo;bar", EndpointId.DOCUMENT));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo%3ebar", EndpointId.DOCUMENT));
    }
}
