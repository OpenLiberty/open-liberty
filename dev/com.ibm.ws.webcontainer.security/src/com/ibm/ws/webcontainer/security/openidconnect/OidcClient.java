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
package com.ibm.ws.webcontainer.security.openidconnect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

public interface OidcClient {

    // authenticated by the oidc propagation token, such as: access_token... etc
    public static final String PROPAGATION_TOKEN_AUTHENTICATED = "com.ibm.ws.webcontainer.security.openidconnect.propagation.token.authenticated";
    // In case it's authenticated by the oidc propagation token, do not create a cookie
    public static final String INBOUND_PROPAGATION_VALUE = "com.ibm.ws.webcontainer.security.openidconnect.inbound.propagation.value";
    public static final String AUTHN_SESSION_DISABLED = "com.ibm.ws.webcontainer.security.openidconnect.authn.session.disabled";
    public static final String ACCESS_TOKEN_IN_LTPA_TOKEN = "com.ibm.ws.webcontainer.security.oidc.accesstoken.in.ltpa";
    public static final String OIDC_ACCESS_TOKEN = "oidc_access_token";

    public static final String inboundNone = "none";
    public static final String inboundRequired = "required";
    public static final String inboundSupported = "supported";

    ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                              HttpServletResponse res,
                                              String provide,
                                              ReferrerURLCookieHandler referrerURLCookieHandler,
                                              boolean firstCall);

    /**
     * Check whether the request is OpenID Connect client or not
     * 
     * @param HttpServletRequest
     * @return String
     */
    String getOidcProvider(HttpServletRequest req);

    boolean isMapIdentityToRegistryUser(String provider);

    boolean isValidRedirectUrl(HttpServletRequest req);

    /**
     * @return
     */
    boolean anyClientIsBeforeSso();
}
