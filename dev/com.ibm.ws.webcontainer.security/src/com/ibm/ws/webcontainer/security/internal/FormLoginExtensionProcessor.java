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

package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class FormLoginExtensionProcessor extends WebExtensionProcessor {
    private static final TraceComponent tc = Tr.register(FormLoginExtensionProcessor.class);
    private final SubjectManager subjectManager;
    private final AuthenticationService authenticationService;
    private final UserRegistry userRegistry;
    private final SecurityMetadata securityMetadata;
    private final WebAppSecurityConfig webAppSecConfig;
    protected SSOCookieHelper ssoCookieHelper;
    private String appName = null;
    private final WebProviderAuthenticatorProxy providerAuthenticatorProxy;
    private ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef = null;
    ReferrerURLCookieHandler referrerURLHandler = null;
    String errorPage = null;
    private WebAppConfig wac = null;

    /**
     * This class handles form login and openId authentication and call by the WebContainer
     * extension processor. It's store the original request and re-direct to a
     * login page for user id and password.
     * If anything wrong in the process, it redirects to an error page.
     *
     * @param webAppSecConfig
     * @param authenticationService
     * @param userRegistry
     * @param webapp
     * @param rpServiceRef
     * @param providerAuthenticatorProxy
     */
    public FormLoginExtensionProcessor(WebAppSecurityConfig webAppSecConfig,
                                       AuthenticationService authenticationService,
                                       UserRegistry userRegistry,
                                       IServletContext webapp,
                                       WebProviderAuthenticatorProxy providerAuthenticatorProxy,
                                       ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {
        super(webapp);
        this.subjectManager = new SubjectManager();
        this.authenticationService = authenticationService;
        this.userRegistry = userRegistry;
        this.webAppSecConfig = webAppSecConfig;
        this.providerAuthenticatorProxy = providerAuthenticatorProxy;
        this.webAuthenticatorRef = webAuthenticatorRef;
        ssoCookieHelper = webAppSecConfig.createSSOCookieHelper();
        referrerURLHandler = webAppSecConfig.createReferrerURLCookieHandler();

        this.wac = webapp.getWebAppConfig();

        WebModuleMetaData wmmd = ((WebAppConfigExtended) wac).getMetaData();
        appName = wac.getApplicationName();
        securityMetadata = (SecurityMetadata) wmmd.getSecurityMetaData();

    }

    /** {@inheritDoc} */
    @Override
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            boolean handleByProvider = handleProviderAuthenticate(request, response);
            if (!handleByProvider) {
                formLogin(request, response, referrerURLHandler);
            }
        }
    }

    private boolean handleProviderAuthenticate(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        WebRequest webRequest =
//                        new WebRequestImpl(request, response, wac.getApplicationName(), null,
//                                        securityMetadata, null, webAppSecConfig);
        HashMap<String, Object> props = null;
        if (isJaspiEnabled()) {
            props = new HashMap<String, Object>();
            props.put("authType", "FORM_LOGIN");
            props.put("webAppConfig", wac);
            props.put("securityMetadata", securityMetadata);
            props.put("webAppSecurityConfig", webAppSecConfig);
        }
        AuthenticationResult authResult = providerAuthenticatorProxy.authenticate(request, response, props);
        if (authResult.getStatus() == AuthResult.CONTINUE) {
            return false;
        }
        if (authResult.getStatus() == AuthResult.REDIRECT_TO_PROVIDER) {
            return true;
        }
        if (authResult.getStatus() != AuthResult.SUCCESS) {
            WebRequest webRequest = new WebRequestImpl(request, response, null, webAppSecConfig);
            WebReply reply = new DenyReply("AuthenticationFailed");
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(reply.getStatusCode()));
            if (!isJaspiEnabled() || !isPostLoginProcessDone(request)) {
                handleError(request, response);
            }
        } else {
            postFormLoginProcess(request, response, authResult.getSubject());
        }
        return true;
    }

    /**
     * @param req
     * @param res
     * @param referrerURLHandler
     * @throws ServletException
     * @throws IOException
     */
    private void formLogin(HttpServletRequest req, HttpServletResponse res, ReferrerURLCookieHandler referrerURLHandler) throws ServletException, IOException {
        CollaboratorUtils collabUtils;

        boolean compliance = webAppSecConfig.getLogoutOnHttpSessionExpire();
        if (compliance && req.getRequestedSessionId() != null && (req.isRequestedSessionIdValid() == false)) {
            req.getSession(true);
        }
        if (!webAppSecConfig.isSingleSignonEnabled()) {
            Tr.error(tc, "SEC_FORM_LOGIN_BAD_CONFIG", new Object[] { appName });
            handleError(req, res);
            return;
        }
        String username = req.getParameter("j_username");
        String password = req.getParameter("j_password");

        if ((username == null) || (password == null) || (password.length() == 0)) {
            handleError(req, res);
            return;
        }
        BasicAuthAuthenticator basicAuthAuthenticator = new BasicAuthAuthenticator(authenticationService, userRegistry, ssoCookieHelper, webAppSecConfig);

        AuthenticationResult authResult = basicAuthAuthenticator.basicAuthenticate(null, username, password, req, res);
        authResult.setTargetRealm(authResult.realm);

        if (authResult.getStatus() != AuthResult.SUCCESS) {
            handleError(req, res);
            String realm = authResult.realm;
            WebRequest webRequest = new WebRequestImpl(req, res, null, webAppSecConfig);
            WebReply reply = new DenyReply("AuthenticationFailed");
            authResult.setAuditCredType("FORM");
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(reply.getStatusCode()));

            return;
        }

        postFormLoginProcess(req, res, authResult.getSubject());
    }

    /**
     * @param res
     * @throws IOException
     */
    private void handleError(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        if (getErrorPage(req, res) != null) {
            res.sendRedirect(res.encodeURL(getErrorPage(req, res)));
        }
    }

    /**
     * @param req
     * @param res
     * @param subject
     * @throws IOException
     */
    protected void postFormLoginProcess(HttpServletRequest req, HttpServletResponse res, Subject subject) throws IOException, RuntimeException {
        String storedReq = null;
        subjectManager.setCallerSubject(subject);
        subjectManager.setInvocationSubject(subject);

        boolean isPostLoginProcessDone = isJaspiEnabled() && isPostLoginProcessDone(req);

        if (!isPostLoginProcessDone) {
            storedReq = getStoredReq(req, referrerURLHandler);
            // If storedReq(WASReqURL) is bad, RuntimeExceptions are thrown in isReferrerHostValid. These exceptions are not caught here. If we return here, WASReqURL is good.
            if (storedReq != null && storedReq.length() > 0) {
                ReferrerURLCookieHandler.isReferrerHostValid(PasswordNullifier.nullifyParams(req.getRequestURL().toString()), PasswordNullifier.nullifyParams(storedReq),
                                                             webAppSecConfig.getWASReqURLRedirectDomainNames());
            }
            ssoCookieHelper.addSSOCookiesToResponse(subject, req, res);
            referrerURLHandler.invalidateReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
            if (!res.isCommitted()) {
                res.sendRedirect(res.encodeURL(storedReq));
            }
        }
    }

    /**
     * Always be redirecting to a stored req with the web app... strip any initial slash
     *
     * @param req
     * @param referrerURLHandler
     * @return storedReq
     */
    @Sensitive
    private String getStoredReq(HttpServletRequest req, ReferrerURLCookieHandler referrerURLHandler) {
        String storedReq = referrerURLHandler.getReferrerURLFromCookies(req, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
        if (storedReq != null) {
            if (storedReq.equals("/"))
                storedReq = "";
            else if (storedReq.startsWith("/"))
                storedReq = storedReq.substring(1);
        } else {
            storedReq = "";
        }
        return storedReq;
    }

    /**
     * Set up an error page as a full URL (http;//host:port/ctx/path)
     *
     * @param req
     * @param loginErrorPage
     * @return errorPage
     */
    private String setUpAFullUrl(HttpServletRequest req, String loginErrorPage, boolean bCtx) {
        String errorPage = null;
        if (loginErrorPage != null) {
            if (loginErrorPage.startsWith("http://") || loginErrorPage.startsWith("https://")) {
                return loginErrorPage;
            }
            if (!loginErrorPage.startsWith("/"))
                loginErrorPage = "/" + loginErrorPage;
            StringBuffer URL = req.getRequestURL();
            String URLString = URL.toString();

            int index = URLString.indexOf("//");
            index = URLString.indexOf("/", index + 2);
            int endindex = URLString.length();

            if (bCtx) {
                String ctx = req.getContextPath();
                if (ctx.equals("/"))
                    ctx = "";
                errorPage = ctx + loginErrorPage;
            } else {
                errorPage = loginErrorPage;
            }

            URL.replace(index, endindex, errorPage);
            errorPage = URL.toString();
        }
        return errorPage;
    }

    /**
     *
     */
    private String getErrorPage(HttpServletRequest req, HttpServletResponse res) {
        boolean bCtx = true;
        String loginErrorPage = getCustomErrorPage(req);
        if (loginErrorPage == null || loginErrorPage.length() == 0) {
            bCtx = false;
            loginErrorPage = getCustomReloginErrorPage(req);
        }

        if (loginErrorPage == null || loginErrorPage.length() == 0) {
            bCtx = true;
            // still nothing?
            loginErrorPage = getErrorPageFromWebXml();
        }
        // look for global error page.
        if (loginErrorPage == null || loginErrorPage.length() == 0) {
            bCtx = false;
            loginErrorPage = webAppSecConfig.getLoginErrorURL();
        }
        if (loginErrorPage != null) {

            return setUpAFullUrl(req, loginErrorPage, bCtx);
        }

        return null;
    }

    /**
     * Get custom error page that specified in the custom login page
     *
     * @param req
     * @return
     */
    private String getCustomErrorPage(HttpServletRequest req) {
        return req.getParameter("error_page");
    }

    /**
     * Get custom error page that specified in the custom login page
     *
     * @param req
     * @return
     */
    private String getCustomReloginErrorPage(HttpServletRequest req) {
        String reLogin = CookieHelper.getCookieValue(req.getCookies(), ReferrerURLCookieHandler.CUSTOM_RELOGIN_URL_COOKIENAME);
        if (reLogin != null && reLogin.length() > 0) {
            if (reLogin.indexOf("?") < 0)
                reLogin += "?error=error";
        }
        return reLogin;
    }

    /**
     * @param loginErrorPage
     * @return
     */
    private String getErrorPageFromWebXml() {
        String loginErrorPage = null;
        FormLoginConfiguration formLoginConfig = null;
        LoginConfiguration loginConfig = securityMetadata.getLoginConfiguration();
        if (loginConfig != null) {
            formLoginConfig = loginConfig.getFormLoginConfiguration();
            if (formLoginConfig != null)
                loginErrorPage = formLoginConfig.getErrorPage();
        }
        return loginErrorPage;
    }

    /**
     * @return true if jaspi is enabled
     */
    private boolean isJaspiEnabled() {
        WebAuthenticator jaspiAuthenticator = webAuthenticatorRef != null ? webAuthenticatorRef.getService("com.ibm.ws.security.jaspi") : null;
        return jaspiAuthenticator != null;
    }

    private boolean isPostLoginProcessDone(HttpServletRequest req) {
        Boolean result = (Boolean)req.getAttribute("com.ibm.ws.security.javaeesec.donePostLoginProcess");
        if (result != null && result) {
            return true;
        }
        return false;
    }

}
