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

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.security.openidconnect.client.OAuthProtectedResourceMetadataService;
import io.openliberty.security.oidcclient.wellknown.common.ServletUtils;

/**
 * Servlet entry point for OAuth 2.0 Protected Resource Metadata requests under the
 * {@code /.well-known/oauth-protected-resource} context path.
 * <p>
 * Resolves the protected resource path from the request, looks up metadata via the
 * OSGi {@link OAuthProtectedResourceMetadataResolver} service, and returns either a
 * JSON response or a {@code 404} when no metadata is available.
 * </p>
 */
public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final ServiceCaller<OAuthProtectedResourceMetadataService> resolverCaller =
            new ServiceCaller<>(OAuthProtectedResourceMetadataServlet.class, OAuthProtectedResourceMetadataService.class);

    /**
     * Handles a metadata discovery request for a protected resource path beneath the servlet
     * context.
     *
     * @param request  the HTTP request targeting a protected resource metadata endpoint
     * @param response the HTTP response to populate
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!ProductInfo.getBetaEdition()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        /*
         * getPathInfo() strips the servlet context root (/.well-known/oauth-protected-resource),
         * leaving just the path to the protected resource, e.g. for a request to
         * /.well-known/oauth-protected-resource/myApp/protected it returns /myApp/protected.
         */
        String protectedResourcePath = toProtectedResourcePath(request.getPathInfo());
        String resourceUrl = ServletUtils.buildResourceUrl(request.getScheme(), request.getServerName(), request.getServerPort(), protectedResourcePath);

        String metadataJson = resolverCaller.run(r -> r.resolveMetadataJson(request, protectedResourcePath, resourceUrl))
                                            .orElse(null);

        if (metadataJson == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(metadataJson);
    }

    /**
     * Converts servlet path info into a normalized protected resource path.
     *
     * @param pathInfo servlet path info from the metadata endpoint request
     * @return normalized protected resource path; empty, {@code null}, or {@code /} values are
     *         normalized to {@code /}; values without a leading slash are prefixed with {@code /}
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
