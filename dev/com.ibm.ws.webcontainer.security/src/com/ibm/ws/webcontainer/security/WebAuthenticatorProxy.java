/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.audit.utils.AuditConstants;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.FormLoginAuthenticator;
import com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The responsibility of this authenticator proxy is to authenticate the web request.
 */
public class WebAuthenticatorProxy implements WebAuthenticator {

    private static final TraceComponent tc = Tr.register(WebAuthenticatorProxy.class);
    private static final String AUTH_TYPE = "AUTH_TYPE";
    protected final AtomicServiceReference<SecurityService> securityServiceRef;
    protected volatile WebAppSecurityConfig webAppSecurityConfig;
    private volatile PostParameterHelper postParameterHelper;
    private final WebProviderAuthenticatorProxy providerAuthenticatorProxy;
    public HashMap<String, Object> extraAuditData = new HashMap<String, Object>();

    public WebAuthenticatorProxy(WebAppSecurityConfig webAppSecurityConfig,
                                 PostParameterHelper postParameterHelper,
                                 AtomicServiceReference<SecurityService> securityServiceRef,
                                 WebProviderAuthenticatorProxy providerAuthenticatorProxy) {
        this.webAppSecurityConfig = webAppSecurityConfig;
        this.postParameterHelper = postParameterHelper;
        this.securityServiceRef = securityServiceRef;
        this.providerAuthenticatorProxy = providerAuthenticatorProxy;
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        AuthenticationResult authResult = providerAuthenticatorProxy.authenticate(webRequest);
        authResult.setTargetRealm(authResult.realm);
        String authType = webRequest.getLoginConfig().getAuthenticationMethod();

        if (authResult.getStatus() == AuthResult.CONTINUE) {
            WebAuthenticator authenticator = getWebAuthenticator(webRequest);
            if (authenticator == null) {
                return new AuthenticationResult(AuthResult.FAILURE, "Unable to get the appropriate WebAuthenticator. Unable to get the appropriate WebAuthenticator.");
            }

            authResult = authenticator.authenticate(webRequest);
            if (authenticator instanceof CertificateLoginAuthenticator &&
                authResult != null && authResult.getStatus() != AuthResult.SUCCESS &&
                webAppSecurityConfig.allowFailOver() && !webRequest.isDisableClientCertFailOver()) {
                extraAuditData.put(AuditConstants.ORIGINAL_AUTH_TYPE, authType);
                authType = getFailOverToAuthType(webRequest);
                extraAuditData.put(AuditConstants.FAILOVER_AUTH_TYPE, authType);
                authenticator = getAuthenticatorForFailOver(authType, webRequest);
                WebReply reply = new DenyReply("AuthenticationFailed");
                Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(reply.getStatusCode()));
                if (authenticator == null) {
                    return new AuthenticationResult(AuthResult.FAILURE, "Unable to get the failover WebAuthenticator. Unable to authenticate request.");
                } else {
                    authResult = authenticator.authenticate(webRequest);
                    if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
                        Audit.audit(Audit.EventID.SECURITY_AUTHN_FAILOVER_01, webRequest, authResult, extraAuditData, Integer.valueOf(HttpServletResponse.SC_OK));
                    } else {
                        Audit.audit(Audit.EventID.SECURITY_AUTHN_FAILOVER_01, webRequest, authResult, extraAuditData, Integer.valueOf(reply.getStatusCode()));
                    }
                }
            }
        }

        if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
            SRTServletRequestUtils.setPrivateAttribute(webRequest.getHttpServletRequest(), AUTH_TYPE, authType);
            if (LoginConfiguration.FORM.equalsIgnoreCase(authType)) {
                postParameterHelper.restore(webRequest.getHttpServletRequest(), webRequest.getHttpServletResponse());
            }
        }
        return authResult;
    }

    /**
     * Get the appropriate Authenticator based on the authType
     *
     * @param authType the auth type, either FORM or BASIC
     * @param the WebRequest
     * @return The WebAuthenticator or {@code null} if the authType is unknown
     */
    private WebAuthenticator getAuthenticatorForFailOver(String authType, WebRequest webRequest) {
        WebAuthenticator authenticator = null;
        if (LoginConfiguration.FORM.equals(authType)) {
            authenticator = createFormLoginAuthenticator(webRequest);
        } else if (LoginConfiguration.BASIC.equals(authType)) {
            authenticator = getBasicAuthAuthenticator();
        }
        return authenticator;
    }

    /**
     * Determines if the application has a FORM login configuration in
     * its web.xml
     *
     * @param webRequest
     * @return {@code true} if the application's web.xml has a valid form login configuration.
     */
    private boolean appHasWebXMLFormLogin(WebRequest webRequest) {
        return webRequest.getFormLoginConfiguration() != null &&
               webRequest.getFormLoginConfiguration().getLoginPage() != null &&
               webRequest.getFormLoginConfiguration().getErrorPage() != null;
    }

    /**
     * Determine if the global WebAppSecurityConfig has a form login page.
     *
     * @return {@code true} if the global FORM login page is set
     */
    private boolean globalWebAppSecurityConfigHasFormLogin() {
        WebAppSecurityConfig globalConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        return globalConfig != null &&
               globalConfig.getLoginFormURL() != null;
    }

    /**
     * Determine the failover type based on available configuration.
     * <p>
     * If both FORM and BASIC are valid for failover, then pick one as follows:
     * If there is a form login configuration present, then FORM login is the
     * intended failover. If form login configuration is not present, then
     * BASIC is the only logical remaining option.
     * <p>
     * If only FORM or BASIC is allowed, select the appropriate one.
     * If neither failover is allowed, null is returned.
     *
     * @return {@link LoginConfiguration#BASIC}, {@link LoginConfiguration#FORM}, or {@code null}. See method description.
     */
    private String getFailOverToAuthType(WebRequest webRequest) {
        String authType = null;
        if (webAppSecurityConfig.getAllowFailOverToAppDefined()) {
            SecurityMetadata securityMetadata = webRequest.getSecurityMetadata();
            LoginConfiguration loginConfig = securityMetadata.getLoginConfiguration();
            if (loginConfig != null) {
                authType = loginConfig.getAuthenticationMethod();
            }
        } else if (webAppSecurityConfig.getAllowFailOverToBasicAuth() && webAppSecurityConfig.getAllowFailOverToFormLogin()) {
            if (appHasWebXMLFormLogin(webRequest) || globalWebAppSecurityConfigHasFormLogin()) {
                authType = LoginConfiguration.FORM;
            } else {
                authType = LoginConfiguration.BASIC;
            }
        } else if (webAppSecurityConfig.getAllowFailOverToFormLogin()) {
            authType = LoginConfiguration.FORM;
        } else if (webAppSecurityConfig.getAllowFailOverToBasicAuth()) {
            authType = LoginConfiguration.BASIC;
        }
        return authType;
    }

    /**
     * Determine the correct WebAuthenticator to use based on the authentication
     * method. If there authentication method is not specified in the web.xml file,
     * then we will use BasicAuth as default.
     *
     * @param webRequest
     * @return The correct WebAuthenticator to handle the webRequest, or {@code null} if it could not be created.
     */
    public WebAuthenticator getWebAuthenticator(WebRequest webRequest) {
        String authMech = webAppSecurityConfig.getOverrideHttpAuthMethod();
        if (authMech != null && authMech.equals("CLIENT_CERT")) {
            return createCertificateLoginAuthenticator();
        }
        SecurityMetadata securityMetadata = webRequest.getSecurityMetadata();
        LoginConfiguration loginConfig = securityMetadata.getLoginConfiguration();

        if (loginConfig != null) {
            String authenticationMethod = loginConfig.getAuthenticationMethod();
            if (LoginConfiguration.FORM.equalsIgnoreCase(authenticationMethod)) {
                return createFormLoginAuthenticator(webRequest);
            } else if (LoginConfiguration.CLIENT_CERT.equalsIgnoreCase(authenticationMethod)) {
                return createCertificateLoginAuthenticator();
            }
        }
        return getBasicAuthAuthenticator();
    }

    /**
     * Create an instance of BasicAuthAuthenticator.
     *
     * @return A BasicAuthAuthenticator or {@code null} if the it could not be created.
     */
    public BasicAuthAuthenticator getBasicAuthAuthenticator() {
        try {
            return createBasicAuthenticator();
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegistryException while trying to create BasicAuthAuthenticator", e);
            }
        }
        return null;
    }

    /**
     * Create an instance of the BasicAuthAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    protected BasicAuthAuthenticator createBasicAuthenticator() throws RegistryException {
        SecurityService securityService = securityServiceRef.getService();
        UserRegistryService userRegistryService = securityService.getUserRegistryService();
        UserRegistry userRegistry = null;
        if (userRegistryService.isUserRegistryConfigured())
            userRegistry = userRegistryService.getUserRegistry();
        SSOCookieHelper sSOCookieHelper = webAppSecurityConfig.createSSOCookieHelper();
        return new BasicAuthAuthenticator(securityService.getAuthenticationService(), userRegistry, sSOCookieHelper, webAppSecurityConfig);
    }

    /**
     * Create an instance of the FormLoginAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    protected FormLoginAuthenticator createFormLoginAuthenticator(WebRequest webRequest) {
        WebAuthenticator ssoAuthenticator = providerAuthenticatorProxy.getSSOAuthenticator(webRequest, null);
        return new FormLoginAuthenticator(ssoAuthenticator, webAppSecurityConfig, providerAuthenticatorProxy);
    }

    /**
     * Create an instance of the CertificateLoginAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    public CertificateLoginAuthenticator createCertificateLoginAuthenticator() {
        SecurityService securityService = securityServiceRef.getService();
        return new CertificateLoginAuthenticator(securityService.getAuthenticationService(), webAppSecurityConfig.createSSOCookieHelper());
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) {
        return null;
    }
}