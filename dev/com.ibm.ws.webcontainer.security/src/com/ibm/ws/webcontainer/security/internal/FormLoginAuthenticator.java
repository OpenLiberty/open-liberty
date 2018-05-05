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

import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;

/**
 * This class perform authentication for web request using form
 * login with user id/pwd or single sign on cookie.
 */
public class FormLoginAuthenticator implements WebAuthenticator {
    private static final TraceComponent tc = Tr.register(FormLoginAuthenticator.class);
    private final WebAuthenticator ssoAuthenticator;
    private final WebAppSecurityConfig webAppSecurityConfig;
    private final PostParameterHelper postParameterHelper;
    private final WebProviderAuthenticatorProxy providerAuthenticatorProxy;

    /**
     * @param providerAuthenticatorProxy
     * @param authenticationService
     * @param securityMetadata
     */
    public FormLoginAuthenticator(WebAuthenticator ssoAuthn,
                                  WebAppSecurityConfig webAppSecConfig,
                                  WebProviderAuthenticatorProxy providerAuthenticatorProxy) {
        webAppSecurityConfig = webAppSecConfig;
        ssoAuthenticator = ssoAuthn;
        this.providerAuthenticatorProxy = providerAuthenticatorProxy;
        postParameterHelper = new PostParameterHelper(webAppSecConfig);
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        return authenticate(webRequest, webAppSecurityConfig);
    }

    /**
     * @param webRequest
     * @return AuthenticationResult
     */
    public AuthenticationResult authenticate(WebRequest webRequest, WebAppSecurityConfig webAppSecurityConfigImpl) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();
        AuthenticationResult authResult = handleFormLogin(req, res, webRequest);
        return authResult;
    }

    /**
     * This method handle formlogin; If the SSO cookie exist, then it will use the cookie to authenticate. If
     * the cookie does not exist, then it will re-redirect to the login page.
     *
     * @param req
     * @param res
     * @param enableRedirect
     * @return
     */
    private AuthenticationResult handleFormLogin(HttpServletRequest req, HttpServletResponse res, WebRequest webRequest) {
        AuthenticationResult authResult = null;
        authResult = ssoAuthenticator.authenticate(webRequest);

        if (authResult != null) {
            authResult.setAuditCredType(AuditEvent.CRED_TYPE_FORM);
        }

        if (authResult != null && authResult.getStatus() != AuthResult.FAILURE) {
            postParameterHelper.restore(req, res);
            return authResult;
        }

        try {
            authResult = providerAuthenticatorProxy.authenticate(req, res, null);
        } catch (Exception e) {
            return new AuthenticationResult(AuthResult.FAILURE, e.getLocalizedMessage());
        }

        if (authResult.getStatus() == AuthResult.CONTINUE) {
            authResult = null;
            if (webRequest.isFormLoginRedirectEnabled()) {
                authResult = handleRedirect(req, res, webRequest);
                if (authResult != null) {
                    authResult.setAuditCredType(AuditEvent.CRED_TYPE_FORM);
                    authResult.setAuditOutcome(AuditEvent.OUTCOME_REDIRECT);
                }

            }
        }
        return authResult;
    }

    /**
     * This method save post parameters in the cookie or session and
     * redirect to a login page.
     *
     * @param req
     * @param res
     * @param loginURL
     * @return authenticationResult
     */
    private AuthenticationResult handleRedirect(HttpServletRequest req,
                                                HttpServletResponse res,
                                                WebRequest webRequest) {
        AuthenticationResult authResult;

        String loginURL = getFormLoginURL(req, webRequest, webAppSecurityConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "form login URL: " + loginURL);
        }

        authResult = new AuthenticationResult(AuthResult.REDIRECT, loginURL);

        if (allowToAddCookieToResponse(webAppSecurityConfig, req)) {
            postParameterHelper.save(req, res, authResult);
            ReferrerURLCookieHandler referrerURLHandler = webAppSecurityConfig.createReferrerURLCookieHandler();
//            referrerURLHandler.setReferrerURLCookie(authResult, getReqURL(req));
            Cookie c = referrerURLHandler.createReferrerURLCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, getReqURL(req), req);
            authResult.setCookie(c);
        }

        return authResult;
    }

    /**
     * @param req
     * @return
     */
    @Sensitive
    private String getReqURL(HttpServletRequest req) {
        String url = null;
        StringBuffer reqURL = req.getRequestURL();
        if (req.getQueryString() != null) {
            reqURL.append("?");
            reqURL.append(req.getQueryString());
        }
        return reqURL.toString();
    }

    private String normalizeURL(String url, String contextPath) {
        if (!url.startsWith("/"))
            url = "/" + url;
        if (contextPath == null) {
            return url;
        } else {
            if (contextPath.equals("/"))
                contextPath = "";
            return contextPath + url;
        }
    }

    /**
     * This method get the form login page from the web.xml if it's existed. Otherwise will get the form
     * login page (loginFormURL) from the webAppSecurity element. The loginFormURL must include the contextRoot
     *
     * @param req
     * @param webRequest
     * @param webAppSecConfig
     * @return
     */
    //TODO: java doc
    private String getFormLoginURL(HttpServletRequest req, WebRequest webRequest, WebAppSecurityConfig webAppSecConfig) {
        FormLoginConfiguration formLoginConfig = webRequest.getFormLoginConfiguration();
        String inURL = null;
        String contextPath = null;
        // if the global login is set as CERT and the fallback is allowed to FORM,
        // use global form login.
        String authMech = webAppSecConfig.getOverrideHttpAuthMethod();
        if (authMech != null && authMech.equals("CLIENT_CERT") && webAppSecConfig.getAllowFailOverToFormLogin()) {
            inURL = webAppSecConfig.getLoginFormURL();
        } else if (formLoginConfig != null) {
            inURL = formLoginConfig.getLoginPage();
            if (inURL != null)
                contextPath = req.getContextPath();
            else
                inURL = webAppSecConfig.getLoginFormURL();
        }
        return buildFormLoginURL(req, inURL, contextPath);
    }

    /**
     * @param req
     * @param inURL
     * @param contextPath
     * @return
     */
    private String buildFormLoginURL(HttpServletRequest req, String inURL, String contextPath) {
        if (inURL == null)
            return null;
        StringBuilder builder = new StringBuilder(req.getRequestURL());
        int hostIndex = builder.indexOf("//");
        int contextIndex = builder.indexOf("/", hostIndex + 2);
        builder.replace(contextIndex, builder.length(), normalizeURL(inURL, contextPath));
        return builder.toString();
    }

    /**
     * This method checks the following conditions:
     * 1) If SSO requires SSL is true and NOT HTTPs request, returns false.
     * 2) Otherwise returns true.
     *
     * @param req
     * @return
     */
    private boolean allowToAddCookieToResponse(WebAppSecurityConfig config, HttpServletRequest req) {
        boolean secureRequest = req.isSecure();
        if (config.getSSORequiresSSL() && !secureRequest) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSO requires SSL. The cookie will not be sent back because the request is not over https.");
            }
            return false;
        }
        return true;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) throws Exception {
        return null;
    }

}
