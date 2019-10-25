/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;

public class AuthorizationCodeAuthenticator {

    public static final TraceComponent tc = Tr.register(AuthorizationCodeAuthenticator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    String authzCode = null;
    SocialLoginConfig socialConfig = null;
    SSLSocketFactory sslSocketFactory = null;

    private Map<String, Object> tokens = new HashMap<String, Object>();
    private String accessToken = null;
    private String userApiResponse = null;
    private JwtToken jwt = null;
    private JwtToken issuedJwt = null;

    OAuthClientUtil clientUtil = new OAuthClientUtil();
    TAIWebUtils taiWebUtils = new TAIWebUtils();
    TAIJwtUtils taiJwtUtils = new TAIJwtUtils();
    TAIUserApiUtils userApiUtils = new TAIUserApiUtils();

    public AuthorizationCodeAuthenticator(HttpServletRequest req, HttpServletResponse res, String authzCode, SocialLoginConfig socialConfig) {
        this.request = req;
        this.response = res;
        this.authzCode = authzCode;
        this.socialConfig = socialConfig;
    }

    // use this only when tokens have already been validated and we just need the jwt's.
    public AuthorizationCodeAuthenticator(SocialLoginConfig config, Map<String, Object> tokens) {
        this.socialConfig = config;
        this.tokens = tokens;
        this.accessToken = getAccessTokenFromTokens();
    }
    
    public AuthorizationCodeAuthenticator(HttpServletRequest req, HttpServletResponse res, SocialLoginConfig socialConfig, String accessToken, boolean openShift) {
        this.request = req;
        this.response = res; 
        this.socialConfig = socialConfig;
        this.tokens.put(ClientConstants.ACCESS_TOKEN, accessToken);
    }

    public Map<String, Object> getTokens() {
        return tokens;
    }

    public String getUserApiResponse() {
        return userApiResponse;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public JwtToken getJwt() {
        return jwt;
    }

    public JwtToken getIssuedJwt() {
        return issuedJwt;
    }

    public void generateJwtAndTokenInformation() throws SocialLoginException {
        createSslSocketFactory();
        getTokensFromTokenEndpoint();
        createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
    }
    
    public void generateJwtAndTokensFromTokenReviewResult() throws SocialLoginException {
        createSslSocketFactory();
        createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
    }

    @FFDCIgnore(Exception.class)
    void createSslSocketFactory() throws SocialLoginException {
        try {
            sslSocketFactory = socialConfig.getSSLSocketFactory();
        } catch (Exception e) {
            throw createExceptionAndLogMessage(e, "AUTH_CODE_ERROR_SSL_CONTEXT", new Object[] { socialConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    /**
     * Obtains access token (and possibly ID token) from the token endpoint.
     */
    void getTokensFromTokenEndpoint() throws SocialLoginException {
        try {
            tokens = getTokensUsingAuthzCode();
        } catch (Exception e) {
            throw createExceptionAndLogMessage(e, "AUTH_CODE_ERROR_GETTING_TOKENS", new Object[] { socialConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
        // Obtain access token (and possibly ID token) from the token endpoint.  Doesn't validate the tokens (?)
        return clientUtil.getTokensFromAuthzCode(socialConfig.getTokenEndpoint(),
                socialConfig.getClientId(),
                socialConfig.getClientSecret(),
                taiWebUtils.getRedirectUrl(request, socialConfig),
                authzCode,
                ClientConstants.AUTHORIZATION_CODE,
                sslSocketFactory,
                false,
                socialConfig.getTokenEndpointAuthMethod(),
                socialConfig.getResource(),
                socialConfig.getUseSystemPropertiesForHttpClientConnections());
    }

    public void createJwtUserApiResponseAndIssuedJwtWithAppropriateToken() throws SocialLoginException {
        String idToken = getIdTokenFromTokens();
        accessToken = getAccessTokenFromTokens();

        if (idToken == null) {
            // oauth flows need to contact the userapi endpoint to get the user info from access token.
            createJwtUserApiResponseAndIssuedJwtFromUserApi();
        } else {
            // oidc has the id token so can create directly.
            createJwtUserApiResponseAndIssuedJwtFromIdToken(idToken);
        }
    }

    String getIdTokenFromTokens() {
        return (String) tokens.get(ClientConstants.ID_TOKEN);
    }

    String getAccessTokenFromTokens() {
        return (String) tokens.get(ClientConstants.ACCESS_TOKEN);
    }

    void createJwtUserApiResponseAndIssuedJwtFromUserApi() throws SocialLoginException {
        createUserApiResponseFromAccessToken();
        createIssuedJwtFromUserApiResponse();
    }

    void createUserApiResponseFromAccessToken() throws SocialLoginException {
        userApiResponse = userApiUtils.getUserApiResponse(clientUtil, socialConfig, accessToken, sslSocketFactory);
        if (userApiResponse == null || userApiResponse.isEmpty()) {
            throw createExceptionAndLogMessage(null, "USER_API_RESPONSE_NULL_OR_EMPTY", new Object[] { socialConfig.getUniqueId() });
        }
    }

    void createIssuedJwtFromUserApiResponse() throws SocialLoginException {
        try {
            if (socialConfig.getJwtRef() != null) {
                issuedJwt = taiJwtUtils.createJwtTokenFromJson(userApiResponse, socialConfig, false); //oauth login flow
            }
        } catch (Exception e) {
            throw createExceptionAndLogMessage(e, "AUTH_CODE_FAILED_TO_CREATE_JWT", new Object[] { socialConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    void createJwtUserApiResponseAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
        createJwtAndIssuedJwtFromIdToken(idToken);
        createUserApiResponseFromIdToken(idToken);
    }

    void createJwtAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
        try {
            createJwtFromIdToken(idToken);
            createIssuedJwtFromIdToken(idToken);
        } catch (Exception e) {
            throw createExceptionAndLogMessage(e, "AUTH_CODE_FAILED_TO_CREATE_JWT", new Object[] { socialConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    void createJwtFromIdToken(String idToken) throws SocialLoginException {
        jwt = taiJwtUtils.createJwtTokenFromIdToken(idToken, socialConfig.getUniqueId());
    }

    void createIssuedJwtFromIdToken(String idToken) throws Exception {
        if (socialConfig.getJwtRef() != null) {
            issuedJwt = taiJwtUtils.createJwtTokenFromJson(idToken, socialConfig, true);
        }
    }

    void createUserApiResponseFromIdToken(String idToken) {
        String payload = JsonUtils.getPayload(idToken);
        userApiResponse = JsonUtils.decodeFromBase64String(payload);
    }

    SocialLoginException createExceptionAndLogMessage(Exception cause, String msgKey, Object[] objects) {
        SocialLoginException exception = new SocialLoginException(msgKey, cause, objects);
        exception.logErrorMessage();
        return exception;
    }

}
