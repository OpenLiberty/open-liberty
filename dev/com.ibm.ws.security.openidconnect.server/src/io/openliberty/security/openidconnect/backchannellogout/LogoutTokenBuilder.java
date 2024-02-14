/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientValidator;
import com.ibm.ws.security.oauth20.plugins.jose4j.JWTData;
import com.ibm.ws.security.oauth20.plugins.jose4j.JwsSigner;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutException;
import com.ibm.ws.security.openidconnect.server.internal.JwtUtils;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.JwtParsingUtils;

public class LogoutTokenBuilder {

    private static TraceComponent tc = Tr.register(LogoutTokenBuilder.class);

    public static final String EVENTS_MEMBER_NAME = "http://schemas.openid.net/event/backchannel-logout";

    private final HttpServletRequest request;
    private final OidcServerConfig oidcServerConfig;
    private final OAuth20Provider oauth20provider;
    private final OAuth20EnhancedTokenCache tokenCache;

    public LogoutTokenBuilder(HttpServletRequest request, OidcServerConfig oidcServerConfig) {
        this.request = request;
        this.oidcServerConfig = oidcServerConfig;
        this.oauth20provider = getOAuth20Provider(oidcServerConfig);
        this.tokenCache = oauth20provider.getTokenCache();
    }

    OAuth20Provider getOAuth20Provider(OidcServerConfig oidcServerConfig) {
        String oauthProviderName = oidcServerConfig.getOauthProviderName();
        return ProvidersService.getOAuth20Provider(oauthProviderName);
    }

    public Map<OidcBaseClient, Set<String>> buildLogoutTokensFromUserName(String user) {
        return buildLogoutTokensForUser(user);
    }

    public Map<OidcBaseClient, Set<String>> buildLogoutTokensFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        validateIdTokenSignature(idTokenString);
        JwtClaims idTokenClaims = getClaimsFromIdTokenString(idTokenString);
        try {
            return buildLogoutTokensForUser(idTokenClaims.getSubject());
        } catch (MalformedClaimException e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN", e);
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    void validateIdTokenSignature(String idTokenString) throws LogoutTokenBuilderException {
        try {
            String oauthProviderName = oidcServerConfig.getOauthProviderName();
            OAuth20Provider oauthProvider = ProvidersService.getOAuth20Provider(oauthProviderName);
            JWT jwt = JwtUtils.createJwt(idTokenString, oauthProvider, oidcServerConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT : " + jwt);
            }
            jwt.verifySignatureOnly();
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN", new Object[] { e });
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    @FFDCIgnore(Exception.class)
    JwtClaims getClaimsFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        try {
            JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(idTokenString);
            JwtClaims claims = jwtContext.getJwtClaims();
            verifyIdTokenContainsRequiredClaims(claims);
            verifyIssuer(claims);
            return claims;
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN", new Object[] { e });
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    void verifyIdTokenContainsRequiredClaims(JwtClaims claims) throws Exception {
        List<String> missingClaims = new ArrayList<>();
        String iss = claims.getIssuer();
        if (iss == null || iss.isEmpty()) {
            missingClaims.add(Claims.ISSUER);
        }
        String sub = claims.getSubject();
        if (sub == null || sub.isEmpty()) {
            missingClaims.add(Claims.SUBJECT);
        }
        List<String> aud = claims.getAudience();
        if (aud == null || aud.isEmpty()) {
            missingClaims.add(Claims.AUDIENCE);
        }
        if (!missingClaims.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_MISSING_REQUIRED_CLAIMS", new Object[] { missingClaims });
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    void verifyIssuer(JwtClaims idTokenClaims) throws MalformedClaimException, IdTokenDifferentIssuerException {
        String issuerClaim = idTokenClaims.getIssuer();
        String expectedIssuer = oidcServerConfig.getIssuerIdentifier();
        if (expectedIssuer != null && !expectedIssuer.isEmpty()) {
            if (!expectedIssuer.equals(issuerClaim)) {
                String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_ISSUER_NOT_THIS_OP", new Object[] { issuerClaim, expectedIssuer, oidcServerConfig.getProviderId() });
                throw new IdTokenDifferentIssuerException(errorMsg);
            }
        } else {
            expectedIssuer = getIssuerFromRequest();
            if (!expectedIssuer.equals(issuerClaim)) {
                String otherExpectedIssuer = expectedIssuer.replace("/oidc/providers/", "/oidc/endpoint/");
                if (!otherExpectedIssuer.equals(issuerClaim)) {
                    String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_ISSUER_NOT_THIS_OP", new Object[] { issuerClaim, expectedIssuer, oidcServerConfig.getProviderId() });
                    throw new IdTokenDifferentIssuerException(errorMsg);
                }
            }
        }
    }

    Map<OidcBaseClient, Set<String>> buildLogoutTokensForUser(String user) {
        Collection<OAuth20Token> allCachedUserTokens = getAllCachedUserTokens(user);
        Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut = getClientsToLogOut(allCachedUserTokens);
        Map<OidcBaseClient, Set<String>> logoutTokens = buildLogoutTokensForClients(clientsToLogOut);
        removeUserTokensFromCache(allCachedUserTokens, clientsToLogOut);
        return logoutTokens;
    }

    Collection<OAuth20Token> getAllCachedUserTokens(String user) {
        return tokenCache.getAllUserTokens(user);
    }

    void removeUserTokensFromCache(Collection<OAuth20Token> allCachedUserTokens, Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut) {
        removeUserIdTokensFromCache(clientsToLogOut);
        removeUserAccessTokensFromCache(allCachedUserTokens, clientsToLogOut);
    }

    void removeUserIdTokensFromCache(Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut) {
        for (Entry<OidcBaseClient, List<OAuth20Token>> clientEntry : clientsToLogOut.entrySet()) {
            for (OAuth20Token cachedIdToken : clientEntry.getValue()) {
                tokenCache.remove(cachedIdToken.getId());
                removeRefreshTokenAssociatedWithOAuthTokenFromCache(cachedIdToken);
            }
        }
    }

    void removeUserAccessTokensFromCache(Collection<OAuth20Token> allCachedUserTokens, Map<OidcBaseClient, List<OAuth20Token>> clientsToCachedIdTokens) {
        if (clientsToCachedIdTokens.isEmpty()) {
            return;
        }
        Set<String> clientIdsBeingLoggedOut = new HashSet<>();
        for (OidcBaseClient client : clientsToCachedIdTokens.keySet()) {
            clientIdsBeingLoggedOut.add(client.getClientId());
        }
        for (OAuth20Token token : allCachedUserTokens) {
            if (isAccessTokenCreatedForUser(token)) {
                String clientIdForAccessToken = token.getClientId();
                if (clientIdsBeingLoggedOut.contains(clientIdForAccessToken)) {
                    // Only remove access tokens for this user for the clients that are being logged out
                    removeAccessTokenAndAssociatedRefreshTokenFromCache(token);
                }
            }
        }
    }

    /**
     * Checks the grant type of the token to ensure it wasn't created via an app password or app token.
     */
    boolean isAccessTokenCreatedForUser(OAuth20Token token) {
        if (!OAuth20Constants.ACCESS_TOKEN.equals(token.getType())) {
            return false;
        }
        String grantType = token.getGrantType();
        if (grantType != null && (grantType.equals(OAuth20Constants.GRANT_TYPE_APP_PASSWORD) || grantType.equals(OAuth20Constants.GRANT_TYPE_APP_TOKEN))) {
            return false;
        }
        return true;
    }

    void removeAccessTokenAndAssociatedRefreshTokenFromCache(OAuth20Token accessToken) {
        String tokenString = accessToken.getTokenString();
        String tokenLookupStr = tokenString;
        if (OidcOAuth20Util.isJwtToken(tokenString)) {
            tokenLookupStr = com.ibm.ws.security.oauth20.util.HashUtils.digest(tokenString);
        }
        tokenCache.remove(tokenLookupStr);
        removeRefreshTokenAssociatedWithOAuthTokenFromCache(accessToken);
    }

    void removeRefreshTokenAssociatedWithOAuthTokenFromCache(OAuth20Token cachedToken) {
        CacheUtil cu = new CacheUtil(tokenCache);
        OAuth20Token refreshToken = cu.getRefreshToken(cachedToken);
        if (refreshToken != null && !refreshTokenHasOfflineAccessScope(refreshToken)) {
            tokenCache.remove(refreshToken.getTokenString());
        }
    }

    boolean refreshTokenHasOfflineAccessScope(OAuth20Token refreshToken) {
        String[] scopes = refreshToken.getScope();
        if (scopes == null) {
            return false;
        }
        for (String scope : scopes) {
            if (OIDCConstants.OIDC_DISC_SCOPES_SUPP_OFFLINE_ACC.equals(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a map of all OAuth clients that will be logged out. Each client to log out maps to a list of ID tokens found in the
     * OP's token cache that were issued to that client and user. The ID token claims can be used to build the claims for the
     * respective logout token(s).
     */
    Map<OidcBaseClient, List<OAuth20Token>> getClientsToLogOut(Collection<OAuth20Token> allCachedUserTokens) {
        if (allCachedUserTokens == null || allCachedUserTokens.isEmpty()) {
            return new HashMap<>();
        }
        return getClientToCachedIdTokensMap(allCachedUserTokens);
    }

    /**
     * Determines all clients and associated ID tokens for the user that should be logged out. Finds all ID tokens in the set of
     * cached tokens for the user and adds them to the map to use later when the creating the logout token(s).
     */
    Map<OidcBaseClient, List<OAuth20Token>> getClientToCachedIdTokensMap(Collection<OAuth20Token> allCachedUserTokens) {
        Map<OidcBaseClient, List<OAuth20Token>> cachedIdTokensMap = new HashMap<>();
        OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();

        // Map to avoid having to query the client provider multiple times for the same client
        Map<String, OidcBaseClient> fetchedClients = new HashMap<>();

        for (OAuth20Token cachedToken : allCachedUserTokens) {
            if (OAuth20Constants.ID_TOKEN.equals(cachedToken.getType())) {
                OidcBaseClient client = getClient(fetchedClients, clientProvider, cachedToken);
                if (client == null) {
                    continue;
                }
                if (isValidClientForBackchannelLogout(client)) {
                    addCachedIdTokenToMap(cachedIdTokensMap, client, cachedToken);
                }
            }
        }
        return cachedIdTokensMap;
    }

    @FFDCIgnore(OidcServerException.class)
    boolean isValidClientForBackchannelLogout(OidcBaseClient client) {
        String logoutUri = client.getBackchannelLogoutUri();
        if (logoutUri == null) {
            return false;
        }
        try {
            OidcBaseClientValidator.validateBackchannelLogoutUri(client, logoutUri);
        } catch (OidcServerException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The {0} OAuth client cannot be used for back-channel logout because its back-channel logout URI ({1}) is not valid: {2}",
                         client.getClientId(), logoutUri, e.getErrorDescription());
            }
            return false;
        }
        return true;
    }

    OidcBaseClient getClient(Map<String, OidcBaseClient> fetchedClients, OidcOAuth20ClientProvider clientProvider, OAuth20Token cachedToken) {
        String cachedTokenClientId = cachedToken.getClientId();
        OidcBaseClient client = fetchedClients.get(cachedTokenClientId);
        if (client == null) {
            try {
                client = clientProvider.get(cachedTokenClientId);
                fetchedClients.put(cachedTokenClientId, client);
            } catch (OidcServerException e) {
                Tr.error(tc, "ERROR_RETRIEVING_CLIENT_TO_BUILD_LOGOUT_TOKENS", oidcServerConfig.getProviderId(), cachedTokenClientId, e);
            }
        }
        return client;
    }

    void addCachedIdTokenToMap(Map<OidcBaseClient, List<OAuth20Token>> cachedIdTokensMap, OidcBaseClient client, OAuth20Token cachedToken) {
        List<OAuth20Token> cachedIdTokens = new ArrayList<>();
        if (cachedIdTokensMap.containsKey(client)) {
            cachedIdTokens = cachedIdTokensMap.get(client);
        }
        cachedIdTokens.add(cachedToken);
        cachedIdTokensMap.put(client, cachedIdTokens);
    }

    Map<OidcBaseClient, Set<String>> buildLogoutTokensForClients(Map<OidcBaseClient, List<OAuth20Token>> clientsToLogOut) {
        Map<OidcBaseClient, Set<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, Set<String>>();
        if (clientsToLogOut == null || clientsToLogOut.isEmpty()) {
            return clientsAndLogoutTokens;
        }
        for (Entry<OidcBaseClient, List<OAuth20Token>> clientAndIdTokens : clientsToLogOut.entrySet()) {
            OidcBaseClient client = clientAndIdTokens.getKey();
            Set<String> logoutTokens = buildLogoutTokensForClient(client, clientAndIdTokens.getValue());
            if (logoutTokens != null && !logoutTokens.isEmpty()) {
                clientsAndLogoutTokens.put(client, logoutTokens);
            }
        }
        return clientsAndLogoutTokens;
    }

    @FFDCIgnore(LogoutTokenBuilderException.class)
    Set<String> buildLogoutTokensForClient(OidcBaseClient client, List<OAuth20Token> cachedIdTokens) {
        Set<String> logoutTokens = new HashSet<>();
        for (OAuth20Token cachedIdToken : cachedIdTokens) {
            try {
                String logoutToken = createLogoutTokenForClientFromCachedIdToken(client, cachedIdToken);
                logoutTokens.add(logoutToken);
            } catch (LogoutTokenBuilderException e) {
                if (e.getCause() instanceof IdTokenDifferentIssuerException) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Will not create a logout token for cached ID token " + cachedIdToken + " because the issuer of the token is different");
                    }
                } else {
                    // Log an error but continue trying to build logout tokens based on other cached ID tokens
                    Tr.error(tc, "ERROR_BUILDING_LOGOUT_TOKEN_BASED_ON_ID_TOKEN", client.getClientId(), e);
                }
            }
        }
        return logoutTokens;
    }

    String createLogoutTokenForClientFromCachedIdToken(OidcBaseClient client, OAuth20Token cachedIdToken) throws LogoutTokenBuilderException {
        JwtClaims cachedIdTokenClaims = getClaimsFromIdTokenString(cachedIdToken.getTokenString());
        return createLogoutTokenForClient(client, cachedIdTokenClaims);
    }

    String createLogoutTokenForClient(OidcBaseClient client, JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        try {
            JwtClaims logoutTokenClaims = populateLogoutTokenClaimsFromIdToken(client, idTokenClaims);

            String sharedKey = client.getClientSecret();
            JWTData jwtData = new JWTData(sharedKey, oidcServerConfig, JWTData.TYPE_JWT_TOKEN);
            jwtData.setTypHeader("logout+jwt");

            // When we add support for JWE ID tokens, this will need to be updated to create a JWE logout token as well
            return JwsSigner.getSignedJwt(logoutTokenClaims, oidcServerConfig, jwtData);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_LOGOUT_TOKEN_BASED_ON_ID_TOKEN_CLAIMS", idTokenClaims, client.getClientId(), e);
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    JwtClaims populateLogoutTokenClaimsFromIdToken(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException, LogoutTokenBuilderException {
        JwtClaims logoutTokenClaims = populateLogoutTokenClaims(client, idTokenClaims);

        String subject = idTokenClaims.getSubject();
        if (subject == null || subject.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_MISSING_REQUIRED_CLAIMS", "sub");
            throw new LogoutTokenBuilderException(errorMsg);
        }
        logoutTokenClaims.setSubject(subject);

        String sid = idTokenClaims.getStringClaimValue("sid");
        if (sid != null && !sid.isEmpty()) {
            logoutTokenClaims.setClaim("sid", sid);
        }
        return logoutTokenClaims;
    }

    JwtClaims populateLogoutTokenClaims(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException, LogoutTokenBuilderException {
        JwtClaims logoutTokenClaims = new JwtClaims();
        String issuer = idTokenClaims.getIssuer();
        if (issuer == null || issuer.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_MISSING_REQUIRED_CLAIMS", new Object[] { "iss" });
            throw new LogoutTokenBuilderException(errorMsg);
        }
        logoutTokenClaims.setIssuer(issuer);
        logoutTokenClaims.setAudience(client.getClientId());
        logoutTokenClaims.setIssuedAtToNow();
        logoutTokenClaims.setExpirationTimeMinutesInTheFuture(2);
        logoutTokenClaims.setGeneratedJwtId();

        Map<String, Object> eventsClaim = new HashMap<>();
        eventsClaim.put(EVENTS_MEMBER_NAME, new HashMap<>());
        logoutTokenClaims.setClaim("events", eventsClaim);

        return logoutTokenClaims;
    }

    String getIssuerFromRequest() {
        String issuerIdentifier = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            issuerIdentifier += ":" + port;
        }
        issuerIdentifier += "/oidc/providers/" + oidcServerConfig.getProviderId();
        return issuerIdentifier;
    }

}
