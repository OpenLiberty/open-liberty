/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.openidconnect.clients.common.OAuthProtectedResourceMetadataResolverBase;
import com.ibm.ws.security.openidconnect.clients.common.OAuthProtectedResourceMetadataService;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;

/**
 * Gets the OAuth 2.0 protected resource metadata for a given protected resource path.
 * <p>
 * This component owns the runtime and configuration integration for the protected resource
 * metadata endpoint. The well-known web bundle delegates request handling to this service
 * so that the endpoint remains part of the {@code openidConnectClient} feature while still
 * using a dedicated web context path.
 * </p>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = OAuthProtectedResourceMetadataService.class)
public class OAuthProtectedResourceMetadataResolver extends OAuthProtectedResourceMetadataResolverBase<OidcClientConfig> implements OAuthProtectedResourceMetadataService {

    @Reference
    private OidcClientImpl oidcClientImpl;

    /**
     * Test hook: allows unit tests to inject a mock {@link OidcClientImpl} without DS.
     */
    void setOidcClient(OidcClientImpl client) {
        this.oidcClientImpl = client;
    }

    @Override
    public String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl) {
        OidcClientImpl client = oidcClientImpl;
        if (client == null) {
            return null;
        }

        // Adapt the metadata request to look like a direct request to the protected resource
        // so that the standard OIDC provider-selection flow can evaluate auth filters against
        // the configured URL patterns.
        HttpServletRequest resourceRequest = new ProtectedResourceRequestWrapper(request, protectedResourcePath);

        String providerId = client.getOidcProvider(resourceRequest);

        if (providerId == null) {
            return null;
        }

        OidcClientConfig config = client.getOidcClientConfig(request, providerId); // OidcClientImpl

        if (config == null) {
            return null;
        }

        if (!config.getServeProtectedResourceMetadata()) {
            return null;
        }

        return createMetadataJson(config, absoluteResourceUrl);
    }

    /**
     * Returns the authorization server identifier from the OIDC client configuration.
     * Prefers the issuer identifier.
     *
     * @param config
     *            matching OIDC client configuration
     * @return authorization server URL, or {@code null} if none is configured
     */
    @Override
    protected String getAuthorizationServer(OidcClientConfig config) {
        String issuer = config.getIssuerIdentifier();
        if (issuer != null && !issuer.trim().isEmpty()) {
            return issuer;
        }
        String validationEndpoint = config.getValidationEndpointUrl();
        if (validationEndpoint != null) {
            int lastSlashIndex = validationEndpoint.lastIndexOf("/");
            return validationEndpoint.substring(0, lastSlashIndex);
        }
        return null;
    }

    @Override
    protected String getConfigId(OidcClientConfig config) {
        return config.getId();
    }

    @Override
    protected String getJwtBuilderId(OidcClientConfig config) {
        return config.getProtectedResourceMetadataJwtBuilderId();
    }

    @Override
    protected List<String> getAdvertisedScopes(OidcClientConfig config) {
        return config.getProtectedResourceMetadataAdvertisedScopes();
    }
}
