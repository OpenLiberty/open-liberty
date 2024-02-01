/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.IssuerUtils;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.server.internal.JwtUtils;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class OidcRpInitiatedLogoutTokenAndRequestData {

    private static TraceComponent tc = Tr.register(OidcRpInitiatedLogoutTokenAndRequestData.class);

    private final HttpServletRequest request;
    private final OidcEndpointServices endpointServices;
    private final OAuth20Provider oauth20Provider;
    private final OidcServerConfig oidcServerConfig;

    private Principal userPrincipal = null;
    private String userPrincipalName = null;
    private String idTokenHintParameter = null;
    private String postLogoutRedirectUriParameter = null;
    private String subjectFromIdToken = null;
    private String clientId = null;
    private String state = null;
    private OAuth20Token cachedIdToken = null;

    private boolean isDataValidForLogout = false;

    public OidcRpInitiatedLogoutTokenAndRequestData(HttpServletRequest request, OidcEndpointServices endpointServices, OAuth20Provider oauth20Provider,
                                                    OidcServerConfig oidcServerConfig) {
        this.request = request;
        this.endpointServices = endpointServices;
        this.oauth20Provider = oauth20Provider;
        this.oidcServerConfig = oidcServerConfig;
    }

    public void populate() {
        isDataValidForLogout = true;

        initializeUserPrincipalData();
        initializeValuesFromRequestParameters();

        cachedIdToken = getCachedIdToken(idTokenHintParameter);

        subjectFromIdToken = ((cachedIdToken == null) ? null : cachedIdToken.getUsername());
        clientId = ((cachedIdToken == null) ? clientId : cachedIdToken.getClientId());

        if (idTokenHintParameter != null && cachedIdToken == null && isDataValidForLogout) {
            parseAndPopulateDataFromIdTokenHint();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "login username : " + userPrincipalName + " IDToken username : " + subjectFromIdToken);
        }

        if (userPrincipalName != null && subjectFromIdToken != null && !userPrincipalName.equals(subjectFromIdToken)) {
            // user mismatch, abort
            Tr.error(tc, "OIDC_SERVER_USERNAME_MISMATCH_ERR", new Object[] { userPrincipalName, subjectFromIdToken });
            isDataValidForLogout = false;
        }
    }

    void initializeUserPrincipalData() {
        userPrincipal = request.getUserPrincipal();
        userPrincipalName = ((userPrincipal == null) ? null : userPrincipal.getName());
    }

    void initializeValuesFromRequestParameters() {
        idTokenHintParameter = request.getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
        if (idTokenHintParameter != null && idTokenHintParameter.isEmpty()) {
            idTokenHintParameter = null;
        }
        postLogoutRedirectUriParameter = request.getParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI);
        if (postLogoutRedirectUriParameter != null && postLogoutRedirectUriParameter.isEmpty()) {
            postLogoutRedirectUriParameter = null;
        }
        clientId = request.getParameter(OIDCConstants.OIDC_LOGOUT_CLIENT_ID);
        if (clientId != null && clientId.isEmpty()) {
            clientId = null;
        }
        state = request.getParameter(OIDCConstants.STATE);
        if (state != null && state.isEmpty()) {
            state = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id_token_hint : " + idTokenHintParameter + ", post_logout_redirect_uri : " + postLogoutRedirectUriParameter + ", client_id : " + clientId + ", state : "
                         + state);
        }
    }

    OAuth20Token getCachedIdToken(String idTokenString) {
        OAuth20Token cachedIdToken = null;
        if (idTokenString != null) {
            OAuth20TokenCache tokenCache = oauth20Provider.getTokenCache();
            if (tokenCache != null) {
                String hash = HashUtils.digest(idTokenString);
                if (hash != null) {
                    cachedIdToken = tokenCache.get(hash);
                    // if idToken is found, this is valid.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "idToken : " + cachedIdToken);
                    }
                } else {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
                    isDataValidForLogout = false;
                }
            }
        }
        return cachedIdToken;
    }

    @FFDCIgnore(IDTokenValidationFailedException.class)
    void parseAndPopulateDataFromIdTokenHint() {
        try {
            parseAndValidateIdTokenHint();
        } catch (IDTokenValidationFailedException ivfe) {
            Throwable cause = ivfe.getCause();
            if (cause != null && cause instanceof IllegalStateException) {
                // this error can be ignored, since this is due to exp, iat expiration.
                // extract sub.
                try {
                    JWTPayload payload = JsonTokenUtil.getPayload(idTokenHintParameter);
                    if (payload != null) {
                        subjectFromIdToken = JsonTokenUtil.getSub(payload);
                        clientId = getVerifiedClientId(payload);
                    }
                } catch (Exception e) {
                    Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e.getMessage() });
                    isDataValidForLogout = false;
                }
            } else {
                Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { ivfe.getMessage() });
                isDataValidForLogout = false;
            }
        } catch (Exception e) {
            Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { e.getMessage() });
            isDataValidForLogout = false;
        }
    }

    void parseAndValidateIdTokenHint() throws Exception {
        JWT jwt = JwtUtils.createJwt(idTokenHintParameter, oauth20Provider, oidcServerConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT : " + jwt);
        }
        if (jwt.verifySignatureOnly()) {
            verifyIdTokenHintIssuer(jwt);
            subjectFromIdToken = JsonTokenUtil.getSub(jwt.getPayload());
            clientId = getVerifiedClientId(jwt.getPayload());
        } else {
            Tr.error(tc, "OIDC_SERVER_IDTOKEN_VERIFY_ERR", new Object[] { "IDTokenValidatonFailedException" });
            isDataValidForLogout = false;
        }
    }

    /**
     * Per https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout: When both client_id and id_token_hint are
     * present, the OP MUST verify that the Client Identifier matches the one used when issuing the ID Token.
     */
    String getVerifiedClientId(JWTPayload payload) throws IDTokenValidationFailedException {
        String clientIdFromPayload = JsonTokenUtil.getAud(payload);
        if (clientId != null && !clientId.equals(clientIdFromPayload)) {
            throw IDTokenValidationFailedException.format(tc, "ID_TOKEN_HINT_CLIENT_ID_DOES_NOT_MATCH_REQUEST_PARAMETER", clientIdFromPayload, clientId);
        }
        return clientIdFromPayload;
    }

    void verifyIdTokenHintIssuer(JWT jwt) throws IDTokenValidationFailedException {
        String iss = JsonTokenUtil.getIss(jwt.getPayload());
        String issuerIdentifier = oidcServerConfig.getIssuerIdentifier();
        if (issuerIdentifier == null || issuerIdentifier.isEmpty()) {
            issuerIdentifier = IssuerUtils.getCalculatedIssuerIdFromRequest(request);
        }
        if (!issuerIdentifier.equals(iss)) {
            throw IDTokenValidationFailedException.format(tc, "ID_TOKEN_ISSUER_NOT_THIS_OP", iss, issuerIdentifier, oidcServerConfig.getProviderId());
        }
    }

    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public String getUserPrincipalName() {
        return userPrincipalName;
    }

    public String getIdTokenHintParameter() {
        return idTokenHintParameter;
    }

    public String getPostLogoutRedirectUriParameter() {
        return postLogoutRedirectUriParameter;
    }

    public String getSubjectFromIdToken() {
        return subjectFromIdToken;
    }

    public String getClientId() {
        return clientId;
    }

    public String getState() {
        return state;
    }

    public OAuth20Token getCachedIdToken() {
        return cachedIdToken;
    }

    public boolean isDataValidForLogout() {
        return isDataValidForLogout;
    }

}
