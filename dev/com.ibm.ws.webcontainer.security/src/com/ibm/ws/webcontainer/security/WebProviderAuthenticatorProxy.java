/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.webcontainer.security.internal.SSOAuthenticator;
import com.ibm.ws.webcontainer.security.internal.TAIAuthenticator;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Service;
import com.ibm.ws.webcontainer.security.openid20.OpenidClientService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
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

    static final List<String> authenticatorOdering = Collections.unmodifiableList(Arrays.asList(new String[] { "com.ibm.ws.security.spnego", "com.ibm.ws.security.openid" }));

    AuthenticationResult OAUTH_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OAuth service said continue...");
    AuthenticationResult OPENID_CLIENT_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID client service said continue...");
    AuthenticationResult OIDC_SERVER_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID Connect server said continue...");
    AuthenticationResult OIDC_CLIENT_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID Connect client said continue...");
    AuthenticationResult SPNEGO_CONT = new AuthenticationResult(AuthResult.CONTINUE, "SPNEGO said continue...");

    protected final AtomicServiceReference<OAuth20Service> oauthServiceRef;
    private final AtomicServiceReference<OpenidClientService> openIdClientServiceRef;
    private final AtomicServiceReference<OidcServer> oidcServerRef;
    private final AtomicServiceReference<OidcClient> oidcClientRef;
    private WebProviderAuthenticatorHelper authHelper;
    private ReferrerURLCookieHandler referrerURLCookieHandler = null;
    private WebAppSecurityConfig webAppSecurityConfig = null;

    protected final ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef;

    public WebProviderAuthenticatorProxy(AtomicServiceReference<SecurityService> securityServiceRef,
                                         AtomicServiceReference<TAIService> taiServiceRef,
                                         ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                         WebAppSecurityConfig webAppSecurityConfig,
                                         AtomicServiceReference<OAuth20Service> oauthServiceRef,
                                         AtomicServiceReference<OpenidClientService> openIdClientServiceRef,
                                         AtomicServiceReference<OidcServer> oidcServerRef,
                                         AtomicServiceReference<OidcClient> oidcClientRef,
                                         ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {

        this.securityServiceRef = securityServiceRef;
        this.taiServiceRef = taiServiceRef;
        this.interceptorServiceRef = interceptorServiceRef;
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.webAuthenticatorRef = webAuthenticatorRef;
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.oauthServiceRef = oauthServiceRef;
        this.oidcServerRef = oidcServerRef;
        this.openIdClientServiceRef = openIdClientServiceRef;
        this.oidcClientRef = oidcClientRef;

        authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);
        referrerURLCookieHandler = new ReferrerURLCookieHandler(webAppSecurityConfig);
    }

    /*
     * need for unit test*
     */
    public void setWebProviderAuthenticatorHelper(WebProviderAuthenticatorHelper authHelper) {
        this.authHelper = authHelper;
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
    public AuthenticationResult authenticate1(HttpServletRequest request,
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

    /*
     * This method is the main method calling by the WebAuthenticatorProxy to handle TAI and SSO
     */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        HttpServletRequest request = webRequest.getHttpServletRequest();
        HttpServletResponse response = webRequest.getHttpServletResponse();
        AuthenticationResult authResult = handleTAI(webRequest, true);
        if (authResult.getStatus() == AuthResult.CONTINUE) {
            authResult = handleAccessToken(webRequest);
            if (authResult.getStatus() == AuthResult.CONTINUE) {
                webRequest.setCallAfterSSO(false);
                authResult = handleSpnego(webRequest);
                if (authResult.getStatus() == AuthResult.CONTINUE) {
                    authResult = handleOidcClient(request, response, true);
                    if (authResult.getStatus() == AuthResult.CONTINUE) {
                        authResult = handleSSO(webRequest, null);
                        if (authResult.getStatus() == AuthResult.CONTINUE) {
                            webRequest.setCallAfterSSO(true);
                            authResult = handleSpnego(webRequest);
                            if (authResult.getStatus() == AuthResult.CONTINUE) {
                                authResult = handleTAI(webRequest, false);
                                if (authResult.getStatus() == AuthResult.CONTINUE) {
                                    authResult = handleOidcClient(request, response, false);
                                }
                            }
                        }
                    }
                }
            }
        }
        return authResult;
    }

    /*
     * This method is called by the FormLoginExtensionProcessor and UserAuthentication
     */
    @Override
    public AuthenticationResult authenticate(HttpServletRequest request,
                                             HttpServletResponse response,
                                             HashMap<String, Object> props) throws Exception {
        AuthenticationResult authResult;
        WebRequest webRequest = new WebRequestImpl(request, response, null, null, null, null, webAppSecurityConfig);
        if (props != null && props.get("authType").equals("com.ibm.ws.security.spnego")) {
            authResult = handleSpnego(webRequest);
        } else {
            authResult = handleJaspi(webRequest, props);
            if (authResult.getStatus() == AuthResult.CONTINUE) {
                authResult = handleOpenidClient(request, response);
            }
        }

        return authResult;
    }

    /**
     * @param webRequest
     * @param req
     * @param res
     * @return
     */
    private AuthenticationResult handleAccessToken(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();

        AuthenticationResult authResult = handleOAuth(req, res);
        if (authResult.getStatus() != AuthResult.CONTINUE) {
            authResult.setAuditCredType(AuditEvent.CRED_TYPE_OAUTH_TOKEN);
        }
        return authResult;
    }

    public AuthenticationResult handleSpnego(WebRequest webRequest) {
        AuthenticationResult authResult = SPNEGO_CONT;
        if (webAuthenticatorRef != null) {
            WebAuthenticator webAuthenticator = getSpnegoAuthenticator();
            if (webAuthenticator != null) {
                authResult = webAuthenticator.authenticate(webRequest);
                if (authResult.getStatus() == AuthResult.SUCCESS) {
                    HttpServletRequest request = webRequest.getHttpServletRequest();
                    HttpServletResponse response = webRequest.getHttpServletResponse();
                    authResult = authHelper.loginWithHashtable(request, response, authResult.getSubject());
                    if (AuthResult.SUCCESS == authResult.getStatus()) {
                        SSOCookieHelper ssoCh = webAppSecurityConfig.createSSOCookieHelper();
                        ssoCh.addSSOCookiesToResponse(authResult.getSubject(), request, response);
                    }
                }
            }
        }
        if (authResult.getStatus() != AuthResult.CONTINUE) {
            authResult.setAuditCredType(AuditEvent.CRED_TYPE_SPNEGO);
        }
        return authResult;
    }

    /**
     * @return
     */
    public WebAuthenticator getSpnegoAuthenticator() {
        WebAuthenticator webAuthenticator = webAuthenticatorRef.getService("com.ibm.ws.security.spnego");
        return webAuthenticator;
    }

    /*
     * The OpenID client redirects the request to OpenID provider for authentication
     */
    private AuthenticationResult handleOpenidClient(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticationResult authResult = OPENID_CLIENT_CONT;
        OpenidClientService openIdClientService = openIdClientServiceRef.getService();
        if (openIdClientService != null) {
            String opId = openIdClientService.getOpenIdIdentifier(request);
            if (opId != null && !opId.isEmpty()) {
                openIdClientService.createAuthRequest(request, response);
                authResult = new AuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, "OpenID client creates auth request...");
            } else if (openIdClientService.getRpRequestIdentifier(request, response) != null) {
                ProviderAuthenticationResult result = openIdClientService.verifyOpResponse(request, response);
                if (result.getStatus() != AuthResult.SUCCESS) {
                    return new AuthenticationResult(AuthResult.FAILURE, "OpenID client failed with status code " + result.getStatus());
                }

                authResult = authHelper.loginWithUserName(request, response, result.getUserName(), result.getSubject(), result.getCustomProperties(),
                                                          openIdClientService.isMapIdentityToRegistryUser());
            }
        }
        if (authResult.getStatus() != AuthResult.CONTINUE) {
            authResult.setAuditCredType(AuditEvent.CRED_TYPE_IDTOKEN);
        }
        return authResult;
    }

    /**
     * The OpenID Connect client redirects a request to the OpenID Connect provider for authentication.
     *
     * @param req
     * @param res
     * @return
     */
    private AuthenticationResult handleOidcClient(HttpServletRequest req, HttpServletResponse res, boolean firstCall) {
        AuthenticationResult authResult = OIDC_CLIENT_CONT;
        OidcClient oidcClient = oidcClientRef.getService();
        if (oidcClient == null) {
            return new AuthenticationResult(AuthResult.CONTINUE, "OpenID Connect client is not available, skipping OpenID Connect client...");
        }

        if (firstCall) {
            // let's check if any oidcClient need to be called beforeSso. If not, return
            if (!oidcClient.anyClientIsBeforeSso()) {
                return authResult;
            }
        }

        String provider = oidcClient.getOidcProvider(req);
        if (provider == null) {
            return new AuthenticationResult(AuthResult.CONTINUE, "not an OpenID Connect client request, skipping OpenID Connect client...");
        }
        ProviderAuthenticationResult oidcResult = oidcClient.authenticate(req, res, provider, referrerURLCookieHandler, firstCall);

        if (oidcResult.getStatus() == AuthResult.CONTINUE) {
            return OIDC_CLIENT_CONT;
        }

        if (oidcResult.getStatus() == AuthResult.REDIRECT_TO_PROVIDER) {
            return new AuthenticationResult(AuthResult.REDIRECT, oidcResult.getRedirectUrl());
        }

        if (oidcResult.getStatus() == AuthResult.FAILURE) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oidcResult.getHttpStatusCode()) {
                // return new AuthenticationResult(AuthResult.SEND_401, "OpenID Connect client failed the request...");
                return new AuthenticationResult(AuthResult.OAUTH_CHALLENGE, "OpenID Connect client failed the request...");
            } else {
                return new AuthenticationResult(AuthResult.FAILURE, "OpenID Connect client failed the request...");
            }
        }

        if (oidcResult.getStatus() != AuthResult.SUCCESS) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oidcResult.getHttpStatusCode()) {
                // return new AuthenticationResult(AuthResult.SEND_401, "OpenID Connect client returned with status: " + oidcResult.getStatus());
                return new AuthenticationResult(AuthResult.OAUTH_CHALLENGE, "OpenID Connect client returned with status: " + oidcResult.getStatus());
            } else {
                return new AuthenticationResult(AuthResult.FAILURE, "OpenID Connect client returned with status: " + oidcResult.getStatus());
            }
        }

        if (oidcResult.getStatus() == AuthResult.SUCCESS && oidcResult.getUserName() != null) {
            authResult = authHelper.loginWithUserName(req, res, oidcResult.getUserName(), oidcResult.getSubject(),
                                                      oidcResult.getCustomProperties(), oidcClient.isMapIdentityToRegistryUser(provider));
            if (AuthResult.SUCCESS == authResult.getStatus()) {
                // If firstCall is true then disableLtpaCookie is true
                boolean bDisableLtpaCookie = firstCall; // let's make it clear
                boolean bPropagationTokenAuthenticated = isNotNullAndTrue(req, OidcClient.PROPAGATION_TOKEN_AUTHENTICATED);
                boolean bAuthnSessionDisabled = (Boolean) req.getAttribute(OidcClient.AUTHN_SESSION_DISABLED); // this will not be null
                String inboundValue = (String) req.getAttribute(OidcClient.INBOUND_PROPAGATION_VALUE); // this will not be null
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Booleans: fisrtCall:" + firstCall +
                                 " tokenAuthenticated:" + bPropagationTokenAuthenticated +
                                 " SessionDisabled:" + bAuthnSessionDisabled +
                                 " inboundValue:" + inboundValue);
                }
                // extra handling when authenticated by the inbound propagation token (task 210993)
                boolean includeAccessTokenInLtpa = (Boolean) req.getAttribute(OidcClient.ACCESS_TOKEN_IN_LTPA_TOKEN);
                if ((OidcClient.inboundNone.equals(inboundValue) && !bDisableLtpaCookie) ||
                    (OidcClient.inboundRequired.equals(inboundValue) && !bAuthnSessionDisabled) ||
                    (OidcClient.inboundSupported.equals(inboundValue) && (!bPropagationTokenAuthenticated) && !bDisableLtpaCookie) ||
                    (includeAccessTokenInLtpa && OidcClient.inboundSupported.equals(inboundValue))) {

                    SSOCookieHelper ssoCh = webAppSecurityConfig.createSSOCookieHelper();

                    if (includeAccessTokenInLtpa) {
                        addAccessTokenToTheCookie(authResult, ssoCh);
                    }
                    ssoCh.addSSOCookiesToResponse(authResult.getSubject(), req, res);
                }
            }
        }

        return authResult;
    }

    private void addAccessTokenToTheCookie(AuthenticationResult authResult, SSOCookieHelper ssoCh) {

        Subject subject = authResult.getSubject();

        if (subject != null) {
            String accessToken = getAccessTokenFromTheSubject(subject, "access_token");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "access token from the subject = ", accessToken);
            }
            SingleSignonToken ssoToken = ssoCh.getDefaultSSOTokenFromSubject(subject);
            if (accessToken != null && ssoToken != null) {
                if (ssoToken.getAttributes(OidcClient.OIDC_ACCESS_TOKEN) == null || ssoToken.getAttributes(OidcClient.OIDC_ACCESS_TOKEN).length < 1) {
                    ssoToken.addAttribute(OidcClient.OIDC_ACCESS_TOKEN, accessToken);
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Successfully added the access token to the single sign on token  = ", accessToken);
                }
            }
        }
    }

    /**
     * @param runAsSubject
     * @param attribKey
     * @return object
     * @throws Throwable
     */
    @FFDCIgnore({ PrivilegedActionException.class })
    static String getAccessTokenFromTheSubject(Subject subject, String attribKey) {
        String result = null;
        try {
            Set<Object> publicCredentials = subject.getPublicCredentials();
            result = getCredentialAttribute(publicCredentials, attribKey, "publicCredentials");
            if (result == null || result.isEmpty()) {
                Set<Object> privateCredentials = subject.getPrivateCredentials();
                result = getCredentialAttribute(privateCredentials, attribKey, "privateCredentials");
            }

        } catch (PrivilegedActionException e) {
            // TODO do we need an error handling in here?
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find a value for the attribute (" + attribKey + ")");
            }
            //throw new Exception(e.getCause());
        }
        return result;
    }

    static String getCredentialAttribute(final Set<Object> credentials, final String attribKey, final String msg) throws PrivilegedActionException {
        // Since this is only for jaxrs client internal usage, it's OK to override java2 security
        Object obj = AccessController.doPrivileged(
                                                   new PrivilegedExceptionAction<Object>() {
                                                       @Override
                                                       public Object run() throws Exception {
                                                           int iCnt = 0;
                                                           for (Object credentialObj : credentials) {
                                                               iCnt++;
                                                               if (tc.isDebugEnabled()) {
                                                                   Tr.debug(tc, msg + "(" + iCnt + ") class:" + credentialObj.getClass().getName());
                                                               }
                                                               if (credentialObj instanceof Map) {
                                                                   Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
                                                                   if (accessToken == null) {
                                                                       continue; // on credentialObj
                                                                   }

                                                                   Object value = ((Map<?, ?>) credentialObj).get(attribKey);
                                                                   if (value != null) {
                                                                       return value;
                                                                   }
                                                               }
                                                           }
                                                           return null;
                                                       }
                                                   });
        if (obj != null) {
            return obj.toString();
        } else {
            return null;
        }
    }

    /**
     * The oauth service will call the provider to authenticate a user with the access token
     *
     * @param webRequest
     * @return
     */
    private AuthenticationResult handleOAuth(HttpServletRequest req, HttpServletResponse res) {
        AuthenticationResult authResult = OAUTH_CONT;
        if (oauthServiceRef != null) {
            OAuth20Service oauthService = oauthServiceRef.getService();
            if (oauthService == null) {
                return new AuthenticationResult(AuthResult.CONTINUE, "OAuth service is not available, skipping OAuth...");
            }

            ProviderAuthenticationResult oauthResult = oauthService.authenticate(req, res);
            if (oauthResult.getStatus() == AuthResult.CONTINUE) {
                return OAUTH_CONT;
            }

            if (oauthResult.getStatus() == AuthResult.FAILURE) {
                if (HttpServletResponse.SC_UNAUTHORIZED == oauthResult.getHttpStatusCode()) {
                    return new AuthenticationResult(AuthResult.OAUTH_CHALLENGE, "OAuth service failed the request");
                }
                return new AuthenticationResult(AuthResult.FAILURE, "OAuth service failed the request...");
            }
            if (oauthResult.getStatus() != AuthResult.SUCCESS) {
                if (HttpServletResponse.SC_UNAUTHORIZED == oauthResult.getHttpStatusCode()) {
                    return new AuthenticationResult(AuthResult.OAUTH_CHALLENGE, "OAuth service failed the request due to unsuccessful request");
                }
                return new AuthenticationResult(AuthResult.FAILURE, "OAuth service returned with status: " + oauthResult.getStatus());
            }
            if (oauthResult.getUserName() != null) {
                authResult = authHelper.loginWithUserName(req, res, oauthResult.getUserName(), oauthResult.getSubject(), oauthResult.getCustomProperties(), true);
            }
        }

        return authResult;
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
            taiAuthenticator = new TAIAuthenticator(taiService, interceptorServiceRef, securityService.getAuthenticationService(), new SSOCookieHelperImpl(webAppSecurityConfig));
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
            cookieHelper = new SSOCookieHelperImpl(webAppSecurityConfig);
        }
        return new SSOAuthenticator(securityService.getAuthenticationService(), securityMetadata, webAppSecurityConfig, cookieHelper);
    }
}
