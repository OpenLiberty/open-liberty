/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.security.Key;
import java.util.HashMap;
import java.util.List;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;

/**
 * This interface is an adapter layer that can be constructed from either an OidcClientConfig or SocialLoginConfig.
 * It provides the minimum common configuration info so the two types of OIDC clients can use some common processing code.
 */
public interface ConvergedClientConfig extends JwtConsumerConfig {

    public boolean isSocial();

    public OidcClientConfig getOidcClientConfig();

    public String getInboundPropagation();

    public boolean getAccessTokenInLtpaCookie();

    public boolean isAuthnSessionDisabled_propagation();

    public long getClockSkewInSeconds();

    public String getAuthorizationEndpointUrl();

    public boolean createSession();

    public long getAuthenticationTimeLimitInSeconds();

    public boolean isHttpsRequired();

    public String getUserInfoEndpointUrl();

    public boolean isUserInfoEnabled();

    @Sensitive
    public String getClientSecret();

    public boolean isClientSideRedirect();

    public String getClientId();

    public String getContextPath();

    public String getTokenEndpointUrl();

    public String getSSLConfigurationName();

    public String getTokenEndpointAuthMethod();

    public String getTokenEndpointAuthSigningAlgorithm();

    public String getKeyAliasName();

    public String getRedirectUrlFromServerToClient();

    public String getRedirectUrlWithJunctionPath(String redirect_url);

    public String getScope();

    public String getAuthContextClassReference();

    public String getGrantType();

    public String getResponseType();

    public boolean isNonceEnabled();

    public String getPrompt();

    public String[] getResources();

    public String getOidcClientCookieName();

    public String getIssuerIdentifier();

    public boolean getUseAccessTokenAsIdToken();

    public boolean isMapIdentityToRegistryUser();

    public boolean isIncludeCustomCacheKeyInSubject();

    public boolean isIncludeIdTokenInSubject();

    public boolean isDisableLtpaCookie();

    public String getGroupIdentifier();

    public String getUserIdentifier();

    public String getUserIdentityToCreateSubject();

    public String getRealmIdentifier();

    public String getRealmName();

    public String getUniqueUserIdentifier();

    public Key getPublicKey() throws Exception;

    public String getJsonWebKey();

    public boolean allowedAllAudiences();

    public boolean disableIssChecking();

    public String getJwkClientId();

    @Sensitive
    public String getJwkClientSecret();

    public String getDiscoveryEndpointUrl();

    public HashMap<String, String> getAuthzRequestParams();

    public HashMap<String, String> getTokenRequestParams();

    public HashMap<String, String> getUserinfoRequestParams();

    public HashMap<String, String> getJwkRequestParams();

    public List<String> getForwardLoginParameter();

    String getIntrospectionTokenTypeHint();

    public OidcSessionCache getOidcSessionCache();

    public String getPkceCodeChallengeMethod();

    public String getTokenRequestOriginHeader();

    public List<String> getTokenOrderToFetchCallerClaims();
}
