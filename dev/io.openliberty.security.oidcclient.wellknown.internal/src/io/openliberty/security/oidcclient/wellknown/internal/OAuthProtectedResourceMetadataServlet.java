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

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet entry point for OAuth 2.0 Protected Resource Metadata requests under the
 * {@code /.well-known/oauth-protected-resource} context path.
 * <p>
 * It extracts the request path, delegates request-specific
 * path handling to {@link OAuthProtectedResourceMetadataHandler}, and writes either a JSON
 * response or a {@code 404} when no metadata is available for the requested protected resource.
 * </p>
 */
public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Handles metadata discovery requests for protected resources hosted on this Liberty server.
     *
     * @param request current HTTP request
     * @param response current HTTP response
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
     * Creates a per-request handler that bridges servlet request context into the metadata
     * resolution hook.
     *
     * @param request current HTTP request
     * @return handler for the current request
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
     * Resolves protected resource metadata for the requested protected resource path.
     * <p>
     * Subclasses supply the actual metadata lookup logic. Returning {@code null} indicates that
     * no metadata should be served for the requested protected resource, which results in a
     * {@code 404} response.
     * </p>
     *
     * @param request current HTTP request
     * @param protectedResourcePath normalized protected resource path, such as {@code /mcp}
     * @return metadata JSON to return to the client, or {@code null} if the protected resource
     *         is unknown or not enabled for metadata serving
     */
    protected String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath) {
        return null;
    }
}

// Made with Bob
