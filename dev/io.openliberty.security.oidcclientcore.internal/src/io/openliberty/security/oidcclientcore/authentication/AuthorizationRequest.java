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

import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.storage.OidcCookieUtils;

public abstract class AuthorizationRequest {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected String clientId;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();
    protected OidcCookieUtils cookieUtils;

    public AuthorizationRequest(HttpServletRequest request, HttpServletResponse response, String clientId) {
        this.request = request;
        this.response = response;
        this.clientId = clientId;
        cookieUtils = new OidcCookieUtils(request, response);
    }

    public ProviderAuthenticationResult sendRequest() throws OidcClientConfigurationException, OidcDiscoveryException {
        getAuthorizationEndpoint();

        createSessionIfNecessary();

        String state = requestUtils.generateStateValue(request);
        storeStateValue(state);

        String redirectUrl = getRedirectUrl();
        return redirectToAuthorizationEndpoint(state, redirectUrl);
    }

    protected abstract String getAuthorizationEndpoint() throws OidcClientConfigurationException, OidcDiscoveryException;

    protected abstract String getRedirectUrl() throws OidcClientConfigurationException;

    protected abstract boolean shouldCreateSession();

    protected abstract void storeStateValue(String state);

    void createSessionIfNecessary() {
        if (shouldCreateSession()) {
            try {
                request.getSession(true);
            } catch (Exception e) {
                // ignore it. Session exists
            }
        }
    }

    protected ProviderAuthenticationResult redirectToAuthorizationEndpoint(String state, String redirectUrl) {
        // TODO
        return null;
    }

}
