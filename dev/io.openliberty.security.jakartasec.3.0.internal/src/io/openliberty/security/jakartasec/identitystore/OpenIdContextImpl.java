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

import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;
import io.openliberty.security.oidcclientcore.storage.Storage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
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
    private final String state; // TODO: Determine if storage values can be obtained without relying on state.
    private final boolean useSession;

    private RefreshToken refreshToken;
    private Long expiresIn;

    public OpenIdContextImpl(String subjectIdentifier, String tokenType, AccessToken accessToken, IdentityToken identityToken, OpenIdClaims userinfoClaims,
                             JsonObject providerMetadata, String state, boolean useSession) {
        this.subjectIdentifier = subjectIdentifier;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.identityToken = identityToken;
        this.userinfoClaims = userinfoClaims;
        this.providerMetadata = providerMetadata;
        this.state = state;
        this.useSession = useSession;
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getStoredValue(HttpServletRequest request, HttpServletResponse response, String key) {
        T value = null;
        Storage storage = getStorage(request, response);

        if (OpenIdConstant.ORIGINAL_REQUEST.equals(key)) {
            String storageName = OidcStorageUtils.getOriginalReqUrlStorageKey(state);
            value = (T) storage.get(storageName);
        }

        return Optional.of(value);
    }

    private Storage getStorage(HttpServletRequest request, HttpServletResponse response) {
        if (useSession) {
            return new SessionBasedStorage(request);
        } else {
            return new CookieBasedStorage(request, response);
        }
    }

}