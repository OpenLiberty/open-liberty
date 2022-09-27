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

import java.util.Map;
import java.util.Set;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;

import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.jakartasec.tokens.AccessTokenImpl;
import io.openliberty.security.jakartasec.tokens.IdentityTokenImpl;
import io.openliberty.security.jakartasec.tokens.RefreshTokenImpl;
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

/**
 *
 */
public class OidcIdentityStore implements IdentityStore {

    public OidcIdentityStore() {

    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        // Use OidcTokensCredential to validate
        if (!(credential instanceof OidcTokensCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        } else if (credential.isValid()) {
            OidcTokensCredential castCredential = (OidcTokensCredential) credential;
            TokenResponse tokenResponse = castCredential.getTokenResponse();
            Client client = castCredential.getClient();
            if (tokenResponse != null && client != null) {
                try {
                    JwtClaims idTokenClaims = client.validate(tokenResponse, castCredential.getRequest(), castCredential.getResponse());

                    OidcClientConfig oidcClientConfig = client.getOidcClientConfig();
                    long tokenMinValidityInMillis = oidcClientConfig.getTokenMinValidity();
                    AccessToken accessToken = createAccessTokenFromTokenResponse(tokenMinValidityInMillis, tokenResponse);
                    IdentityToken identityToken = createIdentityTokenFromTokenResponse(tokenMinValidityInMillis, tokenResponse, idTokenClaims);
                    OpenIdClaims userinfoClaims = createOpenIdClaimsFromUserInfoResponse(); // TODO: Use this interface when creating the CredentialValidationResult

                    CredentialValidationResult credentialValidationResult = createCredentialValidationResult(client.getOidcClientConfig(), accessToken, idTokenClaims);

                    JsonObject providerMetadata = getProviderMetadataAsJsonObject();

                    OpenIdContext openIdContext = createOpenIdContext(credentialValidationResult.getCallerUniqueId(), tokenResponse, accessToken, identityToken, userinfoClaims,
                                                                      providerMetadata);

                    castCredential.setOpenIdContext(openIdContext);

                    return credentialValidationResult;
                } catch (Exception e) {
                    return CredentialValidationResult.INVALID_RESULT;
                }
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    private OpenIdContext createOpenIdContext(String subjectIdentifier, TokenResponse tokenResponse, AccessToken accessToken, IdentityToken identityToken,
                                              OpenIdClaims userinfoClaims, JsonObject providerMetadata) {
        Map<String, String> tokenResponseRawMap = tokenResponse.asMap();
        // TODO: Move getting expires_in to TokenResponse
        long expiresIn = 0L;
        if (tokenResponseRawMap.containsKey(OpenIdConstant.EXPIRES_IN)) {
            expiresIn = Long.parseLong(tokenResponseRawMap.get(OpenIdConstant.EXPIRES_IN));
        }

        OpenIdContextImpl openIdContext = new OpenIdContextImpl(subjectIdentifier, tokenResponseRawMap.get(OpenIdConstant.TOKEN_TYPE), accessToken, identityToken, userinfoClaims, providerMetadata);
        openIdContext.setExpiresIn(expiresIn);

        String refreshTokenString = tokenResponse.getRefreshTokenString();
        if (refreshTokenString != null) {
            openIdContext.setRefreshToken(new RefreshTokenImpl(refreshTokenString));
        }

        return openIdContext;
    }

    private AccessToken createAccessTokenFromTokenResponse(long tokenMinValidityInMillis, TokenResponse tokenResponse) {
        Map<String, String> tokenResponseRawMap = tokenResponse.asMap();
        long expiresIn = 0L;
        if (tokenResponseRawMap.containsKey(OpenIdConstant.EXPIRES_IN)) {
            expiresIn = Long.parseLong(tokenResponseRawMap.get(OpenIdConstant.EXPIRES_IN));
        }

        // TODO: Determine if this is a JWT Access Token and use proper constructor.
        return new AccessTokenImpl(tokenResponse.getAccessTokenString(), tokenResponse.getResponseGenerationTime(), expiresIn, tokenMinValidityInMillis);
    }

    private IdentityToken createIdentityTokenFromTokenResponse(long tokenMinValidityInMillis, TokenResponse tokenResponse, JwtClaims idTokenClaims) {
        return new IdentityTokenImpl(tokenResponse.getIdTokenString(), idTokenClaims.getClaimsMap(), tokenMinValidityInMillis);
    }

    private OpenIdClaims createOpenIdClaimsFromUserInfoResponse() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#caller-name-and-groups, the claim name
     * that is used to define the Caller Name and optionally the Caller Groups from the OP can be defined by the following
     * attributes:
     * <ul>
     * <li>Caller Name: OpenIdAuthenticationMechanismDefinition.claimsDefinition.callerNameClaim</li>
     * <li>Caller Groups: OpenIdAuthenticationMechanismDefinition.claimsDefinition.callerGroupsClaim</li>
     * </ul>
     */
    CredentialValidationResult createCredentialValidationResult(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims) throws MalformedClaimException {
        String issuer = getIssuer(clientConfig, accessToken, idTokenClaims); //realm
        String caller = getCallerName(clientConfig, accessToken, idTokenClaims);
        if (caller == null) {
            return CredentialValidationResult.INVALID_RESULT;
        }
        Set<String> groups = getCallerGroups(clientConfig, accessToken, idTokenClaims);
        return new CredentialValidationResult(issuer, caller, null, caller, groups);
    }

    private JsonObject getProviderMetadataAsJsonObject() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param clientConfig
     * @param accessToken
     * @param idTokenClaims
     * @return
     * @throws MalformedClaimException
     */
    String getIssuer(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims) throws MalformedClaimException {
        String issuer = getClaimValueFromTokens(OpenIdConstant.ISSUER_IDENTIFIER, accessToken, idTokenClaims, String.class);
        if (issuer == null || issuer.isEmpty()) {
            issuer = issuerFromProviderMetadata(clientConfig);
        }
        return issuer;
    }

    /**
     * @param clientConfig
     * @return
     */
    private String issuerFromProviderMetadata(OidcClientConfig clientConfig) {
        return clientConfig.getProviderMetadata().getIssuer(); //TODO: use discovery data
    }

    String getCallerName(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims) throws MalformedClaimException {
        String callerNameClaim = getCallerNameClaim(clientConfig);
        if (callerNameClaim == null || callerNameClaim.isEmpty()) {
            return null;
        }
        return getClaimValueFromTokens(callerNameClaim, accessToken, idTokenClaims, String.class);
    }

    @SuppressWarnings("unchecked")
    Set<String> getCallerGroups(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims) throws MalformedClaimException {
        String callerGroupClaim = getCallerGroupsClaim(clientConfig);
        if (callerGroupClaim == null || callerGroupClaim.isEmpty()) {
            return null;
        }
        return getClaimValueFromTokens(callerGroupClaim, accessToken, idTokenClaims, Set.class);
    }

    String getCallerNameClaim(OidcClientConfig clientConfig) {
        ClaimsMappingConfig claimsMappingConfig = clientConfig.getClaimsMappingConfig();
        if (claimsMappingConfig != null) {
            return claimsMappingConfig.getCallerNameClaim();
        }
        return null;
    }

    String getCallerGroupsClaim(OidcClientConfig clientConfig) {
        ClaimsMappingConfig claimsMappingConfig = clientConfig.getClaimsMappingConfig();
        if (claimsMappingConfig != null) {
            return claimsMappingConfig.getCallerGroupsClaim();
        }
        return null;
    }

    /**
     * Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#caller-name-and-groups, the following
     * logic is used to determine the value of the Caller Name and Caller Groups:
     * <ul>
     * <li>If the specified claim exists and has a non-empty value in the Access Token, this Access Token claim value is taken.
     * <li>If not resolved yet, and the specified claim exists and has a non-empty value in the Identity Token, this Identity Token claim value is taken.
     * <li>If not resolved yet, and the specified claim exists and has a non-empty value in the User Info Token, this User Info Token claim value is taken.
     * </ul>
     */
    <T> T getClaimValueFromTokens(String claim, AccessToken accessToken, JwtClaims idTokenClaims, Class<T> claimType) throws MalformedClaimException {
        T claimValue = getClaimFromAccessToken(accessToken, claim);
        if (valueExistsAndIsNotEmpty(claimValue, claimType)) {
            return claimValue;
        }
        claimValue = getClaimFromIdToken(idTokenClaims, claim, claimType);
        if (valueExistsAndIsNotEmpty(claimValue, claimType)) {
            return claimValue;
        }
        // TODO - get claimValue from User Info
        return null;
    }

    @SuppressWarnings("unchecked")
    <T> T getClaimFromAccessToken(AccessToken accessToken, String claim) {
        if (accessToken.isJWT()) {
            return (T) accessToken.getClaim(claim);
        }
        return null;
    }

    <T> T getClaimFromIdToken(JwtClaims idTokenClaims, String claim, Class<T> claimType) throws MalformedClaimException {
        return idTokenClaims.getClaimValue(claim, claimType);
    }

    @SuppressWarnings("rawtypes")
    <T> boolean valueExistsAndIsNotEmpty(T claimValue, Class<T> claimType) {
        if (claimValue == null) {
            return false;
        }
        if (claimType.equals(String.class) && ((String) claimValue).isEmpty()) {
            return false;
        }
        if (claimType.equals(Set.class) && ((Set) claimValue).isEmpty()) {
            return false;
        }
        return true;
    }

}