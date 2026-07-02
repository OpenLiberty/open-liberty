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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionCache;

public class OAuthProtectedResourceMetadataResolverTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OidcClientImpl mockOidcClient = mock.mock(OidcClientImpl.class, "oidcClient");
    private final HttpServletRequest mockRequest = mock.mock(HttpServletRequest.class, "request");
    private final OidcClientConfigImpl mockConfig = mock.mock(OidcClientConfigImpl.class, "oidcClientConfig");

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    // ---- resolveMetadataJson ------------------------------------------------

    @Test
    public void resolveMetadataJsonReturnsNullWhenOidcClientIsNull() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        // oidcClient field left null — no setOidcClient call

        String result = resolver.resolveMetadataJson(mockRequest, "/myApp/protected", "https://localhost:9443/myApp/protected");

        assertNull("Expected null when oidcClient is not set", result);
    }

    @Test
    public void resolveMetadataJsonReturnsNullWhenNoProviderMatches() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        resolver.setOidcClient(mockOidcClient);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected")));
                allowing(mockRequest).getContextPath();
                will(returnValue(""));
                oneOf(mockOidcClient).getOidcProviderByAuthFilter(with(any(HttpServletRequest.class)));
                will(returnValue(null));
            }
        });

        String result = resolver.resolveMetadataJson(mockRequest, "/myApp/protected", "https://localhost:9443/myApp/protected");

        assertNull("Expected null when no OIDC provider matches", result);
    }

    @Test
    public void resolveMetadataJsonReturnsNullWhenConfigNotFound() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        resolver.setOidcClient(mockOidcClient);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected")));
                allowing(mockRequest).getContextPath();
                will(returnValue(""));
                oneOf(mockOidcClient).getOidcProviderByAuthFilter(with(any(HttpServletRequest.class)));
                will(returnValue("providerA"));
                oneOf(mockOidcClient).getOidcClientConfig(mockRequest, "providerA");
                will(returnValue(null));
            }
        });

        String result = resolver.resolveMetadataJson(mockRequest, "/myApp/protected", "https://localhost:9443/myApp/protected");

        assertNull("Expected null when config lookup returns null", result);
    }

    @Test
    public void resolveMetadataJsonReturnsJsonWhenProviderAndConfigMatch() throws Exception {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        resolver.setOidcClient(mockOidcClient);

        mock.checking(new Expectations() {
            {
                allowing(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected")));
                allowing(mockRequest).getContextPath();
                will(returnValue(""));
                oneOf(mockOidcClient).getOidcProviderByAuthFilter(with(any(HttpServletRequest.class)));
                will(returnValue("providerA"));
                oneOf(mockOidcClient).getOidcClientConfig(mockRequest, "providerA");
                will(returnValue(mockConfig));
                allowing(mockConfig).getIssuerIdentifier();
                will(returnValue("https://issuer.example.com"));
                allowing(mockConfig).getId();
                will(returnValue("providerA"));
            }
        });

        String result = resolver.resolveMetadataJson(mockRequest, "/myApp/protected", "https://localhost:9443/myApp/protected");

        assertNotNull("Expected non-null JSON result", result);
        JSONObject json = JSONObject.parse(result);
        assertEquals("https://localhost:9443/myApp/protected", json.get("resource"));
        assertNotNull("Expected authorization_servers in result", json.get("authorization_servers"));
    }

    // ---- getAuthorizationServer ---------------------------------------------

    @Test
    public void getAuthorizationServerReturnsNullWhenIssuerIsNull() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig(null);

        assertNull(resolver.getAuthorizationServer(config));
    }

    @Test
    public void getAuthorizationServerReturnsNullWhenIssuerIsBlank() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig("   ");

        assertNull(resolver.getAuthorizationServer(config));
    }

    @Test
    public void getAuthorizationServerReturnsIssuerWhenPresent() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig("https://issuer.example.com");

        assertEquals("https://issuer.example.com", resolver.getAuthorizationServer(config));
    }

    // ---- ProtectedResourceRequestWrapper ------------------------------------

    @Test
    public void wrapperGetRequestURIReturnsProtectedResourcePath() {
        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("/myApp/protected", wrapper.getRequestURI());
    }

    @Test
    public void wrapperGetRequestURLStripsWellKnownPrefix() {
        mock.checking(new Expectations() {
            {
                oneOf(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected")));
            }
        });

        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("https://localhost:9443/myApp/protected", wrapper.getRequestURL().toString());
    }

    @Test
    public void wrapperGetRequestURLReturnsOriginalWhenNoWellKnownPrefix() {
        mock.checking(new Expectations() {
            {
                oneOf(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/myApp/protected")));
            }
        });

        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("https://localhost:9443/myApp/protected", wrapper.getRequestURL().toString());
    }

    @Test
    public void wrapperGetServletPathReturnsProtectedResourcePath() {
        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("/myApp/protected", wrapper.getServletPath());
    }

    @Test
    public void wrapperGetContextPathReturnsEmpty() {
        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("", wrapper.getContextPath());
    }

    @Test
    public void wrapperGetPathInfoReturnsNull() {
        OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper wrapper =
                new OAuthProtectedResourceMetadataResolver.ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertNull(wrapper.getPathInfo());
    }

    // ---- createMetadataJson (existing) --------------------------------------

    @Test
    public void createMetadataJsonUsesIssuerIdentifierWhenPresent() {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();
        TestOidcClientConfig config = new TestOidcClientConfig();

        config.id = "client1";
        config.issuerIdentifier = "https://issuer.example.com";

        String metadata = resolver.createMetadataJson(config, "https://localhost:9443/mcp");

        assertEquals("{\"resource\":\"https:\\/\\/localhost:9443\\/mcp\",\"authorization_servers\":[\"https:\\/\\/issuer.example.com\"]}", metadata);
    }

    @Test
    public void createMetadataJsonOmitsAuthorizationServersWhenIssuerIsMissing() throws Exception {
        OAuthProtectedResourceMetadataResolver resolver = new OAuthProtectedResourceMetadataResolver();

        TestOidcClientConfig config = new TestOidcClientConfig(null);
        config.id = "client1";

        String metadataJson = resolver.createMetadataJson(config, "https://localhost:9443/myApp/protected");

        JSONObject metadata = JSONObject.parse(metadataJson);

        assertEquals("https://localhost:9443/myApp/protected", metadata.get("resource"));
        assertFalse(metadata.containsKey("authorization_servers"));
    }

    private static class TestOidcClientConfig implements OidcClientConfig {
        String id;
        String[] resources;
        String contextPath;
        String issuerIdentifier;
        String discoveryEndpointUrl;
        String authorizationEndpointUrl;

        private TestOidcClientConfig() {
            // Default constructor for existing tests.
        }

        /**
         * @param object
         */
        private TestOidcClientConfig(String issuerIdentifier) {
            this.issuerIdentifier = issuerIdentifier;
        }

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
