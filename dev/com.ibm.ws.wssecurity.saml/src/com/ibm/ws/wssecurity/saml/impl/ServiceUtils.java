/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.saml.impl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class ServiceUtils {
    protected static final TraceComponent tc = Tr.register(ServiceUtils.class,
                                                           WssSamlConstants.TR_GROUP,
                                                           WssSamlConstants.TR_RESOURCE_BUNDLE);
    static public final String KEY_SSO_SERVICE = "ssoService";
    static ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRef =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);

    private static WebProviderAuthenticatorHelper authHelper;

    static final String KEY_SECURITY_SERVICE = "securityService";
    private static AtomicServiceReference<SecurityService> securityServiceRef =
                    new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    /**
     * @param ssoservicerefs
     */
    public static void setCommonSsoService(ConcurrentServiceReferenceMap<String, SsoService> activatedSsoServicerefs) {
        ssoServiceRef = activatedSsoServicerefs;
    }

    /*
     * SsoService.TYPE_WSS_SAML = "wssSaml";
     * SsoService.TYPE_WSSECURiTY = "wssecurity";
     * SsoService.TYPE_SAML20 = "saml20";
     */
    public static SsoService getCommonSsoService(String key) {
        return ssoServiceRef.getService(key);
    }

    /**
     * @return the authHelper
     */
    public static WebProviderAuthenticatorHelper getAuthHelper() {
        return authHelper;
    }

    /**
     * @param authHelper the authHelper to set
     */
    public static void setAuthHelper(WebProviderAuthenticatorHelper authHelper) {
        ServiceUtils.authHelper = authHelper;
    }

    /**
     * @return the securityServiceRef
     */
    public static AtomicServiceReference<SecurityService> getSecurityServiceRef() {
        return securityServiceRef;
    }

    /**
     * @param securityServiceRef the securityServiceRef to set
     */
    public static void setSecurityServiceRef(AtomicServiceReference<SecurityService> securityServiceRef) {
        ServiceUtils.securityServiceRef = securityServiceRef;
    }
}
