/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
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
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 *
 */
@Component(service = WebAuthenticatorFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class WebAuthenticatorFactoryImpl implements WebAuthenticatorFactory {

    @Override
    public WebAppSecurityConfig createWebAppSecurityConfigImpl(Map<String, Object> props,
                                                               AtomicServiceReference<WsLocationAdmin> locationAdminRef,
                                                               AtomicServiceReference<SecurityService> securityServiceRef) {
        return new WebAppSecurityConfigImpl(props, locationAdminRef, securityServiceRef);
    }

    @Override
    public AuthenticateApi createAuthenticateApi(SSOCookieHelper ssoCookieHelper,
                                                 AtomicServiceReference<SecurityService> securityServiceRef,
                                                 CollaboratorUtils collabUtils,
                                                 ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef,
                                                 ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef) {
        return new AuthenticateApi(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef);
    }

    @Override
    public WebProviderAuthenticatorProxy createWebProviderAuthenticatorProxy(AtomicServiceReference<SecurityService> securityServiceRef,
                                                                             AtomicServiceReference<TAIService> taiServiceRef,
                                                                             ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                                                             WebAppSecurityConfig webAppSecConfig,
                                                                             ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef) {
        return new WebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecConfig, webAuthenticatorRef);
    }

    @Override
    public WebAuthenticatorProxy createWebAuthenticatorProxy(WebAppSecurityConfig webAppSecConfig, PostParameterHelper postParameterHelper,
                                                             AtomicServiceReference<SecurityService> securityServiceRef, WebProviderAuthenticatorProxy providerAuthenticatorProxy) {
        return new WebAuthenticatorProxy(webAppSecConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy);
    }

    @Override
    public Boolean needToAuthenticateSubject(WebRequest webRequest) {
        return null;
    }
}
