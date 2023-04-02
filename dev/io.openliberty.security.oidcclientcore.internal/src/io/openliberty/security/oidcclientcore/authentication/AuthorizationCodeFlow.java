/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import static io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters.STATE;
import static io.openliberty.security.oidcclientcore.token.TokenConstants.CODE;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;
import io.openliberty.security.oidcclientcore.token.JakartaOidcTokenRequest;

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
    @Override
    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) throws AuthenticationResponseException, TokenRequestException {
        JakartaOidcAuthenticationResponseValidator responseValidator = new JakartaOidcAuthenticationResponseValidator(request, response, oidcClientConfig);
        responseValidator.validateResponse();

        String state = request.getParameter(STATE);
        Storage storage = StorageFactory.instantiateStorage(request, response, oidcClientConfig.isUseSession());

        String originalRequestUrl = getOriginalRequestUrlWithoutQueryParams(storage, state);
        if (shouldRedirectToOriginalResource(request, originalRequestUrl)) {
            return getRedirectToOriginalResourceResult(request, originalRequestUrl);
        }

        removeStoredState(storage, state);

        JakartaOidcTokenRequest tokenRequest = new JakartaOidcTokenRequest(oidcClientConfig, request);
        return tokenRequest.sendRequest();
    }

    private String getOriginalRequestUrlWithoutQueryParams(Storage storage, String state) {
        String originalRequestUrl = storage.get(OidcStorageUtils.getOriginalReqUrlStorageKey(state));
        if (originalRequestUrl == null) {
            return null;
        }

        return originalRequestUrl.split(Pattern.quote("?"))[0]; // without query params
    }

    private boolean shouldRedirectToOriginalResource(HttpServletRequest request, String originalRequestUrl) {
        String currentRequestUrl = request.getRequestURL().toString();
        return oidcClientConfig.isRedirectToOriginalResource() && !currentRequestUrl.equals(originalRequestUrl);
    }

    private ProviderAuthenticationResult getRedirectToOriginalResourceResult(HttpServletRequest request, String originalRequestUrl) {
        String redirectUrl = appendCodeAndStateParams(originalRequestUrl, request);
        return new ProviderAuthenticationResult(AuthResult.REDIRECT, HttpServletResponse.SC_FOUND, null, null, null, redirectUrl);
    }

    private String appendCodeAndStateParams(String originalRequestUrl, HttpServletRequest request) {
        originalRequestUrl += "?" + CODE + "=" + request.getParameter(CODE);
        originalRequestUrl += "&" + STATE + "=" + request.getParameter(STATE);
        return originalRequestUrl;
    }

    private void removeStoredState(Storage storage, String state) {
        storage.remove(OidcStorageUtils.getStateStorageKey(state));
    }

}
