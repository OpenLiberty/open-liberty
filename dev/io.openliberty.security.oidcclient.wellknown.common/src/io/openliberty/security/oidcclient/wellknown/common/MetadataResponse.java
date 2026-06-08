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
 * Simple immutable response model used to decouple protected resource metadata path handling
 * from servlet APIs.
 */
public class MetadataResponse {

    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";

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
        return new MetadataResponse(true, APPLICATION_JSON, UTF_8, body);
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