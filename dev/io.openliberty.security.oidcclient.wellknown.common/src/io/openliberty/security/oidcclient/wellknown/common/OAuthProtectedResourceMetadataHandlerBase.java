/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.common;

/**
 * Base class for OAuth 2.0 Protected Resource Metadata handlers.
 * <p>
 * This class contains shared servlet-independent logic for normalizing protected resource
 * metadata request paths and mapping metadata lookup results into response models.
 * </p>
 */
public abstract class OAuthProtectedResourceMetadataHandlerBase {

    /**
     * Handles a request for protected resource metadata.
     *
     * @param pathInfo servlet path info identifying the protected resource whose metadata is being requested
     * @return response model representing either a JSON metadata document or not found
     */
    public MetadataResponse handle(String pathInfo) {
        String protectedResourcePath = toProtectedResourcePath(pathInfo);
        String metadataJson = resolveMetadataJson(protectedResourcePath);

        if (metadataJson == null) {
            return MetadataResponse.notFound();
        }

        return MetadataResponse.json(metadataJson);
    }

    /**
     * Converts servlet path info into the normalized protected resource path used for metadata lookup.
     *
     * @param pathInfo servlet path info from the incoming request
     * @return normalized protected resource path beginning with {@code /}
     */
    protected String toProtectedResourcePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return "/";
        }

        if (pathInfo.startsWith("/")) {
            return pathInfo;
        }

        return "/" + pathInfo;
    }

    /**
     * Resolves protected resource metadata for the specified protected resource path.
     *
     * @param protectedResourcePath normalized protected resource path, such as {@code /mcp}
     * @return metadata JSON, or {@code null} if no metadata should be served for that path
     */
    protected abstract String resolveMetadataJson(String protectedResourcePath);
}