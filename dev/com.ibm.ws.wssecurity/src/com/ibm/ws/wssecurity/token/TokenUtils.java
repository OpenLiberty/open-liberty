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
package com.ibm.ws.wssecurity.token;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.saml2.core.Assertion;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class TokenUtils {
    protected static final TraceComponent tc = Tr.register(TokenUtils.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);
    static public final String KEY_SSO_SERVICE = "ssoService";
    static ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRef =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);

    private static WebProviderAuthenticatorHelper authHelper;

    static final String KEY_SECURITY_SERVICE = "securityService";
    private static AtomicServiceReference<SecurityService> securityServiceRef =
                    new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    public static Saml20Token createSamlTokenFromAssertion(Assertion assertion) throws Exception {
        Saml20Token token = null;
        SsoService saml20Service = getCommonSsoService(SsoService.TYPE_SAML20);

        if (saml20Service != null) {
            Map<String, Object> requestContext = new HashMap<String, Object>();
            requestContext.put(SsoService.WSSEC_SAML_ASSERTION, assertion);
            Map<String, Object> result = saml20Service.handleRequest(SsoService.WSSEC_SAML_ASSERTION,
                                                                     requestContext);
            if (!result.isEmpty()) {
                token = (Saml20Token) result.get(SsoService.SAML_SSO_TOKEN);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can not find SAML20 SsoService=" + SsoService.TYPE_SAML20);
            }
        }
        return token;
    }

    /**
     * @param ssoservicerefs
     */
    public static void setCommonSsoService(ConcurrentServiceReferenceMap<String, com.ibm.ws.security.sso.common.SsoService> activatedSsoServicerefs) {
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
        TokenUtils.authHelper = authHelper;
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
        TokenUtils.securityServiceRef = securityServiceRef;
    }

}
