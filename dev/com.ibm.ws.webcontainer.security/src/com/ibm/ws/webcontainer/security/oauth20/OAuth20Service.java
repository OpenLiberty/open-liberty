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
package com.ibm.ws.webcontainer.security.oauth20;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public interface OAuth20Service {
    /**
     * Perform OAuth authentication for the given web request. Return an
     * OAuthAuthenticationResult which contains the status and subject
     * 
     * @param HttpServletRequest
     * @param HttpServletResponse
     * @return OAuthAuthenticationResult
     */
    ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                              HttpServletResponse res);

    ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                              HttpServletResponse res,
                                              ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef);

    /**
     * @param req
     * @param protectedOrAll : true - check the protected URI only
     *            : false - check all the Oauth specific URI, no matter protected or not
     * @return
     */
    boolean isOauthSpecificURI(HttpServletRequest req, boolean protectedOrAll);
}
