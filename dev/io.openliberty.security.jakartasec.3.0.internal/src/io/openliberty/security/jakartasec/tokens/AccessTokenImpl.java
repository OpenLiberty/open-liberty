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
package io.openliberty.security.jakartasec.tokens;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;
import jakarta.security.enterprise.identitystore.openid.Scope;

public class AccessTokenImpl implements AccessToken, Serializable {

    private static final long serialVersionUID = 1L;

    private final String tokenString;
    private final Long expirationTimeInSeconds;
    private final Instant responseGenerationTime;
    private final Long tokenMinValidityInMillis;
    private final Type type;

    public AccessTokenImpl(String tokenString, Instant responseGenerationTime, Long expirationTimeInSeconds, Long tokenMinValidityInMillis) {
        this.tokenString = tokenString;
        this.expirationTimeInSeconds = expirationTimeInSeconds;
        this.responseGenerationTime = responseGenerationTime;
        this.tokenMinValidityInMillis = tokenMinValidityInMillis;
        // TODO: Detect if a JWT access token
        type = Type.MAC;
    }

    @Override
    public String getToken() {
        return tokenString;
    }

    @Override
    public boolean isJWT() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JwtClaims getJwtClaims() {
        if (Type.BEARER.equals(type)) {
            // TODO: Create JwtClaims if a JWT access token.
            return null;
        } else {
            return JwtClaims.NONE;
        }
    }

    @Override
    public Map<String, Object> getClaims() {
        if (Type.BEARER.equals(type)) {
            // TODO: Create claims map if a JWT access token.
            return null;
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Object getClaim(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getExpirationTime() {
        return expirationTimeInSeconds;
    }

    @Override
    public boolean isExpired() {
        if (expirationTimeInSeconds != null && expirationTimeInSeconds > 0 && tokenMinValidityInMillis > 0) {
            Instant expirationInstant = responseGenerationTime.plusMillis(expirationTimeInSeconds * 1000);
            Instant nowInstant = Instant.now();
            return nowInstant.isAfter(expirationInstant) || nowInstant.plusMillis(tokenMinValidityInMillis).isAfter(expirationInstant);
        } else if (Type.BEARER.equals(type)) {
            // TODO: Check expiration using 'exp' claim. jwtClaims.isExpired(Clock.systemUTC(), true, Duration.ofMillis(tokenMinValidityInMillis));
        }

        return false;
    }

    @Override
    public Scope getScope() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Type getType() {
        // TODO Auto-generated method stub
        return type;
    }

}
