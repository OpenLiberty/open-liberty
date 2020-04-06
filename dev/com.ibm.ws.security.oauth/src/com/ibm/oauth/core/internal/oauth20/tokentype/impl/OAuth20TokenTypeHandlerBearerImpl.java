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
package com.ibm.oauth.core.internal.oauth20.tokentype.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.oauth.core.internal.oauth20.token.impl.OAuth20AccessTokenBearerImpl;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;

public class OAuth20TokenTypeHandlerBearerImpl implements
        OAuth20TokenTypeHandler {

    final static String CLASS = OAuth20TokenTypeHandlerBearerImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    public void init(OAuthComponentConfiguration config) {
    }

    public String getTypeTokenType() {
        return OAuth20Constants.SUBTYPE_BEARER;
    }

    public OAuth20Token createToken(Map<String, String[]> tokenMap) {
        String methodName = "createToken";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;

        try {
            String clientId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.CLIENT_ID, tokenMap);
            String componentId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.COMPONENTID, tokenMap);
            String username = OAuth20Util.getValueFromMap(
                    OAuth20Constants.USERNAME, tokenMap);
            String redirectUri = OAuth20Util.getValueFromMap(
                    OAuth20Constants.REDIRECT_URI, tokenMap);
            String[] scope = tokenMap.get(OAuth20Constants.SCOPE);
            int length = 0;
            int lifetime = 0;

            String lengthStr = OAuth20Util.getValueFromMap(
                    OAuth20Constants.LENGTH, tokenMap);
            length = Integer.parseInt(lengthStr);

            String lifeStr = OAuth20Util.getValueFromMap(
                    OAuth20Constants.LIFETIME, tokenMap);
            lifetime = Integer.parseInt(lifeStr);

            String stateId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.STATE_ID, tokenMap);
            if (stateId == null) {
                stateId = OAuth20Util.generateUUID();
            }
            String grantType =
                    OAuth20Util.getValueFromMap(OAuth20Constants.GRANT_TYPE, tokenMap);
            Map<String, String[]> externalClaims = OAuth20TokenHelper.getExternalClaims(tokenMap);
            String tokenString = OAuth20Util.getRandom(length);
            token = new OAuth20AccessTokenBearerImpl(tokenString, componentId,
                    clientId, username, redirectUri, stateId, scope, lifetime,
                    externalClaims, grantType);
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    public List<String> getKeysTokenType(AttributeList attributeList)
            throws OAuthException {
        String methodName = "getKeysTokenType";
        _log.entering(CLASS, methodName);
        List<String> tokenKeys = null;

        try {
            String token = attributeList
                    .getAttributeValueByName(OAuth20Constants.ACCESS_TOKEN);

            if (token == null || token.length() <= 0) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.ACCESS_TOKEN, null);
            }

            tokenKeys = new ArrayList<String>();
            tokenKeys.add(token);
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return tokenKeys;
    }

    public void validateRequestTokenType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException {
        String methodName = "validateRequestTokenType";
        _log.entering(CLASS, methodName);

        try {
            /*
             * we just need to make sure the access_token parameter wasn't sent
             * more then once in the request
             */
            String[] tokenArray = attributeList
                    .getAttributeValuesByName(OAuth20Constants.ACCESS_TOKEN);

            if (tokenArray != null && tokenArray.length > 1) {
                throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter",
                        OAuth20Constants.ACCESS_TOKEN);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    public void buildResponseTokenType(AttributeList attributeList,
            List<OAuth20Token> tokens) {
        String methodName = "buildResponseTokenType";
        _log.entering(CLASS, methodName);

        try {
            if (tokens.size() >= 1) {
                OAuth20Token token = tokens.get(0);

                if (token != null) {
                    attributeList
                            .setAttribute(
                                    OAuth20Constants.RESPONSEATTR_EXPIRES,
                                    OAuth20Constants.ATTRTYPE_RESPONSE_DECISION,
                                    new String[] { OAuth20TokenHelper
                                            .expiresUTC(token) });

                    attributeList.setAttribute(OAuth20Constants.USERNAME,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { token.getUsername() });

                    attributeList.setAttribute(OAuth20Constants.ACCESS_TOKEN,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { token.getTokenString() });

                    attributeList.setAttribute(
                            OAuth20Constants.ACCESS_TOKEN_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_META,
                            new String[] { token.getId() });

                    attributeList.setAttribute(
                            OAuth20Constants.OAUTH_TOKEN_CLIENT_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { token.getClientId() });

                    attributeList.setAttribute(OAuth20Constants.STATE_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_STATE,
                            new String[] { token.getStateId() });

                    String[] scope = token.getScope();
                    if (scope != null) {
                        attributeList.setAttribute(OAuth20Constants.SCOPE,
                                OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                                scope);
                    }
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

}
