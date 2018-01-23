/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Represents security configurable options for web applications.
 */
public class WebAppSecurityConfigImpl implements WebAppSecurityConfig {
    private static final TraceComponent tc = Tr.register(WebAppSecurityConfigImpl.class);

    public static final String WLP_USER_DIR = "${wlp.user.dir}";
    public static final String AUTO_GEN_COOKIE_NAME_PREFIX = "WAS_";
    public static final String DEFAULT_SSO_COOKIE_NAME = "LtpaToken2";

    static final String CFG_KEY_LOGOUT_ON_HTTP_SESSION_EXPIRE = "logoutOnHttpSessionExpire";
    public static final String CFG_KEY_SINGLE_SIGN_ON_ENABLED = "singleSignonEnabled";
    static final String CFG_KEY_PRESERVE_FULLY_QUALIFIED_REFERRER_URL = "preserveFullyQualifiedReferrerUrl";
    static final String CFG_KEY_POSTPARAM_SAVE_METHOD = "postParamSaveMethod";
    static final String CFG_KEY_POSTPARAM_COOKIE_SIZE = "postParamCookieSize";
    static final String CFG_KEY_ALLOW_LOGOUT_PAGE_REDIRECT_TO_ANY_HOST = "allowLogoutPageRedirectToAnyHost";
    static final String CFG_KEY_LOGOUT_PAGE_REDIRECT_DOMAIN_NAMES = "logoutPageRedirectDomainNames";
    static final String CFG_KEY_WAS_REQ_URL_REDIRECT_DOMAIN_NAMES = "wasReqURLRedirectDomainNames";
    protected static final String CFG_KEY_SSO_COOKIE_NAME = "ssoCookieName";
    static final String CFG_KEY_AUTO_GEN_SSO_COOKIE_NAME = "autoGenSsoCookieName";
    public static final String CFG_KEY_FAIL_OVER_TO_BASICAUTH = "allowFailOverToBasicAuth";
    static final String CFG_KEY_DISPLAY_AUTHENTICATION_REALM = "displayAuthenticationRealm";
    static final String CFG_KEY_HTTP_ONLY_COOKIES = "httpOnlyCookies";
    static final String CFG_KEY_WEB_ALWAYS_LOGIN = "webAlwaysLogin";
    static final String CFG_KEY_SSO_DOMAIN_NAMES = "ssoDomainNames";
    static final String CFG_KEY_SSO_REQUIRES_SSL = "ssoRequiresSSL";
    static final String CFG_KEY_SSO_USE_DOMAIN_FROM_URL = "ssoUseDomainFromURL";
    public static final String CFG_KEY_USE_AUTH_DATA_FOR_UNPROTECTED = "useAuthenticationDataForUnprotectedResource";
    static final String CFG_KEY_LOGIN_FORM_URL = "loginFormURL";
    static final String CFG_KEY_LOGIN_ERROR_URL = "loginErrorURL";
    public static final String CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD = "allowAuthenticationFailOverToAuthMethod";
    static final String CFG_KEY_INCLUDE_PATH_IN_WAS_REQ_URL = "includePathInWASReqURL";
    static final String CFG_KEY_TRACK_LOGGED_OUT_SSO_COOKIES = "trackLoggedOutSSOCookies";
    static final String CFG_KEY_USE_ONLY_CUSTOM_COOKIE_NAME = "useOnlyCustomCookieName";
    static final String CFG_KEY_OVERRIDE_HAM = "overrideHttpAuthenticationMechanism";
    static final String CFG_KEY_BASIC_AUTH_REALM_NAME = "basicAuthRealmName";
    
    // New attributes must update getChangedProperties method
    private final Boolean logoutOnHttpSessionExpire;
    private final Boolean singleSignonEnabled;
    private final Boolean preserveFullyQualifiedReferrerUrl;
    private final String postParamSaveMethod;
    private final Integer postParamCookieSize;
    private final Boolean allowLogoutPageRedirectToAnyHost;
    private final String wasReqURLRedirectDomainNames;
    private final String logoutPageRedirectDomainNames;
    protected String ssoCookieName;
    protected final Boolean autoGenSsoCookieName;
    private final Boolean allowFailOverToBasicAuth;
    private final Boolean displayAuthenticationRealm;
    private final Boolean httpOnlyCookies;
    private final Boolean webAlwaysLogin;
    private final Boolean ssoRequiresSSL;
    private final String ssoDomainNames;
    private final Boolean ssoUseDomainFromURL;
    private final Boolean useAuthenticationDataForUnprotectedResource;
    private final String loginFormURL;
    private final String loginErrorURL;
    private final String allowFailOverToAuthMethod;
    private final Boolean includePathInWASReqURL;
    private final Boolean trackLoggedOutSSOCookies;
    private final Boolean useOnlyCustomCookieName;
    private final String overrideHttpAuthenticationMechanism;
    private final String basicAuthRealmName;

    protected final AtomicServiceReference<WsLocationAdmin> locationAdminRef;
    protected final AtomicServiceReference<SecurityService> securityServiceRef;

    public WebAppSecurityConfigImpl(Map<String, Object> newProperties,
                                    AtomicServiceReference<WsLocationAdmin> locationAdminRef,
                                    AtomicServiceReference<SecurityService> securityServiceRef) {
        this.locationAdminRef = locationAdminRef;
        this.securityServiceRef = securityServiceRef;
        logoutOnHttpSessionExpire = (Boolean) newProperties.get(CFG_KEY_LOGOUT_ON_HTTP_SESSION_EXPIRE);
        singleSignonEnabled = (Boolean) newProperties.get(CFG_KEY_SINGLE_SIGN_ON_ENABLED);
        preserveFullyQualifiedReferrerUrl = (Boolean) newProperties.get(CFG_KEY_PRESERVE_FULLY_QUALIFIED_REFERRER_URL);
        postParamSaveMethod = (String) newProperties.get(CFG_KEY_POSTPARAM_SAVE_METHOD);
        postParamCookieSize = (Integer) newProperties.get(CFG_KEY_POSTPARAM_COOKIE_SIZE);
        allowLogoutPageRedirectToAnyHost = (Boolean) newProperties.get(CFG_KEY_ALLOW_LOGOUT_PAGE_REDIRECT_TO_ANY_HOST);
        wasReqURLRedirectDomainNames = (String) newProperties.get(CFG_KEY_WAS_REQ_URL_REDIRECT_DOMAIN_NAMES);
        logoutPageRedirectDomainNames = (String) newProperties.get(CFG_KEY_LOGOUT_PAGE_REDIRECT_DOMAIN_NAMES);
        autoGenSsoCookieName = (Boolean) newProperties.get(CFG_KEY_AUTO_GEN_SSO_COOKIE_NAME);
        ssoCookieName = resolveSsoCookieName(newProperties);
        allowFailOverToBasicAuth = (Boolean) newProperties.get(CFG_KEY_FAIL_OVER_TO_BASICAUTH);
        displayAuthenticationRealm = (Boolean) newProperties.get(CFG_KEY_DISPLAY_AUTHENTICATION_REALM);
        httpOnlyCookies = (Boolean) newProperties.get(CFG_KEY_HTTP_ONLY_COOKIES);
        webAlwaysLogin = (Boolean) newProperties.get(CFG_KEY_WEB_ALWAYS_LOGIN);
        ssoRequiresSSL = (Boolean) newProperties.get(CFG_KEY_SSO_REQUIRES_SSL);
        ssoDomainNames = (String) newProperties.get(CFG_KEY_SSO_DOMAIN_NAMES);
        ssoUseDomainFromURL = (Boolean) newProperties.get(CFG_KEY_SSO_USE_DOMAIN_FROM_URL);
        useAuthenticationDataForUnprotectedResource = (Boolean) newProperties.get(CFG_KEY_USE_AUTH_DATA_FOR_UNPROTECTED);
        loginFormURL = (String) newProperties.get(CFG_KEY_LOGIN_FORM_URL);
        loginErrorURL = (String) newProperties.get(CFG_KEY_LOGIN_ERROR_URL);
        allowFailOverToAuthMethod = (String) newProperties.get(CFG_KEY_ALLOW_FAIL_OVER_TO_AUTH_METHOD);
        includePathInWASReqURL = (Boolean) newProperties.get(CFG_KEY_INCLUDE_PATH_IN_WAS_REQ_URL);
        trackLoggedOutSSOCookies = (Boolean) newProperties.get(CFG_KEY_TRACK_LOGGED_OUT_SSO_COOKIES);
        useOnlyCustomCookieName = (Boolean) newProperties.get(CFG_KEY_USE_ONLY_CUSTOM_COOKIE_NAME);
        overrideHttpAuthenticationMechanism = (String) newProperties.get(CFG_KEY_OVERRIDE_HAM);
        basicAuthRealmName = (String) newProperties.get(CFG_KEY_BASIC_AUTH_REALM_NAME);
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(this);
    }

    /**
     *
     */
    protected String resolveSsoCookieName(Map<String, Object> newProperties) {
        String genCookieName = null;
        String cookieName = (String) newProperties.get(CFG_KEY_SSO_COOKIE_NAME);
        if (DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(cookieName) && autoGenSsoCookieName) {
            genCookieName = generateSsoCookieName();
        }

        if (genCookieName != null) {
            return genCookieName;
        } else {
            return cookieName;
        }
    }

    /**
     * @param cookieName
     */
    protected String generateSsoCookieName() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        if (locationAdmin != null) {
            String usrLocation = locationAdmin.resolveString(WLP_USER_DIR).replace('\\', '/');
            String slash = usrLocation.endsWith("/") ? "" : "/";
            String cookieLongName = getHostName() + "_" + usrLocation + slash + "servers/" + locationAdmin.getServerName();
            String cookieHashName = AUTO_GEN_COOKIE_NAME_PREFIX + StringUtil.hash(cookieLongName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cookieHashName: " + cookieHashName + " cookieLongName: " + cookieLongName);
            }
            return cookieHashName;
        } else {
            Tr.error(tc, "OSGI_SERVICE_ERROR", "WsLocationAdmin");
            return null;
        }
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
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
        if (allowFailOverToBasicAuth ||
            (allowFailOverToAuthMethod != null && allowFailOverToAuthMethod.equalsIgnoreCase(LoginConfiguration.BASIC))) {
            return true;
        } else
            return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAllowFailOverToFormLogin() {
        if (allowFailOverToAuthMethod != null && allowFailOverToAuthMethod.equalsIgnoreCase(LoginConfiguration.FORM))
            return true;
        else
            return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowFailOver() {
        return (getAllowFailOverToBasicAuth() || getAllowFailOverToFormLogin());
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

    @Override
    public boolean isUseAuthenticationDataForUnprotectedResourceEnabled() {
        return useAuthenticationDataForUnprotectedResource;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoginFormURL() {
        return loginFormURL;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoginErrorURL() {
        return loginErrorURL;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebAppSecurityConfig#getTrackLoggedOutSSOCookies()
     */
    @Override
    public boolean isTrackLoggedOutSSOCookiesEnabled() {
        return trackLoggedOutSSOCookies;
    }

    @Override
    public boolean isUseOnlyCustomCookieName() {
        return useOnlyCustomCookieName;
    }

    @Override
    public String getOverrideHttpAuthenticationMechanism() {
        return overrideHttpAuthenticationMechanism;
    }

    @Override
    public String getBasicAuthRealmName() {
        return basicAuthRealmName;
    }

    /**
     * @return
     */
    private List<String> domainNamesToList(String domainNames) {
        if (domainNames == null || domainNames.length() == 0)
            return null;
        List<String> domainNameList = new ArrayList<String>();
        String[] sd = domainNames.split("\\|");
        for (int i = 0; i < sd.length; i++) {
            domainNameList.add(sd[i].trim());
        }
        return domainNameList;
    }

    private void appendToBufferIfDifferent(StringBuffer buffer, String name, Object thisValue, Object otherValue) {
        if ((thisValue != otherValue) && (thisValue != null) && (!thisValue.equals(otherValue))) {
            if (buffer.length() > 0) {
                buffer.append(",");
            }
            buffer.append(name);
            buffer.append("=");
            buffer.append(thisValue.toString());
        }
    }

    /**
     * {@inheritDoc}<p>
     * This method needs to be maintained when new attributes are added.
     * Order should be presented in alphabetical order.
     */
    @Override
    public String getChangedProperties(WebAppSecurityConfig original) {
        // Bail out if it is the same object, or if this isn't of the right type.
        if (this == original) {
            return "";
        }
        if (!(original instanceof WebAppSecurityConfigImpl)) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        WebAppSecurityConfigImpl orig = (WebAppSecurityConfigImpl) original;
        appendToBufferIfDifferent(buf, "allowFailOverToBasicAuth",
                                  this.allowFailOverToBasicAuth, orig.allowFailOverToBasicAuth);
        appendToBufferIfDifferent(buf, "allowLogoutPageRedirectToAnyHost",
                                  this.allowLogoutPageRedirectToAnyHost, orig.allowLogoutPageRedirectToAnyHost);
        appendToBufferIfDifferent(buf, "displayAuthenticationRealm",
                                  this.displayAuthenticationRealm, orig.displayAuthenticationRealm);
        appendToBufferIfDifferent(buf, "httpOnlyCookies",
                                  this.httpOnlyCookies, orig.httpOnlyCookies);
        appendToBufferIfDifferent(buf, "logoutOnHttpSessionExpire",
                                  this.logoutOnHttpSessionExpire, orig.logoutOnHttpSessionExpire);
        appendToBufferIfDifferent(buf, "logoutPageRedirectDomainNames",
                                  this.logoutPageRedirectDomainNames, orig.logoutPageRedirectDomainNames);
        appendToBufferIfDifferent(buf, "preserveFullyQualifiedReferrerUrl",
                                  this.preserveFullyQualifiedReferrerUrl, orig.preserveFullyQualifiedReferrerUrl);
        appendToBufferIfDifferent(buf, "postParamCookieSize",
                                  this.postParamCookieSize, orig.postParamCookieSize);
        appendToBufferIfDifferent(buf, "postParamSaveMethod",
                                  this.postParamSaveMethod, orig.postParamSaveMethod);
        appendToBufferIfDifferent(buf, "singleSignonEnabled",
                                  this.singleSignonEnabled, orig.singleSignonEnabled);
        appendToBufferIfDifferent(buf, "ssoCookieName",
                                  this.ssoCookieName, orig.ssoCookieName);
        appendToBufferIfDifferent(buf, "autoGenSsoCookieName",
                                  this.autoGenSsoCookieName, orig.autoGenSsoCookieName);
        appendToBufferIfDifferent(buf, "ssoDomainNames",
                                  this.ssoDomainNames, orig.ssoDomainNames);
        appendToBufferIfDifferent(buf, "ssoRequiresSSL",
                                  this.ssoRequiresSSL, orig.ssoRequiresSSL);
        appendToBufferIfDifferent(buf, "ssoUseDomainFromURL",
                                  this.ssoUseDomainFromURL, orig.ssoUseDomainFromURL);
        appendToBufferIfDifferent(buf, "useAuthenticationDataForUnprotectedResource",
                                  this.useAuthenticationDataForUnprotectedResource, orig.useAuthenticationDataForUnprotectedResource);
        appendToBufferIfDifferent(buf, "webAlwaysLogin",
                                  this.webAlwaysLogin, orig.webAlwaysLogin);
        appendToBufferIfDifferent(buf, "loginFormURL",
                                  this.loginFormURL, orig.loginFormURL);
        appendToBufferIfDifferent(buf, "loginErrorURL",
                                  this.loginErrorURL, orig.loginErrorURL);
        appendToBufferIfDifferent(buf, "allowFailOverToAuthMethod",
                                  this.allowFailOverToAuthMethod, orig.allowFailOverToAuthMethod);
        appendToBufferIfDifferent(buf, "includePathInWASReqURL",
                                  this.includePathInWASReqURL, orig.includePathInWASReqURL);
        appendToBufferIfDifferent(buf, "trackLoggedOutSSOCookies",
                                  this.trackLoggedOutSSOCookies, orig.trackLoggedOutSSOCookies);
        appendToBufferIfDifferent(buf, "useOnlyCustomCookieName",
                                  this.useOnlyCustomCookieName, orig.useOnlyCustomCookieName);
        appendToBufferIfDifferent(buf, "wasReqURLRedirectDomainNames",
                                  this.wasReqURLRedirectDomainNames, orig.wasReqURLRedirectDomainNames);
        appendToBufferIfDifferent(buf, "overrideHttpAuthenticationMechanism",
                                  this.overrideHttpAuthenticationMechanism, orig.overrideHttpAuthenticationMechanism);
        appendToBufferIfDifferent(buf, "basicAuthRealmName",
                                  this.basicAuthRealmName, orig.basicAuthRealmName);
        return buf.toString();
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
        return new WebAuthenticatorProxy(this, null, securityServiceRef, null);
    }

}
