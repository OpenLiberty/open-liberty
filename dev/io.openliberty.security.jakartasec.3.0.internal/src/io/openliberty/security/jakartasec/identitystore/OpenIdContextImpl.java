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

import java.util.Optional;

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

    private static final long serialVersionUID = 1L; // TO-DO, added due to warning, do we need a generated UID?

    @Override
    public AccessToken getAccessToken() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpenIdClaims getClaims() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<Long> getExpiresIn() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public IdentityToken getIdentityToken() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<RefreshToken> getRefreshToken() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public String getSubject() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTokenType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject getProviderMetadata() {
        return null;
    }

    @Override
    public JsonObject getClaimsJson() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional getStoredValue(HttpServletRequest request, HttpServletResponse response, String key) {
        return null;
    }

}