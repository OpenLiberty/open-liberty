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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;

public class AuthorizationCodeFlow extends AbstractFlow {

    public static final TraceComponent tc = Tr.register(AuthorizationCodeFlow.class);

    private final OidcClientConfig oidcClientConfig;

    public AuthorizationCodeFlow(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    @Override
    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) {
        JakartaOidcAuthorizationRequest authzRequest = new JakartaOidcAuthorizationRequest(request, response, oidcClientConfig);
        return authzRequest.sendRequest();
    }

    /**
     * Validates the Authentication Response that was the result of a previous call to <code>startFlow()</code>. If the response is
     * valid, this moves on to the following steps (From https://openid.net/specs/openid-connect-core-1_0.html#CodeFlowSteps):
     * 6. Client requests a response using the Authorization Code at the Token Endpoint.
     * 7. Client receives a response that contains an ID Token and Access Token in the response body.
     * 8. (Not done for Jakarta Security 3.0) Client validates the ID token and retrieves the End-User's Subject Identifier.
     */
    @FFDCIgnore(AuthenticationResponseException.class)
    @Override
    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) throws AuthenticationResponseException {
        JakartaOidcAuthenticationResponseValidator responseValidator = new JakartaOidcAuthenticationResponseValidator(request, response, oidcClientConfig);
        try {
            responseValidator.validateResponse();
            // TODO: Clear stored state value
        } catch (AuthenticationResponseException e) {
            Tr.error(tc, e.getMessage());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        // TODO
        return null;
    }

}
