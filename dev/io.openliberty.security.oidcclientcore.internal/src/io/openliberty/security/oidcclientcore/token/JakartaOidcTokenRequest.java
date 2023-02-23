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
package io.openliberty.security.oidcclientcore.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class JakartaOidcTokenRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcTokenRequest.class);

    public static final String AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE = "TOKEN_RESPONSE";

    private final OidcClientConfig oidcClientConfig;
    private final HttpServletRequest request;

    public JakartaOidcTokenRequest(OidcClientConfig oidcClientConfig, HttpServletRequest request) {
        this.oidcClientConfig = oidcClientConfig;
        this.request = request;
    }

    public ProviderAuthenticationResult sendRequest() throws TokenRequestException {
        TokenResponse tokenEndpointResponse = sendTokenRequest();
        verifyTokenResponseContainsRequiredParameters(tokenEndpointResponse);
        return createAuthenticationResultFromTokenResponse(tokenEndpointResponse);
    }

    public ProviderAuthenticationResult sendTokenRefreshRequest(String refreshToken) throws TokenRequestException {
        TokenResponse tokenEndpointResponse = sendTokenRequestForRefresh(refreshToken);
        return createAuthenticationResultFromTokenResponse(tokenEndpointResponse);
    }

    TokenResponse sendTokenRequest() throws TokenRequestException {
        String tokenEndpoint = getTokenEndpoint();
        String authzCode = request.getParameter(TokenConstants.CODE);
        return sendTokenRequestForCode(tokenEndpoint, authzCode);
    }

    TokenResponse sendTokenRequestForRefresh(String refreshToken) throws TokenRequestException {
        String tokenEndpoint = getTokenEndpoint();
        Builder tokenRequestBuilder = createTokenRequestorBuilderForGeneralRequest(tokenEndpoint, null);
        tokenRequestBuilder.grantType(TokenConstants.REFRESH_TOKEN);
        tokenRequestBuilder.refreshToken(refreshToken);
        //TODO: check do we need to include scope parameter?
        TokenRequestor tokenRequestor = tokenRequestBuilder.build();
        try {
            return tokenRequestor.requestTokens();
        } catch (Exception e) {
            throw new TokenRequestException(oidcClientConfig.getClientId(), e.toString(), e);
        }
    }

    @FFDCIgnore(OidcClientConfigurationException.class)
    String getTokenEndpoint() throws TokenRequestException {
        String tokenEndpoint = null;
        try {
            tokenEndpoint = MetadataUtils.getTokenEndpoint(oidcClientConfig);
        } catch (OidcDiscoveryException | OidcClientConfigurationException e) {
            throw new TokenRequestException(oidcClientConfig.getClientId(), e.getMessage());
        }
        return tokenEndpoint;
    }

    TokenResponse sendTokenRequestForCode(String tokenEndpoint, String authzCode) throws TokenRequestException {
        Builder tokenRequestBuilder = createTokenRequestorBuilderForGeneralRequest(tokenEndpoint, authzCode);
        tokenRequestBuilder.grantType(TokenConstants.AUTHORIZATION_CODE);
        TokenRequestor tokenRequestor = tokenRequestBuilder.build();
        try {
            return tokenRequestor.requestTokens();
        } catch (Exception e) {
            throw new TokenRequestException(oidcClientConfig.getClientId(), e.toString(), e);
        }
    }

    Builder createTokenRequestorBuilderForGeneralRequest(String tokenEndpoint, String authzCode) throws TokenRequestException {
        String clientId = oidcClientConfig.getClientId();
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = oidcClientConfig.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }

        Builder tokenRequestBuilder = createTokenRequestorBuilder(tokenEndpoint, clientId, clientSecret, authzCode);
        tokenRequestBuilder.sslSocketFactory(OidcMetadataService.getSSLSocketFactory());
        return tokenRequestBuilder;
    }

    Builder createTokenRequestorBuilder(String tokenEndpoint, String clientId, @Sensitive String clientSecret, String authzCode) {
        return new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, oidcClientConfig.getRedirectURI(), authzCode);
    }

    /**
     * Verify the token response parameters in accordance with RFC 6749 Section 5.1.
     */
    void verifyTokenResponseContainsRequiredParameters(TokenResponse tokenEndpointResponse) throws TokenRequestException {
        Map<String, String> responseMap = tokenEndpointResponse.asMap();
        List<String> missingParameters = new ArrayList<>();
        List<String> requiredParameters = Arrays.asList(TokenConstants.ACCESS_TOKEN, TokenConstants.TOKEN_TYPE, TokenConstants.ID_TOKEN);
        for (String requiredParameter : requiredParameters) {
            if (!responseMap.containsKey(requiredParameter)) {
                missingParameters.add(requiredParameter);
            }
        }
        if (!missingParameters.isEmpty()) {
            String nlsMessage = Tr.formatMessage(tc, "TOKEN_RESPONSE_MISSING_PARAMETER", missingParameters);
            throw new TokenRequestException(oidcClientConfig.getClientId(), nlsMessage);
        }
    }

    ProviderAuthenticationResult createAuthenticationResultFromTokenResponse(TokenResponse tokenEndpointResponse) {
        Hashtable<String, Object> customProperties = new Hashtable<>();
        customProperties.put(AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE, tokenEndpointResponse);

        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, new Subject(), customProperties, null);
    }

}
