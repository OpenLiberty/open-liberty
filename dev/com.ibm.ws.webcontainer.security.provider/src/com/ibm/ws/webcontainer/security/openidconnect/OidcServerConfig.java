/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.openidconnect;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

public interface OidcServerConfig {
    String getProviderId();

    String getOauthProviderName();

    String getOauthProviderPid();

    String getUserIdentifier();

    String getUniqueUserIdentifier();

    String getIssuerIdentifier();

    String getAudience();

    String getUserIdentity();

    String getGroupIdentifier();

    boolean isCustomClaimsEnabled();

    boolean allowDefaultSsoCookieName();

    boolean cacheIDToken();

    /**
     * @return the customClaims which does not include the default "realmName uniqueSecurityName groupIds"
     */
    Set<String> getCustomClaims();

    boolean isJTIClaimEnabled();

    String getDefaultScope();

    String getExternalClaimNames();

    Properties getScopeToClaimMap();

    Properties getClaimToUserRegistryMap();

    String getSignatureAlgorithm();

    PrivateKey getPrivateKey() throws KeyStoreException, CertificateException;

    boolean isSessionManaged();

    long getIdTokenLifetime();

    String getCheckSessionIframeEndpointUrl();

    // OIDC Discovery Configuration Metadata
    String[] getResponseTypesSupported();

    String[] getSubjectTypesSupported();

    String getIdTokenSigningAlgValuesSupported();

    String[] getScopesSupported();

    String[] getClaimsSupported();

    String[] getResponseModesSupported();

    String[] getGrantTypesSupported();

    String[] getTokenEndpointAuthMethodsSupported();

    String[] getDisplayValuesSupported();

    String[] getClaimTypesSupported();

    boolean isClaimsParameterSupported();

    boolean isRequestParameterSupported();

    boolean isRequestUriParameterSupported();

    boolean isRequireRequestUriRegistration();

    String getBackingIdpUriPrefix();

    String getAuthProxyEndpointUrl();

    String getTrustStoreRef();

    PublicKey getPublicKey(String trustAliasName) throws KeyStoreException, CertificateException;

    Pattern getProtectedEndpointsPattern();

    // End of OIDC Discovery Configuration Metadata

    Pattern getEndpointsPattern();

    /**
     * @return
     */
    Pattern getNonEndpointsPattern();

    boolean isJwkEnabled();

    String getJwkJsonString();

    JSONWebKey getJSONWebKey();

    long getJwkRotationTime();

    int getJwkSigningKeySize();

    boolean isOpenidScopeRequiredForUserInfo();

    String getKeyStoreRef();

    String getKeyAliasName();

}
