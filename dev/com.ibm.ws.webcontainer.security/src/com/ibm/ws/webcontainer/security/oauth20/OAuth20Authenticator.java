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

/**
 * This class handles oauth connect authentication for incoming web requests.
 */
public interface OAuth20Authenticator {
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                                     HttpServletResponse res);

    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                                     HttpServletResponse res,
                                                     ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef);
}
