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
package com.ibm.oauth.core.internal.oauth20.responsetype.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OAuth20ResponseTypeHandlerTokenImpl implements
        OAuth20ResponseTypeHandler {

    final static String CLASS = OAuth20ResponseTypeHandlerTokenImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    @Override
    public void validateRequestResponseType(AttributeList attributeList,
            JsonArray redirectUris, boolean allowRegexpRedirects) throws OAuthException {
        String methodName = "validateRequestResponseType";
        _log.entering(CLASS, methodName);

        try {
            /*
             * redirect_uri... if one was sent, it would have already been
             * validated as the registered redirect, so we just have to make
             * sure either one was sent, or there is a registered one
             */
            String redirect = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.REDIRECT_URI,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);

            // if no redirect URI was provided in the request
            // and if none or greater than 1 registered redirect uris exist
            // or if we tolerate regexps,
            if (OidcOAuth20Util.isNullEmpty(redirect) &&
                    (OidcOAuth20Util.isNullEmpty(redirectUris) || redirectUris.size() != 1 || allowRegexpRedirects)) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.REDIRECT_URI, null);
            }

            // ...validated

            // ...username
            String username = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.USERNAME,
                    OAuth20Constants.ATTRTYPE_REQUEST);

            if (username == null || username.length() == 0) {
                throw new OAuth20AccessDeniedException("security.oauth20.error.access.denied");
            }
            // ...validated
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    @Override
    public List<OAuth20Token> buildTokensResponseType(
            AttributeList attributeList, OAuth20TokenFactory tokenFactory,
            String redirectUri) {
        String methodName = "buildTokensResponseType";
        _log.entering(CLASS, methodName, new Object[] { redirectUri });
        List<OAuth20Token> tokenList = null;

        try {
            String clientId = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String username = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.USERNAME,
                    OAuth20Constants.ATTRTYPE_REQUEST);
            String redirect = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.REDIRECT_URI,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String[] scope = attributeList
                    .getAttributeValuesByName(OAuth20Constants.SCOPE);

            // if a redirect wasn't provided in the request, use the previously
            // registered one, everything else has been previously validated
            if (redirect == null) {
                redirect = redirectUri;
            }

            Map<String, String[]> tokenMap = tokenFactory.buildTokenMap(
                    clientId, username, redirect, null, scope, null, OAuth20Constants.GRANT_TYPE_IMPLICIT);

            OAuth20Util.populateJwtAccessTokenData(attributeList, tokenMap);

            OAuth20TokenHelper.getExternalClaims(tokenMap, attributeList);
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

    @Override
    public void buildResponseResponseType(AttributeList attributeList,
            List<OAuth20Token> tokens) {
        String methodName = "buildResponseResponseType";
        _log.entering(CLASS, methodName);

        try {
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

                    String[] scope = token.getScope();
                    attributeList
                            .setAttribute(
                                    OAuth20Constants.SCOPE,
                                    OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                                    scope);

                    String stateId = token.getStateId();
                    attributeList.setAttribute(OAuth20Constants.STATE_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_STATE,
                            new String[] { stateId });

                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }
}
