/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.List;
import java.util.Map;

/**
 * Encapsulate the web application security settings.
 */
public interface WebAppSecurityConfig {

    /**
     * These values are enumerated in the metatype.xml
     */
    static final String POST_PARAM_SAVE_TO_COOKIE = "Cookie";
    static final String POST_PARAM_SAVE_TO_SESSION = "Session";
    static final String POST_PARAM_SAVE_TO_NONE = "None";

    boolean getLogoutOnHttpSessionExpire();

    boolean isIncludePathInWASReqURL();

    boolean isSingleSignonEnabled();

    boolean getPreserveFullyQualifiedReferrerUrl();

    String getPostParamSaveMethod();

    int getPostParamCookieSize();

    boolean getAllowLogoutPageRedirectToAnyHost();

    String getSSOCookieName();

    /**
     * Is failover to BASIC from CLIENT_CERT allowed?
     *
     * @return {@code true} if BASIC failover is allowed
     */
    boolean getAllowFailOverToBasicAuth();

    boolean getDisplayAuthenticationRealm();

    List<String> getLogoutPageRedirectDomainList();

    boolean getHttpOnlyCookies();

    boolean getWebAlwaysLogin();

    boolean getSSORequiresSSL();

    List<String> getSSODomainList();

    List<String> getWASReqURLRedirectDomainNames();

    boolean getSSOUseDomainFromURL();

    boolean isUseLtpaSSOForJaspic();

    boolean isUseAuthenticationDataForUnprotectedResourceEnabled();

    /**
     * Calculates the delta between this WebAppSecurityConfig and the provided
     * WebAppSecurityConfig. The values returned are the values from this Object.
     * If no properties were changed, an empty String should be returned.
     *
     * @param webAppSecConfig WebAppSecurityConfig object to compare settings against
     * @return String in the format of "name=value, name=value, ..." encapsulating the
     *         properties that are different between this WebAppSecurityConfig and the specified one
     */
    String getChangedProperties(WebAppSecurityConfig original);

    /**
     * Calculates the delta between this WebAppSecurityConfig and the provided
     * WebAppSecurityConfig. The values returned are the values from this Object.
     * If no properties were changed, an empty Map should be returned.
     *
     * @param webAppSecConfig WebAppSecurityConfig object to compare settings against
     * @return Map of modified attributes. key is the name of modified attributes, and value is this object.
     *         if the value is not set, the empty string is set.
     */
    Map<String, String> getChangedPropertiesMap(WebAppSecurityConfig original);

    String getLoginFormURL();

    /**
     * Returns loginErrorURL metadata in webAppSecurity.
     *
     * @return String the URL of the global login error page.
     */
    String getLoginErrorURL();

    /**
     * Is failover to FORM from CLIENT_CERT allowed?
     *
     * @return {@code true} if FORM failover is allowed
     */
    boolean getAllowFailOverToFormLogin();

    /**
     * Is failover to the application defined (either login config or JSR375 HAM) from CLIENT_CERT allowed?
     *
     * @return {@code true} if failover to applicatioin defined is allowed
     */
    boolean getAllowFailOverToAppDefined();

    /**
     * Is any failover from CLIENT_CERT allowed?
     *
     * @return {@code true} if any failover is allowed
     */
    boolean allowFailOver();

    /**
     * Is tracking of logged out LTPA tokens enabled.
     */
    boolean isTrackLoggedOutSSOCookiesEnabled();

    /**
     * Is use only custom cookie name enabled.
     */
    boolean isUseOnlyCustomCookieName();

    SSOCookieHelper createSSOCookieHelper();

    ReferrerURLCookieHandler createReferrerURLCookieHandler();

    WebAuthenticatorProxy createWebAuthenticatorProxy();

    /**
     * Returns the value of overrideHttpAuthMethod
     */
    String getOverrideHttpAuthMethod();

    /**
     * Returns the value of loginFormContextRoot
     */
    String getLoginFormContextRoot();

    /**
     * Returns the value of basicAuthRealmName
     */
    String getBasicAuthRealmName();
}