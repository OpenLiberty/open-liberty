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
package com.ibm.ws.webcontainer.security.internal.extended;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.extended.ReferrerURLCookieHandlerExtended;
import com.ibm.ws.webcontainer.security.extended.SSOCookieHelperImplExtended;
import com.ibm.ws.webcontainer.security.extended.WebAppSecurityConfigExtended;
import com.ibm.ws.webcontainer.security.extended.WebAuthenticatorProxyExtended;
import com.ibm.ws.webcontainer.security.internal.WebAppSecurityConfigImpl;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Represents security configurable options for web applications.
 */
public class WebAppSecurityConfigImplExtended extends WebAppSecurityConfigImpl implements WebAppSecurityConfigExtended {
    private static final TraceComponent tc = Tr.register(WebAppSecurityConfigImplExtended.class);

    protected final AtomicServiceReference<OidcServer> oidcServerRef;
    protected final AtomicServiceReference<OidcClient> oidcClientRef;

    public WebAppSecurityConfigImplExtended(Map<String, Object> newProperties,
                                            AtomicServiceReference<WsLocationAdmin> locationAdminRef,
                                            AtomicServiceReference<SecurityService> securityServiceRef,
                                            AtomicServiceReference<OidcServer> oidcServerRef,
                                            AtomicServiceReference<OidcClient> oidcClientRef) {
        super(newProperties, locationAdminRef, securityServiceRef);
        this.oidcServerRef = oidcServerRef;
        this.oidcClientRef = oidcClientRef;
        setSsoCookieName(oidcServerRef, oidcClientRef);
    }

    /*
     * This method set the runtime auto generate cookie name for OIDC server and client.
     */
    @Override
    public void setSsoCookieName(AtomicServiceReference<OidcServer> oidcServerRef,
                                 AtomicServiceReference<OidcClient> oidcClientRef) {
        if (DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(ssoCookieName) && isRunTimeAutoGenSsoCookieName()) {
            String genCookieName = generateSsoCookieName();
            if (genCookieName != null) {
                ssoCookieName = genCookieName;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "auto generate ssoCookieName: ", ssoCookieName);
                }
            }
        }
    }

    /**
     *
     */
    @Override
    protected String resolveSsoCookieName(Map<String, Object> newProperties) {
        String genCookieName = null;
        String cookieName = (String) newProperties.get(CFG_KEY_SSO_COOKIE_NAME);
        if (DEFAULT_SSO_COOKIE_NAME.equalsIgnoreCase(cookieName) &&
            (autoGenSsoCookieName || isRunTimeAutoGenSsoCookieName())) {
            genCookieName = generateSsoCookieName();
        }

        if (genCookieName != null) {
            return genCookieName;
        } else {
            return cookieName;
        }
    }

    /*
     * This method will turn on the auto generation SSO cookie name if OIDC client and/or server services
     * available.
     */
    private boolean isRunTimeAutoGenSsoCookieName() {
        if ((oidcClientRef != null && oidcClientRef.getService() != null) ||
            (oidcServerRef != null && oidcServerRef.getService() != null && !oidcServerRef.getService().allowDefaultSsoCookieName()))
            return true;
        else
            return false;
    }

    /** {@inheritDoc} */
    @Override
    public SSOCookieHelper createSSOCookieHelper() {
        return new SSOCookieHelperImplExtended(this);
    }

    /** {@inheritDoc} */
    @Override
    public ReferrerURLCookieHandler createReferrerURLCookieHandler() {
        return new ReferrerURLCookieHandlerExtended(this);
    }

    /** {@inheritDoc} */
    @Override
    public WebAuthenticatorProxy createWebAuthenticatorProxy() {
        return new WebAuthenticatorProxyExtended(this, null, securityServiceRef, null, oidcServerRef);
    }

}
