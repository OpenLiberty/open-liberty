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
package com.ibm.oauth.core.internal.oauth20.token;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.token.impl.OAuth20AuthorizationGrantCodeImpl;
import com.ibm.oauth.core.internal.oauth20.token.impl.OAuth20AuthorizationGrantRefreshImpl;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandlerFactory;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OAuth20TokenFactory {

    final static String CLASS = OAuth20TokenFactory.class.getName();
    static Logger _log = Logger.getLogger(CLASS);

    OAuth20ComponentInternal _component;

    public OAuth20TokenFactory(OAuth20ComponentInternal component) {
        _component = component;
    }

    /**
     * This is a helper method to build a Map which can be used to create an
     * OAuth20Token using one of the create methods of this class
     *
     * @param clientId
     * @param username
     * @param redirectUri
     * @param stateId
     * @param scope
     * @param token
     * @return
     */
    public Map<String, String[]> buildTokenMap(String clientId,
            String username, String redirectUri, String stateId,
            String[] scope, OAuth20Token token, String grantType) {
        String methodName = "buildTokenMap";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        Map<String, String[]> result = new HashMap<String, String[]>();

        if (clientId != null) {
            result.put(OAuth20Constants.CLIENT_ID, new String[] { clientId });
        }

        if (username != null) {
            result.put(OAuth20Constants.USERNAME, new String[] { username });
        }

        if (redirectUri != null) {
            result.put(OAuth20Constants.REDIRECT_URI,
                    new String[] { redirectUri });
        }

        if (stateId != null) {
            result.put(OAuth20Constants.STATE_ID, new String[] { stateId });
        }

        if (scope != null && scope.length > 0) {
            result.put(OAuth20Constants.SCOPE, scope);
        }
        if (grantType.equals(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
            insertFunctionalIdAndGroup(result, clientId);
        }

        if (token != null) {
            OAuth20ConfigProvider config = _component.get20Configuration();
            int maxGrantLifetime = config.getMaxAuthGrantLifetimeSeconds();
            int codeLifetime = config.getCodeLifetimeSeconds();
            int remainingLifetime = 0;
            if (token.getType().equals(
                    OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT)) {
                if (token.getSubType().equals(
                        OAuth20Constants.SUBTYPE_AUTHORIZATION_CODE)) {
                    int elapsedLifetime = codeLifetime
                            - Integer.parseInt(OAuth20TokenHelper
                                    .expiresInSeconds(token));

                    if (finestLoggable) {
                        _log.logp(Level.FINEST, CLASS, methodName,
                                "Elapsed time for " + token.getTokenString()
                                        + ": " + elapsedLifetime + " seconds");
                    }

                    remainingLifetime = maxGrantLifetime - elapsedLifetime;
                } else if (token.getSubType().equals(
                        OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN)) {
                    remainingLifetime = Integer.parseInt(OAuth20TokenHelper
                            .expiresInSeconds(token));
                }

                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "Remaining seconds until max authorization grant lifetime: "
                                    + remainingLifetime + " seconds");
                }
                result.put(OAuth20Constants.LIFETIME, new String[] { ""
                        + remainingLifetime });
            }

            OAuth20TokenHelper.addExternalClaims(result, token);
        }

        if (grantType != null && !grantType.isEmpty()) {
            result.put(OAuth20Constants.GRANT_TYPE, new String[] { grantType });
        }

        _log.exiting(CLASS, methodName, result);
        return result;
    }

    /**
     * Insert functionalId and groups into the token map if they are defined in client config.
     * They're only meaningful for the client credentials grant type,
     * because it doesn't have any id or group in the request.
     * @param tokenMap
     * @param clientId
     */
    void insertFunctionalIdAndGroup(Map<String, String[]> tokenMap, String clientId) {
        BaseClient clientConfig = null;
        try {
            clientConfig = _component.get20Configuration().getClientProvider().get(clientId);
        } catch (OidcServerException e) {
            // ffdc
        }
        if (clientConfig == null) {
            return; // should not happen
        }
        if (!(clientConfig instanceof OidcBaseClient)) {
            return;
        }
        OidcBaseClient obc = (OidcBaseClient) clientConfig;
        String funcId = obc.getFunctionalUserId();
        String[] funcGroups = OidcOAuth20Util.getStringArray(obc.getFunctionalUserGroupIds());

        if (funcGroups.length > 0) {
            tokenMap.put(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USER_GROUPIDS, funcGroups);
        }
        if (funcId != null) {
            tokenMap.put(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USERID, new String[] { funcId });
        }

    }

    /**
     * Create an OAuth20 authorization code
     *
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters:
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none present generate a new one
     *            <li>SCOPE - scope from the grant being used
     *            </ul>
     * @return
     */
    public OAuth20Token createAuthorizationCode(Map<String, String[]> tokenMap) {
        String methodName = "createAuthorizationCode";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;

        try {
            OAuth20ConfigProvider config = _component.get20Configuration();
            int lifetime = config.getCodeLifetimeSeconds();
            int length = config.getCodeLength();

            String clientId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.CLIENT_ID, tokenMap);
            String username = OAuth20Util.getValueFromMap(
                    OAuth20Constants.USERNAME, tokenMap);
            String redirectUri = OAuth20Util.getValueFromMap(
                    OAuth20Constants.REDIRECT_URI, tokenMap);
            String[] scope = tokenMap.get(OAuth20Constants.SCOPE);
            String stateId = OAuth20Util.getValueFromMap(
                    OAuth20Constants.STATE_ID, tokenMap);
            if (stateId == null) {
                stateId = OAuth20Util.generateUUID();
            }
            
            Map<String, String[]> externalClaims = OAuth20TokenHelper.getExternalClaims(tokenMap);

            String tokenString = OAuth20Util.getRandom(length);
            token = new OAuth20AuthorizationGrantCodeImpl(tokenString,
                    _component.getParentComponentInstance().getInstanceId(),
                    clientId, username, redirectUri, stateId, scope, lifetime,
                    externalClaims);

            if (token != null && token.isPersistent()) {
                persistToken(token);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    /**
     * Create an OAuth20 access token
     *
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters:
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none is present, it is the
     *            responsibility of OAuth20TokenTypeHandler to generate one.
     *            <li>SCOPE - scope from the grant being used
     *            <li>LIFETIME - remaining seconds to the max authorization
     *            grant lifetime (applicable to authorization code and resource
     *            owner password creds only)
     *            </ul>
     *            As well as any additional parameters that should be passed to
     *            the configured OAuth20TokenTypeHandler implementation
     * @return
     */
    public OAuth20Token createAccessToken(Map<String, String[]> tokenMap) {
        String methodName = "createAccessToken";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            Map<String, String[]> accessTokenMap = new HashMap<String, String[]>();
            accessTokenMap.putAll(tokenMap);
            String componentId = _component.getParentComponentInstance().getInstanceId();
            accessTokenMap.put(OAuth20Constants.COMPONENTID,
                    new String[] { componentId });

            OAuth20ConfigProvider config = _component.get20Configuration();
            int lifetime = config.getTokenLifetimeSeconds();

            String lifeStr;
            if (accessTokenMap.containsKey(OAuth20Constants.LIFETIME)) {
                int remainingLifetime = Integer.parseInt(OAuth20Util
                        .getValueFromMap(OAuth20Constants.LIFETIME,
                                accessTokenMap));

                // access token lifetime = min(remaining max grant lifetime,
                // access token lifetime)
                if (remainingLifetime < lifetime) {
                    lifetime = remainingLifetime;
                }
            }
            lifeStr = Integer.toString(lifetime);

            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Creating access token with remaining lifetime: "
                                + lifeStr + " seconds");
            }

            accessTokenMap.put(OAuth20Constants.LIFETIME,
                    new String[] { (lifeStr) });

            int length = config.getAccessTokenLength();
            String lengthStr = Integer.toString(length);
            accessTokenMap.put(OAuth20Constants.LENGTH,
                    new String[] { lengthStr });

            OAuth20TokenTypeHandler handler = null;
            try {
                handler = OAuth20TokenTypeHandlerFactory.getHandler(_component);
            } catch (OAuthConfigurationException e) {
                // shouldn't happen, but if it does, log the exception
                _log.throwing(CLASS, methodName, e);
            }
            token = handler.createToken(accessTokenMap);

            // In extremely unlikely event we got a duplicate, try again.
            // Don't do this check for jwt's as their id is a hash of their string value
            // and it is possible to create >1 identical jwt within one second for same user and client, which is harmless.
            int i = 0;
            OAuth20Provider prov = ProvidersService.getOAuth20Provider(componentId);
            boolean isJwt = prov != null && (prov.isJwtAccessToken() || prov.isMpJwt());
            while (!isJwt && token != null && token.isPersistent() && isDuplicateToken(token.getId())) {
                token = handler.createToken(accessTokenMap);
                if (++i > 10) {
                    _log.severe("Unexpected failure in OAuth20TokenFactory - unable to get unique token id");
                    break; // avoid hard hang
                }
            }

            if (token != null) {
                OAuth20Provider oauth20Provider = ProvidersService.getOAuth20Provider(componentId);
                if (oauth20Provider != null && oauth20Provider.cacheAccessToken()) {
                    persistToken(token);
                }
                if (oauth20Provider == null) { // this condition is true for unit tests
                    persistToken(token);
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    /**
     * Create an OAuth20 access token
     *
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters:
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none is present, it is the
     *            responsibility of OAuth20TokenTypeHandler to generate one.
     *            <li>SCOPE - scope from the grant being used
     *            <li>LIFETIME - remaining seconds to the max authorization
     *            grant lifetime (applicable to authorization code and resource
     *            owner password creds only)
     *            </ul>
     *            As well as any additional parameters that should be passed to
     *            the configured OAuth20TokenTypeHandler implementation
     * @return
     */
    public OAuth20Token createAccessTokenAsAppPasswordOrToken(Map<String, String[]> tokenMap) {

        String methodName = "createAccessToken";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            Map<String, String[]> accessTokenMap = new HashMap<String, String[]>();
            accessTokenMap.putAll(tokenMap);
            String componentId = _component.getParentComponentInstance().getInstanceId();
            accessTokenMap.put(OAuth20Constants.COMPONENTID,
                    new String[] { componentId });

            OAuth20ConfigProvider config = _component.get20Configuration();

            String lifeStr;
            int remainingLifetime = 0;
            if (accessTokenMap.containsKey(OAuth20Constants.LIFETIME)) {
                remainingLifetime = Integer.parseInt(OAuth20Util
                        .getValueFromMap(OAuth20Constants.LIFETIME,
                                accessTokenMap));

            }
            lifeStr = Integer.toString(remainingLifetime);

            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Creating access token with remaining lifetime: "
                                + lifeStr + " seconds");
            }

            accessTokenMap.put(OAuth20Constants.LIFETIME,
                    new String[] { (lifeStr) });

            int length = config.getAccessTokenLength();
            String lengthStr = Integer.toString(length);
            accessTokenMap.put(OAuth20Constants.LENGTH,
                    new String[] { lengthStr });

            OAuth20TokenTypeHandler handler = null;
            try {
                handler = OAuth20TokenTypeHandlerFactory.getHandler(_component);
            } catch (OAuthConfigurationException e) {
                // shouldn't happen, but if it does, log the exception
                _log.throwing(CLASS, methodName, e);
            }
            token = handler.createToken(accessTokenMap);

            if (token != null) {
                OAuth20Provider oauth20Provider = ProvidersService.getOAuth20Provider(componentId);
                if (oauth20Provider != null && oauth20Provider.cacheAccessToken()) {
                    persistToken(token);
                }
                if (oauth20Provider == null) { // this condition is true for unit tests
                    persistToken(token);
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    /**
     * Create an OAuth20 refresh token
     *
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters:
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none present generate a new one
     *            <li>SCOPE - scope from the grant being used
     *            <li>LIFETIME - remaining seconds to the max authorization
     *            grant lifetime
     *            </ul>
     * @return
     */
    public OAuth20Token createRefreshToken(Map<String, String[]> tokenMap) {
        String methodName = "createRefreshToken";
        _log.entering(CLASS, methodName);
        OAuth20Token token = null;
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        try {
            OAuth20ConfigProvider config = _component.get20Configuration();
            boolean issueRefreshToken = config.isIssueRefreshToken();
            String clientId = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_ID, tokenMap);

            if (issueRefreshToken) {

                try {
                    JsonArray arrayJson = config.getClientProvider().get(clientId).getGrantTypes();
                    if (arrayJson.size() > 0) {
                        issueRefreshToken = false;
                    }
                    for (int i = 0; i < arrayJson.size(); i++) {
                        if (OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN.equals(arrayJson.get(i).getAsString())) {
                            issueRefreshToken = true; // This client is allowed for refresh_token
                        }
                    }
                } catch (Exception e) {
                    // ignore it
                }
            }
            if (issueRefreshToken) {
                int remainingLifetime;

                if (tokenMap.containsKey(OAuth20Constants.LIFETIME)) {
                    remainingLifetime = Integer.parseInt(OAuth20Util
                            .getValueFromMap(OAuth20Constants.LIFETIME,
                                    tokenMap));
                } else {
                    remainingLifetime = config.getMaxAuthGrantLifetimeSeconds();
                }

                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "Creating refresh token with remaining lifetime is: "
                                    + remainingLifetime + " seconds");
                }

                int length = config.getRefreshTokenLength();

                // String clientId = OAuth20Util.getValueFromMap(
                // OAuth20Constants.CLIENT_ID, tokenMap);
                String username = OAuth20Util.getValueFromMap(
                        OAuth20Constants.USERNAME, tokenMap);
                String redirectUri = OAuth20Util.getValueFromMap(
                        OAuth20Constants.REDIRECT_URI, tokenMap);
                String[] scope = tokenMap.get(OAuth20Constants.SCOPE);

                String stateId = OAuth20Util.getValueFromMap(
                        OAuth20Constants.STATE_ID, tokenMap);
                if (stateId == null) {
                    stateId = OAuth20Util.generateUUID();
                }
                Map<String, String[]> externalClaims = OAuth20TokenHelper.getExternalClaims(tokenMap);
                String tokenString = OAuth20Util.getRandom(length);
                // in extremely unlikely event we got a duplicate, try again
                int i = 0;
                while (isDuplicateToken(tokenString)) {
                    tokenString = OAuth20Util.getRandom(length);
                    if (++i > 10) {
                        _log.severe("Unexpected failure - unable to get unique refresh token id");
                        break; // avoid hard hang
                    }
                }
                token = new OAuth20AuthorizationGrantRefreshImpl(
                        tokenString,
                        _component.getParentComponentInstance().getInstanceId(),
                        clientId, username, redirectUri, stateId, scope,
                        remainingLifetime, externalClaims);

                if (token != null && token.isPersistent()) {
                    persistToken(token);
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return token;
    }

    /**
     *
     * @param tokenString
     * @return true if a token with this id has already been cached.
     */
    boolean isDuplicateToken(String tokenString) {
        boolean result = false;
        if (_component.getTokenCache() instanceof OAuth20EnhancedTokenCache) {
            OAuth20EnhancedTokenCache ecache = (OAuth20EnhancedTokenCache) _component.getTokenCache();
            result = ecache.getByHash(MessageDigestUtil.getDigest(tokenString)) != null;
        }
        return result;
    }

    /**
     * Save an OAuth20Token in the configured token cache
     *
     * @param token
     */
    public void persistToken(OAuth20Token token) {
        if (token != null) {
            _component.getTokenCache().add(token.getId(), token,
                    token.getLifetimeSeconds());
        }
    }

    // oidc10
    public OAuth20ComponentInternal getOAuth20ComponentInternal() {
        return _component;
    }
}
