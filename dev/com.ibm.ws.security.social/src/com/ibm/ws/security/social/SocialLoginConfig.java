/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

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

}
