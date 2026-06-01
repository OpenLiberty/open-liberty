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

/**
 * Normalizes protected resource metadata request paths and maps metadata lookup results into a
 * simple response model for the servlet layer.
 * <p>
 * This class is servlet-independent so the request-path behavior can be unit tested without a
 * servlet container. Subclasses provide the actual metadata lookup by implementing
 * {@link #resolveMetadataJson(String)}.
 * </p>
 */
public class OAuthProtectedResourceMetadataHandler {

    /**
     * Handles a request for protected resource metadata.
     *
     * @param pathInfo servlet path info identifying the protected resource whose metadata is being
     *            requested
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
     * Converts servlet path info into the normalized protected resource path used for metadata
     * lookup.
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
        return "/".concat(pathInfo);
    }

    /**
     * Resolves protected resource metadata for the specified protected resource path.
     *
     * @param protectedResourcePath normalized protected resource path, such as {@code /mcp}
     * @return metadata JSON, or {@code null} if no metadata should be served for that path
     */
    protected String resolveMetadataJson(String protectedResourcePath) {
        return null;
    }

    /**
     * Simple immutable response model used to decouple path handling from servlet APIs.
     */
    public static class MetadataResponse {
        private final boolean found;
        private final String contentType;
        private final String characterEncoding;
        private final String body;

        private MetadataResponse(boolean found, String contentType, String characterEncoding, String body) {
            this.found = found;
            this.contentType = contentType;
            this.characterEncoding = characterEncoding;
            this.body = body;
        }

        /**
         * Creates a response representing an unresolved protected resource.
         *
         * @return not-found response model
         */
        public static MetadataResponse notFound() {
            return new MetadataResponse(false, null, null, null);
        }

        /**
         * Creates a JSON response for resolved protected resource metadata.
         *
         * @param body metadata JSON body
         * @return successful JSON response model
         */
        public static MetadataResponse json(String body) {
            return new MetadataResponse(true, "application/json", "UTF-8", body);
        }

        public boolean isFound() {
            return found;
        }

        public String getContentType() {
            return contentType;
        }

        public String getCharacterEncoding() {
            return characterEncoding;
        }

        public String getBody() {
            return body;
        }
    }
}

// Made with Bob
