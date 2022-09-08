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
package io.openliberty.security.oidcclientcore.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.AbstractFlow;
import io.openliberty.security.oidcclientcore.authentication.Flow;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import io.openliberty.security.oidcclientcore.token.TokenResponseValidator;
import io.openliberty.security.oidcclientcore.token.TokenValidationException;

public class Client {

    private final OidcClientConfig oidcClientConfig;

    public Client(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.startFlow(request, response);
    }

    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) throws AuthenticationResponseException, TokenRequestException {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.continueFlow(request, response);
    }
    
    public boolean validate(TokenResponse tokenResponse) throws TokenValidationException {
        TokenResponseValidator tokenResponseValidator = new TokenResponseValidator(this.oidcClientConfig);
        tokenResponseValidator.validate(tokenResponse);
        return false;
    }

    public void logout() {
        // TODO
    }

}
