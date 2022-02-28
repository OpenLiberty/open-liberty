/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.JwsSigner;
import com.ibm.ws.security.jwt.utils.JwtData;
import com.ibm.ws.security.jwt.utils.JwtDataConfig;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
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

    public Map<String, String> buildLogoutTokens() throws LogoutTokenBuilderException {
        return buildLogoutTokens(null);
    }

    public Map<String, String> buildLogoutTokensFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        JwtClaims idTokenClaims = getClaimsFromIdTokenString(idTokenString);
        return buildLogoutTokens(idTokenClaims);
    }

    @FFDCIgnore(Exception.class)
    Map<String, String> buildLogoutTokens(JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        Map<String, String> clientsAndLogoutTokens = new HashMap<String, String>();
        try {
            List<OidcBaseClient> clientsToLogOut = getClientsToLogOut(idTokenClaims);
            if (clientsToLogOut == null || clientsToLogOut.isEmpty()) {
                return new HashMap<String, String>();
            }
            for (OidcBaseClient client : clientsToLogOut) {
                String logoutToken = createLogoutTokenForClient(client, idTokenClaims);
                clientsAndLogoutTokens.put(client.getClientId(), logoutToken);
            }
        } catch (Exception e) {
            throw new LogoutTokenBuilderException(e);
        }
        return clientsAndLogoutTokens;
    }

    @FFDCIgnore(Exception.class)
    JwtClaims getClaimsFromIdTokenString(String idTokenString) throws LogoutTokenBuilderException {
        try {
            JwtContext jwtContext = Jose4jUtil.parseJwtWithoutValidation(idTokenString);
            return jwtContext.getJwtClaims();
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_ERROR_GETTING_CLAIMS_FROM_ID_TOKEN", new Object[] { e });
            throw new LogoutTokenBuilderException(errorMsg, e);
        }
    }

    List<OidcBaseClient> getClientsToLogOut(JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        List<OidcBaseClient> clientsUnderConsideration = getClientsToConsiderLoggingOut(idTokenClaims);
        // TODO
        // Find all RPs with active or recently active sessions with the OP
        return clientsUnderConsideration;
    }

    /**
     * Returns a list of all OAuth clients that should be considered for logging out. If there are no ID token claims provided,
     * this returns all OAuth clients registered with the OP. If there are ID token claims, this returns only the client IDs
     * listed in the audience claim. It is expected that list would be compared to some kind of cache of RPs that have active
     * or recently active sessions with the OP.
     */
    @FFDCIgnore(Exception.class)
    List<OidcBaseClient> getClientsToConsiderLoggingOut(JwtClaims idTokenClaims) throws LogoutTokenBuilderException {
        List<OidcBaseClient> clientsToConsiderLoggingOut = new ArrayList<>();
        try {
            OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();
            Collection<OidcBaseClient> clients = clientProvider.getAll();
            if (idTokenClaims == null) {
                // All registered clients should be considered for logout
                clientsToConsiderLoggingOut.addAll(clients);
            } else {
                // Only clients in the aud claim of the ID token should be considered for logout
                for (OidcBaseClient client : clients) {
                    List<String> idTokenAudiences = idTokenClaims.getAudience();
                    if (idTokenAudiences.contains(client.getClientId())) {
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

    String createLogoutTokenForClient(OidcBaseClient client, JwtClaims idTokenClaims) throws Exception {
        JwtClaims logoutTokenClaims = populateLogoutTokenClaims(client, idTokenClaims);

        String sharedKey = client.getClientSecret();
        JwtDataConfig jwtDataConfig = new JwtDataConfig(oidcServerConfig.getSignatureAlgorithm(), oidcServerConfig.getJSONWebKey(), sharedKey, oidcServerConfig.getPrivateKey(), oidcServerConfig.getKeyAliasName(), oidcServerConfig.getKeyStoreRef(), JwtData.TYPE_JWT_TOKEN, oidcServerConfig.isJwkEnabled());
        JwtData jwtData = new JwtData(jwtDataConfig);

        // When we add support for JWE ID tokens, this will need to be updated to create a JWE logout token as well
        return JwsSigner.getSignedJwt(logoutTokenClaims, jwtData);
    }

    JwtClaims populateLogoutTokenClaims(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException {
        JwtClaims logoutTokenClaims = new JwtClaims();
        if (idTokenClaims != null) {
            logoutTokenClaims = populateLogoutTokenClaimsFromIdToken(client, idTokenClaims);
        } else {
            logoutTokenClaims = populateLogoutTokenClaims(client);
        }
        return logoutTokenClaims;
    }

    JwtClaims populateLogoutTokenClaimsFromIdToken(OidcBaseClient client, JwtClaims idTokenClaims) throws MalformedClaimException {
        JwtClaims logoutTokenClaims = populateLogoutTokenClaims(client);
        // TODO - ensure we end up setting at least sub or sid
        String sub = idTokenClaims.getSubject();
        if (sub != null) {
            logoutTokenClaims.setSubject(sub);
        }
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

        // TODO
        // Set sid

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
