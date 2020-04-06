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
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;

public class OAuth20GrantTypeHandlerClientCredentialsImpl implements
        OAuth20GrantTypeHandler {

    final static String CLASS = OAuth20GrantTypeHandlerClientCredentialsImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    public List<String> getKeysGrantType(AttributeList attributeList)
            throws OAuthException {
        String methodName = "getKeysGrantType";
        _log.entering(CLASS, methodName);
        List<String> tokenKeys = null;

        try {
            // there are no keys to return for the client credentials grant type
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
            /*
             * as there were no tokens to retrieve, similarly there is nothing
             * to validate. The client authentication (and verification that the
             * client_id is present and matches the authenticated client) should
             * already have been done.
             */
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * For the client credentials grant type we return an access token only. It
     * doesn't make any sense to use refresh tokens for this grant type.
     */
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
            OAuth20TokenFactory tokenFactory, List<OAuth20Token> tokens) {
        String methodName = "buildTokensGrantType";
        _log.entering(CLASS, methodName);
        List<OAuth20Token> tokenList = null;

        try {
            String clientId = attributeList
                    .getAttributeValueByName(OAuth20Constants.CLIENT_ID);
            String[] scope = attributeList.getAttributeValuesByNameAndType(
                    OAuth20Constants.SCOPE,
                    OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);

            Map<String, String[]> tokenMap = tokenFactory.buildTokenMap(
                    clientId, clientId, null, null, scope, null, OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS);

            OAuth20Util.populateJwtAccessTokenData(attributeList, tokenMap);

            // for spi
            String proxy = attributeList
                    .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
            tokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

            OAuth20Token token = tokenFactory.createAccessToken(tokenMap);

            tokenList = new ArrayList<OAuth20Token>();
            tokenList.add(token);
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
                OAuth20Token token = (OAuth20Token) tokens.get(0);

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
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }
}
