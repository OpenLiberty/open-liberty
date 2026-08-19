/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import javax.servlet.http.HttpServletRequest;

/**
 * Service interface for resolving OAuth 2.0 protected resource metadata.
 */
public interface OAuthProtectedResourceMetadataService {

    /**
     * Returns the OAuth 2.0 protected resource metadata JSON for the given request and
     * protected resource path, or {@code null} if no OIDC client configuration matches.
     *
     * @param request the incoming metadata endpoint HTTP request
     * @param protectedResourcePath normalized protected resource path
     * @param absoluteResourceUrl absolute protected resource URL to include in the metadata document
     * @return serialized JSON metadata document, or {@code null} if no match
     */
    String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl);
}
