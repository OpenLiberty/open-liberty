/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.JwtClaims;

public class IdentityTokenImpl implements IdentityToken, Serializable {

    private static final long serialVersionUID = 1L;

    private final String tokenString;
    private final Map<String, Object> idTokenClaimsMap;
    private final JwtClaims jwtClaims;
    private final Long tokenMinValidityInMillis;

    public IdentityTokenImpl(String tokenString, Map<String, Object> idTokenClaimsMap, Long tokenMinValidityInMillis) {
        this.tokenString = tokenString;
        this.idTokenClaimsMap = idTokenClaimsMap;
        jwtClaims = new JwtClaimsImpl(idTokenClaimsMap);
        this.tokenMinValidityInMillis = tokenMinValidityInMillis;
    }

    @Override
    public String getToken() {
        return tokenString;
    }

    @Override
    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }

    @Override
    public boolean isExpired() {
        Instant expirationInstant = jwtClaims.getExpirationTime().get();
        Instant nowInstant = Instant.now();
        return nowInstant.isAfter(expirationInstant) || nowInstant.plusMillis(tokenMinValidityInMillis).isAfter(expirationInstant);
    }

    @Override
    public Map<String, Object> getClaims() {
        Map<String, Object> result = new HashMap<String, Object>();
        BiConsumer<? super String, ? super Object> action = new CloneClaimsAction(result);
        idTokenClaimsMap.forEach(action);
        return result;
    }

}
