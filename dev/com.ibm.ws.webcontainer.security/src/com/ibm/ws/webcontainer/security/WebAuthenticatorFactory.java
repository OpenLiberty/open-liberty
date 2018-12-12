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
package com.ibm.ws.webcontainer.security;

import java.util.Map;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

public interface WebAuthenticatorFactory {

    /**
     * @param props
     * @param locationAdminRef
     * @param securityServiceRef
     * @return
     */
    WebAppSecurityConfig createWebAppSecurityConfigImpl(Map<String, Object> props,
                                                        AtomicServiceReference<WsLocationAdmin> locationAdminRef,
                                                        AtomicServiceReference<SecurityService> securityServiceRef);

    /**
     * @param ssoCookieHelper
     * @param securityServiceRef
     * @param collabUtils
     * @param webAuthenticatorRef
     * @param unprotectedResourceServiceRef
     * @param unauthenticatedSubjectService
     * @return
     */
    AuthenticateApi createAuthenticateApi(SSOCookieHelper ssoCookieHelper,
                                          AtomicServiceReference<SecurityService> securityServiceRef,
                                          CollaboratorUtils collabUtils,
                                          ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef,
                                          ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef,
                                          UnauthenticatedSubjectService unauthenticatedSubjectService);

    /**
     * @param securityServiceRef
     * @param taiServiceRef
     * @param interceptorServiceRef
     * @param webAppSecConfig
     * @param webAuthenticatorRef
     * @return
     */
    WebProviderAuthenticatorProxy createWebProviderAuthenticatorProxy(AtomicServiceReference<SecurityService> securityServiceRef,
                                                                      AtomicServiceReference<TAIService> taiServiceRef,
                                                                      ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                                                      WebAppSecurityConfig webAppSecConfig,
                                                                      ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef);

    /**
     * @param webAppSecConfig
     * @param postParameterHelper
     * @param securityServiceRef
     * @param providerAuthenticatorProxy
     * @return
     */
    WebAuthenticatorProxy createWebAuthenticatorProxy(WebAppSecurityConfig webAppSecConfig,
                                                      PostParameterHelper postParameterHelper,
                                                      AtomicServiceReference<SecurityService> securityServiceRef,
                                                      WebProviderAuthenticatorProxy providerAuthenticatorProxy);

    /**
     * @param webRequest
     * @return
     */
    Boolean needToAuthenticateSubject(WebRequest webRequest);
}
