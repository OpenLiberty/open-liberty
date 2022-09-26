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
package io.openliberty.security.jakartasec.identitystore;

import java.io.StringReader;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementation for Jakarta Security 3.0 {@link OpenIdContext}
 */
public class OpenIdContextImpl implements OpenIdContext {

    private static final long serialVersionUID = 1L;

    private final String subjectIdentifier;
    private final String tokenType;
    private final AccessToken accessToken;
    private final IdentityToken identityToken;
    private final OpenIdClaims userinfoClaims;
    private final JsonObject providerMetadata; // TODO: Store JSON String instead for serialization

    private RefreshToken refreshToken;
    private Long expiresIn;

    public OpenIdContextImpl(String subjectIdentifier, String tokenType, AccessToken accessToken, IdentityToken identityToken, OpenIdClaims userinfoClaims,
                             JsonObject providerMetadata) {
        this.subjectIdentifier = subjectIdentifier;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.identityToken = identityToken;
        this.userinfoClaims = userinfoClaims;
        this.providerMetadata = providerMetadata;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String getSubject() {
        return subjectIdentifier;
    }

    @Override
    public String getTokenType() {
        return tokenType;
    }

    @Override
    public AccessToken getAccessToken() {
        return accessToken;
    }

    @Override
    public IdentityToken getIdentityToken() {
        return identityToken;
    }

    @Override
    public Optional<RefreshToken> getRefreshToken() {
        if (refreshToken != null) {
            return Optional.of(refreshToken);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Long> getExpiresIn() {
        if (expiresIn != null) {
            return Optional.of(expiresIn);
        }
        return Optional.empty();
    }

    @Override
    public JsonObject getClaimsJson() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpenIdClaims getClaims() {
        return userinfoClaims;
    }

    @Override
    public JsonObject getProviderMetadata() {
        // Clone providerMetadata before returning it to avoid modifications.
        return Json.createReader(new StringReader(providerMetadata.toString())).readObject();
    }

    @Override
    public <T> Optional<T> getStoredValue(HttpServletRequest request, HttpServletResponse response, String key) {
        // TODO: Get value from Storage subsystem.
        return null;
    }

}