/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.tokens;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;
import jakarta.security.enterprise.identitystore.openid.Scope;

public class AccessTokenImpl implements AccessToken, Serializable {

    public static final TraceComponent tc = Tr.register(AccessTokenImpl.class);

    private static final long serialVersionUID = 1L;

    private final String tokenString;
    private final Long expirationTimeInSeconds;
    private final Instant responseGenerationTime;
    private final Long tokenMinValidityInMillis;
    private Map<String, Object> accessTokenClaimsMap;
    private JwtClaims jwtClaims;
    private Type type;

    public AccessTokenImpl(String tokenString, Instant responseGenerationTime, Long expirationTimeInSeconds, Long tokenMinValidityInMillis) {
        this.tokenString = tokenString;
        this.expirationTimeInSeconds = expirationTimeInSeconds;
        this.responseGenerationTime = responseGenerationTime;
        this.tokenMinValidityInMillis = tokenMinValidityInMillis;
        this.accessTokenClaimsMap = Collections.emptyMap();
        jwtClaims = JwtClaims.NONE;
        type = Type.MAC;
    }

    public AccessTokenImpl(String tokenString, Map<String, Object> accessTokenClaimsMap, Instant responseGenerationTime, Long expirationTimeInSeconds,
                           Long tokenMinValidityInMillis) {
        this(tokenString, responseGenerationTime, expirationTimeInSeconds, tokenMinValidityInMillis);
        this.accessTokenClaimsMap = accessTokenClaimsMap == null ? Collections.emptyMap() : accessTokenClaimsMap;
        jwtClaims = accessTokenClaimsMap == null ? JwtClaims.NONE : new JwtClaimsImpl(accessTokenClaimsMap);
        type = Type.BEARER;
    }

    @Override
    public String getToken() {
        return tokenString;
    }

    @Override
    public boolean isJWT() {
        return Type.BEARER.equals(type);
    }

    @Override
    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }

    @Override
    public Map<String, Object> getClaims() {
        if (Type.BEARER.equals(type)) {
            Map<String, Object> result = new HashMap<String, Object>();
            BiConsumer<? super String, ? super Object> action = new CloneClaimsAction(result);
            accessTokenClaimsMap.forEach(action);
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Object getClaim(String key) {
        return accessTokenClaimsMap.get(key);
    }

    @Override
    public Long getExpirationTime() {
        return expirationTimeInSeconds;
    }

    @Override
    public boolean isExpired() {
        Instant expirationInstant = getExpirationInstant();
        Instant nowInstant = Instant.now();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Current time: " + nowInstant + ", expirationInstant: " + expirationInstant + " = when the token response was generated (" + responseGenerationTime
                         + ") + expiration time (" + expirationTimeInSeconds + "), tokenMinValidityInMillis: " + tokenMinValidityInMillis);
            Tr.debug(tc,
                     "Token is considered expired if the current time is after expiration instant, or if the current time + tokenMinValidityInMillis is after the expiration instant");
        }
        return nowInstant.isAfter(expirationInstant) || nowInstant.plusMillis(tokenMinValidityInMillis).isAfter(expirationInstant);
    }

    private Instant getExpirationInstant() {
        Instant expirationInstant = Instant.MIN;
        if (expirationTimeInSeconds != null && !(expirationTimeInSeconds < 0)) {
            expirationInstant = responseGenerationTime.plusMillis(expirationTimeInSeconds * 1000);
        } else if (Type.BEARER.equals(type)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Access token is a bearer token");
            }
            Optional<Instant> expirationOptionalInstant = jwtClaims.getExpirationTime();
            if (expirationOptionalInstant.isPresent()) {
                expirationInstant = expirationOptionalInstant.get();
            }
        }
        return expirationInstant;
    }

    @Override
    public Scope getScope() {
        if (Type.BEARER.equals(type)) {
            Optional<String> optionalScope = jwtClaims.getStringClaim(OpenIdConstant.SCOPE);
            if (optionalScope.isPresent()) {
                return Scope.parse(optionalScope.get());
            }
        }
        return null;
    }

    @Override
    public Type getType() {
        return type;
    }

}
