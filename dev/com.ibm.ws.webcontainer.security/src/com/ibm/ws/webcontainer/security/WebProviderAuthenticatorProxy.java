/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.internal.TAIAuthenticator;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * The responsibility of this WebProviderAuthenticatorProxy is to authenticate request with TAI and SSO
 *
 */
public class WebProviderAuthenticatorProxy implements WebAuthenticator {

    private static final TraceComponent tc = Tr.register(WebProviderAuthenticatorProxy.class);

    AuthenticationResult JASPI_CONT = new AuthenticationResult(AuthResult.CONTINUE, "JASPI said continue...");
    protected final AtomicServiceReference<SecurityService> securityServiceRef;
    protected final AtomicServiceReference<TAIService> taiServiceRef;
    protected final ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef;
    protected volatile WebAppSecurityConfig webAppSecurityConfig;

    protected final ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef;

    public WebProviderAuthenticatorProxy(AtomicServiceReference<SecurityService> securityServiceRef,
                                         AtomicServiceReference<TAIService> taiServiceRef,
                                         ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                         WebAppSecurityConfig webAppSecurityConfig,
                                         ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {

        this.securityServiceRef = securityServiceRef;
        this.taiServiceRef = taiServiceRef;
        this.interceptorServiceRef = interceptorServiceRef;
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.webAuthenticatorRef = webAuthenticatorRef;
    }

    /*
     * This method is the main method calling by the WebAuthenticatorProxy to handle TAI and SSO
     */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        AuthenticationResult authResult = handleTAI(webRequest, true);
        if (authResult.getStatus() == AuthResult.CONTINUE) {
            authResult = handleSSO(webRequest, null);
            if (authResult.getStatus() == AuthResult.CONTINUE) {
                webRequest.setCallAfterSSO(true);
                authResult = handleTAI(webRequest, false);
            }
        }

        return authResult;

    }

    /**
     * @param webRequest
     * @return
     */
    protected AuthenticationResult handleJaspi(final WebRequest webRequest, final HashMap<String, Object> props) {
        AuthenticationResult authResult = JASPI_CONT;
        if (webAuthenticatorRef != null) {
            JaspiService jaspiService = (JaspiService) webAuthenticatorRef.getService("com.ibm.ws.security.jaspi");
            if (jaspiService != null) {
                HttpServletRequest request = webRequest.getHttpServletRequest();
                if (props == null) {
                    authResult = authenticateForOtherMechanisms(webRequest, authResult, jaspiService);
                } else {
                    authResult = authenticateForFormMechanism(webRequest, props, jaspiService);
                }

                if (authResult.getStatus() == AuthResult.SUCCESS) {
                    if (System.getSecurityManager() == null) {
                        processAuthenticationSuccess(webRequest, props, authResult);
                    } else {
                        final AuthenticationResult authResultFinal = authResult;
                        AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                processAuthenticationSuccess(webRequest, props, authResultFinal);
                                return null;
                            }
                        });
                    }
                }
            }
        }
        return authResult;
    }

    private AuthenticationResult authenticateForOtherMechanisms(WebRequest webRequest, AuthenticationResult authResult, JaspiService jaspiService) {
        authResult = handleSSO(webRequest, null);
        if (AuthResult.SUCCESS.equals(authResult.getStatus()) && webAppSecurityConfig.isUseLtpaSSOForJaspic()) {
            return authResult;
        }

        Subject subject = authResult.getSubject();
        List<String> tokenUsage = null;
        if (subject != null) {
            tokenUsage = getTokenUsageFromSSOToken(subject, webAppSecurityConfig.createSSOCookieHelper());
            // in order to avoid using the cached subject which was created as a result of SSO from non JASPIC LTPAToken,
            // clear the cached object if LTPAToken is not created by JASPIC.
            if (!isJaspicSessionOrJsr375Form(tokenUsage)) {
                // clear the cache.
                clearCacheData(subject);
            }
        }
        boolean isNewAuth = jaspiService.isProcessingNewAuthentication(webRequest.getHttpServletRequest());

        if (!isJaspicForm(tokenUsage)) {
            if (!isNewAuth && isJaspicSessionOrJsr375Form(tokenUsage)) {
                Map<String, Object> requestProps = new HashMap<String, Object>();
                requestProps.put("javax.servlet.http.registerSession.subject", subject);
                webRequest.setProperties(requestProps);
            }
            authResult = jaspiService.authenticate(webRequest);
        }
        AuthResult result = authResult.getStatus();
        if (result != AuthResult.CONTINUE) {
            if (!isNewAuth) {
                if ("BASIC".equals(authResult.getAuditAuthConfigProviderAuthType())) {
                    // check BA header, and if it exists, use denied and set username, otherwise, challenge
                    String authHeader = webRequest.getHttpServletRequest().getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Basic ")) {
                        String basicAuthHeader = decodeCookieString(authHeader.substring(6));
                        int index = basicAuthHeader.indexOf(':');
                        String uid = basicAuthHeader.substring(0, index);
                        authResult.setAuditCredValue(uid);
                    }
                    if (result == AuthResult.SEND_401) {
                        authResult.setAuditOutcome(AuditEvent.OUTCOME_CHALLENGE);
                    }
                }
                if (result == AuthResult.RETURN) {
                    authResult.setAuditOutcome(AuditEvent.OUTCOME_DENIED);
                }
                authResult.setAuditCredType(AuditEvent.CRED_TYPE_JASPIC);
            } else {
                //TODO: is audit event required?? if so, how to get uid??
            }
        }
        return authResult;
    }

    private AuthenticationResult authenticateForFormMechanism(WebRequest webRequest, HashMap<String, Object> props, JaspiService jaspiService) {
        AuthenticationResult authResult;
        try {
            HttpServletRequest req = webRequest.getHttpServletRequest();
            authResult = jaspiService.authenticate(req,
                                                   webRequest.getHttpServletResponse(),
                                                   props);
            if (authResult.getStatus() != AuthResult.CONTINUE) {
                String authHeader = webRequest.getHttpServletRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Basic ")) {
                    String basicAuthHeader = decodeCookieString(authHeader.substring(6));
                    int index = basicAuthHeader.indexOf(':');
                    String uid = basicAuthHeader.substring(0, index);
                    authResult.setAuditCredValue(uid);
                } else {
                    // best effort to set the user id for audit message upon error.
                    String username = req.getParameter("j_username");
                    if (username != null) {
                        authResult.setAuditCredValue(username);
                    }
                }
                authResult.setAuditCredType(AuditEvent.CRED_TYPE_JASPIC);
                authResult.setAuditOutcome(AuditEvent.OUTCOME_DENIED);
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error handling JASPI request", e);
            }
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        }
        return authResult;
    }

    private void processAuthenticationSuccess(WebRequest webRequest, HashMap<String, Object> props, AuthenticationResult authResult) {
        Subject subject = authResult.getSubject();
        attemptToRestorePostParams(webRequest);
        boolean isRegisterSession = false;
        Map<String, Object> reqProps = webRequest.getProperties();
        if (reqProps != null) {
            isRegisterSession = Boolean.valueOf((String) reqProps.get("javax.servlet.http.registerSession")).booleanValue();
        }
        final SSOCookieHelper ssoCh = webAppSecurityConfig.createSSOCookieHelper();
        if (isRegisterSession) {
            registerSession(webRequest, subject, ssoCh);
        } else {
            List<String> tokenUsage = getTokenUsageFromSSOToken(subject, ssoCh);
            if (isJaspicAttribute(tokenUsage)) {
                if (isFormLogin(props)) {
                    // since the form login flow does not propagate props form jaspicService, as an alternative,
                    // check whether the subject contains jaspicSession attribute when form login is carried out.
                    // if that's the case, generate SSO cookie.
                    registerSession(webRequest, subject, ssoCh);
                } else if (!isJaspicSession(tokenUsage)) {
                    if (webAppSecurityConfig.isUseLtpaSSOForJaspic() == false) {
                        // there is a sso cookie for form login, it can be deleted now.
                        ssoCh.createLogoutCookies(webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
                    }
                }
            } else {
                if (webAppSecurityConfig.isUseLtpaSSOForJaspic() == false) {
                    attemptToRemoveLtpaToken(webRequest, props);
                }
            }
        }
    }

    private void registerSession(final WebRequest webRequest, final Subject subject, final SSOCookieHelper ssoCh) {
        if (System.getSecurityManager() == null) {
            ssoCh.addSSOCookiesToResponse(subject, webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    ssoCh.addSSOCookiesToResponse(subject, webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
                    return null;
                }
            });
        }
    }

    private HttpServletResponse attemptToRestorePostParams(WebRequest webRequest) {
        HttpServletResponse res = webRequest.getHttpServletResponse();
        if (!res.isCommitted()) {
            restorePostParams(webRequest);
        }
        return res;
    }

    protected void restorePostParams(WebRequest webRequest) {
        PostParameterHelper postParameterHelper = new PostParameterHelper(webAppSecurityConfig);
        postParameterHelper.restore(webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
    }

    /*
     * Remove LTPA token if this is not a FORM login and the JASPI provider has not committed the response.
     */
    private void attemptToRemoveLtpaToken(WebRequest webRequest, HashMap<String, Object> props) {
        SSOCookieHelper ssoCh = webAppSecurityConfig.createSSOCookieHelper();
        if (!isFormLogin(props)) {
            HttpServletResponse res = webRequest.getHttpServletResponse();
            if (!res.isCommitted()) {
                ssoCh.removeSSOCookieFromResponse(res);
            }
        }
    }

    /*
     * This method is called by the FormLoginExtensionProcessor
     */
    @Override
    public AuthenticationResult authenticate(HttpServletRequest request,
                                             HttpServletResponse response,
                                             HashMap<String, Object> props) throws Exception {
        WebRequest webRequest = new WebRequestImpl(request, response, null, null, null, null, null);
        AuthenticationResult authResult = handleJaspi(webRequest, props);
        return authResult;
    }

    /**
     * @param taiAuthenticator
     * @param webRequest
     * @param beforeSSO
     * @return
     */
    protected AuthenticationResult handleTAI(WebRequest webRequest, boolean beforeSSO) {
        TAIAuthenticator taiAuthenticator = getTaiAuthenticator();
        AuthenticationResult authResult = null;
        if (taiAuthenticator == null) {
            authResult = new AuthenticationResult(AuthResult.CONTINUE, "TAI invoke " + (beforeSSO == true ? "before" : "after") + " SSO is not available, skipping TAI...");
        } else {
            authResult = taiAuthenticator.authenticate(webRequest, beforeSSO);
            if (authResult.getStatus() != AuthResult.CONTINUE) {
                authResult.setAuditCredType(AuditEvent.CRED_TYPE_TAI);
            }
        }
        return authResult;
    }

    protected AuthenticationResult handleSSO(WebRequest webRequest, String ssoCookieName) {
        WebAuthenticator authenticator = getSSOAuthenticator(webRequest, ssoCookieName);
        AuthenticationResult authResult = authenticator.authenticate(webRequest);
        if (authResult == null || authResult.getStatus() != AuthResult.SUCCESS) {
            authResult = new AuthenticationResult(AuthResult.CONTINUE, "SSO did not succeed, so continue ...");
        }
        return authResult;
    }

    /**
     * @param req
     * @param propagationTokenAuthenticated
     * @return
     */
    protected boolean isNotNullAndTrue(HttpServletRequest req, String key) {
        Boolean result = (Boolean) req.getAttribute(key);
        if (result != null) {
            return result.booleanValue();
        }
        return false;
    }

    /**
     * @return
     */
    protected TAIAuthenticator getTaiAuthenticator() {
        TAIAuthenticator taiAuthenticator = null;
        TAIService taiService = taiServiceRef.getService();
        Iterator<TrustAssociationInterceptor> interceptorServices = interceptorServiceRef.getServices();
        if (taiService != null || (interceptorServices != null && interceptorServices.hasNext())) {
            SecurityService securityService = securityServiceRef.getService();
            taiAuthenticator = new TAIAuthenticator(taiService, interceptorServiceRef, securityService.getAuthenticationService(), webAppSecurityConfig.createSSOCookieHelper());
        }

        return taiAuthenticator;
    }

    /**
     * Create an instance of SSOAuthenticator.
     *
     * @param webRequest
     * @return The SSOAuthenticator, or {@code null} if it could not be created.
     */
    public WebAuthenticator getSSOAuthenticator(WebRequest webRequest, String ssoCookieName) {
        SecurityMetadata securityMetadata = webRequest.getSecurityMetadata();
        SecurityService securityService = securityServiceRef.getService();
        SSOCookieHelper cookieHelper;
        if (ssoCookieName != null) {
            cookieHelper = new SSOCookieHelperImpl(webAppSecurityConfig, ssoCookieName);
        } else {
            cookieHelper = webAppSecurityConfig.createSSOCookieHelper();
        }
        return new SSOAuthenticator(securityService.getAuthenticationService(), securityMetadata, webAppSecurityConfig, cookieHelper);
    }

    /**
     * @return
     */
    public ConcurrentServiceReferenceMap<String, WebAuthenticator> getWebAuthenticatorRefs() {
        return webAuthenticatorRef;
    }

    @Sensitive
    private String decodeCookieString(@Sensitive String cookieString) {
        try {
            return Base64Coder.base64Decode(cookieString);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> getTokenUsageFromSSOToken(final Subject subject, final SSOCookieHelper ssoCh) {
        SingleSignonToken ssoToken = ssoCh.getDefaultSSOTokenFromSubject(subject);
        if (ssoToken != null) {
            String[] attrs = ssoToken.getAttributes(AuthenticationConstants.INTERNAL_AUTH_PROVIDER);
            if (attrs != null) {
                return Arrays.asList(attrs);
            }
        }
        return null;
    }

    private boolean isJaspicSessionOrJsr375Form(List<String> attrs) {
        if (attrs != null
            && (attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC) || attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JSR375_FORM))) {
            return true;
        }
        return false;
    }

    private boolean isJaspicSession(List<String> attrs) {
        if (attrs != null && attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC)) {
            return true;
        }
        return false;
    }

    private boolean isJaspicForm(List<String> attrs) {
        if (attrs != null && attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC_FORM)) {
            return true;
        }
        return false;
    }

    private boolean isJaspicAttribute(List<String> attrs) {
        if (attrs != null && (attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC) || attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JSR375_FORM)
                              || attrs.contains(AuthenticationConstants.INTERNAL_AUTH_PROVIDER_JASPIC_FORM))) {
            return true;
        }
        return false;
    }

    private boolean isFormLogin(Map<String, Object> props) {
        if (props != null && "FORM_LOGIN".equals(props.get("authType"))) {
            return true;
        }
        return false;
    }

    private void clearCacheData(Subject subject) {
        AuthCacheService authCacheService = securityServiceRef.getService().getAuthenticationService().getAuthCacheService();
        WSCredential credential = subject.getPublicCredentials(WSCredential.class).iterator().next();
        try {
            String authUserName = credential.getRealmName() + ":" + credential.getSecurityName();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Deleting cache entry of user : " + authUserName);
            }
            authCacheService.remove(authUserName);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A cache entry cannot be deleted. An exception is caught : " + e);
            }
        }
    }

}
