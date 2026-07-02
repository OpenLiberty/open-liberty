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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.openidconnect.client.internal.OAuthProtectedResourceMetadataResolver;

/**
 * Declarative Services component that wires the DS-managed
 * {@link OAuthProtectedResourceMetadataResolver} into
 * {@link OAuthProtectedResourceMetadataServlet} via a static reference.
 * <p>
 * The servlet is registered by the web container from {@code web.xml}. Because the web
 * container creates the servlet instance independently of DS, the resolver is bridged
 * through a static field — the same pattern used by {@code OidcRedirectServlet}.
 * </p>
 */
@Component(name = "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletService",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = {},
           property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataServletService {

    @Reference
    protected void setResolver(OAuthProtectedResourceMetadataResolver resolver) {
        OAuthProtectedResourceMetadataServlet.setResolver(resolver);
    }

    protected void unsetResolver(OAuthProtectedResourceMetadataResolver resolver) {
        OAuthProtectedResourceMetadataServlet.setResolver(null);
    }

    @Deactivate
    protected void deactivate() {
        OAuthProtectedResourceMetadataServlet.setResolver(null);
    }
}
