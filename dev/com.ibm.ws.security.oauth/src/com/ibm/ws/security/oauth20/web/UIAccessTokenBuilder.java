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
package com.ibm.ws.security.oauth20.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentImpl;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

/**
 *  Given an authenticated user, and a client id, build an access token for that user directly,
 *  without using any of the oauth grants.
 *  This is used by the accountManager and TokenManager UI so it has an access token required by the rest endpoints.
 */
public class UIAccessTokenBuilder {
    private static TraceComponent tc = Tr.register(UIAccessTokenBuilder.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    OAuth20Component _component = null;
    OAuth20Provider _provider = null;
    HttpServletRequest _req = null;

    UIAccessTokenBuilder(OAuth20Provider provider, HttpServletRequest req) {
        _component = provider.getComponent();
        _provider = provider;
        _req = req;
    }

    /**
     * create a token and auth header values and place on request as params for ui to use
     */
    void createHeaderValuesForUI() {
        OidcBaseClient client = getClient();
        OAuth20Token token = createAccessTokenForAuthenticatedUser();
        String authHeader = createAuthHeaderValueFromClientIdAndSecret();
        if (token != null && authHeader != null) {
            _req.setAttribute("ui_token", token.getId());
            _req.setAttribute("ui_authheader", authHeader);
            _req.setAttribute("ui_app_pw_enabled", (client == null ? false : client.isAppPasswordAllowed()));
            _req.setAttribute("ui_app_tok_enabled", (client == null ? false : client.isAppTokenAllowed()));
        }
    }

    OidcBaseClient getClient() {
        String clientId = _provider.getInternalClientId();
        if (clientId == null) {
            return null;
        }
        OidcBaseClient client = null;
        try {
            client = _provider.getClientProvider().get(clientId);
        } catch (OidcServerException e) {
            // ffdc
        }
        return client;
    }

    OAuth20Token createAccessTokenForAuthenticatedUser() {
        // todo: should we check that provider supports implicit grant type? that's the closest to what this does,
        // but some customers that want this might not want external implicit gth support.
        if (_component == null || _provider == null || _req == null) {
            return null;
        }
        OAuth20TokenFactory tokenFactory = new OAuth20TokenFactory((OAuth20ComponentImpl) _component);
        String clientId = _provider.getInternalClientId();
        String user = _req.getUserPrincipal() != null ? _req.getUserPrincipal().getName() : null;

        if (clientId == null || clientId.isEmpty() || user == null) {
            // isUIEnabled should have checked the clientId so we're just down to not authenticated here.
            // should never get this far, but just in case, 1440e
            Tr.error(tc, "OAUATH_BASIC_AUTH_FAIL", new Object[] {}); // CWWKS1440E
            return null;
        }
        Map<String, String[]> tokenAttributesMap = getTokenAttributesMap(tokenFactory, clientId, user);
        OAuth20Token token = tokenFactory.createAccessToken(tokenAttributesMap);
        return token;
    }

    Map<String, String[]> getTokenAttributesMap(OAuth20TokenFactory factory, String clientId, String user) {
        return factory.buildTokenMap(clientId, user, null, null, getScopes(clientId), null, OAuth20Constants.GRANT_TYPE_IMPLICIT_INTERNAL);
    }

    String[] getScopes(String clientId) {
        try {
            String scopes = _provider.getClientProvider().get(clientId).getPreAuthorizedScope();
            return scopes == null ? null : scopes.trim().split("\\s+"); // one or more spaces
        } catch (OidcServerException e) {
            // ffdc
            return null;
        }
    }

    private String createAuthHeaderValueFromClientIdAndSecret() {
        String clientId = _provider.getInternalClientId();
        String secret = _provider.getInternalClientSecret();
        String result = null;

        if (clientId != null && secret != null && clientId.length() > 0 && secret.length() > 0) {
            result = "Basic " + Base64Coder.base64Encode(clientId + ":" + secret);
        }
        return result;
    }
}
