/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
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
import com.ibm.ws.security.oauth20.plugins.jose4j.JWTData;
import com.ibm.ws.security.oauth20.plugins.jose4j.JwsSigner;
import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutException;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.server.plugins.IDTokenImpl;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class LogoutTokenBuilder {

    private static TraceComponent tc = Tr.register(LogoutTokenBuilder.class);

    public static final String EVENTS_MEMBER_NAME = "http://schemas.openid.net/event/backchannel-logout";

    private final HttpServletRequest request;
    private final OidcServerConfig oidcServerConfig;
    private final OAuth20Provider oauth20provider;

    public LogoutTokenBuilder(HttpServletRequest request, OidcServerConfig oidcServerConfig) {
        this.request = request;
        this.oidcServerConfig = oidcServerConfig;
        this.oauth20provider = getOAuth20Provider(oidcServerConfig);
    }

    OAuth20Provider getOAuth20Provider(OidcServerConfig oidcServerConfig) {
        String oauthProviderName = oidcServerConfig.getOauthProviderName();
        return ProvidersService.getOAuth20Provider(oauthProviderName);
    }

    public Map<OidcBaseClient, Set<String>> buildLogoutTokensFromUserName(String user) {
        return buildLogoutTokensForUser(user);
    }

    public Map<OidcBaseClient, Set<String>> buildLogoutTokensFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        JwtClaims idTokenClaims = getClaimsFromIdTokenString(idTokenString);
        try {
            return buildLogoutTokensForUser(idTokenClaims.getSubject());
        } catch (MalformedClaimException e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN", e);
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    @FFDCIgnore(Exception.class)
    JwtClaims getClaimsFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        try {
            JwtContext jwtContext = Jose4jUtil.parseJwtWithoutValidation(idTokenString);
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
        String expectedIssuer = getIssuer();
        if (!expectedIssuer.equals(issuerClaim)) {
            String errorMsg = Tr.formatMessage(tc, "ID_TOKEN_ISSUER_NOT_THIS_OP", new Object[] { issuerClaim, expectedIssuer, oidcServerConfig.getProviderId() });
            throw new IdTokenDifferentIssuerException(errorMsg);
        }
    }

    Map<OidcBaseClient, Set<String>> buildLogoutTokensForUser(String user) {
        Map<OidcBaseClient, List<IDTokenImpl>> clientsToLogOut = getClientsToLogOut(user);
        return buildLogoutTokensForClients(clientsToLogOut);
    }

    /**
     * Returns a map of all OAuth clients that will be logged out. Each client to log out maps to a list of ID tokens found in the
     * OP's token cache that were issued to that client and user. The ID token claims can be used to build the claims for the
     * respective logout token(s).
     */
    Map<OidcBaseClient, List<IDTokenImpl>> getClientsToLogOut(String user) {
        Collection<OAuth20Token> allCachedUserTokens = getAllCachedUserTokens(user);
        if (allCachedUserTokens == null || allCachedUserTokens.isEmpty()) {
            return new HashMap<>();
        }
        return getClientToCachedIdTokensMap(allCachedUserTokens);
    }

    Collection<OAuth20Token> getAllCachedUserTokens(String user) {
        OAuth20EnhancedTokenCache tokenCache = oauth20provider.getTokenCache();
        return tokenCache.getAllUserTokens(user);
    }

    /**
     * Determines all clients and associated ID tokens for the user that should be logged out. Finds all ID tokens in the set of
     * cached tokens for the user and adds them to the map to use later when the creating the logout token(s).
     */
    Map<OidcBaseClient, List<IDTokenImpl>> getClientToCachedIdTokensMap(Collection<OAuth20Token> allCachedUserTokens) {
        Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap = new HashMap<>();
        OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();

        // Map to avoid having to query the client provider multiple times for the same client
        Map<String, OidcBaseClient> fetchedClients = new HashMap<>();

        for (OAuth20Token cachedToken : allCachedUserTokens) {
            if (OAuth20Constants.ID_TOKEN.equals(cachedToken.getType())) {
                OidcBaseClient client = getClient(fetchedClients, clientProvider, cachedToken);
                if (client == null) {
                    continue;
                }
                // Only log out clients that have a backchannel_logout_uri configured
                String logoutUri = client.getBackchannelLogoutUri();
                if (logoutUri != null) {
                    addCachedIdTokenToMap(cachedIdTokensMap, client, (IDTokenImpl) cachedToken);
                }
            }
        }
        return cachedIdTokensMap;
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

    void addCachedIdTokenToMap(Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap, OidcBaseClient client, IDTokenImpl cachedToken) {
        List<IDTokenImpl> cachedIdTokens = new ArrayList<>();
        if (cachedIdTokensMap.containsKey(client)) {
            cachedIdTokens = cachedIdTokensMap.get(client);
        }
        cachedIdTokens.add(cachedToken);
        cachedIdTokensMap.put(client, cachedIdTokens);
    }

    Map<OidcBaseClient, Set<String>> buildLogoutTokensForClients(Map<OidcBaseClient, List<IDTokenImpl>> clientsToLogOut) {
        Map<OidcBaseClient, Set<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, Set<String>>();
        if (clientsToLogOut == null || clientsToLogOut.isEmpty()) {
            return clientsAndLogoutTokens;
        }
        for (Entry<OidcBaseClient, List<IDTokenImpl>> clientAndIdTokens : clientsToLogOut.entrySet()) {
            OidcBaseClient client = clientAndIdTokens.getKey();
            Set<String> logoutTokens = buildLogoutTokensForClient(client, clientAndIdTokens.getValue());
            if (logoutTokens != null && !logoutTokens.isEmpty()) {
                clientsAndLogoutTokens.put(client, logoutTokens);
            }
        }
        return clientsAndLogoutTokens;
    }

    @FFDCIgnore(LogoutTokenBuilderException.class)
    Set<String> buildLogoutTokensForClient(OidcBaseClient client, List<IDTokenImpl> cachedIdTokens) {
        Set<String> logoutTokens = new HashSet<>();
        for (IDTokenImpl cachedIdToken : cachedIdTokens) {
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

    String createLogoutTokenForClientFromCachedIdToken(OidcBaseClient client, IDTokenImpl cachedIdToken) throws LogoutTokenBuilderException {
        JwtClaims cachedIdTokenClaims = getClaimsFromIdTokenString(cachedIdToken.getTokenString());
        return createLogoutTokenForClient(client, cachedIdTokenClaims);
    }

    String createLogoutTokenForClient(OidcBaseClient client, JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        try {
            JwtClaims logoutTokenClaims = populateLogoutTokenClaimsFromIdToken(client, idTokenClaims);

            String sharedKey = client.getClientSecret();
            JWTData jwtData = new JWTData(sharedKey, oidcServerConfig, JWTData.TYPE_JWT_TOKEN);

            // When we add support for JWE ID tokens, this will need to be updated to create a JWE logout token as well
            return JwsSigner.getSignedJwt(logoutTokenClaims, oidcServerConfig, jwtData);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_LOGOUT_TOKEN_BASED_ON_ID_TOKEN_CLAIMS", idTokenClaims, client.getClientId(), e);
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    JwtClaims populateLogoutTokenClaimsFromIdToken(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException {
        JwtClaims logoutTokenClaims = populateLogoutTokenClaims(client);

        logoutTokenClaims.setSubject(idTokenClaims.getSubject());

        String sid = idTokenClaims.getStringClaimValue("sid");
        if (sid != null && !sid.isEmpty()) {
            logoutTokenClaims.setClaim("sid", sid);
        }
        return logoutTokenClaims;
    }

    JwtClaims populateLogoutTokenClaims(OidcBaseClient client) {
        JwtClaims logoutTokenClaims = new JwtClaims();
        logoutTokenClaims.setIssuer(getIssuer());
        logoutTokenClaims.setAudience(client.getClientId());
        logoutTokenClaims.setIssuedAtToNow();
        logoutTokenClaims.setGeneratedJwtId();

        Map<String, Object> eventsClaim = new HashMap<>();
        eventsClaim.put(EVENTS_MEMBER_NAME, new HashMap<>());
        logoutTokenClaims.setClaim("events", eventsClaim);

        return logoutTokenClaims;
    }

    String getIssuer() {
        String issuerIdentifier = oidcServerConfig.getIssuerIdentifier();
        if (issuerIdentifier != null && !issuerIdentifier.isEmpty()) {
            return issuerIdentifier;
        }
        issuerIdentifier = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            issuerIdentifier += ":" + port;
        }
        String requestUri = request.getRequestURI();
        requestUri = requestUri.substring(0, requestUri.lastIndexOf("/"));
        issuerIdentifier += requestUri;
        return issuerIdentifier;
    }

}
