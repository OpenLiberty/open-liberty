/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.security.Key;
import java.util.HashMap;
import java.util.List;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.jwk.impl.JWKSet;

/**
 * This interface is an adapter layer that can be constructed from either an OidcClientConfig or SocialLoginConfig.
 * It provides the minimum common configuration info so the two types of OIDC clients can use some common processing code.
 */
public interface ConvergedClientConfig {

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

    public String getSslRef();

    public String getTokenEndpointAuthMethod();

    public boolean isHostNameVerificationEnabled();

    public String getId();

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

    public boolean getUseSystemPropertiesForHttpClientConnections();

    public boolean isMapIdentityToRegistryUser();

    public boolean isIncludeCustomCacheKeyInSubject();

    public boolean isIncludeIdTokenInSubject();

    public boolean isDisableLtpaCookie();

    public String getSignatureAlgorithm();

    public String getGroupIdentifier();

    public String getUserIdentifier();

    public String getUserIdentityToCreateSubject();

    public String getRealmIdentifier();

    public String getRealmName();

    public String getUniqueUserIdentifier();

    @Sensitive
    public String getSharedKey();

    public String getJwkEndpointUrl();

    public Key getPublicKey() throws Exception;

    public boolean getTokenReuse();

    public String getJsonWebKey();

    public boolean allowedAllAudiences();

    public boolean disableIssChecking();

    public List<String> getAudiences();

    public JWKSet getJwkSet();

    public String getJwkClientId();

    @Sensitive
    public String getJwkClientSecret();

    public String getDiscoveryEndpointUrl();

    public HashMap<String, String> getAuthzRequestParams();

    public HashMap<String, String> getTokenRequestParams();

    public HashMap<String, String> getUserinfoRequestParams();

    public HashMap<String, String> getJwkRequestParams();

    public List<String> getForwardLoginParameter();

}
