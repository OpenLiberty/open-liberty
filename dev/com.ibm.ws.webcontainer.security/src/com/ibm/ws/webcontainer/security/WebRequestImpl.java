/*******************************************************************************
 * Copyright (c) 2011, 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

/**
 *
 */
public class WebRequestImpl implements WebRequest {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final String appName;
    private final WebSecurityContext webSecurityContext;
    private final MatchResponse matchResponse;
    private final SecurityMetadata securityMetadata;
    private final WebAppSecurityConfig config;
    private boolean formLoginRedirect = true;
    private boolean callAfterSSO = true;
    private boolean unprotectedURI = false;
    private boolean specialUnprotectedURI = false;
    private Map<String, Object> propMap = null;
    private boolean requestAuthenticate = false;
    private boolean disableClientCertFailOver = false;

    public WebRequestImpl(HttpServletRequest req, HttpServletResponse resp,
                          SecurityMetadata securityMetadata, WebAppSecurityConfig config) {
        this(req, resp, null, null, securityMetadata, null, config);
    }

    public WebRequestImpl(HttpServletRequest req, HttpServletResponse resp,
                          String appName, WebSecurityContext webSecurityContext,
                          SecurityMetadata securityMetadata, MatchResponse matchResponse,
                          WebAppSecurityConfig config) {
        this.request = req;
        this.response = resp;
        this.appName = appName;
        this.webSecurityContext = webSecurityContext;
        this.matchResponse = matchResponse;
        this.securityMetadata = securityMetadata;
        this.config = config;
    }

    /** {@inheritDoc} */
    @Override
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public HttpServletResponse getHttpServletResponse() {
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public String getApplicationName() {
        return appName;
    }

    /** {@inheritDoc} */
    @Override
    public WebSecurityContext getWebSecurityContext() {
        return webSecurityContext;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFormLoginRedirectEnabled() {
        return formLoginRedirect;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getRequiredRoles() {
        return matchResponse.getRoles();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSSLRequired() {
        return matchResponse.isSSLRequired();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAccessPrecluded() {
        return matchResponse.isAccessPrecluded();
    }

    /** {@inheritDoc} */
    @Override
    public SecurityMetadata getSecurityMetadata() {
        return securityMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public LoginConfiguration getLoginConfig() {
        LoginConfiguration loginConfig = securityMetadata != null ? securityMetadata.getLoginConfiguration() : null;
        return loginConfig;
    }

    /** {@inheritDoc} */
    @Override
    public FormLoginConfiguration getFormLoginConfiguration() {
        return securityMetadata.getLoginConfiguration().getFormLoginConfiguration();
    }

    @Override
    public MatchResponse getMatchResponse() {
        return this.matchResponse;
    }

    /**
     * This unfortunately consolidates and duplicates a lot of logic from the different
     * authenticators. However, this is necessary to avoid additional work if we don't
     * have any of this information.
     *
     * @return {@code true} if some authentication data is available, {@code false} otherwise.
     */
    private boolean determineIfRequestHasAuthenticationData() {
        // Do we have a BasicAuth header?
        boolean hasBasicAuthHeader = request.getHeader(BasicAuthAuthenticator.BASIC_AUTH_HEADER_NAME) != null;

        // Do we have client cert data?
        boolean hasClientCertHeader = false;

        LoginConfiguration loginConfig = getLoginConfig();
        String authenticationMethod = null;
        if (loginConfig != null) {
            authenticationMethod = loginConfig.getAuthenticationMethod();
        }
        if (LoginConfiguration.CLIENT_CERT.equals(authenticationMethod)) {
            X509Certificate certChain[] = (X509Certificate[]) request.getAttribute(CertificateLoginAuthenticator.PEER_CERTIFICATES);
            if (certChain == null || certChain.length == 0) {
                hasClientCertHeader = false;
            } else {
                hasClientCertHeader = true;
            }
        }

        // Do we have an SSO cookie?
        boolean hasSSOToken = false;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            hasSSOToken = false;
        } else {
            // check SSO Cookie
            String cookieName = config.getSSOCookieName();
            String[] hdrVals = CookieHelper.getCookieValues(cookies, cookieName);
            if (hdrVals == null && !SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(cookieName)) {
                hdrVals = CookieHelper.getCookieValues(cookies, SSOAuthenticator.DEFAULT_SSO_COOKIE_NAME);
            }
            if (hdrVals != null) {
                hasSSOToken = true;
            }
        }

        // If any of that is true, we have authentication data!
        return hasBasicAuthHeader || hasClientCertHeader || hasSSOToken;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAuthenticationData() {
        return determineIfRequestHasAuthenticationData();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUnprotectedURI() {
        return unprotectedURI;
    }

    /** {@inheritDoc} */
    @Override
    public void setUnprotectedURI(boolean unprotectedURI) {
        this.unprotectedURI = unprotectedURI;
    }

    @Override
    public void disableFormLoginRedirect() {
        formLoginRedirect = false;
    }

    @Override
    public boolean isProviderSpecialUnprotectedURI() {
        return specialUnprotectedURI;
    }

    @Override
    public void setProviderSpecialUnprotectedURI(boolean specialUnprotectedURI) {
        this.specialUnprotectedURI = specialUnprotectedURI;
    }

    @Override
    public void setCallAfterSSO(boolean callAfterSSO) {
        this.callAfterSSO = callAfterSSO;
    }

    @Override
    public boolean isCallAfterSSO() {
        return callAfterSSO;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebRequest#getProperties()
     */
    @Override
    public Map<String, Object> getProperties() {
        return propMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.WebRequest#setProperties(java.util.HashMap)
     */
    @Override
    public void setProperties(Map<String, Object> props) {
        propMap = props;
    }

    @Override
    public boolean isRequestAuthenticate() {
        return requestAuthenticate;
    }

    /*
     * Set to true if handling an HttpServletRequest.authenticate.
     */
    @Override
    public void setRequestAuthenticate(boolean requestAuthenticate) {
        this.requestAuthenticate = requestAuthenticate;
    }

    @Override
    public boolean isDisableClientCertFailOver() {
        return disableClientCertFailOver;
    }

    @Override
    public void setDisableClientCertFailOver(boolean isDisable) {
        disableClientCertFailOver = isDisable;
    }

}
