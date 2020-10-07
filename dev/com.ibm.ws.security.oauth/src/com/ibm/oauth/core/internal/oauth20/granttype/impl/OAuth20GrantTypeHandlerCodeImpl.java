/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.granttype.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AuthorizationCodeInvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MismatchedRedirectUriException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.oauth.core.internal.oauth20.token.impl.OAuth20AuthorizationGrantCodeImpl;
import com.ibm.ws.security.oauth20.util.HashUtils;


public class OAuth20GrantTypeHandlerCodeImpl implements OAuth20GrantTypeHandler {
    final static String CLASS = OAuth20GrantTypeHandlerCodeImpl.class.getName();

    private static Logger _log = Logger.getLogger(CLASS);

    public List<String> getKeysGrantType(AttributeList attributeList)
            throws OAuthException {
        String methodName = "getKeysGrantType";
        _log.entering(CLASS, methodName);
        List<String> tokenKeys = null;

        try {
            String code = attributeList
                    .getAttributeValueByNameAndType(OAuth20Constants.CODE,
                            OAuth20Constants.ATTRTYPE_PARAM_BODY);

            if (code == null) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.CODE, null);
            }

            tokenKeys = new ArrayList<String>();
            tokenKeys.add(code);
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenKeys;
    }

    /**
     * See the interface for a description of what has already been validated
     */
    public void validateRequestGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException {
        String methodName = "validateRequestGrantType";
        _log.entering(CLASS, methodName);

        try {
            // index 0 in the token list should be the authorization code
            if (tokens.size() >= 1) {
                OAuth20Token code = (OAuth20Token) tokens.get(0);

                if (code != null) {
                    // code... validated

                    // client_id (existence is already validated)...
                    String clientId = attributeList
                            .getAttributeValueByName(OAuth20Constants.CLIENT_ID);

                    if (!clientId.equals(code.getClientId())) {
                        throw new OAuth20AuthorizationCodeInvalidClientException("security.oauth20.error.invalid.authorizationcode",
                                code.getTokenString(), clientId);
                    }
                    // ...validated

                    // redirect_uri...
                    String redirectUri = attributeList
                            .getAttributeValueByNameAndType(
                                    OAuth20Constants.REDIRECT_URI,
                                    OAuth20Constants.ATTRTYPE_PARAM_BODY);

                    /*
                     * If no redirect URI was provided in the request, make sure
                     * a redirect URI wasn't provided in the request for the
                     * authorization grant
                     */
                    String codeUri = code.getRedirectUri();
                    if (redirectUri == null) {
                        if (codeUri != null) {
                            throw new OAuth20MismatchedRedirectUriException("security.oauth20.error.mismatched.redirecturi.null.request.redirecturi",
                                    redirectUri, codeUri);
                        }
                    }
                    /*
                     * if a redirect URI was provided in the request, validate
                     * it against the one provided in the request for the
                     * authorization grant
                     */
                    else {
                        if (codeUri == null || !codeUri.equals(redirectUri)) {
                            throw new OAuth20MismatchedRedirectUriException("security.oauth20.error.mismatched.redirecturi",
                                    redirectUri, codeUri);
                        }
                    }
                    // ...validated
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }
        
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
            OAuth20TokenFactory tokenFactory, List<OAuth20Token> tokens) {
        String methodName = "buildTokensGrantType";
        _log.entering(CLASS, methodName);
        List<OAuth20Token> tokenList = null;

        try {
            // index 0 in the token list should be the authorization code
            if (tokens.size() >= 1) {
                OAuth20Token code = (OAuth20Token) tokens.get(0);

                if (code != null) {
                    String clientId = attributeList
                            .getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    String redirectUri = attributeList
                            .getAttributeValueByNameAndType(
                                    OAuth20Constants.REDIRECT_URI,
                                    OAuth20Constants.ATTRTYPE_PARAM_BODY);
                    String[] scope = code.getScope();
                    String username = code.getUsername();

                    String stateId = code.getStateId();

                    Map<String, String[]> refreshTokenMap = tokenFactory
                            .buildTokenMap(clientId, username, redirectUri,
                                    stateId, scope, code, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    
                    String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.REFRESH_TOKEN_ORIGINAL_GT;
                    refreshTokenMap.put(key, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE }); 

                    OAuth20Token refresh = tokenFactory
                            .createRefreshToken(refreshTokenMap);

                    Map<String, String[]> accessTokenMap = tokenFactory
                            .buildTokenMap(clientId, username, redirectUri,
                                    stateId, scope, code, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    if (refresh != null) {
                        accessTokenMap.put(OAuth20Constants.REFRESH_TOKEN_KEY, new String[] { refresh.getId() });
                    }

                    OAuth20Util.populateJwtAccessTokenData(attributeList, accessTokenMap);
                    // for spi
                    String proxy = attributeList
                            .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
                    accessTokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

                    OAuth20Token access = tokenFactory
                            .createAccessToken(accessTokenMap);

                    tokenList = new ArrayList<OAuth20Token>();
                    tokenList.add(access);

                    /*
                     * check the refresh token in case they aren't supported for
                     * this component configuration
                     */
                    if (refresh != null) {
                        tokenList.add(refresh);
                    }
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenList;
    }

    public void buildResponseGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens) {
        String methodName = "buildResponseGrantType";
        _log.entering(CLASS, methodName);

        try {
            // index 0 in the token list should be the access token
            if (tokens.size() >= 1) {
                OAuth20Token token = tokens.get(0);

                if (token != null) {
                    String accessToken = token.getTokenString();
                    attributeList.setAttribute(OAuth20Constants.ACCESS_TOKEN,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { accessToken });

                    String accessTokenId = token.getId();
                    attributeList.setAttribute(
                            OAuth20Constants.ACCESS_TOKEN_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_META,
                            new String[] { accessTokenId });

                    String type = token.getSubType();
                    attributeList.setAttribute(OAuth20Constants.TOKEN_TYPE,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { type });

                    String expires = OAuth20TokenHelper.expiresInSeconds(token);
                    attributeList.setAttribute(OAuth20Constants.EXPIRES_IN,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { expires });

                    String stateId = token.getStateId();
                    attributeList.setAttribute(OAuth20Constants.STATE_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_STATE,
                            new String[] { stateId });

                    String[] scope = token.getScope();
                    attributeList
                            .setAttribute(
                                    OAuth20Constants.SCOPE,
                                    OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                                    scope);
                }
            }

            // if refresh tokens are supported, it will be in index 1
            if (tokens.size() >= 2) {
                OAuth20Token refresh = tokens.get(1);

                if (refresh != null) {
                    String refreshToken = refresh.getTokenString();
                    attributeList.setAttribute(OAuth20Constants.REFRESH_TOKEN,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { refreshToken });

                    String refreshTokenId = refresh.getId();
                    attributeList.setAttribute(
                            OAuth20Constants.REFRESH_TOKEN_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_META,
                            new String[] { refreshTokenId });
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

}
