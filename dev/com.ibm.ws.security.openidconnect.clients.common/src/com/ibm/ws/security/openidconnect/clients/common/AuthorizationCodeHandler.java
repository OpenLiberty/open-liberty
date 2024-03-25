/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.common.ssl.NoSSLSocketFactoryException;
import com.ibm.ws.security.common.structures.BoundedHashMap;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.pkce.ProofKeyForCodeExchangeHelper;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException;
import io.openliberty.security.oidcclientcore.http.BadPostRequestException;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import io.openliberty.security.oidcclientcore.token.TokenRequestor;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import io.openliberty.security.oidcclientcore.token.auth.PrivateKeyJwtAuthMethod;
import io.openliberty.security.oidcclientcore.token.auth.TokenEndpointAuthMethod;

public class AuthorizationCodeHandler {
    private static final TraceComponent tc = Tr.register(AuthorizationCodeHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ConvergedClientConfig clientConfig;
    private final String configId;
    private final String clientId;

    private OidcClientUtil oidcClientUtil = null;
    private OIDCClientAuthenticatorUtil authenticatorUtil = null;
    private SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;
    private static Map<String, Object> usedAuthCodes = Collections.synchronizedMap(new BoundedHashMap(20));

    public AuthorizationCodeHandler(HttpServletRequest request, HttpServletResponse response, ConvergedClientConfig clientConfig, SSLSupport sslsupt) {
        this.request = request;
        this.response = response;
        this.clientConfig = clientConfig;
        clientId = clientConfig.getClientId();
        configId = clientConfig.getId();

        oidcClientUtil = getOidcClientUtil();
        authenticatorUtil = getOIDCClientAuthenticatorUtil();
        sslSupport = sslsupt;
        jose4jUtil = getJose4jUtil(sslSupport);
    }

    protected OidcClientUtil getOidcClientUtil() {
        return new OidcClientUtil();
    }

    protected OIDCClientAuthenticatorUtil getOIDCClientAuthenticatorUtil() {
        return new OIDCClientAuthenticatorUtil();
    }

    protected Jose4jUtil getJose4jUtil(SSLSupport sslSupport) {
        return new Jose4jUtil(sslSupport);
    }

    /**
     * This method handle the authorization code; it's validated the response state and call the server to get the tokens using
     * the authorization code.
     */
    public ProviderAuthenticationResult handleAuthorizationCode(String authzCode, String responseState) {
        OidcClientRequest oidcClientRequest = (OidcClientRequest) request.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST);
        ProviderAuthenticationResult oidcResult = null;
        oidcResult = authenticatorUtil.verifyResponseState(request, response, responseState, clientConfig);
        if (oidcResult != null) {
            return oidcResult; // only if something bad happened, otherwise proceed to exchange auth code for tokens.
        }
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, clientConfig.getTokenEndpointUrl())) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", clientConfig.getTokenEndpointUrl());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        String redirectUrl = OIDCClientAuthenticatorUtil.setRedirectUrlIfNotDefined(request, clientConfig);
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, redirectUrl)) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", redirectUrl);
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return getAndValidateTokens(oidcClientRequest, authzCode, responseState, redirectUrl);
    }

    ProviderAuthenticationResult getAndValidateTokens(OidcClientRequest oidcClientRequest, String authzCode, String responseState, String redirectUrl) {
        ProviderAuthenticationResult oidcResult = null;
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = getSSLSocketFactory();
        } catch (Exception e) {
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        try {
            oidcResult = sendTokenRequestAndValidateResult(oidcClientRequest, sslSocketFactory, authzCode, responseState, redirectUrl);
        } catch (BadPostRequestException e) {
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getMessage(), configId, clientConfig.getTokenEndpointUrl() });
            sendErrorJSON(e.getStatusCode(), "invalid_request", e.getMessage());
            return new ProviderAuthenticationResult(AuthResult.FAILURE, e.getStatusCode());
        } catch (Exception e) {
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getLocalizedMessage(), configId, clientConfig.getTokenEndpointUrl() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return oidcResult;
    }

    SSLSocketFactory getSSLSocketFactory() throws SSLException, NoSSLSocketFactoryException {
        SSLSocketFactory sslSocketFactory = null;
        boolean throwExc = clientConfig.getTokenEndpointUrl() != null && clientConfig.getTokenEndpointUrl().startsWith("https");
        try {
            sslSocketFactory = new OidcClientHttpUtil().getSSLSocketFactory(clientConfig.getSSLConfigurationName(), sslSupport);
        } catch (SSLException e) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e, configId });
            throw e;
        } catch (NoSSLSocketFactoryException e) {
            String nlsMessage = Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", configId });
            Tr.error(tc, nlsMessage);
            if (throwExc) {
                throw new SSLException(nlsMessage);
            }
            throw e;
        }
        return sslSocketFactory;
    }

    ProviderAuthenticationResult sendTokenRequestAndValidateResult(OidcClientRequest oidcClientRequest, SSLSocketFactory sslSocketFactory, String authzCode, String responseState, String redirectUrl) throws MalformedURLException, Exception {
        String url = clientConfig.getTokenEndpointUrl();
        if (url == null || url.length() == 0) {
            String message = Tr.formatMessage(tc, "OIDC_CLIENT_NULL_TOKEN_ENDPOINT", configId);
            throw new MalformedURLException(message);
        }

        Builder tokenRequestBuilder = new TokenRequestor.Builder(url, clientId, clientConfig.getClientSecret(), redirectUrl, authzCode);
        tokenRequestBuilder.sslSocketFactory(sslSocketFactory);
        tokenRequestBuilder.grantType(clientConfig.getGrantType());
        tokenRequestBuilder.isHostnameVerification(clientConfig.isHostNameVerificationEnabled());
        tokenRequestBuilder.resources(OIDCClientAuthenticatorUtil.getResources(clientConfig));
        tokenRequestBuilder.customParams(getTokenRequestCustomParameters(responseState));
        tokenRequestBuilder.useSystemPropertiesForHttpClientConnections(clientConfig.getUseSystemPropertiesForHttpClientConnections());
        String tokenEndpointAuthMethod = clientConfig.getTokenEndpointAuthMethod();
        tokenRequestBuilder.authMethod(tokenEndpointAuthMethod);
        tokenRequestBuilder.originHeaderValue(clientConfig.getTokenRequestOriginHeader());
        setAuthMethodSpecificSettings(tokenRequestBuilder, tokenEndpointAuthMethod);

        TokenRequestor tokenRequestor = tokenRequestBuilder.build();
        TokenResponse tokenResponse = tokenRequestor.requestTokens();
        Map<String, String> tokens = tokenResponse.asMap();

        oidcClientRequest.setTokenType(ClientConstants.TYPE_ID_TOKEN);
        ProviderAuthenticationResult oidcResult = jose4jUtil.createResultWithJose4J(responseState, tokens, clientConfig, oidcClientRequest, sslSocketFactory);
        addAuthCodeToUsedList(authzCode);

        return oidcResult;
    }

    HashMap<String, String> getTokenRequestCustomParameters(String state) {
        HashMap<String, String> customParams = clientConfig.getTokenRequestParams();
        String codeChallengeMethod = clientConfig.getPkceCodeChallengeMethod();
        if (codeChallengeMethod != null && !ClientConstants.PKCE_CODE_CHALLENGE_DISABLED.equals(codeChallengeMethod)) {
            customParams = addPkceParameters(state, customParams);
        }
        return customParams;
    }

    HashMap<String, String> addPkceParameters(String state, HashMap<String, String> parameters) {
        ProofKeyForCodeExchangeHelper pkceHelper = new ProofKeyForCodeExchangeHelper();
        return pkceHelper.addCodeVerifierToTokenRequestParameters(state, parameters);
    }

    void setAuthMethodSpecificSettings(Builder tokenRequestBuilder, String tokenEndpointAuthMethod) throws TokenEndpointAuthMethodSettingsException {
        TokenEndpointAuthMethod authMethod = getTokenEndpointAuthMethodInstance(tokenEndpointAuthMethod, clientConfig);
        if (authMethod == null) {
            return;
        }
        authMethod.setAuthMethodSpecificSettings(tokenRequestBuilder);
    }

    TokenEndpointAuthMethod getTokenEndpointAuthMethodInstance(String authMethod, ConvergedClientConfig clientConfig) throws TokenEndpointAuthMethodSettingsException {
        if (authMethod == null || authMethod.isEmpty()) {
            return null;
        }
        if (PrivateKeyJwtAuthMethod.AUTH_METHOD.equals(authMethod)) {
            return new PrivateKeyJwtAuthMethod(clientConfig.getId(), clientId, clientConfig.getTokenEndpointUrl(), clientConfig.getTokenEndpointAuthSigningAlgorithm(), clientConfig.getTrustStoreRef(), clientConfig.getSslRef(), clientConfig.getKeyAliasName());
        }
        return null;
    }

    // refactored from Oauth SendErrorJson.  Only usable for sending an http400.
    private void sendErrorJSON(int statusCode, String errorCode, String errorDescription) {
        final String error = "error";
        final String error_description = "error_description";
        try {
            if (errorCode != null) {
                response.setStatus(statusCode);
                response.setHeader(ClientConstants.REQ_CONTENT_TYPE_NAME,
                        "application/json;charset=UTF-8");

                JSONObject responseJSON = new JSONObject();
                responseJSON.put(error, errorCode);
                if (errorDescription != null) {
                    responseJSON.put(error_description, errorDescription);
                }
                PrintWriter pw;
                pw = response.getWriter();
                pw.write(responseJSON.toString());
                pw.flush();
            } else {
                response.sendError(statusCode);
            }
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error sending error message", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "yet another internal error, give up", ioe);
            }
        }

    }

    boolean isAuthCodeReused(String authzCode) {
        return usedAuthCodes.containsKey(authzCode);
    }

    void addAuthCodeToUsedList(String authzCode) {
        usedAuthCodes.put(authzCode, null);
    }
}
