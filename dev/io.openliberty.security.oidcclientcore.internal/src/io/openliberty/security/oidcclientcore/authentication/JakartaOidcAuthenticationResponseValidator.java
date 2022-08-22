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

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;

public class JakartaOidcAuthenticationResponseValidator extends AuthenticationResponseValidator {

    private final OidcClientConfig oidcClientConfig;

    public JakartaOidcAuthenticationResponseValidator(HttpServletRequest request, HttpServletResponse response, OidcClientConfig oidcClientConfig) {
        super(request, response);
        this.oidcClientConfig = oidcClientConfig;
    }

    @Override
    public void validateResponse() throws AuthenticationResponseException {
        // TODO
    }

}
