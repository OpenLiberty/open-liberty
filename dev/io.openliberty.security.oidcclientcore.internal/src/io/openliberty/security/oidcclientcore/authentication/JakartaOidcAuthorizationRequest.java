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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;

public class JakartaOidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcAuthorizationRequest.class);

    private final OidcClientConfig config;
    private final OidcProviderMetadata providerMetadata;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();

    public JakartaOidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, OidcClientConfig config) {
        super(request, response, config.getClientId());
        this.config = config;
        this.providerMetadata = (config == null) ? null : config.getProviderMetadata();
    }

    @Override
    @FFDCIgnore(Exception.class)
    public ProviderAuthenticationResult sendRequest() {
        try {
            return super.sendRequest();
        } catch (Exception e) {
            Tr.error(tc, "ERROR_SENDING_AUTHORIZATION_REQUEST", clientId, e.getMessage());
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected String getAuthorizationEndpoint() throws OidcClientConfigurationException, OidcDiscoveryException {
        if (providerMetadata != null) {
            // Provider metadata overrides properties discovered via providerUri
            String authzEndpoint = providerMetadata.getAuthorizationEndpoint();
            if (authzEndpoint != null) {
                return authzEndpoint;
            }
        }
        performDiscovery();
        // TODO - get endpoint from discovery data
        return null;
    }

    void performDiscovery() throws OidcClientConfigurationException {
        String discoveryrUri = config.getProviderURI();
        if (discoveryrUri == null || discoveryrUri.isEmpty()) {
            String nlsMessage = Tr.formatMessage(tc, "OIDC_CLIENT_MISSING_PROVIDER_URI", clientId);
            throw new OidcClientConfigurationException(clientId, nlsMessage);
        }
        // TODO - perform discovery
        // TODO - how to get SSLSocketFactory?
    }

    @Override
    protected String getRedirectUrl() {
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
        cookieUtils.createAndAddStateCookie(state, clientSecret, OidcClientStorageConstants.STATE_COOKIE_DEFAULT_LIFETIME);
    }

}
