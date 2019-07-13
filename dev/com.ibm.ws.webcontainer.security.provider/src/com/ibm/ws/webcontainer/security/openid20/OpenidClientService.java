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
package com.ibm.ws.webcontainer.security.openid20;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

public interface OpenidClientService {
    public String getOpenIdIdentifier(HttpServletRequest req);

    public void createAuthRequest(HttpServletRequest req, HttpServletResponse res)
                    throws Exception;

    public String getRpRequestIdentifier(HttpServletRequest req, HttpServletResponse res);

    public ProviderAuthenticationResult verifyOpResponse(HttpServletRequest request, HttpServletResponse response)
                    throws Exception;

    public boolean isMapIdentityToRegistryUser();
}
