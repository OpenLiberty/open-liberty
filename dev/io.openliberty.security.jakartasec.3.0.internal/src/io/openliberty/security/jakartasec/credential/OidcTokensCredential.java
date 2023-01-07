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
package io.openliberty.security.jakartasec.credential;

import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class OidcTokensCredential implements Credential {

    TokenResponse tokenResponse;
    Client client;
    OidcClientConfig oidcClientConfig;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private OpenIdContext openIdContext;

    public OidcTokensCredential(TokenResponse tokenResponse, Client client) {
        this.tokenResponse = tokenResponse;
        this.client = client;
    }

    /**
     * @param tokenResponse
     * @param client
     * @param request
     * @param response
     */
    public OidcTokensCredential(TokenResponse tokenResponse, Client client, HttpServletRequest request, HttpServletResponse response) {
        this.tokenResponse = tokenResponse;
        this.client = client;
        this.request = request;
        this.response = response;
    }

    public TokenResponse getTokenResponse() {
        return this.tokenResponse;
    }

    public Client getClient() {
        return this.client;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    public void setOpenIdContext(OpenIdContext openIdContext) {
        this.openIdContext = openIdContext;
    }

    public OpenIdContext getOpenIdContext() {
        return openIdContext;
    }

}
