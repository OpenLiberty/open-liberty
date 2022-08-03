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

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.exceptions.OidcUrlNotHttpsException;

public class JakartaOidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcAuthorizationRequest.class);

    final int defaultStateCookieLifetime = 420;

    private final OidcClientConfig config;
    private final OidcProviderMetadata providerMetadata;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();

    public JakartaOidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, OidcClientConfig config) {
        super(request, response);
        this.config = config;
        this.providerMetadata = (config == null) ? null : config.getProviderMetadata();
    }

    @Override
    protected String getAuthorizationEndpoint() throws OidcUrlNotHttpsException {
        if (providerMetadata == null) {
            // TODO - perform discovery?
        }
        return providerMetadata.getAuthorizationEndpoint();
    }

    @Override
    protected String getRedirectUrl() throws OidcUrlNotHttpsException {
        return config.getRedirectURI();
    }

    @Override
    protected boolean shouldCreateSession() {
        return config.isUseSession();
    }

    @Override
    protected void storeStateValue(String state) {
        if (config != null && config.isUseSession()) {
            storeStateValueInHttpSession(state);
        } else {
            storeStateValueInCookie(state);
        }
    }

    void storeStateValueInHttpSession(String state) {
        // TODO - store in HTTP session
    }

    void storeStateValueInCookie(String state) {
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = config.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }
        cookieUtils.createAndAddStateCookie(state, clientSecret, defaultStateCookieLifetime);
    }

}
