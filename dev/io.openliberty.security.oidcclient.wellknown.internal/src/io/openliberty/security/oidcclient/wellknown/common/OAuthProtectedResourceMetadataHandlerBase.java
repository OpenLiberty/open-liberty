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
public class OAuthProtectedResourceMetadataHandlerBase {

    private final ProtectedResourceMetadataResolver metadataResolver;

    public OAuthProtectedResourceMetadataHandlerBase(ProtectedResourceMetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    /**
     * Handles a request for protected resource metadata.
     *
     * @param pathInfo servlet path info from the metadata endpoint request. This is the portion
     *                     of the request path after {@code /.well-known/oauth-protected-resource}. For
     *                     example, for {@code /.well-known/oauth-protected-resource/myApp/protected},
     *                     this value is {@code /myApp/protected}. It is not the full request URL or the
     *                     full request URI.
     * @return response model representing either a JSON metadata document or not found
     */
    public MetadataResponse handle(String pathInfo) {
        String protectedResourcePath = toProtectedResourcePath(pathInfo);
        String metadataJson = metadataResolver.resolveMetadataJson(protectedResourcePath);

        if (metadataJson == null) {
            return MetadataResponse.notFound();
        }

        return MetadataResponse.json(metadataJson);
    }

    /**
     * Converts servlet path info into a normalized protected resource path.
     *
     * @param pathInfo servlet path info from the metadata endpoint request
     * @return normalized protected resource path. Empty, {@code null}, or {@code /} values are
     *         normalized to {@code /}. Values without a leading slash are prefixed with {@code /}.
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
}