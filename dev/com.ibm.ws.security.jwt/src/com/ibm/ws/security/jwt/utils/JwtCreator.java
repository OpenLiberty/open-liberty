/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.internal.ClaimsImpl;
import com.ibm.ws.security.jwt.internal.JwtTokenException;
import com.ibm.ws.security.jwt.registry.RegistryClaims;

/**
 *
 */
public class JwtCreator {
    // private static TraceComponent tc = Tr.register(JwtCreator.class,
    // TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String JTI_CLAIM = "jti"; // idtoken

    public static class JwtResult {
        private final String compact;
        private Map<String, Object> header;
        private final Claims claims = new ClaimsImpl();

        public JwtResult(String compact, JwtClaims claims) throws Exception {
            this.compact = compact;
            initializeHeader();
            initializeClaims(claims);
        }

        public String getCompact() {
            return compact;
        }

        public Map<String, Object> getHeader() {
            return header;
        }

        public Claims getClaims() {
            return claims;
        }

        private void initializeClaims(JwtClaims jwtClaims) throws Exception {
            Map<String, Object> claimsMap = jwtClaims.getClaimsMap();
            if (claimsMap != null && !claimsMap.isEmpty()) {
                claims.putAll(claimsMap);
            }
        }

        private void initializeHeader() throws Exception {
            if (compact == null) {
                header = null;
            }
            String[] parts = JwtUtils.splitTokenString(compact);
            String headerString = JwtUtils.fromBase64ToJsonString(parts[0]);
            header = JwtUtils.claimsFromJson(headerString);
        }
    }

    @FFDCIgnore({ Exception.class })
    public static JwtResult createJwt(JwtData jwtData, Claims jwtclaims) throws JwtTokenException {
        boolean bJwt = jwtData.isJwt();

        try {
            String jwt = null;
            // Create the Claims, which will be the content of the JWT
            JwtClaims claims = new JwtClaims();
            if (bJwt) {
                claims = populateClaims(jwtData, jwtclaims);
            }

            // A JWT is a JWS and/or a JWE with JSON claims as the payload.
            jwt = JwsSigner.getSignedJwt(claims, jwtData);

            if (jwtData.isJwe()) {
                jwt = JweHelper.createJweString(jwt, jwtData);
            }

            return new JwtResult(jwt, claims);
        } catch (Exception e) {
            JwtTokenException jte = JwtTokenException.newInstance(false, "JWT_CREATE_FAIL", new Object[] { e.getLocalizedMessage() });
            jte.initCause(e);
            throw jte;
        }
    }

    public static JwtClaims populateClaims(JwtData jwtData, Claims jwtclaims) throws MalformedClaimException {
        JwtClaims claims = new JwtClaims();
        String token_type = (String) jwtclaims.get(Claims.TOKEN_TYPE);
        if (token_type != null) {
            claims.setClaim(Claims.TOKEN_TYPE, token_type);
        }
        List<String> audiences = jwtclaims.getAudience();
        //claims.setClaim("token_type", "Bearer"); // JWT
        // only
        if (audiences != null && audiences.size() > 0) {
            claims.setAudience(audiences); // to whom the token is
                                           // intended
                                           // to be sent
        }

        String username = jwtclaims.getSubject();
        if (username != null) {
            claims.setSubject(username); // the subject/principal
            // add if there are any extra claims
            if (jwtData.getConfig().getClaims() != null) {
                addCustomClaims(claims, jwtData.getConfig(), username);
            }
        }

        // if (scope != null && scope.length > 0) { // JWT only
        // claims.setStringListClaim("scope", scope);
        // }

        // jwtclaims.getAllClaims().entrySet();
        for (Map.Entry<String, Object> e : jwtclaims.getAllClaims().entrySet()) {
            if (e.getKey() != JwtUtils.AUDIENCE && e.getKey() != JwtUtils.EXPIRATION && e.getKey() != JwtUtils.ID && e.getKey() != JwtUtils.ISSUER && e.getKey() != JwtUtils.ISSUED_AT && e.getKey() != JwtUtils.NOT_BEFORE) {
                claims.setClaim(e.getKey(), e.getValue());
            }
        }
        String jti = null;
        jti = jwtclaims.getJwtId();
        if (jti != null) { // addOptionalClaims for
                           // IDToken and jwt too
                           // 224198
            claims.setClaim(JTI_CLAIM, jti);
        }
        String issuer = jwtclaims.getIssuer(); // who
        if (issuer != null) {
            claims.setIssuer(jwtclaims.getIssuer()); // who
        }

        // IAT, EXP, NBF
        long validForInSeconds = jwtData.getConfig().getValidTime();
        long expTimeInSeconds = jwtclaims.getExpiration();
        long issueTimeInSeconds = jwtclaims.getIssuedAt();
        long timeInSeconds = System.currentTimeMillis() / 1000;

        if (expTimeInSeconds > 0) {
            // if it is a positive number, then it must have been set by the user already
            claims.setExpirationTime(NumericDate.fromSeconds(expTimeInSeconds));
        } else if (expTimeInSeconds == -2) {
            // otherwise, set the expiration to based on the token valid time specified in the config
            claims.setExpirationTime(NumericDate.fromSeconds(timeInSeconds + validForInSeconds));
        }
        // issue time should be same as the current time or before?
        // assuming we are not issuing tokens with the future time
        if (issueTimeInSeconds > 0) {
            claims.setIssuedAt(NumericDate.fromSeconds(issueTimeInSeconds));
        } else if (issueTimeInSeconds == -2) {
            claims.setIssuedAt(NumericDate.fromSeconds(timeInSeconds));
        }
        // claims.setExpirationTimeMinutesInTheFuture(((float)lifetimeSeconds)/60);
        // // time when the token will expire (10 minutes from now)

        // claims.setIssuedAtToNow(); // when the token was
        // issued/created
        // (now)
        // claims.setNotBeforeMinutesInThePast(2); // time before which
        // the
        // token is not yet valid (2 minutes ago) 224202
        // NBF is optional
        long notBeforeInSeconds = jwtclaims.getNotBefore();
        if (notBeforeInSeconds > 0) { // TODO maybe we need to do some extra checking for nbf
            claims.setNotBefore(NumericDate.fromSeconds(notBeforeInSeconds));
        }

        // Sets nbf claim to elapsedNbf since token issued
        long elapsedNbfInSeconds = jwtData.getConfig().getElapsedNbfTime();
        if (elapsedNbfInSeconds >= 0) {
            claims.setNotBefore(NumericDate.fromSeconds(claims.getIssuedAt().getValue() + elapsedNbfInSeconds));
        }

        return claims;
    }

    private static void addCustomClaims(JwtClaims claims, JwtConfig jwtConfig, String username) {

        Map<String, Object> customMap = null;
        RegistryClaims regClaims = new RegistryClaims(username);
        customMap = regClaims.fetchExtraClaims(jwtConfig);
        Set<Map.Entry<String, Object>> entries = customMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            claims.setClaim(entry.getKey(), entry.getValue());
        }
    }
}
