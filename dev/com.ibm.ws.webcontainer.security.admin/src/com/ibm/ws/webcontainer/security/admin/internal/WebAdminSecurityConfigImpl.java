/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.admin.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;

/**
 * Represents security configurable options for web admin applications.
 */
class WebAdminSecurityConfigImpl implements WebAppSecurityConfig {

    private final Boolean logoutOnHttpSessionExpire = false;
    private final Boolean singleSignonEnabled = true;
    private final Boolean preserveFullyQualifiedReferrerUrl = false;
    private final String postParamSaveMethod = "Cookie";
    private final Integer postParamCookieSize = 16384;
    private final Boolean allowLogoutPageRedirectToAnyHost = false;
    private final String wasReqURLRedirectDomainNames = null;
    private final String logoutPageRedirectDomainNames = null;
    private final String ssoCookieName = "LtpaToken2";
    // Admin supports CLIENT_CERT, but also needs to support basic auth
    private final Boolean allowFailOverToBasicAuth = true;
    private final Boolean displayAuthenticationRealm = false;
    private final Boolean httpOnlyCookies = true;
    private final Boolean webAlwaysLogin = false;
    private final Boolean ssoRequiresSSL = true;
    private final String ssoDomainNames = null;
    private final Boolean ssoUseDomainFromURL = false;
    private final Boolean useLtpaSSOForJaspic = false;
    private final Boolean useAuthenticationDataForUnprotectedResource = true;
    private final Boolean allowFailOverToFormLogin = true;
    // in order to maintain the original behavior, APP_DEFINED is not supported.
    // if APP_DEFINED is supported, if login_config in web.xml is set as CLIENT_CERT,
    // it no longer can failover to FORM.
    private final Boolean allowFailOverToAppDefined = false;
    private final Boolean includePathInWASReqURL = false;
    private final Boolean trackLoggedOutSSOCookies = true;
    private final Boolean useOnlyCustomCookieName = false;
    private final String sameSiteCookie = "Disabled";

    WebAdminSecurityConfigImpl(Map<String, Object> newProperties) {
        //nothing to do, values are hard-coded
    }

    /** {@inheritDoc} */
    @Override
    public boolean getLogoutOnHttpSessionExpire() {
        return logoutOnHttpSessionExpire;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludePathInWASReqURL() {
        return includePathInWASReqURL;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSingleSignonEnabled() {
        return singleSignonEnabled.booleanValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getPreserveFullyQualifiedReferrerUrl() {
        return preserveFullyQualifiedReferrerUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String getPostParamSaveMethod() {
        return postParamSaveMethod;
    }

    /** {@inheritDoc} */
    @Override
    public int getPostParamCookieSize() {
        return postParamCookieSize.intValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAllowLogoutPageRedirectToAnyHost() {
        return allowLogoutPageRedirectToAnyHost;
    }

    /** {@inheritDoc} */
    @Override
    public String getSSOCookieName() {
        return ssoCookieName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAllowFailOverToBasicAuth() {
        return allowFailOverToBasicAuth;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getDisplayAuthenticationRealm() {
        return displayAuthenticationRealm;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getWASReqURLRedirectDomainNames() {
        return domainNamesToList(wasReqURLRedirectDomainNames);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getLogoutPageRedirectDomainList() {
        return domainNamesToList(logoutPageRedirectDomainNames);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getHttpOnlyCookies() {
        return httpOnlyCookies;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getWebAlwaysLogin() {
        return webAlwaysLogin;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getSSORequiresSSL() {
        return ssoRequiresSSL;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getSSODomainList() {
        return domainNamesToList(ssoDomainNames);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getSSOUseDomainFromURL() {
        return ssoUseDomainFromURL;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUseLtpaSSOForJaspic() {
        return useLtpaSSOForJaspic;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUseAuthenticationDataForUnprotectedResourceEnabled() {
        return useAuthenticationDataForUnprotectedResource;
    }

    /** {@inheritDoc} */
    private List<String> domainNamesToList(String domainNames) {
        if (domainNames == null || domainNames.length() == 0)
            return null;
        List<String> domainNameList = new ArrayList<String>();
        String[] sd = domainNames.split("\\|");
        for (int i = 0; i < sd.length; i++) {
            domainNameList.add(sd[i]);
        }
        return domainNameList;
    }

    /**
     * {@inheritDoc}<p>
     * This does not need an implemented as the Admin Application security
     * configuration properties never change.
     *
     * @return {@code null}
     */
    @Override
    public String getChangedProperties(WebAppSecurityConfig original) {
        return null;
    }

    /**
     * {@inheritDoc}<p>
     * This does not need an implemented as the Admin Application security
     * configuration properties never change.
     *
     * @return {@code null}
     */
    @Override
    public Map<String, String> getChangedPropertiesMap(WebAppSecurityConfig original) {
        return null;
    }

    /**
     * {@inheritDoc} Admin Applications do not have a default Form Login URL.
     *
     * @return {@code null}
     */
    @Override
    public String getLoginFormURL() {
        return null;
    }

    /**
     * {@inheritDoc} Admin Applications do not have a default Form Error URL.
     *
     * @return {@code null}
     */
    @Override
    public String getLoginErrorURL() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAllowFailOverToFormLogin() {
        return allowFailOverToFormLogin;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAllowFailOverToAppDefined() {
        return allowFailOverToAppDefined;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowFailOver() {
        return allowFailOverToBasicAuth || allowFailOverToFormLogin || allowFailOverToAppDefined;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrackLoggedOutSSOCookiesEnabled() {
        return trackLoggedOutSSOCookies;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUseOnlyCustomCookieName() {
        return useOnlyCustomCookieName;
    }

    /** {@inheritDoc} */
    @Override
    public SSOCookieHelper createSSOCookieHelper() {
        return new SSOCookieHelperImpl(this);
    }

    /** {@inheritDoc} */
    @Override
    public ReferrerURLCookieHandler createReferrerURLCookieHandler() {
        return new ReferrerURLCookieHandler(this);
    }

    /** {@inheritDoc} */
    @Override
    public WebAuthenticatorProxy createWebAuthenticatorProxy() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getOverrideHttpAuthMethod() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoginFormContextRoot() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getBasicAuthRealmName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getSameSiteCookie() {
        WebAppSecurityConfig globalConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        if (globalConfig != null)
            return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().getSameSiteCookie();
        else
            return sameSiteCookie;
    }
}
