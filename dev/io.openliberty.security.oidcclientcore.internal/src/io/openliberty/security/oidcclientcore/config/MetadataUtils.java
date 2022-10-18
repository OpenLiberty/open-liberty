/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import java.util.function.Function;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;

public class MetadataUtils {

    public static final TraceComponent tc = Tr.register(MetadataUtils.class);

    /**
     * Returns a parameterized value from the configured OidcProviderMetadata, or from the OP's discovery document if the value
     * cannot be found in the OidcProviderMetadata.
     */
    public static <T> T getValueFromProviderOrDiscoveryMetadata(EndpointRequest endpointRequestClass, OidcClientConfig oidcClientConfig,
                                                                Function<OidcProviderMetadata, T> metadataMethodToCall,
                                                                String discoveryMetadataKey) throws OidcDiscoveryException {
        OidcProviderMetadata providerMetadata = oidcClientConfig.getProviderMetadata();
        if (providerMetadata != null) {
            T value = metadataMethodToCall.apply(providerMetadata);
            if (value != null && !value.toString().isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, discoveryMetadataKey + " found in the provider metadata: [" + value + "]");
                }
                return value;
            }
        }
        return getValueFromDiscoveryMetadata(endpointRequestClass, oidcClientConfig, discoveryMetadataKey);
    }

    /**
     * Returns a parameterized value from the OP's discovery document.
     *
     * @throws OidcDiscoveryException Thrown if the value cannot be found in the discovery document or if its value is empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueFromDiscoveryMetadata(EndpointRequest endpointRequestClass, OidcClientConfig oidcClientConfig, String key) throws OidcDiscoveryException {
        T value = null;
        JSONObject providerDiscoveryMetadata = endpointRequestClass.getProviderDiscoveryMetadata(oidcClientConfig);
        if (providerDiscoveryMetadata != null) {
            value = (T) providerDiscoveryMetadata.get(key);
        }
        if (value == null || value.toString().isEmpty()) {
            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", key);
            throw new OidcDiscoveryException(oidcClientConfig.getClientId(), oidcClientConfig.getProviderURI(), nlsMessage);
        }
        return value;
    }

    public static String getUserInfoEndpoint(EndpointRequest endpointRequestClass, OidcClientConfig oidcClientConfig) throws OidcDiscoveryException {
        return getValueFromProviderOrDiscoveryMetadata(endpointRequestClass,
                                                       oidcClientConfig,
                                                       metadata -> metadata.getUserinfoEndpoint(),
                                                       OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
    }

    public static String getJwksUri(EndpointRequest endpointRequestClass, OidcClientConfig oidcClientConfig) throws OidcDiscoveryException {
        return getValueFromProviderOrDiscoveryMetadata(endpointRequestClass,
                                                       oidcClientConfig,
                                                       metadata -> metadata.getJwksURI(),
                                                       OidcDiscoveryConstants.METADATA_KEY_JWKS_URI);
    }

}
