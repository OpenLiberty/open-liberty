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

import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;

@Component(service = MetadataUtils.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MetadataUtils {

    public static final TraceComponent tc = Tr.register(MetadataUtils.class);

    private static volatile OidcMetadataService oidcMetadataService;

    @Reference(name = OidcMetadataService.KEY_METADATA_SERVICE, policy = ReferencePolicy.DYNAMIC)
    public void setOidcMetadataService(OidcMetadataService oidcMetadataServiceRef) {
        oidcMetadataService = oidcMetadataServiceRef;
    }

    public void unsetOidcMetadataService(OidcMetadataService oidcMetadataServiceRef) {
        oidcMetadataService = null;
    }

    /**
     * Returns a parameterized value from the configured OidcProviderMetadata, or from the OP's discovery document if the value
     * cannot be found in the OidcProviderMetadata.
     *
     * @throws OidcDiscoveryException Thrown if the value cannot be found in the discovery document or if its value is empty.
     * @throws OidcClientConfigurationException Thrown if the client configuration is missing the providerURI.
     */
    public static <T> T getValueFromProviderOrDiscoveryMetadata(OidcClientConfig oidcClientConfig, Function<OidcProviderMetadata, T> metadataMethodToCall,
                                                                String discoveryMetadataKey) throws OidcDiscoveryException, OidcClientConfigurationException {
        T value = getValueFromProviderMetadata(oidcClientConfig, metadataMethodToCall, discoveryMetadataKey);
        if (value != null) {
            return value;
        }
        return getValueFromDiscoveryMetadata(oidcClientConfig, discoveryMetadataKey);
    }

    /**
     * Returns a parameterized value from the configured OidcProviderMetadata, if present.
     */
    public static <T> T getValueFromProviderMetadata(OidcClientConfig oidcClientConfig, Function<OidcProviderMetadata, T> metadataMethodToCall, String discoveryMetadataKey) {
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
        return null;
    }

    /**
     * Provide the Jakarta JsonObject from the Discovery endpoint.
     *
     * @param oidcClientConfig
     * @return
     * @throws OidcClientConfigurationException
     * @throws OidcDiscoveryException
     */
    public static JSONObject getProviderDiscoveryMetaData(OidcClientConfig oidcClientConfig) throws OidcClientConfigurationException, OidcDiscoveryException {
        return oidcMetadataService.getProviderDiscoveryMetadata(oidcClientConfig);

    }

    /**
     * Returns a parameterized value from the OP's discovery document.
     *
     * @throws OidcDiscoveryException Thrown if the value cannot be found in the discovery document or if its value is empty.
     * @throws OidcClientConfigurationException Thrown if the client configuration is missing the providerURI.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueFromDiscoveryMetadata(OidcClientConfig oidcClientConfig, String key) throws OidcDiscoveryException, OidcClientConfigurationException {
        T value = null;
        JSONObject providerDiscoveryMetadata = oidcMetadataService.getProviderDiscoveryMetadata(oidcClientConfig);
        if (providerDiscoveryMetadata != null) {
            value = (T) providerDiscoveryMetadata.get(key);
        }
        if (value == null || value.toString().isEmpty()) {
            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", key);
            throw new OidcDiscoveryException(oidcClientConfig.getClientId(), oidcClientConfig.getProviderURI(), nlsMessage);
        }
        return value;
    }

    public static String[] getStringArrayValueFromProviderOrDiscoveryMetadata(OidcClientConfig oidcClientConfig, Function<OidcProviderMetadata, String[]> metadataMethodToCall,
                                                                              String discoveryMetadataKey) throws OidcDiscoveryException, OidcClientConfigurationException {
        if (metadataMethodToCall != null) {
            String[] value = getValueFromProviderMetadata(oidcClientConfig, metadataMethodToCall, discoveryMetadataKey);
            if (value != null && value.length > 0) {
                return value;
            }
        }
        return getAndConvertJsonArrayFromDiscoveryData(oidcClientConfig, discoveryMetadataKey);
    }

    static String[] getAndConvertJsonArrayFromDiscoveryData(OidcClientConfig oidcClientConfig,
                                                            String discoveryMetadataKey) throws OidcDiscoveryException, OidcClientConfigurationException {
        JSONArray valueFromDiscovery = getValueFromDiscoveryMetadata(oidcClientConfig, discoveryMetadataKey);
        String[] values = new String[valueFromDiscovery.size()];
        for (int i = 0; i < valueFromDiscovery.size(); i++) {
            values[i] = (String) valueFromDiscovery.get(i);
        }
        return values;
    }

    public static String getAuthorizationEndpoint(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getAuthorizationEndpoint(),
                                                       OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT);
    }

    public static String getTokenEndpoint(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getTokenEndpoint(),
                                                       OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
    }

    public static String getUserInfoEndpoint(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getUserinfoEndpoint(),
                                                       OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
    }

    public static String getJwksUri(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getJwksURI(),
                                                       OidcDiscoveryConstants.METADATA_KEY_JWKS_URI);
    }

    public static String getEndSessionEndpoint(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getEndSessionEndpoint(),
                                                       OidcDiscoveryConstants.METADATA_KEY_ENDSESSION_ENDPOINT);
    }

    public static String getIssuer(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                       metadata -> metadata.getIssuer(),
                                                       OidcDiscoveryConstants.METADATA_KEY_ISSUER);
    }

    public static String[] getIdTokenSigningAlgorithmsSupported(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getStringArrayValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                                  metadata -> metadata.getIdTokenSigningAlgorithmsSupported(),
                                                                  OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public static String[] getUserInfoSigningAlgorithmsSupported(OidcClientConfig oidcClientConfig) throws OidcDiscoveryException, OidcClientConfigurationException {
        return getStringArrayValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                                  null,
                                                                  OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
    }

}
