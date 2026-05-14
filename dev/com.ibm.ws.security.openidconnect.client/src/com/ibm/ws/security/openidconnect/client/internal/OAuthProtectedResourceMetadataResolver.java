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

import java.util.Iterator;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

/**
 * Resolves OAuth 2.0 protected resource metadata for configured OIDC clients.
 * <p>
 * This component owns the runtime/config integration for the protected resource metadata endpoint.
 * The well-known web bundle delegates request handling to this service so the endpoint remains part
 * of the {@code oidcConnectClient} feature while still using a dedicated web context path.
 * </p>
 */
@Component(name = "com.ibm.ws.security.openidconnect.client.internal.OAuthProtectedResourceMetadataResolver", service = OAuthProtectedResourceMetadataResolver.class, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataResolver {

    private static final TraceComponent tc = Tr.register(OAuthProtectedResourceMetadataResolver.class);

    private static final ConcurrentServiceReferenceSet<OidcClientConfig> oidcClientConfigRef = new ConcurrentServiceReferenceSet<OidcClientConfig>("oidcClientConfigService");

    @Reference(name = "oidcClientConfigService", service = OidcClientConfig.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setOidcClientConfigService(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.addReference(reference);
    }

    protected void unsetOidcClientConfigService(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.removeReference(reference);
    }

    /**
     * Resolves metadata JSON for the supplied protected resource path.
     *
     * @param protectedResourcePath normalized protected resource path beginning with {@code /}
     * @return metadata JSON, or {@code null} if no matching protected resource exists
     */
    public String resolveMetadataJson(String protectedResourcePath) {
        OidcClientConfig matchingConfig = getMatchingConfig(protectedResourcePath);
        if (matchingConfig == null) {
            return null;
        }
        return createMetadataJson(matchingConfig, protectedResourcePath);
    }

    OidcClientConfig getMatchingConfig(String protectedResourcePath) {
        Iterator<ServiceAndServiceReferencePair<OidcClientConfig>> servicesWithRefs = oidcClientConfigRef.getServicesWithReferences();
        while (servicesWithRefs.hasNext()) {
            ServiceAndServiceReferencePair<OidcClientConfig> configServiceAndRef = servicesWithRefs.next();
            OidcClientConfig config = configServiceAndRef.getService();
            if (matches(config, protectedResourcePath)) {
                return config;
            }
        }
        return null;
    }

    boolean matches(OidcClientConfig config, String protectedResourcePath) {
        String[] configuredResources = config.getResources();
        if (configuredResources != null) {
            for (String resource : configuredResources) {
                if (matchesResource(resource, protectedResourcePath)) {
                    return true;
                }
            }
        }

        String contextPath = config.getContextPath();
        return matchesResource(contextPath, protectedResourcePath);
    }

    boolean matchesResource(String configuredPath, String protectedResourcePath) {
        if (configuredPath == null || configuredPath.isEmpty()) {
            return false;
        }
        return normalizePath(configuredPath).equals(protectedResourcePath);
    }

    String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    String createMetadataJson(OidcClientConfig config, String protectedResourcePath) {
        JSONObject metadata = new JSONObject();
        metadata.put("resource", protectedResourcePath);

        JSONArray authorizationServers = new JSONArray();
        String issuer = getAuthorizationServer(config);
        if (issuer != null && !issuer.isEmpty()) {
            authorizationServers.add(issuer);
        }
        metadata.put("authorization_servers", authorizationServers);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Resolved OAuth protected resource metadata for path [" + protectedResourcePath + "] using client [" + config.getId() + "]");
        }

        return metadata.toString();
    }

    String getAuthorizationServer(OidcClientConfig config) {
        String issuer = config.getIssuerIdentifier();
        if (issuer != null && !issuer.isEmpty()) {
            return issuer;
        }

        String discoveryEndpointUrl = config.getDiscoveryEndpointUrl();
        if (discoveryEndpointUrl != null && !discoveryEndpointUrl.isEmpty()) {
            return discoveryEndpointUrl;
        }

        return config.getAuthorizationEndpointUrl();
    }
}

// Made with Bob
