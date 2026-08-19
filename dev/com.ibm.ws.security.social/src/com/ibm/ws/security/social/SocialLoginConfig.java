/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.social.error.SocialLoginException;

public interface SocialLoginConfig {

    /*
     * socialLogin ID could be changed during Server.xml dynamic changes It
     * oughts to be consistent with SocialLoginService
     *
     * @return Id of socialLogin
     */
    public String getUniqueId();

    public String getClientId();

    @Sensitive
    public String getClientSecret();

    public String getAuthorizationEndpoint();

    public String getTokenEndpoint();

    public UserApiConfig[] getUserApis();

    public String getUserApi();

    public String getUserApiResponseIdentifier();

    public Cache getSocialLoginCookieCache();

    String getDisplayName();

    String getWebsite();

    String getSslRef();

    AuthenticationFilter getAuthFilter();

    SSLSocketFactory getSSLSocketFactory() throws SocialLoginException;

    HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException;

    String getScope();

    String getResponseType();

    String getGrantType();

    boolean createNonce();

    String getResource();

    boolean isClientSideRedirectSupported();

    String getTokenEndpointAuthMethod();

    String getRedirectToRPHostAndPort();

    String getJwksUri();

    String getRealmName();

    String getRealmNameAttribute();

    String getUserNameAttribute();

    String getGroupNameAttribute();

    String getUserUniqueIdAttribute();

    boolean getMapToUserRegistry();

    String getJwtRef();

    public String[] getJwtClaims();

    String getRequestTokenUrl();

    public PublicKey getPublicKey() throws SocialLoginException;

    public PrivateKey getPrivateKey() throws SocialLoginException;

    public String getAlgorithm();

    boolean getUserApiNeedsSpecialHeader();

    String getResponseMode();

    public boolean getUseSystemPropertiesForHttpClientConnections();

    public String getUserApiType();

    @Sensitive
    public String getUserApiToken();

    public boolean isAccessTokenRequired();

    public boolean isAccessTokenSupported();

    public String getAccessTokenHeaderName();

    public long getApiResponseCacheTime();

    public String getIntrospectionTokenTypeHint();

    /**
     * Get whether or not to serve protected resource metadata.
     * 
     * @return {@code true} if protected resource metadata is enabled
     */
    default boolean getServeProtectedResourceMetadata() {
        return false;
    }

    
    /**
     * Get the advertised scopes for the protected resource metadata.
     *
     * @return A list of scopes, or null if not configured
     */
    default List<String> getProtectedResourceMetadataAdvertisedScopes() {
        return null;
    }

    /**
     * Get the JWT builder reference for the protected resource metadata.
     *
     * @return The JWT builder reference ID (OSGi PID), or null if not configured
     */
    default String getProtectedResourceMetadataJwtBuilderRef() {
        return null;
    }

    /**
     * Get the user-facing JWT builder id for the protected resource metadata.
     * This is the {@code id} attribute value from {@code <jwtBuilder id="..."/>},
     * resolved from the OSGi PID stored in {@link #getProtectedResourceMetadataJwtBuilderRef()}.
     * Use this for constructing the {@code jwks_uri} endpoint URL.
     *
     * @return The user-facing JWT builder id, or null if not configured or unresolvable
     */
    default String getProtectedResourceMetadataJwtBuilderId() {
        return null;
    }
    
    /**
     * Get the authorization server issuer identifier (as described in RFC 8414) to be included
     * in the protected resource metadata.
     * 
     * @return The authorization server issuer identifier.
     */
    default String getProtectedResourceMetadataAuthServer() {
        return null;
    }

}
