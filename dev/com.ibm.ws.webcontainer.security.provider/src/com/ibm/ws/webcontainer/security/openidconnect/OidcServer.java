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
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Service;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public interface OidcServer {
    /**
     * Perform OpenID Connect authentication for the given web request. Return an
     * OidcAuthenticationResult which contains the status and subject
     * 
     * @param HttpServletRequest
     * @param HttpServletResponse
     * @param AtomicServiceReference<OAuth20Service>
     * @return ProviderAuthenticationResult
     */
    ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                              HttpServletResponse res,
                                              AtomicServiceReference<OAuth20Service> oauthServiceRef);

    /**
     * @param req
     * @param protectedOrAll :true -- check the protected URI only
     *            :false -- check all OIDC specific URI, no metter protected or not.
     * @return
     */
    boolean isOIDCSpecificURI(HttpServletRequest req, boolean protectedOrAll);

    boolean allowDefaultSsoCookieName();
}
