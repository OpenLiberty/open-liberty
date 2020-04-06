/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.token.propagation.TokenPropagationHelper;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.jose4j.JWTData;
import com.ibm.ws.security.oauth20.plugins.jose4j.JwtCreator;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.security.oauth20.JwtAccessTokenMediator;

/*
 * Copied from the core common component and updated to use a Serializable OAuth20BearerTokenImpl
 */
public class BaseTokenHandler implements OAuth20TokenTypeHandler {
    private static TraceComponent tc = Tr.register(BaseTokenHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    final static String CLASS = BaseTokenHandler.class.getName();

    private static Logger _log = Logger.getLogger(CLASS);

    @Override
    public void init(OAuthComponentConfiguration config) {
    }

    @Override
    public String getTypeTokenType() {
        return OAuth20Constants.SUBTYPE_BEARER;
    }

    @Override
    public OAuth20Token createToken(Map<String, String[]> tokenMap) {
        String methodName = "createToken";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;

        try {
            String clientId = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_ID, tokenMap);
            String componentId = OAuth20Util.getValueFromMap(OAuth20Constants.COMPONENTID, tokenMap);
            String username = OAuth20Util.getValueFromMap(OAuth20Constants.USERNAME, tokenMap);
            String redirectUri = OAuth20Util.getValueFromMap(OAuth20Constants.REDIRECT_URI, tokenMap);
            String[] scope = tokenMap.get(OAuth20Constants.SCOPE);
            int length = 0;
            int lifetime = 0;

            String lengthStr = OAuth20Util.getValueFromMap(OAuth20Constants.LENGTH, tokenMap);
            length = Integer.parseInt(lengthStr);

            String lifeStr = OAuth20Util.getValueFromMap(OAuth20Constants.LIFETIME, tokenMap);
            lifetime = Integer.parseInt(lifeStr);

            String stateId = OAuth20Util.getValueFromMap(OAuth20Constants.STATE_ID, tokenMap);
            if (stateId == null) {
                stateId = OAuth20Util.generateUUID();
            }
            String grantType = OAuth20Util.getValueFromMap(OAuth20Constants.GRANT_TYPE, tokenMap);
            boolean isAppPasswordOrTokenGT = OAuth20Constants.APP_PASSWORD.equals(grantType) ||  OAuth20Constants.APP_TOKEN.equals(grantType);
            Map<String, String[]> externalClaims = OAuth20TokenHelper.getExternalClaims(tokenMap);
            if (isAppPasswordOrTokenGT) {
                length = length + 2;
            }
            String tokenId = OAuth20Util.getRandom(length);
            String tokenContent = tokenId;
            OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(componentId);
            OAuth20Provider oauth20Provider = ProvidersService.getOAuth20Provider(componentId);
            if (oidcServerConfig != null && oauth20Provider != null && !isAppPasswordOrTokenGT) { // if the grant type is not app-password or app-token
                boolean jwtAccessToken = oauth20Provider.isJwtAccessToken(); // Read from oidc provider 2.0 config
                // boolean jwtSpi = false;
                boolean jwtSpi = isJwtMediatorSpi(); // check if spi is loaded
                String jsonFromSpi = null;
                Subject priorSubject = null;
                boolean subjectPushed = false;

                if (jwtSpi) { // getting token from SPI.
                    synchronized (this) {
                        try {
                            // 238871 - push authenticated subject onto thread for mediator to use in token creation.
                            priorSubject = TokenPropagationHelper.getRunAsSubject();
                            subjectPushed = TokenPropagationHelper.pushSubject(username);
                            // 1. pass tokenMap to SPI as input parameter
                            // 2. expect SPI to Json String
                            // com.ibm.wsspi.security.oauth20.jwtMediator(tokenMap)
                            jsonFromSpi = getJwtFromMediatorSpi(tokenMap);
                            // if jsonFromSpi is null then we'll default to opaque access token creation
                        } finally {
                            if (subjectPushed) {
                                TokenPropagationHelper.setRunAsSubject(priorSubject);
                            }
                        }
                    }
                    if (jsonFromSpi != null) {
                        // 3. merge claims and create JWT
                        JWTData jwtData = getJwtData(tokenMap, oidcServerConfig);
                        tokenContent = JwtCreator.createJwtAsStringForSpi(jsonFromSpi, oidcServerConfig, clientId, username, scope, lifetime,
                                tokenMap, grantType, null, jwtData);
                        tokenId = com.ibm.ws.security.oauth20.util.HashUtils.digest(tokenContent);
                    }
                } else if (jwtAccessToken) {
                    JWTData jwtData = getJwtData(tokenMap, oidcServerConfig);
                    tokenContent = JwtCreator.createJwtAsString(oidcServerConfig,
                            clientId,
                            username,
                            scope,
                            lifetime,
                            tokenMap,
                            (Map<String, Object>) null,
                            jwtData,
                            oauth20Provider.isMpJwt());
                    tokenId = com.ibm.ws.security.oauth20.util.HashUtils.digest(tokenContent);

                }
            }

            token = new OAuth20BearerTokenImpl(tokenId, tokenContent, componentId,
                    clientId, username, redirectUri, stateId, scope, lifetime,
                    externalClaims, grantType);

            if (token != null) {
                updateAccessToken(token, tokenMap, componentId);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    private JWTData getJwtData(Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        String sharedKey = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_SECRET, tokenMap);
        return new JWTData(sharedKey, oidcServerConfig, JWTData.TYPE_JWT_TOKEN);
    }

    boolean isJwtMediatorSpi() {
        if (ConfigUtils.getJwtAccessTokenMediatorService().size() > 0) {
            if (OAuth20Constants.JAVA_VERSION_6) {
                Tr.warning(tc, "JWT_MEDIATOR_SPI_REQUIRES_JDK", OAuth20Constants.JAVA_VERSION);
                return false;
            }
            return true;
        }
        return false;
    }

    protected String getJwtFromMediatorSpi(Map<String, String[]> tokenMap) {
        String jwtStr = null;
        Iterator<JwtAccessTokenMediator> jwtMediators = ConfigUtils.getJwtAccessTokenMediatorService().getServices();
        if (jwtMediators.hasNext()) {
            JwtAccessTokenMediator jwtMediator = jwtMediators.next();

            jwtStr = jwtMediator.mediateToken(tokenMap);
        }
        return jwtStr;

    }

    protected void updateAccessToken(OAuth20Token token, Map<String, String[]> accessTokenMap,
            String componentId) {
        if (accessTokenMap.containsKey(OAuth20Constants.REFRESH_TOKEN_KEY)) {
            String refreshId = OAuth20Util.getValueFromMap(OAuth20Constants.REFRESH_TOKEN_KEY, accessTokenMap);
            ((OAuth20TokenImpl) token).setRefreshTokenKey(refreshId);
            // _log.logp(Level.FINE, CLASS, "updateAccessToken", "updating access token with : " + refreshId);
            if (accessTokenMap.containsKey(OAuth20Constants.OLD_REFRESH_TOKEN_KEY)) {
                String oldrefreshId = OAuth20Util.getValueFromMap(OAuth20Constants.OLD_REFRESH_TOKEN_KEY, accessTokenMap);
                if (refreshId != null && !(refreshId.equals(oldrefreshId))) {
                    updateExistingAccessTokens(componentId, refreshId, accessTokenMap);
                }
            }
        }
    }

    protected void updateExistingAccessTokens(String componentId, String refreshid, Map<String, String[]> tokenMap) {

        OAuth20Provider provider = ProvidersService.getOAuth20Provider(componentId);

        if (provider != null && provider.getTokenCache() != null) {
            OAuth20EnhancedTokenCache cache = provider.getTokenCache();
            String username = OAuth20Util.getValueFromMap(
                    OAuth20Constants.USERNAME, tokenMap);
            String clientId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.CLIENT_ID, tokenMap);

            if (username != null && clientId != null) {
                Collection<OAuth20Token> tokens = cache.getAllUserTokens(username);
                for (OAuth20Token token : tokens) {
                    // _log.logp(Level.FINE, CLASS, "updateExistingAccessTokens", "token from cache =" + token.getId());
                    if (!(OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(token.getGrantType())) && OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(token.getType()) && clientId.equals(token.getClientId())) {
                        ((OAuth20TokenImpl) token).setRefreshTokenKey(refreshid);
                    }
                }
            }
        }

    }

    @Override
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

    @Override
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

    @Override
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
