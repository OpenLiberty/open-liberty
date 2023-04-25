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

import org.junit.Test;

import static org.junit.Assert.*;

public class OpenAPIEndpointManagerTest {

    private static final String DEFAULT_OPENAPI_ENDPOINT="/openapi";
    private static final String ALTERNATIVE_OPENAPI_ENDPOINT="/docs";

    private static String METRICS_PATH="/metrics";
    private static String API_PATH="/ibm/api";
    private static String HEALTH_PATH="/health";
    private static String id1="ui";
    private static String id2="doc";

    @Test
    public void resolvePathValidPath(){
        assertEquals(OpenAPIEndpointManager.resolvePath("/path"),"/path");
    }

    @Test
    public void resolvePathAddStartingSlash(){
        assertEquals(OpenAPIEndpointManager.resolvePath("path"),"/path");
    }

    @Test
    public void resolvePathRemoveTraillingSlash(){
        assertEquals(OpenAPIEndpointManager.resolvePath("/path/"),"/path");
    }

    @Test
    public void resolvePathReplaceMultiSlash(){
        assertEquals(OpenAPIEndpointManager.resolvePath("/path//path2////path3"),"/path/path2/path3");
    }

    @Test
    public void resolvePathSingleSlash(){
        assertEquals(OpenAPIEndpointManager.resolvePath("/"),"/");
    }

    @Test
    public void resolvePathEmptyPath(){
        assertEquals(OpenAPIEndpointManager.resolvePath(""),"/");
    }

    @Test
    public void validatePathSlashPath(){
         assertTrue(OpenAPIEndpointManager.validatePath("/", id1));
    }

    @Test
    public void validatePathSimpleValidPath(){
        assertTrue(OpenAPIEndpointManager.validatePath("/foo/bar", id1));
    }

    @Test
    public void validatePathOpenAPIPath(){
        //UI Path becomes `/openapi`, while Document path is registered as `/docs`
        assertTrue(OpenAPIEndpointManager.validatePath(DEFAULT_OPENAPI_ENDPOINT, id1));
    }

    @Test
    public void validatePathDotPathStart(){
        assertFalse(OpenAPIEndpointManager.validatePath("/.", id1));
    }

    @Test
    public void validatePathDoubleDotPathStart(){
        assertFalse(OpenAPIEndpointManager.validatePath("/..", id1));
    }

    @Test
    public void validatePathInvalidDotSegments(){
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/./bar", id2));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/../bar", id2));
    }

    @Test
    public void validatePathMultiDotSegments(){
        assertTrue(OpenAPIEndpointManager.validatePath("/foo/..../bar",id2));
    }

    @Test
    public void validatePathFeaturePaths(){
        assertTrue(OpenAPIEndpointManager.validatePath(METRICS_PATH,id1));
        assertTrue(OpenAPIEndpointManager.validatePath(HEALTH_PATH,id2));
        assertTrue(OpenAPIEndpointManager.validatePath(API_PATH,id1));
    }

    @Test
    public void validatePathEncodedCharacter(){
        assertFalse(OpenAPIEndpointManager.validatePath("/foo%3ebar",id2));
    }

    @Test
    public void validatePathValidNonAlphaCharacters(){
        assertTrue(OpenAPIEndpointManager.validatePath("/foo.bar",id1));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo..bar",id1));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo-bar",id1));
        assertTrue(OpenAPIEndpointManager.validatePath("/foo_bar",id1));
        assertTrue(OpenAPIEndpointManager.validatePath("/f00/bar",id1));
    }

    @Test
    public void validatePathInvalidCharacters(){
        assertFalse(OpenAPIEndpointManager.validatePath("/foo\\bar",id2));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo/bar?query=param",id2));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo#bar",id2));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo;bar",id2));
        assertFalse(OpenAPIEndpointManager.validatePath("/foo%3ebar",id2));
    }
}
