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
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20RefreshTokenInvalidClientException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;

public class OAuth20GrantTypeHandlerRefreshImpl implements
        OAuth20GrantTypeHandler {

    final static String CLASS = OAuth20GrantTypeHandlerRefreshImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    public List<String> getKeysGrantType(AttributeList attributeList)
            throws OAuthException {
        String methodName = "getKeysGrantType";
        _log.entering(CLASS, methodName);
        List<String> tokenKeys = null;

        try {
            String refresh = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.REFRESH_TOKEN,
                    OAuth20Constants.ATTRTYPE_PARAM_BODY);

            if (refresh == null) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.REFRESH_TOKEN, null);
            }

            tokenKeys = new ArrayList<String>();
            tokenKeys.add(refresh);
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenKeys;
    }

    public void validateRequestGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException {
        String methodName = "validateRequestGrantType";
        _log.entering(CLASS, methodName);

        try {
            // index 0 in the token list should be the refresh token
            if (tokens.size() >= 1) {
                OAuth20Token refresh = tokens.get(0);

                if (refresh != null) {
                    // refresh token... validated

                    // client_id...
                    String clientId = attributeList
                            .getAttributeValueByName(OAuth20Constants.CLIENT_ID);

                    if (!clientId.equals(refresh.getClientId())) {
                        throw new OAuth20RefreshTokenInvalidClientException("security.oauth20.error.refreshtoken.invalid.client",
                                refresh.getTokenString(), clientId);
                    }
                    // ...validated

                    // scope...
                    String[] requestedScope = attributeList
                            .getAttributeValuesByNameAndType(
                                    OAuth20Constants.SCOPE,
                                    OAuth20Constants.ATTRTYPE_PARAM_BODY);
                    String[] approvedScope = refresh.getScope();

                    if (!OAuth20Util.scopeEquals(requestedScope, approvedScope)) {
                        throw new OAuth20InvalidScopeException("security.oauth20.error.invalid.scope", requestedScope,
                                approvedScope);
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
            // index 0 in the token list should be the refresh token
            if (tokens.size() >= 1) {
                OAuth20Token refresh = (OAuth20Token) tokens.get(0);

                if (refresh != null) {
                    String clientId = attributeList
                            .getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    String username = refresh.getUsername();
                    String redirectUri = refresh.getRedirectUri();
                    String[] origScope = refresh.getScope();
                    String[] requestScope = attributeList
                            .getAttributeValuesByNameAndType(
                                    OAuth20Constants.SCOPE,
                                    OAuth20Constants.ATTRTYPE_PARAM_BODY);

                    /*
                     * if scope isn't requested, grant previous scope per
                     * http://tools.ietf.org/html/draft-ietf-oauth-v2#section-6
                     */
                    if (requestScope == null || requestScope.length == 0) {
                        requestScope = origScope;
                    }

                    String stateId = refresh.getStateId();
                    String grantType = refresh.getGrantType();
                    /*
                     * create the new refresh token but maintain the originally
                     * granted scope
                     */
                    Map<String, String[]> refreshTokenMap = tokenFactory
                            .buildTokenMap(clientId, username, redirectUri,
                                    stateId, origScope, refresh, grantType);
                    // we are loosing the original grant type of this refresh token when creating the new one, so save it in the ext properties for now. This way, we don't break existing behavior
                    String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.REFRESH_TOKEN_ORIGINAL_GT;
                    String[] originalGrantType = refresh.getExtensionProperty(key);
                    
                    if (originalGrantType != null && originalGrantType.length > 0) {
                        refreshTokenMap.put(key, originalGrantType);
                    }
                    
                    OAuth20Token newRefresh = tokenFactory
                            .createRefreshToken(refreshTokenMap);

                    // create the new access token with the requested scope
                    Map<String, String[]> tokenMap = tokenFactory
                            .buildTokenMap(clientId, username, redirectUri,
                                    stateId, requestScope, refresh, grantType);
                    if (newRefresh != null) {
                        tokenMap.put(OAuth20Constants.REFRESH_TOKEN_KEY, new String[] { newRefresh.getId() });
                        tokenMap.put(OAuth20Constants.OLD_REFRESH_TOKEN_KEY, new String[] { refresh.getId() });
                    }

                    OAuth20Util.populateJwtAccessTokenData(attributeList, tokenMap);

                    // for spi
                    String proxy = attributeList
                            .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
                    tokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

                    OAuth20Token token = tokenFactory
                            .createAccessToken(tokenMap);

                    tokenList = new ArrayList<OAuth20Token>();
                    tokenList.add(token);

                    /*
                     * check if a new refresh token was successfully created,
                     * there shouldalways be a new refresh token if we are
                     * processing the refresh token grant type
                     */
                    if (newRefresh != null) {
                        tokenList.add(newRefresh);
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

            /*
             * if refresh tokens are supported, it will be in index 1, there
             * should always be a new refresh token if we are processing the
             * refresh token grant type
             */
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
