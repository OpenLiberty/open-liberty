/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionCache;

public class OAuthProtectedResourceMetadataResolverTest {

    @Test
    public void normalizePathAddsLeadingSlash() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();

        assertEquals("/mcp", resolver.normalizePath("mcp"));
    }

    @Test
    public void normalizePathReturnsRootForEmptyValues() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();

        assertEquals("/", resolver.normalizePath(null));
        assertEquals("/", resolver.normalizePath(""));
        assertEquals("/", resolver.normalizePath("/"));
    }

    @Test
    public void matchesResourceReturnsFalseForNullConfiguredPath() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();

        assertFalse(resolver.matchesResource(null, "/mcp"));
    }

    @Test
    public void matchesUsesConfiguredResourcesBeforeContextPath() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();
        config.resources = new String[] { "/mcp", "/api" };
        config.contextPath = "/different";

        assertTrue(resolver.matches(config, "/mcp"));
        assertTrue(resolver.matches(config, "/api"));
        assertFalse(resolver.matches(config, "/unknown"));
    }

    @Test
    public void matchesFallsBackToContextPath() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();
        config.contextPath = "/mcp";

        assertTrue(resolver.matches(config, "/mcp"));
        assertFalse(resolver.matches(config, "/other"));
    }

    @Test
    public void createMetadataJsonUsesIssuerIdentifierWhenPresent() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();
        config.id = "client1";
        config.issuerIdentifier = "https://issuer.example.com";

        String metadata = resolver.createMetadataJson(config, "/mcp");

        assertEquals("{\"resource\":\"\\/mcp\",\"authorization_servers\":[\"https:\\/\\/issuer.example.com\"]}", metadata);
    }

    @Test
    public void createMetadataJsonFallsBackToDiscoveryEndpoint() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();
        config.id = "client1";
        config.discoveryEndpointUrl = "https://issuer.example.com/.well-known/openid-configuration";

        String metadata = resolver.createMetadataJson(config, "/mcp");

        assertEquals("{\"resource\":\"\\/mcp\",\"authorization_servers\":[\"https:\\/\\/issuer.example.com\\/.well-known\\/openid-configuration\"]}", metadata);
    }

    @Test
    public void createMetadataJsonFallsBackToAuthorizationEndpoint() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();
        config.id = "client1";
        config.authorizationEndpointUrl = "https://issuer.example.com/authorize";

        String metadata = resolver.createMetadataJson(config, "/mcp");

        assertEquals("{\"resource\":\"\\/mcp\",\"authorization_servers\":[\"https:\\/\\/issuer.example.com\\/authorize\"]}", metadata);
    }

    private static class TestOidcClientConfig implements OidcClientConfig {
        String id;
        String[] resources;
        String contextPath;
        String issuerIdentifier;
        String discoveryEndpointUrl;
        String authorizationEndpointUrl;

        @Override
        public String[] getResources() {
            return resources;
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getIssuerIdentifier() {
            return issuerIdentifier;
        }

        @Override
        public String getDiscoveryEndpointUrl() {
            return discoveryEndpointUrl;
        }

        @Override
        public String getAuthorizationEndpointUrl() {
            return authorizationEndpointUrl;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getIssuer() {
            return null;
        }

        @Override
        public String getSharedKey() {
            return null;
        }

        @Override
        public String getGrantType() {
            return null;
        }

        @Override
        public boolean isValidateAccessTokenLocally() {
            return false;
        }

        @Override
        public String getTrustAliasName() {
            return null;
        }

        @Override
        public String getValidationEndpointUrl() {
            return null;
        }

        @Override
        public int getInitialStateCacheCapacity() {
            return 0;
        }

        @Override
        public String getTrustStoreRef() {
            return null;
        }

        @Override
        public String getKeyStoreRef() {
            return null;
        }

        @Override
        public String getTrustedAlias() {
            return null;
        }

        @Override
        public String getAuthFilterId() {
            return null;
        }

        @Override
        public String getValidationMethod() {
            return null;
        }

        @Override
        public String getJwtAccessTokenRemoteValidation() {
            return null;
        }

        @Override
        public String getHeaderName() {
            return null;
        }

        @Override
        public boolean isValidConfig() {
            return true;
        }

        @Override
        public boolean isReAuthnOnAccessTokenExpire() {
            return false;
        }

        @Override
        public long getReAuthnCushion() {
            return 0;
        }

        @Override
        public String getResponseType() {
            return null;
        }

        @Override
        public boolean isInboundPropagationEnabled() {
            return false;
        }

        @Override
        public boolean isOidcclientRequestParameterSupported() {
            return false;
        }

        @Override
        public String jwtRef() {
            return null;
        }

        @Override
        public String[] getJwtClaims() {
            return null;
        }

        @Override
        public String getRedirectUrlWithJunctionPath(String redirect_url) {
            return redirect_url;
        }

        @Override
        public boolean requireExpClaimForIntrospection() {
            return false;
        }

        @Override
        public boolean requireIatClaimForIntrospection() {
            return false;
        }

        @Override
        public com.ibm.ws.security.common.structures.SingleTableCache getCache() {
            return null;
        }

        @Override
        public boolean getAccessTokenCacheEnabled() {
            return false;
        }

        @Override
        public long getAccessTokenCacheTimeout() {
            return 0;
        }

        @Override
        public boolean isSocial() {
            return false;
        }

        @Override
        public OidcClientConfig getOidcClientConfig() {
            return this;
        }

        @Override
        public String getInboundPropagation() {
            return null;
        }

        @Override
        public boolean getAccessTokenInLtpaCookie() {
            return false;
        }

        @Override
        public boolean isAuthnSessionDisabled_propagation() {
            return false;
        }

        @Override
        public long getClockSkewInSeconds() {
            return 0;
        }

        @Override
        public long getClockSkew() {
            return 0;
        }

        @Override
        public boolean createSession() {
            return false;
        }

        @Override
        public long getAuthenticationTimeLimitInSeconds() {
            return 0;
        }

        @Override
        public boolean isHttpsRequired() {
            return false;
        }

        @Override
        public String getUserInfoEndpointUrl() {
            return null;
        }

        @Override
        public boolean isUserInfoEnabled() {
            return false;
        }

        @Override
        public String getClientSecret() {
            return null;
        }

        @Override
        public boolean isClientSideRedirect() {
            return false;
        }

        @Override
        public String getClientId() {
            return null;
        }

        @Override
        public String getTokenEndpointUrl() {
            return null;
        }

        @Override
        public String getSSLConfigurationName() {
            return null;
        }

        @Override
        public String getTokenEndpointAuthMethod() {
            return null;
        }

        @Override
        public String getTokenEndpointAuthSigningAlgorithm() {
            return null;
        }

        @Override
        public String getKeyAliasName() {
            return null;
        }

        @Override
        public String getRedirectUrlFromServerToClient() {
            return null;
        }

        @Override
        public String getScope() {
            return null;
        }

        @Override
        public String getAuthContextClassReference() {
            return null;
        }

        @Override
        public boolean isNonceEnabled() {
            return false;
        }

        @Override
        public String getPrompt() {
            return null;
        }

        @Override
        public String getOidcClientCookieName() {
            return null;
        }

        @Override
        public boolean getUseAccessTokenAsIdToken() {
            return false;
        }

        @Override
        public boolean isMapIdentityToRegistryUser() {
            return false;
        }

        @Override
        public boolean isIncludeCustomCacheKeyInSubject() {
            return false;
        }

        @Override
        public boolean isIncludeIdTokenInSubject() {
            return false;
        }

        @Override
        public boolean isDisableLtpaCookie() {
            return false;
        }

        @Override
        public String getGroupIdentifier() {
            return null;
        }

        @Override
        public String getUserIdentifier() {
            return null;
        }

        @Override
        public String getUserIdentityToCreateSubject() {
            return null;
        }

        @Override
        public String getRealmIdentifier() {
            return null;
        }

        @Override
        public String getRealmName() {
            return null;
        }

        @Override
        public String getUniqueUserIdentifier() {
            return null;
        }

        @Override
        public Key getPublicKey() throws Exception {
            return null;
        }

        @Override
        public Key getPublicKey(String alias) throws Exception {
            return null;
        }

        @Override
        public String getJsonWebKey() {
            return null;
        }

        @Override
        public boolean allowedAllAudiences() {
            return false;
        }

        @Override
        public boolean ignoreAudClaimIfNotConfigured() {
            return false;
        }

        @Override
        public List<String> getAudiences() {
            return null;
        }

        @Override
        public String[] getAllowedSignatureAlgorithms() {
            return null;
        }

        @Override
        public HashMap<String, String> getAuthzRequestParams() {
            return null;
        }

        @Override
        public HashMap<String, String> getTokenRequestParams() {
            return null;
        }

        @Override
        public HashMap<String, String> getUserinfoRequestParams() {
            return null;
        }

        @Override
        public HashMap<String, String> getJwkRequestParams() {
            return null;
        }

        @Override
        public boolean getJwkEnabled() {
            return false;
        }

        @Override
        public String getJwkEndpointUrl() {
            return null;
        }

        @Override
        public com.ibm.ws.security.jwt.config.ConsumerUtils getConsumerUtils() {
            return null;
        }

        @Override
        public boolean isValidationRequired() {
            return false;
        }

        @Override
        public boolean isHostNameVerificationEnabled() {
            return false;
        }

        @Override
        public String getSslRef() {
            return null;
        }

        @Override
        public com.ibm.ws.security.common.jwk.impl.JWKSet getJwkSet() {
            return null;
        }

        @Override
        public boolean getTokenReuse() {
            return false;
        }

        @Override
        public boolean getUseSystemPropertiesForHttpClientConnections() {
            return false;
        }

        @Override
        public java.util.List<String> getAMRClaim() {
            return null;
        }

        @Override
        public String getJwkClientId() {
            return null;
        }

        @Override
        public String getPkceCodeChallengeMethod() {
            return null;
        }

        @Override
        public String getJwkClientSecret() {
            return null;
        }

        @Override
        public OidcSessionCache getOidcSessionCache() {
            return null;
        }

        @Override
        public String getTokenRequestOriginHeader() {
            return null;
        }

        @Override
        public String getIntrospectionTokenTypeHint() {
            return null;
        }

        @Override
        public boolean disableIssChecking() {
            return false;
        }

        @Override
        public java.util.List<String> getForwardLoginParameter() {
            return null;
        }

        @Override
        public Key getJweDecryptionKey() throws GeneralSecurityException {
            return null;
        }

        @Override
        public String getKeyManagementKeyAlias() {
            return null;
        }

        @Override
        public String getSignatureAlgorithm() {
            return null;
        }

        @Override
        public java.util.Collection<String> getTrustedCertAliases(String trustStoreRef) throws Exception {
            return null;
        }

        @Override
        public java.util.List<String> getTokenOrderToFetchCallerClaims() {
            return null;
        }
    }
}

// Made with Bob
