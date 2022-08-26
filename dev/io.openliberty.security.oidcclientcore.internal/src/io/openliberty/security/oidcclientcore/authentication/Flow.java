/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

public interface Flow {

    /**
     * From https://openid.net/specs/openid-connect-core-1_0.html:
     * 1. Client prepares an Authentication Request containing the desired request parameters.
     * 2. Client sends the request to the Authorization Server.
     */
    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response);

    /**
     * Validates the Authentication Response that was the result of a previous call to <code>startFlow()</code>. If the response is
     * valid, this moves on to complete the authentication flow and obtains whatever tokens are appropriate.
     */
    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response);

}
