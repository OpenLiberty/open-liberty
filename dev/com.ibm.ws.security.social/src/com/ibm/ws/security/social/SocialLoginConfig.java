/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.Cache;

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

    SSLContext getSSLContext() throws SocialLoginException;

    SSLSocketFactory getSSLSocketFactory() throws SocialLoginException;

    HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException;

    String getScope();

    String getResponseType();

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

}
