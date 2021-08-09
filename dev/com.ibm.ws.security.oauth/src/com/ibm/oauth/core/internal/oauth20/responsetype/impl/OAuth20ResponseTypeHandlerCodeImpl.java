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
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OAuth20ResponseTypeHandlerCodeImpl implements
        OAuth20ResponseTypeHandler {

    final static String CLASS = OAuth20ResponseTypeHandlerCodeImpl.class
            .getName();
    static Logger _log = Logger.getLogger(CLASS);

    @Override
    public void validateRequestResponseType(AttributeList attributeList, JsonArray redirectUris, boolean allowRegexpRedirects) throws OAuthException {
        String methodName = "validateRequestResponseType";
        _log.entering(CLASS, methodName, new Object[] { redirectUris });
        try {
            /*
             * redirect_uri... if one was sent, it would have already been
             * validated as a match for the registered redirect uri, so we just
             * have to make sure either one was sent, or there is a registered
             * one
             */
            String redirect = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.REDIRECT_URI,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);

            // if no redirect URI was provided in the request
            // and if a redirect URI wasn't previously registered
            // or if we tolerate regexps, in which case we must have one supplied to match against.
            if (OidcOAuth20Util.isNullEmpty(redirect) &&
                    (OidcOAuth20Util.isNullEmpty(redirectUris) || redirectUris.size() != 1 || allowRegexpRedirects)) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.REDIRECT_URI, null);
            }

            // ...validated

            // ...username - required for the authorization endpoint
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
        List<OAuth20Token> result = null;
        String methodName = "buildTokensResponseType";
        _log.entering(CLASS, methodName, new Object[] { redirectUri });
        try {
            String clientId = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String code_challenge = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.CODE_CHALLENGE,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String code_challenge_method = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.CODE_CHALLENGE_METHOD,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String username = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.USERNAME,
                    OAuth20Constants.ATTRTYPE_REQUEST);
            String redirect = attributeList.getAttributeValueByNameAndType(
                    OAuth20Constants.REDIRECT_URI,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String[] scope = attributeList
                    .getAttributeValuesByName(OAuth20Constants.SCOPE);

            Map<String, String[]> tokenMap = tokenFactory.buildTokenMap(
                    clientId, username, redirect, null, scope, null, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
            
            if (code_challenge != null && code_challenge_method != null) {
                addPKCEAttributes(code_challenge, code_challenge_method, tokenMap);
            }
            
            OAuth20TokenHelper.getExternalClaims(tokenMap, attributeList);
            OAuth20Token code = tokenFactory.createAuthorizationCode(tokenMap);

            result = new ArrayList<OAuth20Token>();
            result.add(code);

        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }

    /**
     * @param code_challenge
     * @param code_challenge_method
     * @param tokenMap
     */
    private void addPKCEAttributes(String code_challenge, String code_challenge_method, Map<String, String[]> tokenMap) {
        String methodName = "addPKCEAttributes";
        _log.entering(CLASS, methodName, tokenMap);
        String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.CODE_CHALLENGE;
        tokenMap.put(key, new String[] { code_challenge });
        key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.CODE_CHALLENGE_METHOD;
        tokenMap.put(key, new String[] { code_challenge_method });
        
        _log.exiting(CLASS, methodName, tokenMap);
        
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
                    String code = token.getTokenString();
                    attributeList.setAttribute(OAuth20Constants.CODE,
                            OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                            new String[] { code });

                    String codeId = token.getId();
                    attributeList.setAttribute(
                            OAuth20Constants.AUTHORIZATION_CODE_ID,
                            OAuth20Constants.ATTRTYPE_RESPONSE_META,
                            new String[] { codeId });

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
