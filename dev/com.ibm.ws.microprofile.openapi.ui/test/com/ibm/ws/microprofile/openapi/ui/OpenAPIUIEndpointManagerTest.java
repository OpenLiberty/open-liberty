/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.openapi.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class OpenAPIUIEndpointManagerTest {

    private static final String DEFAULT_OPENAPI_ENDPOINT="/openapi";
    private static final String ALTERNATIVE_OPENAPI_ENDPOINT="/docs";

    private static String METRICS_PATH="/metrics";
    private static String API_PATH="/ibm/api";
    private static String HEALTH_PATH="/health";

    @Test
    public void resolvePathValidPath(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath("/path"),"/path");
    }

    @Test
    public void resolvePathAddStartingSlash(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath("path"),"/path");
    }

    @Test
    public void resolvePathRemoveTraillingSlash(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath("/path/"),"/path");
    }

    @Test
    public void resolvePathReplaceMultiSlash(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath("/path//path2////path3"),"/path/path2/path3");
    }

    @Test
    public void resolvePathSingleSlash(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath("/"),"/");
    }

    @Test
    public void resolvePathEmptyPath(){
        assertEquals(OpenAPIUIEndpointManager.resolvePath(""),"/");
    }

    @Test
    public void validatePathSlashPath(){
         assertTrue(OpenAPIUIEndpointManager.validatePath("/",DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathSimpleValidPath(){
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo/bar", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathConflictOpenAPIPath(){
        assertFalse(OpenAPIUIEndpointManager.validatePath(DEFAULT_OPENAPI_ENDPOINT, DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathOpenAPIPath(){
        //UI Path becomes `/openapi`, while Document path is registered as `/docs`
        assertTrue(OpenAPIUIEndpointManager.validatePath(DEFAULT_OPENAPI_ENDPOINT,ALTERNATIVE_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathDotPathStart(){
        assertFalse(OpenAPIUIEndpointManager.validatePath("/.", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathDoubleDotPathStart(){
        assertFalse(OpenAPIUIEndpointManager.validatePath("/..", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathInvalidDotSegments(){
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo/./bar", DEFAULT_OPENAPI_ENDPOINT));
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo/../bar", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathMultiDotSegments(){
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo/..../bar", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathFeaturePaths(){
        assertTrue(OpenAPIUIEndpointManager.validatePath(METRICS_PATH, DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath(HEALTH_PATH, DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath(API_PATH, DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathEncodedCharacter(){
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo%3ebar", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathValidNonAlphaCharacters(){
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo.bar", DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo..bar", DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo-bar", DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath("/foo_bar", DEFAULT_OPENAPI_ENDPOINT));
        assertTrue(OpenAPIUIEndpointManager.validatePath("/f00/bar", DEFAULT_OPENAPI_ENDPOINT));
    }

    @Test
    public void validatePathInvalidCharacters(){
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo\\bar", DEFAULT_OPENAPI_ENDPOINT));
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo/bar?query=param", DEFAULT_OPENAPI_ENDPOINT));
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo#bar", DEFAULT_OPENAPI_ENDPOINT));
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo;bar", DEFAULT_OPENAPI_ENDPOINT));
        assertFalse(OpenAPIUIEndpointManager.validatePath("/foo%3ebar", DEFAULT_OPENAPI_ENDPOINT));
    }

}
