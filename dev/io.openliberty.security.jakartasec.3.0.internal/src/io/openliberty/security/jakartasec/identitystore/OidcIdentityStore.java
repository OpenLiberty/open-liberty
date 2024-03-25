/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.identitystore;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.cdi40.internal.utils.CDI40Utils;
import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.jakartasec.tokens.AccessTokenImpl;
import io.openliberty.security.jakartasec.tokens.IdentityTokenImpl;
import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImpl;
import io.openliberty.security.jakartasec.tokens.RefreshTokenImpl;
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import io.openliberty.security.oidcclientcore.userinfo.UserInfoHandler;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 *
 */
public class OidcIdentityStore implements IdentityStore {

    public static final TraceComponent tc = Tr.register(OidcIdentityStore.class);

    public OidcIdentityStore() {

    }

    @Override
    @FFDCIgnore(Exception.class)
    public CredentialValidationResult validate(Credential credential) {
        // Use OidcTokensCredential to validate
        if (!(credential instanceof OidcTokensCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        } else if (credential.isValid()) {
            OidcTokensCredential castCredential = (OidcTokensCredential) credential;
            TokenResponse tokenResponse = castCredential.getTokenResponse();
            Client client = castCredential.getClient();
            if (tokenResponse != null && client != null && tokenResponse.getAccessTokenString() != null) {
                try {
                    HttpServletRequest request = castCredential.getRequest();
                    JwtClaims idTokenClaims = client.validate(tokenResponse, request, castCredential.getResponse());

                    OidcClientConfig oidcClientConfig = client.getOidcClientConfig();
                    long tokenMinValidityInMillis = oidcClientConfig.getTokenMinValidity();
                    AccessToken accessToken = createAccessTokenFromTokenResponse(tokenMinValidityInMillis, tokenResponse);
                    IdentityToken identityToken = createIdentityTokenFromTokenResponse(tokenMinValidityInMillis, tokenResponse, idTokenClaims);
                    OpenIdClaims userInfoClaims = createOpenIdClaimsFromUserInfoResponse(oidcClientConfig, accessToken);

                    CredentialValidationResult credentialValidationResult = createCredentialValidationResult(client.getOidcClientConfig(), accessToken, idTokenClaims,
                                                                                                             userInfoClaims);

                    JsonObject providerMetadata = getProviderMetadataAsJsonObject(client.getOidcClientConfig());

                    setOpenIdContext(castCredential.getOpenIdContext(), credentialValidationResult.getCallerUniqueId(), tokenResponse, accessToken, identityToken,
                                     userInfoClaims, providerMetadata, request.getParameter(OpenIdConstant.STATE),
                                     oidcClientConfig.isUseSession(), client.getOidcClientConfig().getClientId());

                    return credentialValidationResult;
                } catch (Exception e) {
                    /*
                     * Optionally provide the stack in trace, since we're ignoring the FFDC here.
                     */
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validate", "Exception occurred for client " + client.getOidcClientConfig().getClientId(), e);
                    }
                    Tr.error(tc, "CREDENTIAL_VALIDATION_ERROR", client.getOidcClientConfig().getClientId(), e.toString());
                    return CredentialValidationResult.INVALID_RESULT;
                }
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    private void setOpenIdContext(OpenIdContext openIdContext, String subjectIdentifier, TokenResponse tokenResponse, AccessToken accessToken,
                                  IdentityToken identityToken, OpenIdClaims userinfoClaims, JsonObject providerMetadata, String state, boolean useSession, String clientId) {

        OpenIdContextImpl openIdContextImpl = (OpenIdContextImpl) CDI40Utils.getContextualInstanceFromProxy(openIdContext);

        Map<String, String> tokenResponseRawMap = tokenResponse.asMap();
        // TODO: Move getting expires_in to TokenResponse
        long expiresIn = 0L;
        if (tokenResponseRawMap.containsKey(OpenIdConstant.EXPIRES_IN)) {
            expiresIn = Long.parseLong(tokenResponseRawMap.get(OpenIdConstant.EXPIRES_IN));
        }

        openIdContextImpl.setSubject(subjectIdentifier);
        openIdContextImpl.setTokenType(tokenResponseRawMap.get(OpenIdConstant.TOKEN_TYPE));
        openIdContextImpl.setAccessToken(accessToken);
        openIdContextImpl.setIdentityToken(identityToken);
        openIdContextImpl.setExpiresIn(expiresIn);
        openIdContextImpl.setClaims(userinfoClaims);
        openIdContextImpl.setProviderMetadata(providerMetadata);
        openIdContextImpl.setState(state);
        openIdContextImpl.setUseSession(useSession);
        openIdContextImpl.setClientId(clientId);

        String refreshTokenString = tokenResponse.getRefreshTokenString();
        if (refreshTokenString != null) {
            openIdContextImpl.setRefreshToken(new RefreshTokenImpl(refreshTokenString));
        } else {
            openIdContextImpl.setRefreshToken(null);
        }
    }

    @FFDCIgnore({ Exception.class })
    protected AccessToken createAccessTokenFromTokenResponse(long tokenMinValidityInMillis, TokenResponse tokenResponse) {
        final String METHODNAME = "createAccessTokenFromTokenResponse";

        Map<String, String> tokenResponseRawMap = tokenResponse.asMap();
        long expiresIn = 0L;
        if (tokenResponseRawMap != null && tokenResponseRawMap.containsKey(OpenIdConstant.EXPIRES_IN)) {
            expiresIn = Long.parseLong(tokenResponseRawMap.get(OpenIdConstant.EXPIRES_IN));
        }

        String accessTokenString = tokenResponse.getAccessTokenString();

        boolean isJWT = false;
        Map<String, Object> jwtClaims = null;

        String[] parts = accessTokenString.split(Pattern.quote(JakartaSec30Constants.DELIMITER)); // split out the "parts" (header, payload and signature)

        if (parts.length > 1) {
            try {
                try {
                    String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[0]), "UTF-8");
                    org.jose4j.jwt.JwtClaims.parse(claimsAsJsonString).getClaimsMap();
                    isJWT = true;
                } catch (Exception e1) {
                    String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[1]), "UTF-8");
                    jwtClaims = org.jose4j.jwt.JwtClaims.parse(claimsAsJsonString).getClaimsMap();
                    isJWT = true;
                }
            } catch (Exception e2) {
                // do nothing
            }
        } else {
            // do nothing
        }

        if (isJWT) {
            if (jwtClaims == null) { // grab the claims map if we haven't already
                try {
                    String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[1]), "UTF-8");
                    jwtClaims = org.jose4j.jwt.JwtClaims.parse(claimsAsJsonString).getClaimsMap();
                } catch (Exception e1) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME, "The tokenResponse accessTokenString was parsable for the first part, but couldn't parse out a claimsMap.", e1);
                    }
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME, "Creating a jwt access token based on the tokenResponse accessTokenString");
            }
            return new AccessTokenImpl(accessTokenString, jwtClaims, tokenResponse.getResponseGenerationTime(), expiresIn, tokenMinValidityInMillis);
        }

        return new AccessTokenImpl(accessTokenString, tokenResponse.getResponseGenerationTime(), expiresIn, tokenMinValidityInMillis);
    }

    private IdentityToken createIdentityTokenFromTokenResponse(long tokenMinValidityInMillis, TokenResponse tokenResponse, JwtClaims idTokenClaims) {
        String idTokenString = tokenResponse.getIdTokenString();
        if (idTokenString == null) {
            return null;
        }
        return new IdentityTokenImpl(idTokenString, idTokenClaims.getClaimsMap(), tokenMinValidityInMillis);
    }

    @FFDCIgnore(Exception.class)
    private OpenIdClaims createOpenIdClaimsFromUserInfoResponse(OidcClientConfig oidcClientConfig, AccessToken accessToken) {
        UserInfoHandler userInfoHandler = getUserInfoHandler();
        Map<String, Object> userInfoClaims = null;
        try {
            userInfoClaims = userInfoHandler.getUserInfoClaims(oidcClientConfig, accessToken.getToken());
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The {0} OpenID Connect client cannot create claims from the UserInfo data that was returned from the OpenID Connect provider. {1}",
                         oidcClientConfig.getClientId(), e.toString());
            }
        }
        if (userInfoClaims == null) {
            return null;
        }
        return new OpenIdClaimsImpl(userInfoClaims);
    }

    UserInfoHandler getUserInfoHandler() {
        return new UserInfoHandler();
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
    CredentialValidationResult createCredentialValidationResult(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims,
                                                                OpenIdClaims userInfoClaims) throws MalformedClaimException {
        String issuer = getIssuer(clientConfig, accessToken, idTokenClaims, userInfoClaims); //realm
        String caller = getCallerName(clientConfig, accessToken, idTokenClaims, userInfoClaims);
        if (caller == null) {
            return CredentialValidationResult.INVALID_RESULT;
        }
        Set<String> groups = getCallerGroups(clientConfig, accessToken, idTokenClaims, userInfoClaims);
        return new CredentialValidationResult(issuer, caller, null, caller, groups);
    }

    @FFDCIgnore(OidcDiscoveryException.class)
    private JsonObject getProviderMetadataAsJsonObject(OidcClientConfig oidcClientConfig) {
        try {

            return OpenIdContextUtils.convertJsonObject(MetadataUtils.getProviderDiscoveryMetaData(oidcClientConfig));

        } catch (OidcClientConfigurationException | OidcDiscoveryException oe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getProviderMetadataAsJsonObject", "getProviderDiscoveryMetaData threw an exception, the providerMetadata JsonObject will be null.", oe);
            }
        }

        return null;
    }

    String getIssuer(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims, OpenIdClaims userInfoClaims) throws MalformedClaimException {
        String issuer = getClaimValueFromTokens(OpenIdConstant.ISSUER_IDENTIFIER, accessToken, idTokenClaims, userInfoClaims, String.class);
        if (issuer == null || issuer.isEmpty()) {
            issuer = issuerFromProviderMetadata(clientConfig);
        }
        return issuer;
    }

    private String issuerFromProviderMetadata(OidcClientConfig clientConfig) {
        return clientConfig.getProviderMetadata().getIssuer(); //TODO: use discovery data
    }

    String getCallerName(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims, OpenIdClaims userInfoClaims) throws MalformedClaimException {
        String callerNameClaim = getCallerNameClaim(clientConfig);
        if (callerNameClaim == null || callerNameClaim.isEmpty()) {
            return null;
        }
        return getClaimValueFromTokens(callerNameClaim, accessToken, idTokenClaims, userInfoClaims, String.class);
    }

    @SuppressWarnings("unchecked")
    Set<String> getCallerGroups(OidcClientConfig clientConfig, AccessToken accessToken, JwtClaims idTokenClaims, OpenIdClaims userInfoClaims) throws MalformedClaimException {
        String callerGroupClaim = getCallerGroupsClaim(clientConfig);
        if (callerGroupClaim == null || callerGroupClaim.isEmpty()) {
            return null;
        }
        List<String> groups = getClaimValueFromTokens(callerGroupClaim, accessToken, idTokenClaims, userInfoClaims, List.class);
        if (groups != null) {
            return Set.copyOf(groups);
        }
        return null;
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
    <T> T getClaimValueFromTokens(String claim, AccessToken accessToken, JwtClaims idTokenClaims, OpenIdClaims userInfoClaims, Class<T> claimType) throws MalformedClaimException {
        T claimValue = getClaimFromAccessToken(accessToken, claim);
        if (valueExistsAndIsNotEmpty(claimValue, claimType)) {
            return claimValue;
        }
        claimValue = getClaimFromIdToken(idTokenClaims, claim, claimType);
        if (valueExistsAndIsNotEmpty(claimValue, claimType)) {
            return claimValue;
        }
        claimValue = getClaimFromUserInfo(userInfoClaims, claim, claimType);
        if (valueExistsAndIsNotEmpty(claimValue, claimType)) {
            return claimValue;
        }
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

    @SuppressWarnings("unchecked")
    <T> T getClaimFromUserInfo(OpenIdClaims userInfoClaims, String claim, Class<T> claimType) {
        if (userInfoClaims == null) {
            return null;
        }
        if (claimType.equals(String.class)) {
            Optional<String> claimValue = userInfoClaims.getStringClaim(claim);
            if (claimValue.isPresent()) {
                return (T) claimValue.get();
            }
            return null;
        }
        if (claimType.equals(List.class)) {
            return (T) userInfoClaims.getArrayStringClaim(claim);
        }
        // Other types can be supported later if needed
        return null;
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
        if (claimType.equals(List.class) && ((List) claimValue).isEmpty()) {
            return false;
        }
        return true;
    }

}