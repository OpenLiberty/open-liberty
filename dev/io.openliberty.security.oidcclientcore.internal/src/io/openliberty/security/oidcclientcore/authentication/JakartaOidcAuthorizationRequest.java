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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.http.OriginalResourceRequest;
import io.openliberty.security.oidcclientcore.storage.CookieStorageProperties;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;
import io.openliberty.security.oidcclientcore.storage.StorageProperties;

public class JakartaOidcAuthorizationRequest extends AuthorizationRequest {

    public static final TraceComponent tc = Tr.register(JakartaOidcAuthorizationRequest.class);

    public static final String IS_CONTAINER_INITIATED_FLOW = "IS_CONTAINER_INITIATED_FLOW";

    private enum StorageType {
        COOKIE, SESSION
    }

    private OidcClientConfig config = null;
    private StorageType storageType;

    protected AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();

    public JakartaOidcAuthorizationRequest(HttpServletRequest request, HttpServletResponse response, OidcClientConfig config) {
        super(request, response, config.getClientId());
        this.config = config;
        instantiateStorage(config);
    }

    private void instantiateStorage(OidcClientConfig config) {
        boolean useSession = config.isUseSession();
        this.storage = StorageFactory.instantiateStorage(request, response, useSession);
        this.storageType = useSession ? StorageType.SESSION : StorageType.COOKIE;
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
        return MetadataUtils.getAuthorizationEndpoint(config);
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
            storeFullRequest(state);
        }
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, authzEndPointUrlWithQuery);
    }

    String buildAuthorizationUrlWithQuery(String state, String redirectUrl) throws OidcClientConfigurationException, OidcDiscoveryException {
        String authorizationEndpoint = MetadataUtils.getAuthorizationEndpoint(config);
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
            String[] keyAndValue = extraParamAndValue.split("=", 2);
            if (keyAndValue.length < 2) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "addExtraParameters", "skipping extra param '" + extraParamAndValue + "' because it is not in the format key=value");
                }
                continue;
            }
            String key = keyAndValue[0];
            String value = keyAndValue[1];
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
        Object isContainerInitiatedFlow = request.getAttribute(IS_CONTAINER_INITIATED_FLOW);
        if (isContainerInitiatedFlow instanceof Boolean) {
            request.removeAttribute(IS_CONTAINER_INITIATED_FLOW);
            return (boolean) isContainerInitiatedFlow;
        }
        return false;
    }

    void storeFullRequest(String state) {
        OriginalResourceRequest.storeFullRequest(request, storage, state);
    }

}
