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

/**
 * Converts servlet path information into a normalized protected resource path and
 * packages the resulting metadata lookup into a simple response model.
 */
public class OAuthProtectedResourceMetadataHandler {

    /**
     * Resolves metadata for the supplied servlet path info.
     *
     * @param pathInfo the servlet path info relative to the well-known context path
     * @return a response wrapper describing whether metadata was found and, if so, the JSON payload
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
     * Normalizes servlet path info into a protected resource path.
     *
     * @param pathInfo the path info from the servlet request
     * @return {@code "/"} for the root protected resource, or the supplied path prefixed with
     *         {@code /} when necessary
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
     * Resolves the JSON metadata document for the supplied protected resource path.
     *
     * @param protectedResourcePath the normalized protected resource path
     * @return the metadata JSON, or {@code null} if no metadata exists for the path
     */
    protected String resolveMetadataJson(String protectedResourcePath) {
        return null;
    }

    /**
     * Immutable result of attempting to resolve protected resource metadata.
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
         * Creates a response representing the absence of metadata for the requested path.
         *
         * @return a not-found response
         */
        public static MetadataResponse notFound() {
            return new MetadataResponse(false, null, null, null);
        }

        /**
         * Creates a JSON metadata response.
         *
         * @param body the metadata JSON body
         * @return a found response containing JSON metadata
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