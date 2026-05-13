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

public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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

    protected OAuthProtectedResourceMetadataHandler createHandler(HttpServletRequest request) {
        return new OAuthProtectedResourceMetadataHandler() {
            @Override
            protected String resolveMetadataJson(String protectedResourcePath) {
                return OAuthProtectedResourceMetadataServlet.this.resolveMetadataJson(request, protectedResourcePath);
            }
        };
    }

    protected String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath) {
        return null;
    }
}

// Made with Bob
