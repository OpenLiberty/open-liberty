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
package com.ibm.ws.webcontainer.security.extended;

import java.security.AccessController;
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
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebRequestImpl;
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
 */
public class WebProviderAuthenticatorProxyExtended extends WebProviderAuthenticatorProxy {

    private static final TraceComponent tc = Tr.register(WebProviderAuthenticatorProxyExtended.class);

    static final List<String> authenticatorOdering = Collections.unmodifiableList(Arrays.asList(new String[] { "com.ibm.ws.security.spnego", "com.ibm.ws.security.openid" }));

    AuthenticationResult OAUTH_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OAuth service said continue...");
    AuthenticationResult OPENID_CLIENT_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID client service said continue...");
    AuthenticationResult OIDC_SERVER_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID Connect server said continue...");
    AuthenticationResult OIDC_CLIENT_CONT = new AuthenticationResult(AuthResult.CONTINUE, "OpenID Connect client said continue...");
    AuthenticationResult SPNEGO_CONT = new AuthenticationResult(AuthResult.CONTINUE, "SPNEGO said continue...");

    private final AtomicServiceReference<OAuth20Service> oauthServiceRef;
    private final AtomicServiceReference<OpenidClientService> openIdClientServiceRef;
    private final AtomicServiceReference<OidcServer> oidcServerRef;
    private final AtomicServiceReference<OidcClient> oidcClientRef;
    private WebProviderAuthenticatorHelper authHelper;
    private ReferrerURLCookieHandler referrerURLCookieHandler = null;
    private WebAppSecurityConfig webAppSecurityConfig = null;

    public WebProviderAuthenticatorProxyExtended(AtomicServiceReference<SecurityService> securityServiceRef,
                                                 AtomicServiceReference<TAIService> taiServiceRef,
                                                 ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                                 WebAppSecurityConfig webAppSecurityConfig,
                                                 AtomicServiceReference<OAuth20Service> oauthServiceRef,
                                                 AtomicServiceReference<OpenidClientService> openIdClientServiceRef,
                                                 AtomicServiceReference<OidcServer> oidcServerRef,
                                                 AtomicServiceReference<OidcClient> oidcClientRef,
                                                 ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {

        super(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecurityConfig, webAuthenticatorRef);
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.oauthServiceRef = oauthServiceRef;
        this.oidcServerRef = oidcServerRef;
        this.openIdClientServiceRef = openIdClientServiceRef;
        this.oidcClientRef = oidcClientRef;

        authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);
        referrerURLCookieHandler = new ReferrerURLCookieHandlerExtended(webAppSecurityConfig);
    }

    /*
     * need for unit test*
     */
    public void setWebProviderAuthenticatorHelper(WebProviderAuthenticatorHelper authHelper) {
        this.authHelper = authHelper;
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

    /**
     * @param webRequest
     * @return
     */
    @Override
    protected AuthenticationResult handleJaspi(WebRequest webRequest, HashMap<String, Object> props) {
        return super.handleJaspi(webRequest, props);
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
                ssoToken.addAttribute(OidcClient.OIDC_ACCESS_TOKEN, accessToken);
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
    @Override
    protected TAIAuthenticator getTaiAuthenticator() {
        TAIAuthenticator taiAuthenticator = null;
        TAIService taiService = taiServiceRef.getService();
        Iterator<TrustAssociationInterceptor> interceptorServices = interceptorServiceRef.getServices();
        if (taiService != null || (interceptorServices != null && interceptorServices.hasNext())) {
            SecurityService securityService = securityServiceRef.getService();
            taiAuthenticator = new TAIAuthenticator(taiService, interceptorServiceRef, securityService.getAuthenticationService(), new SSOCookieHelperImplExtended(webAppSecurityConfig, oidcServerRef));
        }

        return taiAuthenticator;
    }

    /**
     * Create an instance of SSOAuthenticator.
     *
     * @param webRequest
     * @return The SSOAuthenticator, or {@code null} if it could not be created.
     */
    @Override
    public WebAuthenticator getSSOAuthenticator(WebRequest webRequest, String ssoCookieName) {
        SecurityMetadata securityMetadata = webRequest.getSecurityMetadata();
        SecurityService securityService = securityServiceRef.getService();
        SSOCookieHelper cookieHelper;
        if (ssoCookieName != null) {
            cookieHelper = new SSOCookieHelperImplExtended(webAppSecurityConfig, ssoCookieName);
        } else {
            cookieHelper = new SSOCookieHelperImplExtended(webAppSecurityConfig, oidcServerRef);
        }
        return new SSOAuthenticator(securityService.getAuthenticationService(), securityMetadata, webAppSecurityConfig, cookieHelper);
    }
}
