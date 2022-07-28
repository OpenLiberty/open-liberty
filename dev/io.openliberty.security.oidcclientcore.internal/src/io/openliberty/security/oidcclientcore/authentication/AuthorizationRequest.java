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

import io.openliberty.security.oidcclientcore.exceptions.OidcUrlNotHttpsException;

public abstract class AuthorizationRequest {

    protected HttpServletRequest request;
    protected HttpServletResponse response;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();

    public AuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public ProviderAuthenticationResult sendRequest() throws OidcUrlNotHttpsException {
        getAuthorizationEndpoint();

        createSessionIfNecessary();

        String state = requestUtils.generateStateValue(request);
        storeStateValue(state);

        String redirectUrl = getRedirectUrl();
        return redirectToAuthorizationEndpoint(state, redirectUrl);
    }

    protected abstract String getAuthorizationEndpoint() throws OidcUrlNotHttpsException;

    protected abstract String getRedirectUrl() throws OidcUrlNotHttpsException;

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

//    void createAndAddStateCookie(String state) {
//        String cookieName = OidcClientStorageConstants.WAS_OIDC_STATE_KEY + Utils.getStrHashCode(state);
    // TODO
//        String cookieValue = OidcCookieUtils.createStateCookieValue(clientConfig.getClientSecret(), state);
//        createAndAddCookie(cookieName, cookieValue);
//    }

//    void createAndAddCookie(String cookieName, String cookieValue) {
//        int cookieLifeTime = (int) clientConfig.getAuthenticationTimeLimitInSeconds();
//        Cookie c = OidcClientUtil.createCookie(cookieName, cookieValue, cookieLifeTime, request);
//        boolean isHttpsRequest = request.getScheme().toLowerCase().contains("https");
//        if (clientConfig.isHttpsRequired() && isHttpsRequest) {
//            c.setSecure(true);
//        }
//        response.addCookie(c);
//    }

    protected ProviderAuthenticationResult redirectToAuthorizationEndpoint(String state, String redirectUrl) {
        // TODO
        return null;
//        String authzEndPointUrlWithQuery = null;
//        try {
//            authzEndPointUrlWithQuery = buildAuthorizationUrlWithQuery((OidcClientRequest) request.getAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST), state, redirectUrl,
//                                                                       acr_values);
//
//            savePostParameters();
//
//            // Redirect to OP
//            // If clientSideRedirect is true (default is true) then do the
//            // redirect.  If the user agent doesn't support javascript then config can set this to false.
//            if (clientConfig.isClientSideRedirect()) {
//                String domain = OidcClientUtil.getSsoDomain(request);
//                doClientSideRedirect(authzEndPointUrlWithQuery, state, domain);
//            } else {
//                createAndAddWasReqUrlCookie(state);
//            }
//
//        } catch (Exception ioe) {
//            // TODO - NLS
//            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
//
//        }
//        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, authzEndPointUrlWithQuery);
    }

}
