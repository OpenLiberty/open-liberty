/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;

public class ImplicitFlow extends AbstractFlow {

    public static final TraceComponent tc = Tr.register(ImplicitFlow.class);

    private final OidcClientConfig oidcClientConfig;

    public ImplicitFlow(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    @Override
    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) {
        // TODO
        return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Validates the Authentication Response that was the result of a previous call to <code>startFlow()</code>. If the response is
     * valid, this moves on to the following steps (From https://openid.net/specs/openid-connect-core-1_0.html#ImplicitFlowSteps):
     * 6. (Not done for Jakarta Security 3.0) Client validates the ID token and retrieves the End-User's Subject Identifier.
     */
    @Override
    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) {
        // TODO
        return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
    }

}
