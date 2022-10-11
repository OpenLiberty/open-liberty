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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;
import io.openliberty.security.oidcclientcore.storage.Storage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
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

    private static final TraceComponent tc = Tr.register(OpenIdContextImpl.class);

    private final String subjectIdentifier;
    private final String tokenType;
    private final AccessToken accessToken;
    private final IdentityToken identityToken;
    private final OpenIdClaims userinfoClaims;
    private JsonObject userinfoClaimsAsJson = null;
    private final JsonObject providerMetadata; // TODO: Store JSON String instead for serialization
    private final String state; // TODO: Determine if storage values can be obtained without relying on state.
    private final boolean useSession;
    private final String clientId;

    private RefreshToken refreshToken;
    private Long expiresIn;

    public OpenIdContextImpl(String subjectIdentifier, String tokenType, AccessToken accessToken, IdentityToken identityToken, OpenIdClaims userinfoClaims,
                             JsonObject providerMetadata, String state, boolean useSession, String clientId) {
        this.subjectIdentifier = subjectIdentifier;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.identityToken = identityToken;
        this.userinfoClaims = userinfoClaims;
        this.providerMetadata = providerMetadata;
        this.state = state;
        this.useSession = useSession;
        this.clientId = clientId;
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
        if (userinfoClaimsAsJson == null) {
            userinfoClaimsAsJson = createClaimsJson();
        }
        return userinfoClaimsAsJson;
    }

    /**
     * Create a JsonObject with the data available in the OpenIdClaims object.
     *
     * @return
     */
    private JsonObject createClaimsJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (userinfoClaims == null) {
            return null;
        }

        try {
            if (userinfoClaims.getSubject() != null && !userinfoClaims.getSubject().isEmpty()) {
                builder.add(OpenIdConstant.SUBJECT_IDENTIFIER, userinfoClaims.getSubject());
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "JAKARTASEC_WARNING_MISSING_SUBJECT_CLAIMS", new Object[] { clientId });
                }
            }
        } catch (IllegalArgumentException iae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAKARTASEC_WARNING_MISSING_SUBJECT_CLAIMS", new Object[] { clientId });
            }
        }
        if (userinfoClaims.getAddress().isPresent()) {
            builder.add(OpenIdConstant.ADDRESS, userinfoClaims.getAddress().get());
        }
        if (userinfoClaims.getBirthdate().isPresent()) {
            builder.add(OpenIdConstant.BIRTHDATE, userinfoClaims.getBirthdate().get());
        }
        if (userinfoClaims.getEmail().isPresent()) {
            builder.add(OpenIdConstant.EMAIL, userinfoClaims.getEmail().get());
        }
        if (userinfoClaims.getEmailVerified().isPresent()) {
            builder.add(OpenIdConstant.EMAIL_VERIFIED, userinfoClaims.getEmailVerified().get());
        }
        if (userinfoClaims.getFamilyName().isPresent()) {
            builder.add(OpenIdConstant.FAMILY_NAME, userinfoClaims.getFamilyName().get());
        }
        if (userinfoClaims.getGender().isPresent()) {
            builder.add(OpenIdConstant.GENDER, userinfoClaims.getGender().get());
        }
        if (userinfoClaims.getGivenName().isPresent()) {
            builder.add(OpenIdConstant.GIVEN_NAME, userinfoClaims.getGivenName().get());
        }
        if (userinfoClaims.getLocale().isPresent()) {
            builder.add(OpenIdConstant.LOCALE, userinfoClaims.getLocale().get());
        }
        if (userinfoClaims.getMiddleName().isPresent()) {
            builder.add(OpenIdConstant.MIDDLE_NAME, userinfoClaims.getMiddleName().get());
        }
        if (userinfoClaims.getName().isPresent()) {
            builder.add(OpenIdConstant.NAME, userinfoClaims.getName().get());
        }
        if (userinfoClaims.getNickname().isPresent()) {
            builder.add(OpenIdConstant.NICKNAME, userinfoClaims.getNickname().get());
        }
        if (userinfoClaims.getPhoneNumber().isPresent()) {
            builder.add(OpenIdConstant.PHONE_NUMBER, userinfoClaims.getPhoneNumber().get());
        }
        if (userinfoClaims.getPhoneNumberVerified().isPresent()) {
            builder.add(OpenIdConstant.PHONE_NUMBER_VERIFIED, userinfoClaims.getPhoneNumberVerified().get());
        }
        if (userinfoClaims.getPicture().isPresent()) {
            builder.add(OpenIdConstant.PICTURE, userinfoClaims.getPicture().get());
        }
        if (userinfoClaims.getPreferredUsername().isPresent()) {
            builder.add(OpenIdConstant.PREFERRED_USERNAME, userinfoClaims.getPreferredUsername().get());
        }
        if (userinfoClaims.getProfile().isPresent()) {
            builder.add(OpenIdConstant.PROFILE, userinfoClaims.getProfile().get());
        }
        if (userinfoClaims.getUpdatedAt().isPresent()) {
            builder.add(OpenIdConstant.UPDATED_AT, userinfoClaims.getUpdatedAt().get());
        }
        if (userinfoClaims.getWebsite().isPresent()) {
            builder.add(OpenIdConstant.WEBSITE, userinfoClaims.getWebsite().get());
        }
        if (userinfoClaims.getZoneinfo().isPresent()) {
            builder.add(OpenIdConstant.ZONEINFO, userinfoClaims.getZoneinfo().get());
        }

        JsonObject json = builder.build();
        return json;
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