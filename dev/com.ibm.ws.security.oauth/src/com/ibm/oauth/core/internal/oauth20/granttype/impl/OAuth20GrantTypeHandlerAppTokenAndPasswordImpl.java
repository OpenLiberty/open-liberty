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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 *
 */
public class OAuth20GrantTypeHandlerAppTokenAndPasswordImpl implements OAuth20GrantTypeHandler {
    final static String CLASS = OAuth20GrantTypeHandlerAppTokenAndPasswordImpl.class
            .getName();

    private static Logger _log = Logger.getLogger(CLASS);

    final static ArrayList<String> _emptyList = new ArrayList<String>();
    String grant_type;
    com.ibm.ws.security.oauth20.api.OAuth20Provider oauth20Config;

    public OAuth20GrantTypeHandlerAppTokenAndPasswordImpl(String gt, com.ibm.ws.security.oauth20.api.OAuth20Provider config) {
        grant_type = gt;
        oauth20Config = config;
    }

    @Override
    public List<String> getKeysGrantType(@Sensitive AttributeList attributeList)
            throws OAuthException {
        //
        // we won't provide a cached key to get the cached data
        return _emptyList; //
    }

    /**
    * {@inheritDoc}
    */
    @Override
    @FFDCIgnore({ InvalidGrantException.class })
    public void validateRequestGrantType(@Sensitive AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException {
        String methodName = "validateRequestGrantType";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        // let's debug what attributes we get
        _log.entering(CLASS, methodName);

        if (finestLoggable) {
            List<Attribute> attributes = attributeList.getAllAttributes();
            for (Attribute attribute : attributes) {
                // String attribName = attribute.getName();
                _log.logp(Level.FINEST, CLASS, methodName, "attrib: " + attribute.getName() + " :" + attribute.toString());
            }
        }

        // verification
        String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String client_secret = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_SECRET);
        OidcServerConfig oidcServerConfig = getOidcServerConfig(attributeList);

        String tokenString = getTokenString(attributeList);

        try {
            // verify access token that is supplied
            verifyAccessToken(tokenString);

        } catch (InvalidGrantException e) {
            throw e;
        } catch (Exception e) {
            // when verify and de-serialize failed, it should throw an Exception
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = e.toString();
            }
            // Tr.error and ffdc had been handled by JWTVerifier or its sub-routines
            throw new InvalidGrantException(message, e);
        }

    }

    /**
     * @param tokenString
     */
    private boolean verifyAccessToken(String tokenString) throws InvalidGrantException {
        // TODO Auto-generated method stub
        return true;
    }

    /**
    * Example of data in the attributeList:
    * {name: access_token type: urn:ibm:names:body:param values: [access token]}
    * {name: grant_type type: urn:ibm:names:body:param values: [urn:ietf:params:oauth:grant-type:app_password]}
    *
    */
    @Override
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
            OAuth20TokenFactory tokenFactory,
            List<OAuth20Token> tokens) {

        String methodName = "buildTokensGrantType";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        List<OAuth20Token> tokenList = new ArrayList<OAuth20Token>();
        // generate access_token and id_token, but no refresh_token
        // token lifetime=accesToken LifeTime

        String clientId = attributeList
                .getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String username = attributeList
                .getAttributeValueByName(OAuth20Constants.USERNAME);
        String[] scope = attributeList.getAttributeValuesByNameAndType(
                OAuth20Constants.SCOPE,
                OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
        String stateId = null;// OAuth20Util.generateUUID();
        if (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(grant_type)) {
            stateId = OAuth20Constants.APP_PASSWORD_STATE_ID;
        } else if (OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(grant_type)) {
            stateId = OAuth20Constants.APP_TOKEN_STATE_ID;
        }

        // String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String[] redirectUris = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.REDIRECT_URI,
                OAuth20Constants.ATTRTYPE_PARAM_OAUTH);

        String redirectUri = redirectUris == null ? null : (redirectUris.length > 0 ? redirectUris[0] : null);

        Map<String, String[]> accessTokenMap = tokenFactory.buildTokenMap(clientId, username, redirectUri,
                stateId, scope, (OAuth20Token) null, grant_type);

        if (oauth20Config != null) {
            String appid = OAuth20Util.getRandom(oauth20Config.getAccessTokenLength());
            String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_ID;
            accessTokenMap.put(key, new String[] { appid });
            if (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(grant_type)) {
                accessTokenMap.put(OAuth20Constants.LIFETIME, new String[] { ""
                        + oauth20Config.getAppPasswordLifetime() });

            } else {
                accessTokenMap.put(OAuth20Constants.LIFETIME, new String[] { ""
                        + oauth20Config.getAppTokenLifetime() });
            }
        }

        OAuth20Util.populateJwtAccessTokenData(attributeList, accessTokenMap);
        // for spi
        String proxy = attributeList
                .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
        accessTokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

        OAuth20TokenHelper.getExternalClaims(accessTokenMap, attributeList); // used_for, used_by, app_name etc...

        OAuth20Token access = tokenFactory.createAccessTokenAsAppPasswordOrToken(accessTokenMap);

        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, methodName, "access token is " + access);
        }
        if (access != null)
            tokenList.add(access);

        return tokenList;
    }

    @Override
    public void buildResponseGrantType(AttributeList attributeList, List<OAuth20Token> tokens) {
        String methodName = "buildResponseGrantType";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            for (OAuth20Token token : tokens) {
                String strTokenType = token.getType();
                if (OAuth20Constants.ACCESS_TOKEN.equals(strTokenType)) {
                    handleAccessToken(attributeList, token);
                } else {
                    // tr.error
                    _log.logp(Level.FINEST, CLASS, methodName, "Unknown token type:'" + strTokenType + "'");
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    public void handleAccessToken(AttributeList attributeList, OAuth20Token token) {

        String accessToken = token.getTokenString();
        attributeList.setAttribute(grant_type, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { accessToken });

        String[] id;
        id = token.getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_ID);
        attributeList.setAttribute(OAuth20Constants.APP_ID, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { id[0] });

        String created = "" + token.getCreatedAt();
        attributeList.setAttribute(OAuth20Constants.CREATED_AT, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { created });

        long expiresAt = token.getCreatedAt() + (token.getLifetimeSeconds() * 1000L);

        String expires = "" + expiresAt;
        attributeList.setAttribute(OAuth20Constants.EXPIRES_AT, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { expires });

        // String[] scope = token.getScope();
        // attributeList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, scope);
    }

    /**
    * @param scopes
    * @return
    */
    protected String convertToString(String[] scopes) {
        StringBuffer st = new StringBuffer("");
        boolean bFirst = true;
        for (String scope : scopes) {
            if (!bFirst) {
                st.append(" ");
            }
            st.append(scope);
            bFirst = false;
        }
        return st.toString();
    }

    /**
    * @param user
    */
    protected void verifyJwtSub(String user) throws OAuth20Exception {
        if (user == null) { // required

            throw new InvalidGrantException("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", null);
        }

        // verify if the sub is in userRegistry
        // If it's not in the user registry, throw Exception
        // if (!isInUserRegistry(user)) {
        // //Tr.error(tc, "JWT_TOKEN_SUB_NOT_FOUND_ERR", user);
        // throw new InvalidGrantException("JWT_TOKEN_SUB_NOT_FOUND_ERR", user, null);
        // }
    }

    public String getString(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof String[]) {
            String[] strings = (String[]) obj;
            if (strings.length > 0)
                return strings[0];
            else
                return null;
        }
        return obj.toString();
    }

    protected OidcServerConfig getOidcServerConfig(AttributeList attributeList) throws OAuthException {
        OidcServerConfig oidcServerConfig = null;

        String requestType = attributeList.getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
        if (OAuth20Constants.REQUEST_FEATURE_OAUTH2.equals(requestType)) {
            // oauth2 request should not get the oidcServerConfig
            return null;
        }

        return oidcServerConfig;
    }

    protected String getTokenString(AttributeList attributeList) throws OAuthException {
        String tokenString = null;
        // get the jwt token
        String ACCESS_TOKEN = "access_token";
        String[] tokenStrings = attributeList.getAttributeValuesByName(ACCESS_TOKEN);
        if (tokenStrings == null || tokenStrings.length < 1 || tokenStrings[0].isEmpty()) {

            throw new InvalidGrantException("JWT_TOKEN_NO_TOKEN_EXTERNAL_ERR", null);
        } else if (tokenStrings.length > 1) {

            throw new InvalidGrantException("JWT_TOKEN_TOO_MANY_TOKENS_ERR", null);
        } else {
            tokenString = tokenStrings[0];
        }
        return tokenString;
    }

    protected String[] getStrings(Object obj) {
        if (obj == null || obj instanceof String[]) {
            return (String[]) obj;
        }
        if (obj instanceof String) {
            return new String[] { (String) obj };
        }
        if (obj instanceof List) {
            List list = (List) obj;
            String[] result = new String[list.size()];
            int iCnt = 0;
            for (Object aObj : list) {
                result[iCnt++] = aObj.toString();
            }
            return result;
        }
        if (obj instanceof Object) {
            return new String[] { obj.toString() };
        }

        return new String[] {};
    }

}
