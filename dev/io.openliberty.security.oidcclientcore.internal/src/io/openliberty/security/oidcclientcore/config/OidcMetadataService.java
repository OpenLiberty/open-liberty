/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.discovery.DiscoveryHandler;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;

@Component(service = OidcMetadataService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class OidcMetadataService {

    public static final TraceComponent tc = Tr.register(OidcMetadataService.class);

    public static final String KEY_METADATA_SERVICE = "oidcMetadataService";

    private static final String KEY_SSL_SUPPORT = "sslSupport";
    protected static volatile SSLSupport sslSupport;

    @Reference(name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC)
    public void setSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = sslSupportSvc;
    }

    public void unsetSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = null;
    }

    public static SSLSupport getSSLSupport() {
        return sslSupport;
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        if (sslSupport != null) {
            return sslSupport.getSSLSocketFactory();
        }
        return null;
    }

    public DiscoveryHandler getDiscoveryHandler() {
        SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        return new DiscoveryHandler(sslSocketFactory);
    }

    public JSONObject getProviderDiscoveryMetadata(OidcClientConfig oidcClientConfig) throws OidcClientConfigurationException, OidcDiscoveryException {
        JSONObject discoveryData = null;
        String discoveryUri = oidcClientConfig.getProviderURI();
        if (discoveryUri == null || discoveryUri.isEmpty()) {
            String clientId = oidcClientConfig.getClientId();
            String nlsMessage = Tr.formatMessage(tc, "OIDC_CLIENT_MISSING_PROVIDER_URI", clientId);
            throw new OidcClientConfigurationException(clientId, nlsMessage);
        }
        discoveryUri = addWellKnownSuffixIfNeeded(discoveryUri);
        discoveryData = fetchProviderMetadataFromDiscoveryUrl(discoveryUri, oidcClientConfig.getClientId());
        return discoveryData;
    }

    /**
     * Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#metadata-configuration, the providerURI
     * "defines the base URL of the OpenID Connect Provider where the /.well-known/openid-configuration is appended to (or used
     * as-is when it is the well known configuration URL itself)."
     */
    String addWellKnownSuffixIfNeeded(String providerUri) {
        if (!providerUri.endsWith(OidcDiscoveryConstants.WELL_KNOWN_SUFFIX)) {
            if (!providerUri.endsWith("/")) {
                providerUri += "/";
            }
            providerUri += OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        }
        return providerUri;
    }

    JSONObject fetchProviderMetadataFromDiscoveryUrl(String discoveryUri, String clientId) throws OidcDiscoveryException {
        DiscoveryHandler discoveryHandler = getDiscoveryHandler();
        return discoveryHandler.fetchDiscoveryDataJson(discoveryUri, clientId);
    }

}
