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
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;

public class OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl implements
        OAuth20GrantTypeHandler {

    final static String CLASS = OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    @Override
    public List<String> getKeysGrantType(AttributeList attributeList)
            throws OAuthException {
        String methodName = "getKeysGrantType";
        _log.entering(CLASS, methodName);
        List<String> tokenKeys = null;

        try {
            /*
             * there are no keys to return for the resource owner credentials
             * grant type
             */
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenKeys;
    }

    /**
     * See the interface for a description of what has already been validated
     */
    @Override
    public void validateRequestGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException {
        String methodName = "validateRequestGrantType";
        _log.entering(CLASS, methodName);

        try {
            String username = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.USERNAME,
                    OAuth20Constants.ATTRTYPE_PARAM_BODY);
            String password = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.PASSWORD,
                    OAuth20Constants.ATTRTYPE_PARAM_BODY);
            /*
             * At the very least we should check the username and password are
             * present. We can also either validate them (method TBD) or
             * delegate that responsibility to the user's mapping rule.
             */
            if (username == null || username.length() <= 0) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.USERNAME, null);
            }
            if (password == null || password.length() <= 0) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.PASSWORD, null);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * For the resource owner credentials grant type we return an access token
     * and refresh token.
     */
    @Override
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
            OAuth20TokenFactory tokenFactory, List<OAuth20Token> tokens) {
        String methodName = "buildTokensGrantType";
        _log.entering(CLASS, methodName);
        List<OAuth20Token> tokenList = null;

        try {
            String clientId = attributeList
                    .getAttributeValueByName(OAuth20Constants.CLIENT_ID);
            String username = attributeList
                    .getAttributeValueByName(OAuth20Constants.USERNAME);
            String[] scope = attributeList.getAttributeValuesByNameAndType(
                    OAuth20Constants.SCOPE,
                    OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
            String stateId = OAuth20Util.generateUUID();

            Map<String, String[]> refreshTokenMap = tokenFactory.buildTokenMap(
                    clientId, username, null, stateId, scope, null, OAuth20Constants.GRANT_TYPE_RESOURCE_OWNER);
            String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.REFRESH_TOKEN_ORIGINAL_GT;
            refreshTokenMap.put(key, new String[] { OAuth20Constants.GRANT_TYPE_RESOURCE_OWNER });

            OAuth20Token refresh = tokenFactory.createRefreshToken(refreshTokenMap);

            Map<String, String[]> tokenMap = tokenFactory.buildTokenMap(
                    clientId, username, null, stateId, scope, null, OAuth20Constants.GRANT_TYPE_RESOURCE_OWNER);
            if (refresh != null) {
                tokenMap.put(OAuth20Constants.REFRESH_TOKEN_KEY, new String[] { refresh.getId() });
            }

            OAuth20Util.populateJwtAccessTokenData(attributeList, tokenMap);

            // for spi
            String password = attributeList
                    .getAttributeValueByName(OAuth20Constants.PASSWORD);
            tokenMap.put(OAuth20Constants.PASSWORD, new String[] { password });
            String proxy = attributeList
                    .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
            tokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

            OAuth20Token token = tokenFactory.createAccessToken(tokenMap);

            // String stateId = token.getStateId();

            tokenList = new ArrayList<OAuth20Token>();
            tokenList.add(token);

            /*
             * check the refresh token in case they aren't supported for this
             * component configuration
             */
            if (refresh != null) {
                tokenList.add(refresh);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenList;
    }

    @Override
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

                    /*
                     * It's not clear in the spec if scope should be returned
                     * for the client_credentials grant type, but it shouldn't
                     * cause any problems if we do so
                     */
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
