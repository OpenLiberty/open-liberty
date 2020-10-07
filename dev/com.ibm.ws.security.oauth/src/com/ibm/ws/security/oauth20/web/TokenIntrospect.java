/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.oauth20.AuthnContext;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.internal.AuthnContextImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.TokenIntrospectProvider;

public class TokenIntrospect {
    private static TraceComponent tc = Tr.register(TokenIntrospect.class);
    protected static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";

    private static TraceComponent tcMsg = Tr.register(TokenIntrospect.class,
            TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String DEFAULT_GROUP_IDENTIFIER = "groupIds";
    public static final String KEY_TOKEN_INTROSPECT_PROVIDER = "tokenIntrospectProvider";
    private static ConcurrentServiceReferenceMap<String, TokenIntrospectProvider> tokenIntrospectProviderRef = new ConcurrentServiceReferenceMap<String, TokenIntrospectProvider>(KEY_TOKEN_INTROSPECT_PROVIDER);;

    public static void setTokenIntrospect(ConcurrentServiceReferenceMap<String, TokenIntrospectProvider> tokenIntProviderRef) {
        tokenIntrospectProviderRef = tokenIntProviderRef;
    }

    /**
     * Get the access token from the request's token parameter and look it up in
     * the token cache.
     *
     * If the access token is found in the cache return status 200 and a JSON object.
     *
     * If the token is not found or the request had errors return status 400.
     *
     */
    public void introspect(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws OidcServerException, IOException {
        String accessTokenString = request.getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
        if (accessTokenString == null || accessTokenString.isEmpty()) {
            // send 400 per OAuth Token Introspection spec
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_INTROSPECT_NO_TOKEN",
                    new Object[] { request.getRequestURI() },
                    "CWWKS1405E: The introspect request did not have a token parameter. The request URI was {0}.");
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_REQUEST, errorMsg);
            Tr.error(tc, errorMsg);
            return;
        }
        JSONObject responseJSON = new JSONObject();
        // Check whether the token is opaque or jwt type
        // change the lookup based on the token type
        String tokenLookupStr = accessTokenString;
        boolean isAppPasswordOrToken = false;
        if (OidcOAuth20Util.isJwtToken(accessTokenString)) {
            tokenLookupStr = com.ibm.ws.security.oauth20.util.HashUtils.digest(accessTokenString);
        } else if (tokenLookupStr.length() == (provider.getAccessTokenLength() + 2)) {
            // app-token or app-password
            String encode = provider.getAccessTokenEncoding();
            if (OAuth20Constants.PLAIN_ENCODING.equals(encode)) { // must be app-password or app-token
                tokenLookupStr = EndpointUtils.computeTokenHash(accessTokenString);
            } else {
                tokenLookupStr = EndpointUtils.computeTokenHash(accessTokenString, encode);
            }
            isAppPasswordOrToken = true;
        }

        OAuth20Token accessToken = null;// provider.getTokenCache().get(tokenLookupStr); // it's also null when the token expires
        if (isAppPasswordOrToken) {
            accessToken = provider.getTokenCache().getByHash(tokenLookupStr);
        } else {
            accessToken = provider.getTokenCache().get(tokenLookupStr);
        }

        if (accessToken != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "token type: " + accessToken.getType() + " grant type: " + accessToken.getGrantType());
            }
            boolean isAccessToken = accessToken.getType().equals(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN);
            // disallow introspection of app-password,
            boolean isAppPassword = accessToken.getGrantType() != null && accessToken.getGrantType().equals(OAuth20Constants.APP_PASSWORD);
            if (!isAccessToken || isAppPassword) {
                // OAuth Token Introspection spec says token is either active or not
                // if (!provider.isLocalStoreUsed()) {}
                Tr.error(tcMsg,
                        // OAUTH_SERVER_INVALID_ACCESS_TOKEN=CWWKS1454E: The request failed because the access_token is not valid or expires.
                        "OAUTH_SERVER_INVALID_ACCESS_TOKEN",
                        new Object[] {});
                responseJSON.put(Constants.INTROSPECT_CLAIM_ACTIVE, false);
                WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, responseJSON);
                return;
            }
        } else {
            // the token either can not be found in the cache or already expired
        }
        ClientAuthnData clientAuthData = null;
        try {
            clientAuthData = new ClientAuthnData(request, response);
        } catch (OAuth20DuplicateParameterException e) {
            // Duplicate parameter found in request
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_REQUEST, e.getMessage());
            Tr.error(tc, e.getMessage());
            return;
        }
        if (!clientAuthData.hasAuthnData()) {
            // client is not authorized. send 400 per OAuth Token Introspection spec
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_INVALID_CLIENT",
                    new Object[] { request.getRequestURI() },
                    "CWWKS1406E: The introspect request had an invalid client credential. The request URI was {0}.");
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_CLIENT, errorMsg);
            Tr.error(tc, "security.oauth20.endpoint.client.auth.error", new Object[] { accessToken.getClientId() });
            return;
        }
        OidcBaseClient client = null;
        client = provider.getClientProvider().get(clientAuthData.getUserName());
        if (!client.isIntrospectTokens()) {
            // client is not authorized. send 400 per OAuth Token Introspection spec
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_INTROSPECT_CLIENT_NOT_AUTHORIZED",
                    new Object[] { request.getRequestURI() },
                    "CWWKS1419E: The client is not authorized to introspect access tokens. The request URI was {0}.");
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_CLIENT, errorMsg);
            String serverLogMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_INTROSPECT_CLIENT_NOT_AUTHORIZED_SERVER_LOG",
                    new Object[] { clientAuthData.getUserName(), request.getRequestURI() },
                    "CWWKS1420E: The client {0} is not authorized to introspect access tokens. The request URI was {1}.");
            Tr.error(tc, serverLogMsg);
            return;
        }
        // Will return 200 and a JSON Object
        if (accessToken == null || OAuth20TokenHelper.isTokenExpired(accessToken)) {
            // introspect in-active
            responseJSON.put(Constants.INTROSPECT_CLAIM_ACTIVE, false);
            // ** Error handling
            // The accessTokenString is not null for sure, otherwise, it already fails out in the first checking.
            // The client has to be there and introspect is enabled, otherwise it fails already in the above session.
            // If the accessToken is null, it either can not be found in the cache or it expires and get removed during accessing the cache
            // If the accessToken is not null then it is expired.
            Tr.error(tcMsg,
                    // OAUTH_SERVER_INVALID_ACCESS_TOKEN=CWWKS1454E: The request failed because the access_token is not valid or expires.
                    "OAUTH_SERVER_INVALID_ACCESS_TOKEN",
                    new Object[] {});
            WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, responseJSON);
        } else {
            // introspect active
            if (!isClientAllowedToIntrospectToken(accessToken, client, provider)) {
                responseJSON.put(Constants.INTROSPECT_CLAIM_ACTIVE, false);
                WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, responseJSON);
                return;
            }
            if (tokenIntrospectProviderRef.isEmpty()) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "tokenIntrospectProviderRef.isEmpty");
                introspectActive(provider, request, response, accessToken, client);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "tokenIntrospectProviderRef is not Empty");
                callTokenIntrospect(request, response, accessToken);
            }
        }
    }

    boolean isClientAllowedToIntrospectToken(OAuth20Token accessToken, OidcBaseClient client, OAuth20Provider provider) {
        boolean isAppToken = accessToken.getGrantType() != null && accessToken.getGrantType().equals(OAuth20Constants.GRANT_TYPE_APP_TOKEN);
        if (!isAppToken) {
            return true; // normal access token
        }
        String[] usedByArray = accessToken.getUsedBy();
        String clientIdFromAuthnData = client.getClientId();
        if (usedByArray == null) {
            // Associate this access token (app-token) exclusively with this client
            Map<String, String[]> extensionProperties = accessToken.getExtensionProperties();
            if (extensionProperties != null) {
                extensionProperties.put(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_BY, new String[] { clientIdFromAuthnData });
                if (!provider.isLocalStoreUsed()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "persist the token : " + accessToken.getId() + " to database after adding usedBy ext with the client : " + clientIdFromAuthnData);
                    }
                    OAuth20EnhancedTokenCache cache = provider.getTokenCache();
                    cache.remove(accessToken.getId()); // some db's can't handle insert on existing record.
                    cache.add(accessToken.getId(), accessToken, accessToken.getLifetimeSeconds());
                }
                return true;
            }

            return false;
        }
        List<String> usedBy = Arrays.asList(usedByArray);
        if (usedBy.contains(clientIdFromAuthnData)) {
            return true;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Client : " + clientIdFromAuthnData + " is not specified in the usedBy ext of the app token : " + accessToken.getId());
        }
        return false;
    }

    /**
     * @throws IOException
     *
     */
    void callTokenIntrospect(HttpServletRequest request,
            HttpServletResponse response,
            OAuth20Token accessToken) throws IOException {
        TokenIntrospectProvider tokenIntrospectProvider = null;
        Iterator<TokenIntrospectProvider> it = tokenIntrospectProviderRef.getServices();
        JSONObject providerJSON = null;
        int providersInstalled = tokenIntrospectProviderRef.size();

        while (it.hasNext()) {
            tokenIntrospectProvider = it.next();
            providerJSON = getJsonObjectFromTokenIntrospectProvider(accessToken, tokenIntrospectProvider, request, response);
            if (providerJSON != null) {
                if (providersInstalled > 1) // if there is more than one TokenIntrospectProvider configured, log a warning
                    Tr.info(tcMsg, "OAUTH_SERVER_MULTIPLE_TOKEN_INTROSPECT_PROVIDER_CONFIGURED");
                break;
            }
        }
        if (providerJSON == null) {
            // Log error and exit method
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Tr.error(tcMsg, "OAUTH_SERVER_TOKEN_INTROSPECT_PROVIDER_INTERNAL_ERROR",
                    new Object[] { accessToken.getUsername(), tokenIntrospectProvider.getClass().getName() });
            return;
        }
        WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, providerJSON);
    }

    /**
     * Return the JsonObject that will be returned for the userinfo endpoint, this method invokes the userinfo provider SPI
     * that has being installed on liberty
     * @param accessToken the OAuth20Token used to get authentication context
     * @param tokenIntrospectProvider.getUserInfo(authnContext) the implementation of userinfoProvider which is installed at runtime
     * @param request the HTTPRequest for the userinfo endpoint
     * @param response the response for the userinfo endpoint request.
     * @return The JsonObject for userinfo endpoint
     * @throws IOException
     */
    private JSONObject getJsonObjectFromTokenIntrospectProvider(OAuth20Token accessToken,
            TokenIntrospectProvider tokenIntrospectProvider,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthnContext authnContext = new AuthnContextImpl(request,
                response,
                accessToken.getTokenString(),
                accessToken.getScope(),
                accessToken.getCreatedAt(),
                accessToken.getLifetimeSeconds(),
                accessToken.getUsername(),
                accessToken.getExtensionProperties());
        String strJsonObject = tokenIntrospectProvider.getUserInfo(authnContext);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getUserInfo:'" + strJsonObject + "'");
        if (strJsonObject != null) {
            try {
                return JSONObject.parse(strJsonObject);
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the access token from the request's token parameter and look it up in
     * the token cache.
     *
     * If the access token is found in the cache return status 200 and a JSON object.
     *
     * If the token is not found or the request had errors return status 400.
     *
     * @param provider
     * @param request
     * @param response
     * @throws OidcServerException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void introspectActive(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response,
            OAuth20Token accessToken,
            OidcBaseClient client) throws OidcServerException, IOException {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put(Constants.INTROSPECT_CLAIM_ACTIVE, true);
        responseJSON.put(Constants.INTROSPECT_CLAIM_SUB, accessToken.getUsername());
        responseJSON.put(Constants.INTROSPECT_CLIENT_ID, accessToken.getClientId());
        String scopes = getScopes(accessToken);
        responseJSON.put(Constants.INTROSPECT_CLAIM_SCOPE, scopes);
        // responseJSON.put(Constants.INTROSPECT_CLAIM_AUD, accessToken.getRedirectUri());
        long iat = accessToken.getCreatedAt() / 1000;
        responseJSON.put(Constants.INTROSPECT_CLAIM_IAT, iat);
        long exp = iat + accessToken.getLifetimeSeconds();
        responseJSON.put(Constants.INTROSPECT_CLAIM_EXP, exp);
        responseJSON.put(Constants.INTROSPECT_CLAIM_TOKEN_TYPE, OAuth20Constants.SUBTYPE_BEARER);
        responseJSON.put(Constants.INTROSPECT_CLAIM_ISS, getCalculatedIssuerId(provider, request));

        // Get the user claims (the same ones that go in the idtoken)
        Map<String, Object> userClaimsMap = getUserClaims(provider, accessToken, false);
        if (userClaimsMap != null) {
            responseJSON.putAll(userClaimsMap);
        }
        String grantType = accessToken.getGrantType();
        if (grantType != null && !grantType.isEmpty()) {
            responseJSON.put(Constants.INTROSPECT_CLAIM_GRANT_TYPE, grantType);
            if (grantType.equals(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
                client = provider.getClientProvider().get(accessToken.getClientId());
                if (client.getFunctionalUserId() != null) {
                    responseJSON.put(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USERID, client.getFunctionalUserId());
                    if (client.getFunctionalUserGroupIds() != null &&
                            !client.getFunctionalUserGroupIds().isJsonNull() &&
                            client.getFunctionalUserGroupIds().size() > 0) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "client.getFunctionalUserGroupIds(): " + client.getFunctionalUserGroupIds());
                        JSONArray groupIds = JSONArray.parse(client.getFunctionalUserGroupIds().toString());
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "groupIds: " + groupIds);
                        responseJSON.put(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USER_GROUPIDS, groupIds);
                    }
                }
            }
        }
        WebUtils.setJSONResponse(response, HttpServletResponse.SC_OK, responseJSON);
    }

    private String getCalculatedIssuerId(OAuth20Provider provider, HttpServletRequest request) {
        String issuerIdentifier = getCalculatedIssuerIdFromOidcServerConfig(provider);
        if (issuerIdentifier == null) {
            issuerIdentifier = getCalculatedIssuerIdFromRequest(request);
        }
        return issuerIdentifier;
    }

    private String getCalculatedIssuerIdFromOidcServerConfig(OAuth20Provider provider) {
        if (provider == null) {
            return null;
        }
        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(provider.getID());
        if (oidcServerConfig == null) {
            return null;
        }
        return oidcServerConfig.getIssuerIdentifier();
    }

    private String getCalculatedIssuerIdFromRequest(HttpServletRequest request) {
        String hostname = request.getServerName();
        String scheme = request.getScheme();
        int port = request.getLocalPort();
        String path = request.getRequestURI();
        int lastSlashIndex = path.lastIndexOf("/");
        String issuerIdentifier = scheme + "://" + hostname + ":" + port + path.substring(0, lastSlashIndex);

        return issuerIdentifier;
    }

    /**
     * Return the given access token's scopes as a space delimited String
     *
     * @param accessToken
     * @return
     */
    private String getScopes(OAuth20Token accessToken) {
        StringBuffer sb = new StringBuffer();
        String[] scopeArray = accessToken.getScope();
        if (scopeArray != null) {
            for (String scope : scopeArray) {
                sb.append(scope);
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    protected static Map<String, Object> getUserClaims(OAuth20Provider provider, OAuth20Token accessToken, boolean groupsOnly) throws IOException {
        UserClaims userClaims = getUserClaimsObj(provider, accessToken);
        Map<String, Object> userClaimsMap = getUserClaimsMap(userClaims, groupsOnly);
        return userClaimsMap;
    }

    /**
     * @param provider
     * @param responseJSON
     * @param accessToken
     * @param groupsOnly
     * @throws IOException
     */
    protected static Map<String, Object> getUserClaimsMap(UserClaims userClaims,
            boolean groupsOnly) throws IOException {
        Map<String, Object> userClaimsMap = null;
        if (userClaims != null) {
            String groupIdentifier = userClaims.getGroupIdentifier();
            Map<String, Object> claimsMap = userClaims.asMap();
            if (groupsOnly) {
                userClaimsMap = new HashMap<String, Object>();
            } else {
                userClaimsMap = claimsMap;
            }
            if (claimsMap.get(groupIdentifier) != null) {
                JSONArray groups = new JSONArray();
                groups.addAll(userClaims.getGroups());
                userClaimsMap.put(groupIdentifier, groups);
            }
        }
        return userClaimsMap;
    }

    protected static UserClaims getUserClaimsObj(OAuth20Provider provider, OAuth20Token accessToken) throws IOException {
        UserClaims userClaims = null;
        UserClaimsRetrieverService ucrService = ConfigUtils.getUserClaimsRetrieverService();
        if (ucrService != null) {
            String groupIdentifier = getGroupIdentifier(provider);
            userClaims = ucrService.getUserClaims(accessToken.getUsername(), groupIdentifier);
        }
        return userClaims;
    }

    /**
     * @param oauth20provider
     * @return
     */
    private static String getGroupIdentifier(OAuth20Provider oauth20provider) {
        String groupIdentifier = DEFAULT_GROUP_IDENTIFIER;
        OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(oauth20provider.getID());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OidcServerConfig: " + oidcServerConfig);
        }
        if (oidcServerConfig != null) {
            groupIdentifier = oidcServerConfig.getGroupIdentifier();
        }
        return groupIdentifier;
    }

}