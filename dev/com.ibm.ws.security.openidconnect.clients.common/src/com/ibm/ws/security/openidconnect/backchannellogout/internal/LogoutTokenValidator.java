/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutException;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionCache;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionInfo;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionsStore;
import com.ibm.ws.security.openidconnect.jose4j.Jose4jValidator;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.wsspi.ssl.SSLSupport;

@Component(name = "com.ibm.ws.security.openidconnect.backchannellogout.LogoutTokenValidator", service = {}, property = { "service.vendor=IBM" })
public class LogoutTokenValidator {

    private static TraceComponent tc = Tr.register(LogoutTokenValidator.class);

    public static final String EVENTS_MEMBER_NAME = "http://schemas.openid.net/event/backchannel-logout";

    private static SSLSupport SSL_SUPPORT = null;
    private static BackchannelLogoutJtiCache jtiCache = new BackchannelLogoutJtiCache(10 * 60 * 1000);

    private ConvergedClientConfig config = null;
    private Jose4jUtil jose4jUtil = null;

    @Reference
    protected void setSslSupport(SSLSupport sslSupport) {
        SSL_SUPPORT = sslSupport;
    }

    protected void unsetSslSupport() {
        SSL_SUPPORT = null;
    }

    /**
     * Do not use; needed for this to be a valid @Component object.
     */
    public LogoutTokenValidator() {
    }

    public LogoutTokenValidator(ConvergedClientConfig config) {
        this.config = config;
        jose4jUtil = new Jose4jUtil(SSL_SUPPORT);
    }

    /**
     * Valides an OIDC back-channel logout token per https://openid.net/specs/openid-connect-backchannel-1_0.html.
     */
    @FFDCIgnore(Exception.class)
    public JwtClaims validateToken(String logoutTokenString) throws BackchannelLogoutException {
        try {
            JwtContext jwtContext = jose4jUtil.validateJwtStructureAndGetContext(logoutTokenString, config);
            JwtClaims claims = jose4jUtil.validateJwsSignature(jwtContext, config);

            verifyAllRequiredClaimsArePresent(claims);
            verifyIssAudIatExpClaims(claims);
            verifySubAndOrSidPresent(claims);
            verifyEventsClaim(claims);
            verifyNonceClaimNotPresent(claims);
            doOptionalVerificationChecks(claims);

            return claims;
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "BACKCHANNEL_LOGOUT_TOKEN_ERROR", new Object[] { e });
            throw new BackchannelLogoutException(errorMsg, e);
        }
    }

    void verifyAllRequiredClaimsArePresent(JwtClaims claims) throws MalformedClaimException, BackchannelLogoutException {
        List<String> missingClaims = new ArrayList<>();
        String iss = claims.getIssuer();
        if (iss == null) {
            missingClaims.add(Claims.ISSUER);
        }
        List<String> aud = claims.getAudience();
        if (aud == null || aud.isEmpty()) {
            missingClaims.add(Claims.AUDIENCE);
        }
        NumericDate iat = claims.getIssuedAt();
        if (iat == null) {
            missingClaims.add(Claims.ISSUED_AT);
        }
        NumericDate exp = claims.getExpirationTime();
        if (exp == null) {
            missingClaims.add(Claims.EXPIRATION);
        }
        String jti = claims.getJwtId();
        if (jti == null) {
            missingClaims.add(Claims.ID);
        }
        Object events = claims.getClaimValue("events");
        if (events == null) {
            missingClaims.add("events");
        }
        if (!missingClaims.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_MISSING_CLAIMS", new Object[] { missingClaims });
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    /**
     * Validate the iss, aud, iat, and exp Claims in the same way they are validated in ID Tokens.
     */
    void verifyIssAudIatExpClaims(JwtClaims claims) throws IDTokenValidationFailedException, Exception, MalformedClaimException, JWTTokenValidationFailedException {
        Jose4jValidator validator = getJose4jValidator();
        validator.verifyIssForIdToken(claims.getIssuer());
        validator.verifyAudForIdToken(claims.getAudience());
        validator.verifyIatAndExpClaims(claims.getIssuedAt(), claims.getExpirationTime(), claims.getSubject());
    }

    Jose4jValidator getJose4jValidator() {
        return new Jose4jValidator(null, config.getClockSkewInSeconds(), OIDCClientAuthenticatorUtil.getIssuerIdentifier(config), config.getClientId(), config.getSignatureAlgorithm(), null);
    }

    /**
     * Verifies that the Logout Token contains a sub Claim, a sid Claim, or both.
     */
    void verifySubAndOrSidPresent(JwtClaims claims) throws MalformedClaimException, BackchannelLogoutException {
        String sub = claims.getSubject();
        String sid = claims.getClaimValue("sid", String.class);
        if (sub == null && sid == null) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_MISSING_SUB_AND_SID");
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    /**
     * Verify that the Logout Token contains an events Claim whose value is JSON object containing the member name
     * http://schemas.openid.net/event/backchannel-logout.
     */
    @SuppressWarnings({ "unchecked", "unused" })
    @FFDCIgnore({ MalformedClaimException.class, ClassCastException.class })
    void verifyEventsClaim(JwtClaims claims) throws BackchannelLogoutException {
        try {
            Map<String, Object> events = claims.getClaimValue("events", Map.class);
            if (!events.containsKey(EVENTS_MEMBER_NAME)) {
                String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_EVENTS_CLAIM_MISSING_EXPECTED_MEMBER", new Object[] { EVENTS_MEMBER_NAME, events });
                throw new BackchannelLogoutException(errorMsg);
            }
            try {
                // Verify that the value is a JSON object
                Map<String, Object> eventsEntry = (Map<String, Object>) events.get(EVENTS_MEMBER_NAME);
            } catch (ClassCastException e) {
                String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_EVENTS_MEMBER_VALUE_NOT_JSON", new Object[] { EVENTS_MEMBER_NAME });
                throw new BackchannelLogoutException(errorMsg, e);
            }
        } catch (MalformedClaimException e) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_EVENTS_CLAIM_WRONG_TYPE", new Object[] { e.getMessage() });
            throw new BackchannelLogoutException(errorMsg, e);
        }
    }

    /**
     * Verify that the Logout Token does not contain a nonce Claim.
     */
    void verifyNonceClaimNotPresent(JwtClaims claims) throws BackchannelLogoutException {
        Object nonce = claims.getClaimValue("nonce");
        if (nonce != null) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_CONTAINS_NONCE_CLAIM");
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    void doOptionalVerificationChecks(JwtClaims claims) throws MalformedClaimException, BackchannelLogoutException {
        OidcSessionCache oidcSessionCache = config.getOidcSessionCache();

        verifyTokenWithSameJtiNotRecentlyReceived(claims);
        String sub = claims.getSubject();
        if (sub != null) {
            verifySubAndSidClaimsMatchRecentSession(claims, oidcSessionCache);
        } else {
            verifySidClaimMatchesRecentSession(claims, oidcSessionCache);
        }
    }

    /**
     * Verify that another Logout Token with the same jti value has not been recently received.
     */
    void verifyTokenWithSameJtiNotRecentlyReceived(JwtClaims claims) throws MalformedClaimException, BackchannelLogoutException {
        String jti = claims.getJwtId();
        if (jti == null) {
            return;
        }
        String configId = config.getId();
        Object cachedJwtContext = jtiCache.get(jti, configId);
        if (cachedJwtContext != null) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_DUP_JTI", jti, configId);
            throw new BackchannelLogoutException(errorMsg);
        }
        // Logout token with this jti is not in the cache, so put the token data in the cache and allow validation to continue
        long clockSkew = config.getClockSkew();
        jtiCache.put(jti, configId, claims, clockSkew);
    }

    /**
     * Verifies that any sub Logout Token Claim matches the sub Claim in an ID Token issued for the current session or a recent
     * session of this RP with the OP. Also verifies that the iss Logout Token Claim matches the iss Claim in the same session.
     */
    void verifySubAndSidClaimsMatchRecentSession(JwtClaims claims, OidcSessionCache oidcSessionCache) throws MalformedClaimException, BackchannelLogoutException {
        String sub = HashUtils.digest(claims.getSubject());
        Map<String, OidcSessionsStore> subToSessionsMap = oidcSessionCache.getSubMap();
        if (!subToSessionsMap.containsKey(sub)) {
            String errorMsg = Tr.formatMessage(tc, "NO_RECENT_SESSIONS_WITH_CLAIMS", config.getId(), claims.getIssuer(), claims.getSubject(), claims.getStringClaimValue("sid"));
            throw new BackchannelLogoutException(errorMsg);
        }
        OidcSessionsStore sessionDataForSub = subToSessionsMap.get(sub);

        String iss = HashUtils.digest(claims.getIssuer());
        String sid = HashUtils.digest(claims.getStringClaimValue("sid"));
        OidcSessionInfo matchingSession = findSessionMatchingIssAndSid(sessionDataForSub, iss, sid);
        if (matchingSession == null) {
            String errorMsg = Tr.formatMessage(tc, "NO_RECENT_SESSIONS_WITH_CLAIMS", config.getId(), claims.getIssuer(), claims.getSubject(), claims.getStringClaimValue("sid"));
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    OidcSessionInfo findSessionMatchingIssAndSid(OidcSessionsStore sessionDataForSub, String iss, String sid) {
        if (sid != null) {
            // Must find a matching session with the same sid and iss values
            return findSessionMatchingIssAndNonNullSid(sessionDataForSub, iss, sid);
        } else {
            // Without a sid, just ensure there's a session with the same iss value
            return findSessionMatchingOnlyIss(sessionDataForSub, iss);
        }
    }

    OidcSessionInfo findSessionMatchingIssAndNonNullSid(OidcSessionsStore sessionDataForSub, String iss, String sid) {
        OidcSessionInfo matchingSession = sessionDataForSub.getSession(sid);
        if (matchingSession != null) {
            String sessionIss = matchingSession.getIss();
            if (!iss.equals(sessionIss)) {
                // Since the iss value doesn't match, this shouldn't be considered a matching session
                matchingSession = null;
            }
        }
        return matchingSession;
    }

    OidcSessionInfo findSessionMatchingOnlyIss(OidcSessionsStore sessionDataForSub, String iss) {
        List<OidcSessionInfo> sessions = sessionDataForSub.getSessions();
        for (OidcSessionInfo sessionInfo : sessions) {
            String sessionIss = sessionInfo.getIss();
            if (iss.equals(sessionIss)) {
                return sessionInfo;
            }
        }
        return null;
    }

    /**
     * Verify that any sid Logout Token Claim matches the sid Claim in an ID Token issued for the current session or a recent
     * session of this RP with the OP. Also verifies that the iss Logout Token Claim matches the iss Claim in the same session.
     */
    void verifySidClaimMatchesRecentSession(JwtClaims claims, OidcSessionCache oidcSessionCache) throws MalformedClaimException, BackchannelLogoutException {
        String sid = HashUtils.digest(claims.getStringClaimValue("sid"));
        if (sid == null) {
            // Token is not required to contain a sid claim
            return;
        }
        OidcSessionInfo matchingSession = null;
        String iss = HashUtils.digest(claims.getIssuer());
        Map<String, OidcSessionsStore> subToSessionsMap = oidcSessionCache.getSubMap();
        for (Entry<String, OidcSessionsStore> entry : subToSessionsMap.entrySet()) {
            OidcSessionsStore sessionsStore = entry.getValue();
            matchingSession = findSessionMatchingIssAndSid(sessionsStore, iss, sid);
            if (matchingSession != null) {
                break;
            }
        }
        if (matchingSession == null) {
            String errorMsg = Tr.formatMessage(tc, "NO_RECENT_SESSIONS_WITH_CLAIMS", config.getId(), claims.getIssuer(), claims.getSubject(), claims.getStringClaimValue("sid"));
            throw new BackchannelLogoutException(errorMsg);
        }
    }

}
