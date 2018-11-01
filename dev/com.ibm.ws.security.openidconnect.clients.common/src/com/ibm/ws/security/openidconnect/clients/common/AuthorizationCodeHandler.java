/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.ssl.SSLSupport;

public class AuthorizationCodeHandler {
    private static final TraceComponent tc = Tr.register(AuthorizationCodeHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private OidcClientUtil oidcClientUtil = null;
    private OIDCClientAuthenticatorUtil authenticatorUtil = null;
    private SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;

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
            sslSocketFactory = getSSLSocketFactory(clientConfig.getTokenEndpointUrl(), clientConfig.getSSLConfigurationName(), clientId);
        } catch (SSLException e) {
            Tr.error(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e.getMessage() != null ? e.getMessage() : "invalid ssl context", clientConfig.getClientId() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        // go get the tokens and validate them.
        try {

            String url = clientConfig.getTokenEndpointUrl();
            if (url == null || url.length() == 0) {
                throw new MalformedURLException("MalformedURLException");
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

            //if tokens were valid, go get the userinfo if configured to do so, and update the authentication result to include it.
            UserInfoHelper uih = new UserInfoHelper(clientConfig);
            if (uih.willRetrieveUserInfo()) {
                OidcTokenImplBase idToken = null;
                if (oidcResult.getCustomProperties() != null) {
                    idToken = (OidcTokenImplBase) oidcResult.getCustomProperties().get(Constants.ID_TOKEN_OBJECT);
                }
                String subjFromIdToken = null;
                if (idToken != null) {
                    subjFromIdToken = idToken.getSubject();
                }
                if (subjFromIdToken != null) {
                    uih.getUserInfo(oidcResult, sslSocketFactory, tokens.get(Constants.ACCESS_TOKEN), subjFromIdToken);
                }
            }

        } catch (BadPostRequestException e) {
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getErrorMessage(), clientId, clientConfig.getTokenEndpointUrl() });
            sendErrorJSON(res, e.getStatusCode(), "invalid_request", e.getErrorMessage());
            oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, e.getStatusCode());
        } catch (Exception e) {
            Tr.error(tc, "OIDC_CLIENT_TOKEN_REQUEST_FAILURE", new Object[] { e.getLocalizedMessage(), clientId, clientConfig.getTokenEndpointUrl() });
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return oidcResult;
    }

    protected SSLSocketFactory getSSLSocketFactory(String tokenUrl, String sslConfigurationName, String clientId) throws SSLException {
        SSLSocketFactory sslSocketFactory = null;

        try {
            sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
        } catch (javax.net.ssl.SSLException e) {
            throw new SSLException(e.getMessage());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
        }

        if (sslSocketFactory == null) {
            if (tokenUrl != null && tokenUrl.startsWith("https")) {
                throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", clientId }));
            }
        }
        return sslSocketFactory;
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
}
