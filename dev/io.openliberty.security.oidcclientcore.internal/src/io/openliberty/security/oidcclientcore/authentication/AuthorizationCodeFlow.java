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

import java.util.Hashtable;

import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenRequestor;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;
import io.openliberty.security.oidcclientcore.token.TokenResponse;

@Component(service = AuthorizationCodeFlow.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class AuthorizationCodeFlow extends AbstractFlow {

    public static final TraceComponent tc = Tr.register(AuthorizationCodeFlow.class);

    public static final String AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE = "TOKEN_RESPONSE";

    private static final String KEY_SSL_SUPPORT = "sslSupport";
    private static volatile SSLSupport sslSupport;

    private OidcClientConfig oidcClientConfig;

    /**
     * Do not use; needed for this to be a valid @Component object.
     */
    @Deprecated
    public AuthorizationCodeFlow() {
        // Only for OSGi initialization
    }

    public AuthorizationCodeFlow(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    @Reference(name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC)
    protected void setSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = sslSupportSvc;
    }

    protected void unsetSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = null;
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

        TokenResponse tokenEndpointResponse = sendTokenRequest(request);

        return createAuthenticationResultFromTokenResponse(tokenEndpointResponse);
    }

    TokenResponse sendTokenRequest(HttpServletRequest request) throws TokenRequestException {
        String tokenEndpoint = getTokenEndpoint();
        if (tokenEndpoint == null || tokenEndpoint.isEmpty()) {
            String clientId = oidcClientConfig.getClientId();
            String message = Tr.formatMessage(tc, "TOKEN_ENDPOINT_MISSING", clientId);
            throw new TokenRequestException(clientId, message);
        }
        String authzCode = request.getParameter(TokenConstants.CODE);
        return sendTokenRequestForCode(tokenEndpoint, authzCode);
    }

    String getTokenEndpoint() {
        String tokenEndpoint = null;
        OidcProviderMetadata providerMetadata = oidcClientConfig.getProviderMetadata();
        if (providerMetadata != null) {
            // Provider metadata overrides properties discovered via providerUri
            tokenEndpoint = providerMetadata.getTokenEndpoint();
            if (tokenEndpoint != null && !tokenEndpoint.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Token endpoint found in the provider metadata: [" + tokenEndpoint + "]");
                }
                return tokenEndpoint;
            }
        }
        return getTokenEndpointFromDiscoveryMetadata();
    }

    String getTokenEndpointFromDiscoveryMetadata() {
        String tokenEndpoint = null;
        // TODO
//        JSONObject discoveryData = getProviderMetadata();
//        tokenEndpoint = (String) discoveryData.get(OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
//        if (tokenEndpoint == null) {
//            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
//            throw new OidcDiscoveryException(oidcClientConfig.getClientId(), oidcClientConfig.getProviderURI(), nlsMessage);
//        }
        return tokenEndpoint;
    }

    TokenResponse sendTokenRequestForCode(String tokenEndpoint, String authzCode) throws TokenRequestException {
        String clientId = oidcClientConfig.getClientId();
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = oidcClientConfig.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }

        Builder tokenRequestBuilder = new TokenRequestor.Builder(tokenEndpoint, clientId, clientSecret, oidcClientConfig.getRedirectURI(), authzCode);
        tokenRequestBuilder.sslSocketFactory(getSSLSocketFactory());
        tokenRequestBuilder.grantType(TokenConstants.AUTHORIZATION_CODE);
        TokenRequestor tokenRequestor = tokenRequestBuilder.build();
        try {
            return tokenRequestor.requestTokens();
        } catch (Exception e) {
            throw new TokenRequestException(clientId, e.toString(), e);
        }
    }

    SSLSocketFactory getSSLSocketFactory() {
        if (sslSupport != null) {
            return sslSupport.getSSLSocketFactory();
        }
        return null;
    }

    ProviderAuthenticationResult createAuthenticationResultFromTokenResponse(TokenResponse tokenEndpointResponse) {
        Hashtable<String, Object> customProperties = new Hashtable<>();
        customProperties.put(AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE, tokenEndpointResponse);

        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, new Subject(), customProperties, null);
    }

}
