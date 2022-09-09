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
package io.openliberty.security.oidcclientcore.token;

import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class JakartaOidcTokenRequest extends EndpointRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcTokenRequest.class);

    public static final String AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE = "TOKEN_RESPONSE";

    private final OidcClientConfig oidcClientConfig;
    private final OidcProviderMetadata providerMetadata;
    private final HttpServletRequest request;

    public JakartaOidcTokenRequest(OidcClientConfig oidcClientConfig, HttpServletRequest request) {
        this.oidcClientConfig = oidcClientConfig;
        this.providerMetadata = (oidcClientConfig == null) ? null : oidcClientConfig.getProviderMetadata();
        this.request = request;
    }

    public ProviderAuthenticationResult sendRequest() throws TokenRequestException {
        TokenResponse tokenEndpointResponse = sendTokenRequest();
        return createAuthenticationResultFromTokenResponse(tokenEndpointResponse);
    }

    @FFDCIgnore(OidcDiscoveryException.class)
    TokenResponse sendTokenRequest() throws TokenRequestException {
        String tokenEndpoint = null;
        try {
            tokenEndpoint = getTokenEndpoint();
        } catch (OidcDiscoveryException e) {
            throw new TokenRequestException(oidcClientConfig.getClientId(), e.getMessage());
        }
        if (tokenEndpoint == null || tokenEndpoint.isEmpty()) {
            String clientId = oidcClientConfig.getClientId();
            String message = Tr.formatMessage(tc, "TOKEN_ENDPOINT_MISSING", clientId);
            throw new TokenRequestException(clientId, message);
        }
        String authzCode = request.getParameter(TokenConstants.CODE);
        return sendTokenRequestForCode(tokenEndpoint, authzCode);
    }

    String getTokenEndpoint() throws OidcDiscoveryException {
        String tokenEndpoint = getTokenEndpointFromProviderMetadata();
        if (tokenEndpoint != null) {
            return tokenEndpoint;
        }
        return getTokenEndpointFromDiscoveryMetadata();
    }

    String getTokenEndpointFromProviderMetadata() {
        if (providerMetadata != null) {
            // Provider metadata overrides properties discovered via providerUri
            String tokenEndpoint = providerMetadata.getTokenEndpoint();
            if (tokenEndpoint != null && !tokenEndpoint.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Token endpoint found in the provider metadata: [" + tokenEndpoint + "]");
                }
                return tokenEndpoint;
            }
        }
        return null;
    }

    String getTokenEndpointFromDiscoveryMetadata() throws OidcDiscoveryException {
        String tokenEndpoint = null;
        JSONObject discoveryData = getProviderDiscoveryMetadata(oidcClientConfig);
        if (discoveryData != null) {
            tokenEndpoint = (String) discoveryData.get(OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
        }
        if (tokenEndpoint == null) {
            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
            throw new OidcDiscoveryException(oidcClientConfig.getClientId(), oidcClientConfig.getProviderURI(), nlsMessage);
        }
        return tokenEndpoint;
    }

    TokenResponse sendTokenRequestForCode(String tokenEndpoint, String authzCode) throws TokenRequestException {
        String clientId = oidcClientConfig.getClientId();
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = oidcClientConfig.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }

        Builder tokenRequestBuilder = createTokenRequestorBuilder(tokenEndpoint, clientId, clientSecret, authzCode);
        tokenRequestBuilder.sslSocketFactory(getSSLSocketFactory());
        tokenRequestBuilder.grantType(TokenConstants.AUTHORIZATION_CODE);
        TokenRequestor tokenRequestor = tokenRequestBuilder.build();
        try {
            return tokenRequestor.requestTokens();
        } catch (Exception e) {
            throw new TokenRequestException(clientId, e.toString(), e);
        }
    }

    Builder createTokenRequestorBuilder(String tokenEndpoint, String clientId, @Sensitive String clientSecret, String authzCode) {
        return new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, oidcClientConfig.getRedirectURI(), authzCode);
    }

    ProviderAuthenticationResult createAuthenticationResultFromTokenResponse(TokenResponse tokenEndpointResponse) {
        Hashtable<String, Object> customProperties = new Hashtable<>();
        customProperties.put(AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE, tokenEndpointResponse);

        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, new Subject(), customProperties, null);
    }

}
