/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.ws.security.SecurityService;

public interface OAuth20Provider extends OAuth20ConfigProvider, OAuthComponentInstance {

    public OAuth20Component getComponent();

    public String getID();

    @Override
    public OidcOAuth20ClientProvider getClientProvider();

    @Override
    public OAuth20EnhancedTokenCache getTokenCache();

    public void createCoreClasses();

    public boolean isRequestAccepted(HttpServletRequest request);

    public OAuthResult processResourceRequest(HttpServletRequest request);

    public OAuthResult processAuthorization(HttpServletRequest request, HttpServletResponse response, AttributeList options);

    public OAuthResult processTokenRequest(String authenticatedClient, HttpServletRequest request, HttpServletResponse response);

    public long getAuthorizationGrantLifetime();

    public long getAuthorizationCodeLifetime();

    public int getAuthorizationCodeLength();

    public long getAccessTokenLifetime();

    @Override
    public int getAccessTokenLength();

    @Override
    public boolean isIssueRefreshToken();

    @Override
    public int getRefreshTokenLength();

    public String getMediatorClassname();

    @Override
    public boolean isAllowPublicClients();

    public String[] getGrantTypesAllowed();

    public String getAuthorizationFormTemplate();

    public String getAuthorizationErrorTemplate();

    public String getCustomLoginURL();

    public String getAutoAuthorizeParam();

    public boolean isAutoAuthorize();

    public String[] getAutoAuthorizeClients();

    public String getClientURISubstitutions();

    public long getClientTokenCacheSize();

    public String getFilter();

    public String getCharacterEncoding();

    public boolean isOauthOnly();

    public boolean isIncludeTokenInSubject();

    public long getConsentCacheEntryLifetime();

    public long getConsentCacheSize();

    public boolean isHttpsRequired();

    public long getJwtMaxJtiCacheSize();

    public long getJwtClockSkew();

    public long getJwtTokenMaxLifetime();

    boolean isJwtAccessToken(); // is jwt, is not microprofile format jwt.

    public boolean isMpJwt(); // use microprofile JWT token format
    // if both above are false, is opaque access token.

    public boolean cacheAccessToken();

    public boolean getJwtIatRequired();

    public SecurityService getSecurityService();

    public long getCoverageMapSessionMaxAge();

    public String getClientAdmin();

    public byte[] getDefaultAuthorizationFormTemplateContent();

    public boolean isCertAuthentication();

    public boolean isAllowCertAuthentication();

    public boolean isAllowSpnegoAuthentication();

    /**
     * Return true if the provider config is valid
     *
     * @return
     */
    public boolean isValid();

    public boolean isLocalStoreUsed();

    public OauthConsentStore getConsentCache();

    public boolean isSkipUserValidation();

    public boolean isMiscUri(HttpServletRequest req);

    String getLogoutRedirectURL();

    public boolean getRevokeAccessTokensWithRefreshTokens();

    public String getAccessTokenEncoding();

    public boolean isPasswordGrantRequiresAppPassword();

    public long getAppPasswordLifetime();

    public long getAppTokenLifetime();

    public long getAppTokenOrPasswordLimit();

    public String getInternalClientId();

    public String getInternalClientSecret();

    public boolean isROPCPreferUserSecurityName();

    public boolean isTrackOAuthClients();

}
