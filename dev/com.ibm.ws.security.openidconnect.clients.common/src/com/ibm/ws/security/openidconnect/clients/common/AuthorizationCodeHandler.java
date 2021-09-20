/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
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
import com.ibm.ws.security.common.structures.BoundedHashMap;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.ssl.SSLSupport;

public class AuthorizationCodeHandler {
    private static final TraceComponent tc = Tr.register(AuthorizationCodeHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private OidcClientUtil oidcClientUtil = null;
    private OIDCClientAuthenticatorUtil authenticatorUtil = null;
    private SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;
    private static Map<String, Object> usedAuthCodes = Collections.synchronizedMap(new BoundedHashMap(20));

    public AuthorizationCodeHandler(SSLSupport sslsupt) {
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
     * This method handle the authorization code; it's validated the response
     * state and call the server to get the tokens using the authorization code.
     *
     * @param req
     * @param res
     * @param authzCode
     * @return
     */
    public ProviderAuthenticationResult handleAuthorizationCode(HttpServletRequest req,
            HttpServletResponse res,
            String authzCode,
            String responseState,
            ConvergedClientConfig clientConfig) {

        String clientId = clientConfig.getClientId();
        OidcClientRequest oidcClientRequest = (OidcClientRequest) req.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST);
        ProviderAuthenticationResult oidcResult = null;
        oidcResult = authenticatorUtil.verifyResponseState(req, res, responseState, clientConfig);
        if (oidcResult != null) {
            return oidcResult; // only if something bad happened, otherwise proceed to exchange auth code for tokens.
        }
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, clientConfig.getTokenEndpointUrl())) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", clientConfig.getTokenEndpointUrl());
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            return oidcResult;
        }
        String redirect_url = authenticatorUtil.setRedirectUrlIfNotDefined(req, clientConfig);
        if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, redirect_url)) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", redirect_url);
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            return oidcResult;
        }

        SSLSocketFactory sslSocketFactory = null;
        try {
            //sslSocketFactory = getSSLSocketFactory(clientConfig.getTokenEndpointUrl(), clientConfig.getSSLConfigurationName(), clientId);
            boolean throwExc = clientConfig.getTokenEndpointUrl() != null && clientConfig.getTokenEndpointUrl().startsWith("https");
            sslSocketFactory = new OidcClientHttpUtil().getSSLSocketFactory(clientConfig, sslSupport, throwExc, false);
        } catch (SSLException e) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e, clientConfig.getClientId() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        // go get the tokens and validate them.
        try {

            String url = clientConfig.getTokenEndpointUrl();
            if (url == null || url.length() == 0) {
                String message = Tr.formatMessage(tc, "OIDC_CLIENT_NULL_TOKEN_ENDPOINT", clientConfig.getClientId());
                throw new MalformedURLException(message);
            }
            HashMap<String, String> tokens = oidcClientUtil.getTokensFromAuthzCode(url,
                    clientId,
                    clientConfig.getClientSecret(),
                    redirect_url,
                    authzCode,
                    clientConfig.getGrantType(),
                    sslSocketFactory,
                    clientConfig.isHostNameVerificationEnabled(),
                    clientConfig.getTokenEndpointAuthMethod(),
                    OIDCClientAuthenticatorUtil.getResources(clientConfig),
                    clientConfig.getTokenRequestParams(),
                    clientConfig.getUseSystemPropertiesForHttpClientConnections());

            oidcClientRequest.setTokenType(ClientConstants.TYPE_ID_TOKEN);

            // this has a LOT of dependencies.
            oidcResult = jose4jUtil.createResultWithJose4J(responseState, tokens, clientConfig, oidcClientRequest);

            //go get the userinfo if configured to do so, and update the authentication result to include it.
            new UserInfoHelper(clientConfig, sslSupport).getUserInfoIfPossible(oidcResult, tokens, sslSocketFactory, oidcClientRequest);

            addAuthCodeToUsedList(authzCode);

        } catch (BadPostRequestException e) { //CWWKS1708E
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getErrorMessage(), clientId, clientConfig.getTokenEndpointUrl() });
            sendErrorJSON(res, e.getStatusCode(), "invalid_request", e.getErrorMessage());
            oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, e.getStatusCode());
        } catch (Exception e) {
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getLocalizedMessage(), clientId, clientConfig.getTokenEndpointUrl() });
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return oidcResult;
    }

    // refactored from Oauth SendErrorJson.  Only usable for sending an http400.
    private void sendErrorJSON(HttpServletResponse response, int statusCode, String errorCode, String errorDescription) {
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
