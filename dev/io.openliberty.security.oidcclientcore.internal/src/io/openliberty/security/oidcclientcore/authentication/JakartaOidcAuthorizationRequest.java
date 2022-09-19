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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.CookieStorageProperties;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;
import io.openliberty.security.oidcclientcore.storage.StorageProperties;

public class JakartaOidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcAuthorizationRequest.class);

    private enum StorageType {
        COOKIE, SESSION
    }

    private OidcClientConfig config = null;
    private OidcProviderMetadata providerMetadata = null;

    private StorageType storageType;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();

    public JakartaOidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, OidcClientConfig config) {
        super(request, response, config.getClientId());
        this.config = config;
        this.providerMetadata = (config == null) ? null : config.getProviderMetadata();
        instantiateStorage(config);
    }

    private void instantiateStorage(OidcClientConfig config) {
        if (config.isUseSession()) {
            this.storage = new SessionBasedStorage(request);
            this.storageType = StorageType.SESSION;
        } else {
            this.storage = new CookieBasedStorage(request, response);
            this.storageType = StorageType.COOKIE;
        }
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
        String authzEndpoint = getAuthorizationEndpointFromProviderMetadata();
        if (authzEndpoint != null) {
            return authzEndpoint;
        }
        return getAuthorizationEndpointFromDiscoveryMetadata();
    }

    String getAuthorizationEndpointFromProviderMetadata() {
        if (providerMetadata != null) {
            // Provider metadata overrides properties discovered via providerUri
            String authzEndpoint = providerMetadata.getAuthorizationEndpoint();
            if (authzEndpoint != null && !authzEndpoint.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authorization endpoint found in the provider metadata: [" + authzEndpoint + "]");
                }
                return authzEndpoint;
            }
        }
        return null;
    }

    String getAuthorizationEndpointFromDiscoveryMetadata() throws OidcDiscoveryException {
        String authzEndpoint = null;
        JSONObject discoveryData = getProviderDiscoveryMetadata(config);
        if (discoveryData != null) {
            authzEndpoint = (String) discoveryData.get(OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT);
        }
        if (authzEndpoint == null) {
            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT);
            throw new OidcDiscoveryException(clientId, config.getProviderURI(), nlsMessage);
        }
        return authzEndpoint;
    }

    @Override
    protected String getRedirectUrl() {
        return config.getRedirectURI();
    }

    @Override
    protected boolean shouldCreateSession() {
        return storageType == StorageType.SESSION;
    }

    @Override
    protected String createStateValueForStorage(String state) {
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = config.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }
        return OidcStorageUtils.createStateStorageValue(state, clientSecret);
    }

    @Override
    protected String createNonceValueForStorage(String nonce, String state) {
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = config.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }
        return OidcStorageUtils.createNonceStorageValue(nonce, state, clientSecret);
    }

    @Override
    protected StorageProperties getStateStorageProperties() {
        StorageProperties superProps = super.getStateStorageProperties();
        if (storageType == StorageType.COOKIE) {
            // Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog,
            // "In the case of storage through a Cookie, the Cookie must be defined as HTTPonly and must have the Secure flag set."
            CookieStorageProperties props = new CookieStorageProperties();
            props.setStorageLifetimeSeconds(superProps.getStorageLifetimeSeconds());
            props.setHttpOnly(true);
            props.setSecure(true);
            return props;
        } else {
            return superProps;
        }
    }

    @Override
    protected StorageProperties getNonceStorageProperties() {
        StorageProperties superProps = super.getNonceStorageProperties();
        if (storageType == StorageType.COOKIE) {
            // Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog,
            // "In the case of storage through a Cookie, the Cookie must be defined as HTTPonly and must have the Secure flag set."
            CookieStorageProperties props = new CookieStorageProperties();
            props.setStorageLifetimeSeconds(superProps.getStorageLifetimeSeconds());
            props.setHttpOnly(true);
            props.setSecure(true);
            return props;
        } else {
            return superProps;
        }
    }

    @Override
    protected StorageProperties getOriginalRequestUrlStorageProperties() {
        StorageProperties superProps = super.getOriginalRequestUrlStorageProperties();
        if (storageType == StorageType.COOKIE) {
            CookieStorageProperties props = new CookieStorageProperties();
            props.setStorageLifetimeSeconds(superProps.getStorageLifetimeSeconds());
            return props;
        } else {
            return superProps;
        }
    }

    @Override
    @FFDCIgnore(Exception.class)
    protected ProviderAuthenticationResult redirectToAuthorizationEndpoint(String state, String redirectUrl) {
        String authzEndPointUrlWithQuery = null;
        try {
            authzEndPointUrlWithQuery = buildAuthorizationUrlWithQuery(state, redirectUrl);
        } catch (Exception e) {
            Tr.error(tc, "ERROR_BUILDING_AUTHORIZATION_ENDPOINT_URL", clientId, e);
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);

        }
        storeOriginalRequestUrl(state);
        if (shouldFullRequestBeStored()) {
            storeFullRequest();
        }
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, authzEndPointUrlWithQuery);
    }

    String buildAuthorizationUrlWithQuery(String state, String redirectUrl) throws OidcClientConfigurationException, OidcDiscoveryException {
        String authorizationEndpoint = getAuthorizationEndpoint();
        String scopes = getScopeString();
        String responseType = config.getResponseType();

        AuthorizationRequestParameters authzParameters = new AuthorizationRequestParameters(authorizationEndpoint, scopes, responseType, clientId, redirectUrl, state);

        addOptionalParameters(authzParameters, state);

        return authzParameters.buildRequestUrl();
    }

    String getScopeString() {
        String scopes = "";
        Set<String> scopesSet = config.getScope();
        for (String scope : scopesSet) {
            scopes += scope + " ";
        }
        scopes = scopes.trim();
        return scopes;
    }

    void addOptionalParameters(AuthorizationRequestParameters authzParameters, String state) {
        if (config.isUseNonce()) {
            String nonceValue = requestUtils.generateNonceValue();
            storeNonceValue(nonceValue, state);
            authzParameters.addParameter(AuthorizationRequestParameters.NONCE, nonceValue);
        }
        String prompt = config.getPromptParameter();
        if (prompt != null) {
            authzParameters.addParameter(AuthorizationRequestParameters.PROMPT, prompt);
        }
        String responseMode = config.getResponseMode();
        if (responseMode != null) {
            authzParameters.addParameter(AuthorizationRequestParameters.RESPONSE_MODE, responseMode);
        }
        String display = config.getDisplayParameter();
        if (display != null) {
            authzParameters.addParameter(AuthorizationRequestParameters.DISPLAY, display);
        }
        addExtraParameters(authzParameters);
    }

    void addExtraParameters(AuthorizationRequestParameters authzParameters) {
        String[] extraParametersArray = config.getExtraParameters();
        if (extraParametersArray == null) {
            return;
        }
        for (String extraParamAndValue : extraParametersArray) {
            String[] keyAndValue = extraParamAndValue.split("=");
            String key = keyAndValue[0];
            String value = "";
            if (keyAndValue.length > 1) {
                value = keyAndValue[1];
            }
            authzParameters.addParameter(key, value);
        }
    }

    boolean shouldFullRequestBeStored() {
        // Per https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog:
        // Additionally, if OpenIdAuthenticationMechanismDefinition.redirectToOriginalResource is set to 'true' and the
        // authentication flow is container-initiated (as opposed to caller-initiated authentication) the authentication mechanism
        // must store the full request as well. The full request here means all data that makes up the HttpServletRequest so that
        // the container can restore this request later on in a similar way to how the "LoginToContinue Annotation" behaves.
        return (config.isRedirectToOriginalResource() && isContainerInitiatedFlow());
    }

    boolean isContainerInitiatedFlow() {
        // TODO
        return false;
    }

    void storeFullRequest() {
        // TODO

    }

}
