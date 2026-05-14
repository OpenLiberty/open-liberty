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

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet that serves OAuth 2.0 protected resource metadata from the
 * {@code /.well-known/oauth-protected-resource} context path.
 * <p>
 * It extracts the protected resource path from the incoming request, delegates lookup to
 * {@link OAuthProtectedResourceMetadataHandler}, and writes either a JSON response or a
 * {@code 404 Not Found} response.
 */
public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Handles a metadata discovery request for a protected resource path beneath the servlet
     * context.
     *
     * @param request the HTTP request targeting a protected resource metadata endpoint
     * @param response the HTTP response to populate
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OAuthProtectedResourceMetadataHandler.MetadataResponse metadataResponse = createHandler(request).handle(request.getPathInfo());

        if (!metadataResponse.isFound()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(metadataResponse.getContentType());
        response.setCharacterEncoding(metadataResponse.getCharacterEncoding());
        response.getWriter().write(metadataResponse.getBody());
    }

    /**
     * Creates the request-scoped handler used to translate the path info and resolve metadata.
     *
     * @param request the current HTTP request
     * @return a handler bound to the current request
     */
    protected OAuthProtectedResourceMetadataHandler createHandler(HttpServletRequest request) {
        return new OAuthProtectedResourceMetadataHandler() {
            @Override
            protected String resolveMetadataJson(String protectedResourcePath) {
                return OAuthProtectedResourceMetadataServlet.this.resolveMetadataJson(request, protectedResourcePath);
            }
        };
    }

    /**
     * Resolves the metadata JSON for the supplied protected resource path.
     * <p>
     * Subclasses override this method to bridge from the servlet layer to the runtime OIDC
     * client configuration and metadata generation logic.
     *
     * @param request the current HTTP request
     * @param protectedResourcePath the normalized protected resource path, beginning with {@code /}
     * @return the metadata JSON to return, or {@code null} if the path has no metadata
     */
    protected String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath) {
        return null;
    }
}

// Made with Bob
