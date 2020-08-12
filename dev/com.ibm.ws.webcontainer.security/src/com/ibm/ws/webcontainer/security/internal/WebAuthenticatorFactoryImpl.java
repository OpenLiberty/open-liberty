/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.security.sso.SSOService;
import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebAuthenticatorFactory;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Service;
import com.ibm.ws.webcontainer.security.openid20.OpenidClientService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 *
 */
@Component(service = WebAuthenticatorFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class WebAuthenticatorFactoryImpl implements WebAuthenticatorFactory {

    static final String KEY_OAUTH_SERVICE = "oauthService";
    static final String KEY_OIDC_SERVER = "oidcServer";
    static final String KEY_OIDC_CLIENT = "oidcClient";
    static final String KEY_OPENID_CLIENT_SERVICE = "openidClientService";
    private AtomicServiceReference<SSOService> ssoServiceRef;

    protected final AtomicServiceReference<OAuth20Service> oauthServiceRef = new AtomicServiceReference<OAuth20Service>(KEY_OAUTH_SERVICE);
    protected final AtomicServiceReference<OidcServer> oidcServerRef = new AtomicServiceReference<OidcServer>(KEY_OIDC_SERVER);
    protected final AtomicServiceReference<OidcClient> oidcClientRef = new AtomicServiceReference<OidcClient>(KEY_OIDC_CLIENT);
    protected final AtomicServiceReference<OpenidClientService> openidClientRef = new AtomicServiceReference<OpenidClientService>(KEY_OPENID_CLIENT_SERVICE);

//    private UnauthenticatedSubjectService unauthSubjectService;
    protected WebAppSecurityConfig globalConfig = null;
    protected WebProviderAuthenticatorProxy providerAuthenticatorProxy = null;
    protected WebAuthenticatorProxy authenticatorProxy = null;

    @Override
    public WebAppSecurityConfig createWebAppSecurityConfigImpl(Map<String, Object> props,
                                                               AtomicServiceReference<WsLocationAdmin> locationAdminRef,
                                                               AtomicServiceReference<SecurityService> securityServiceRef) {
        globalConfig = new WebAppSecurityConfigImpl(props, locationAdminRef, securityServiceRef, oidcServerRef, oidcClientRef, ssoServiceRef);
        return globalConfig;
    }

    @Override
    public AuthenticateApi createAuthenticateApi(SSOCookieHelper ssoCookieHelper,
                                                 AtomicServiceReference<SecurityService> securityServiceRef,
                                                 CollaboratorUtils collabUtils,
                                                 ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef,
                                                 ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef,
                                                 UnauthenticatedSubjectService unauthSubjectService,
                                                 AtomicServiceReference<SSOService> ssoServiceRef) {
        return new AuthenticateApi(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef, unauthSubjectService, ssoServiceRef);
    }

    @Override
    public WebProviderAuthenticatorProxy createWebProviderAuthenticatorProxy(AtomicServiceReference<SecurityService> securityServiceRef,
                                                                             AtomicServiceReference<TAIService> taiServiceRef,
                                                                             ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                                                             WebAppSecurityConfig webAppSecConfig,
                                                                             ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {
        providerAuthenticatorProxy = new WebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecConfig, oauthServiceRef, openidClientRef, oidcServerRef, oidcClientRef, webAuthenticatorRef, ssoServiceRef);
        return providerAuthenticatorProxy;
    }

    @Override
    public WebAuthenticatorProxy createWebAuthenticatorProxy(WebAppSecurityConfig webAppSecConfig,
                                                             PostParameterHelper postParameterHelper,
                                                             AtomicServiceReference<SecurityService> securityServiceRef,
                                                             WebProviderAuthenticatorProxy providerAuthenticatorProxy,
                                                             AtomicServiceReference<SSOService> ssoServiceRef) {
        authenticatorProxy = new WebAuthenticatorProxy(webAppSecConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy, oidcServerRef, ssoServiceRef);
        return authenticatorProxy;
    }

    @Override
    public Boolean needToAuthenticateSubject(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        OAuth20Service oauthService = oauthServiceRef.getService();
        OidcServer oidcServer = oidcServerRef.getService();
        if (isProviderSpecialProtectedURI(req, oauthService, oidcServer, false)) {
            // all oauth or oidc URI, no matter protected or not-protected
            if (isProviderSpecialProtectedURI(req, oauthService, oidcServer, true)) {
                // oauth or oidc URI which are protected
                webRequest.setProviderSpecialUnprotectedURI(true);
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else {
            return null;
        }
    }

    @Override
    public WebProviderAuthenticatorProxy getWebProviderAuthenticatorProxy() {
        return providerAuthenticatorProxy;
    }

    @Override
    public WebAuthenticatorProxy getWebAuthenticatorProxy() {
        return authenticatorProxy;
    }

    private boolean isProviderSpecialProtectedURI(HttpServletRequest req,
                                                  OAuth20Service oauthService,
                                                  OidcServer oidcServer,
                                                  boolean protectedUri) {
        if (oidcServer != null) {
            if (oidcServer.isOIDCSpecificURI(req, protectedUri)) {
                return true;
            }
        }
        if (oauthService != null) {
            if (oauthService.isOauthSpecificURI(req, protectedUri)) {
                return true;
            }
        }
        return false;
    }

    @Reference(name = KEY_OAUTH_SERVICE, service = OAuth20Service.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOauthService(ServiceReference<OAuth20Service> reference) {
        oauthServiceRef.setReference(reference);
    }

    protected void unsetOauthService(ServiceReference<OAuth20Service> reference) {
        oauthServiceRef.unsetReference(reference);
    }

    @Reference(name = KEY_OIDC_SERVER, service = OidcServer.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOidcServer(ServiceReference<OidcServer> reference) {
        oidcServerRef.setReference(reference);
        WebAppSecurityConfig webAppSecConfig = WebSecurityHelperImpl.getWebAppSecurityConfig();
        if (webAppSecConfig != null && webAppSecConfig instanceof WebAppSecurityConfig) {
            ((WebAppSecurityConfigImpl) webAppSecConfig).setSsoCookieName(oidcServerRef, oidcClientRef);
        }
    }

    protected void unsetOidcServer(ServiceReference<OidcServer> reference) {
        oidcServerRef.unsetReference(reference);
    }

    @Reference(name = KEY_OIDC_CLIENT, service = OidcClient.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOidcClient(ServiceReference<OidcClient> reference) {
        oidcClientRef.setReference(reference);
        WebAppSecurityConfig webAppSecConfig = WebSecurityHelperImpl.getWebAppSecurityConfig();
        if (webAppSecConfig != null) {
            ((WebAppSecurityConfigImpl) webAppSecConfig).setSsoCookieName(oidcServerRef, oidcClientRef);
        }
    }

    protected void unsetOidcClient(ServiceReference<OidcClient> reference) {
        oidcClientRef.unsetReference(reference);
    }

    @Reference(name = KEY_OPENID_CLIENT_SERVICE, service = OpenidClientService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOpenidClientService(ServiceReference<OpenidClientService> reference) {
        openidClientRef.setReference(reference);
    }

    protected void unsetOpenidClientService(ServiceReference<OpenidClientService> reference) {
        openidClientRef.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        oauthServiceRef.activate(cc);
        oidcServerRef.activate(cc);
        oidcClientRef.activate(cc);
        openidClientRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        oauthServiceRef.deactivate(cc);
        oidcServerRef.deactivate(cc);
        oidcClientRef.deactivate(cc);
        openidClientRef.deactivate(cc);
    }
}
