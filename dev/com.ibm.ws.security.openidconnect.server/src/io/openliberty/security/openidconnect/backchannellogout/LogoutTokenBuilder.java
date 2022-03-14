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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.JwsSigner;
import com.ibm.ws.security.jwt.utils.JwtData;
import com.ibm.ws.security.jwt.utils.JwtDataConfig;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
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

    public Map<OidcBaseClient, List<String>> buildLogoutTokensFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        JwtClaims idTokenClaims = getClaimsFromIdTokenString(idTokenString);
        return buildLogoutTokens(idTokenClaims);
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

    @FFDCIgnore(Exception.class)
    Map<OidcBaseClient, List<String>> buildLogoutTokens(JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        Map<OidcBaseClient, List<String>> clientsAndLogoutTokens = new HashMap<OidcBaseClient, List<String>>();
        try {
            Map<OidcBaseClient, List<IDTokenImpl>> clientsToLogOut = getClientsToLogOut(idTokenClaims);
            if (clientsToLogOut == null || clientsToLogOut.isEmpty()) {
                return new HashMap<OidcBaseClient, List<String>>();
            }
            for (OidcBaseClient client : clientsToLogOut.keySet()) {
                List<IDTokenImpl> cachedIdTokens = clientsToLogOut.get(client);
                for (IDTokenImpl cachedIdToken : cachedIdTokens) {
                    createLogoutTokenForClientFromCachedIdToken(clientsAndLogoutTokens, client, cachedIdToken);
                }
            }
        } catch (Exception e) {
            throw new LogoutTokenBuilderException(e);
        }
        return clientsAndLogoutTokens;
    }

    /**
     * Returns a map of all OAuth clients that will be logged out. Each client to log out maps to a list of ID tokens found in the
     * OP's token cache that were issued to that client. The ID token claims can be used to build the claims for the respective
     * logout token(s).
     */
    Map<OidcBaseClient, List<IDTokenImpl>> getClientsToLogOut(JwtClaims idTokenClaims) throws LogoutTokenBuilderException, MalformedClaimException {
        String user = idTokenClaims.getSubject();

        List<OidcBaseClient> clientsUnderConsideration = getClientsToConsiderLoggingOut(idTokenClaims);
        if (clientsUnderConsideration == null || clientsUnderConsideration.isEmpty()) {
            return new HashMap<>();
        }
        // Find all RPs with active or recently active sessions with the OP. Do this by looking for any ID tokens in the
        // provider's token cache.
        OAuth20EnhancedTokenCache tokenCache = oauth20provider.getTokenCache();
        Collection<OAuth20Token> allCachedUserTokens = tokenCache.getAllUserTokens(user);
        if (allCachedUserTokens == null || allCachedUserTokens.isEmpty()) {
            return new HashMap<>();
        }

        return getClientToCachedIdTokensMap(clientsUnderConsideration, allCachedUserTokens, idTokenClaims);
    }

    /**
     * Returns a list of all OAuth clients that should be considered for logging out. This should be only the client IDs listed
     * in the ID token's audience claim. It is expected that list would be compared to some kind of cache of RPs that have active
     * or recently active sessions with the OP.
     */
    @FFDCIgnore(Exception.class)
    List<OidcBaseClient> getClientsToConsiderLoggingOut(JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        List<OidcBaseClient> clientsToConsiderLoggingOut = new ArrayList<>();
        try {
            OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();
            Collection<OidcBaseClient> clients = clientProvider.getAll();
            // Only clients in the aud claim of the ID token should be considered for logout
            List<String> idTokenAudiences = idTokenClaims.getAudience();
            for (OidcBaseClient client : clients) {
                if (idTokenAudiences.contains(client.getClientId())) {
                    // Only consider clients that have a backchannel_logout_uri configured
                    String logoutUri = client.getBackchannelLogoutUri();
                    if (logoutUri != null) {
                        clientsToConsiderLoggingOut.add(client);
                    }
                }
            }
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLIENTS_TO_LOG_OUT", new Object[] { e });
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
        return clientsToConsiderLoggingOut;
    }

    /**
     * Determines all clients and associated ID tokens for the user that should be logged out. Finds all ID tokens in the set of
     * cached tokens for the user and compares them against the claims in the ID token hint. Any tokens with matching client IDs,
     * sub claims, and sid claims (if present) will be added to the map to use later when the creating the logout token(s).
     */
    Map<OidcBaseClient, List<IDTokenImpl>> getClientToCachedIdTokensMap(List<OidcBaseClient> clientsUnderConsiderationForLogout, Collection<OAuth20Token> allCachedUserTokens,
                                                                        JwtClaims idTokenClaims) throws MalformedClaimException, LogoutTokenBuilderException {
        Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap = new HashMap<>();
        for (OAuth20Token cachedToken : allCachedUserTokens) {
            if (OAuth20Constants.ID_TOKEN.equals(cachedToken.getType())) {
                for (OidcBaseClient client : clientsUnderConsiderationForLogout) {
                    if (isIdTokenWithMatchingClaims(idTokenClaims, cachedToken, client)) {
                        addCachedIdTokenToMap(cachedIdTokensMap, client, (IDTokenImpl) cachedToken);
                    }
                }
            }
        }
        return cachedIdTokensMap;
    }

    @FFDCIgnore(LogoutTokenBuilderException.class)
    boolean isIdTokenWithMatchingClaims(JwtClaims idTokenClaims, OAuth20Token cachedToken, OidcBaseClient client) throws MalformedClaimException, LogoutTokenBuilderException {
        if (!isSameClientId(cachedToken, client)) {
            return false;
        }
        IDTokenImpl cachedIdToken = (IDTokenImpl) cachedToken;
        JwtClaims cachedIdTokenClaims = null;
        try {
            cachedIdTokenClaims = getClaimsFromIdTokenString(cachedIdToken.getTokenString());
        } catch (LogoutTokenBuilderException e) {
            // Don't throw the exception if the issuer simply didn't match the expected issuer value; just return false instead
            if (e.getCause() instanceof IdTokenDifferentIssuerException) {
                return false;
            } else {
                throw e;
            }
        }
        if (!isSameAud(client, cachedIdTokenClaims)) {
            return false;
        }
        if (!isSameSub(idTokenClaims, cachedIdTokenClaims)) {
            return false;
        }
        if (!isSameSid(idTokenClaims, cachedIdTokenClaims)) {
            return false;
        }
        return true;
    }

    boolean isSameClientId(OAuth20Token cachedToken, OidcBaseClient client) {
        if (!client.getClientId().equals(cachedToken.getClientId())) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cached token client ID [" + cachedToken.getClientId() + "] did not match OAuth client ID [" + client.getClientId() + "]");
            }
            return false;
        }
        return true;
    }

    boolean isSameAud(OidcBaseClient client, JwtClaims cachedIdTokenClaims) throws MalformedClaimException {
        List<String> cachedIdTokenAud = cachedIdTokenClaims.getAudience();
        if (cachedIdTokenAud == null || cachedIdTokenAud.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cached token aud claim was null or empty");
            }
            return false;
        }
        String expectedAud = client.getClientId();
        if (!cachedIdTokenAud.contains(expectedAud)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cached token aud claim [" + cachedIdTokenAud + "] did not contain client ID [" + expectedAud + "]");
            }
            return false;
        }
        return true;
    }

    boolean isSameSub(JwtClaims idTokenClaims, JwtClaims cachedIdTokenClaims) throws MalformedClaimException {
        String sub = idTokenClaims.getSubject();
        String cachedIdTokenSub = cachedIdTokenClaims.getSubject();
        if (sub == null && cachedIdTokenSub == null) {
            return true;
        }
        if (sub != null && cachedIdTokenSub != null && sub.equals(cachedIdTokenSub)) {
            return true;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Cached token sub claim [" + cachedIdTokenSub + "] did not match sub claim in ID token hint [" + sub + "]");
        }
        return false;
    }

    boolean isSameSid(JwtClaims idTokenClaims, JwtClaims cachedIdTokenClaims) throws MalformedClaimException {
        String sid = idTokenClaims.getStringClaimValue("sid");
        String cachedIdTokenSid = cachedIdTokenClaims.getStringClaimValue("sid");
        if (sid != null && !sid.isEmpty()) {
            if (cachedIdTokenSid != null && sid.equals(cachedIdTokenSid)) {
                return true;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cached token sid claim [" + cachedIdTokenSid + "] did not match sid claim in ID token hint [" + sid + "]");
            }
            return false;
        } else {
            // ID token hint didn't contain a "sid" claim; ensure the cached ID token doesn't either
            if (cachedIdTokenSid != null && !cachedIdTokenSid.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cached token sid claim [" + cachedIdTokenSid + "] did not match sid claim in ID token hint [" + sid + "]");
                }
                return false;
            }
        }
        return true;
    }

    void addCachedIdTokenToMap(Map<OidcBaseClient, List<IDTokenImpl>> cachedIdTokensMap, OidcBaseClient client, IDTokenImpl cachedToken) {
        List<IDTokenImpl> cachedIdTokens = new ArrayList<>();
        if (cachedIdTokensMap.containsKey(client)) {
            cachedIdTokens = cachedIdTokensMap.get(client);
        }
        cachedIdTokens.add(cachedToken);
        cachedIdTokensMap.put(client, cachedIdTokens);
    }

    void createLogoutTokenForClientFromCachedIdToken(Map<OidcBaseClient, List<String>> clientsAndLogoutTokens, OidcBaseClient client, IDTokenImpl cachedIdToken) throws Exception {
        JwtClaims cachedIdTokenClaims = getClaimsFromIdTokenString(cachedIdToken.getTokenString());
        String logoutToken = createLogoutTokenForClient(client, cachedIdTokenClaims);

        List<String> logoutTokensForClient = new ArrayList<>();
        if (clientsAndLogoutTokens.containsKey(client)) {
            logoutTokensForClient = clientsAndLogoutTokens.get(client);
        }
        logoutTokensForClient.add(logoutToken);
        clientsAndLogoutTokens.put(client, logoutTokensForClient);
    }

    String createLogoutTokenForClient(OidcBaseClient client, JwtClaims idTokenClaims) throws Exception {
        JwtClaims logoutTokenClaims = populateLogoutTokenClaimsFromIdToken(client, idTokenClaims);

        String sharedKey = client.getClientSecret();
        JwtDataConfig jwtDataConfig = new JwtDataConfig(oidcServerConfig.getSignatureAlgorithm(), oidcServerConfig.getJSONWebKey(), sharedKey, oidcServerConfig.getPrivateKey(), oidcServerConfig.getKeyAliasName(), oidcServerConfig.getKeyStoreRef(), JwtData.TYPE_JWT_TOKEN, oidcServerConfig.isJwkEnabled());
        JwtData jwtData = new JwtData(jwtDataConfig);

        // When we add support for JWE ID tokens, this will need to be updated to create a JWE logout token as well
        return JwsSigner.getSignedJwt(logoutTokenClaims, jwtData);
    }

    JwtClaims populateLogoutTokenClaimsFromIdToken(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException, LogoutTokenBuilderException {
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
