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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.CertificateLoginAuthenticator;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The responsibility of this authenticator proxy is to authenticate the web request.
 */
public class WebAuthenticatorProxyExtended extends WebAuthenticatorProxy {

    private static final TraceComponent tc = Tr.register(WebAuthenticatorProxyExtended.class);
    private final AtomicServiceReference<OidcServer> oidcServerRef;

    public WebAuthenticatorProxyExtended(WebAppSecurityConfig webAppSecurityConfig,
                                         PostParameterHelper postParameterHelper,
                                         AtomicServiceReference<SecurityService> securityServiceRef,
                                         WebProviderAuthenticatorProxy providerAuthenticatorProxy,
                                         AtomicServiceReference<OidcServer> oidcServerRef) {
        super(webAppSecurityConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy);
        this.oidcServerRef = oidcServerRef;
    }

    /**
     * Create an instance of the BasicAuthAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    @Override
    protected BasicAuthAuthenticator createBasicAuthenticator() throws RegistryException {
        SecurityService securityService = securityServiceRef.getService();
        UserRegistryService userRegistryService = securityService.getUserRegistryService();
        UserRegistry userRegistry = null;
        if (userRegistryService.isUserRegistryConfigured())
            userRegistry = userRegistryService.getUserRegistry();
        SSOCookieHelper sSOCookieHelper = new SSOCookieHelperImplExtended(webAppSecurityConfig, oidcServerRef);
        return new BasicAuthAuthenticator(securityService.getAuthenticationService(), userRegistry, sSOCookieHelper, webAppSecurityConfig);
    }

    /**
     * Create an instance of the CertificateLoginAuthenticator.
     * <p>
     * Protected so it can be overridden in unit tests.
     */
    @Override
    public CertificateLoginAuthenticator createCertificateLoginAuthenticator() {
        SecurityService securityService = securityServiceRef.getService();
        return new CertificateLoginAuthenticator(securityService.getAuthenticationService(), new SSOCookieHelperImplExtended(webAppSecurityConfig, oidcServerRef));
    }
}