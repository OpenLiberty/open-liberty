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

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.openidconnect.client.internal.OAuthProtectedResourceMetadataResolver;

/**
 * Declarative Services component that registers the Jakarta protected resource metadata
 * servlet with the web container.
 * <p>
 * This class is the runtime integration point for the servlet bundle. It delegates protected
 * resource metadata lookup to
 * {@link com.ibm.ws.security.openidconnect.client.internal.OAuthProtectedResourceMetadataResolver}
 * so the web layer remains thin and the OIDC runtime owns config matching and metadata generation.
 * </p>
 */
@Component(name = "io.openliberty.security.oidcclient.wellknown.internal.jakarta.OAuthProtectedResourceMetadataServletService", service = {}, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataServletService extends OAuthProtectedResourceMetadataServlet {

    private static final long serialVersionUID = 1L;

    private volatile OAuthProtectedResourceMetadataResolver metadataResolver;

    @Reference
    protected void setMetadataResolver(OAuthProtectedResourceMetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    protected void unsetMetadataResolver(OAuthProtectedResourceMetadataResolver metadataResolver) {
        if (this.metadataResolver == metadataResolver) {
            this.metadataResolver = null;
        }
    }

    /**
     * Resolves metadata for the requested protected resource path.
     *
     * @param request the current HTTP request
     * @param protectedResourcePath the normalized protected resource path
     * @return the metadata JSON to return, or {@code null} when the path is unknown
     */
	@Override
	protected String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath) {
		OAuthProtectedResourceMetadataResolver resolver = metadataResolver;

		if (resolver == null) {
			return null;
		}

		String resourceUrl = buildResourceUrl(request, protectedResourcePath);
		return resolver.resolveMetadataJson(protectedResourcePath, resourceUrl);
	}
}

// Made with Bob
