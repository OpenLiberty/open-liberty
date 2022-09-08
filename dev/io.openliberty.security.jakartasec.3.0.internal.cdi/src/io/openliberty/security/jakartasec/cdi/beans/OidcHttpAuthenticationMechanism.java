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
package io.openliberty.security.jakartasec.cdi.beans;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.Utils;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionWrapper;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationCodeFlow;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestUtils;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.ClientManager;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Default
@ApplicationScoped
public class OidcHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    IdentityStoreHandler identityStoreHandler;

    private ModulePropertiesProvider mpp = null;
    private final Utils utils;
    private final AuthorizationRequestUtils requestUtils;

    public OidcHttpAuthenticationMechanism() {
        mpp = getModulePropertiesProvider();
        utils = getUtils();
        requestUtils = getRequestUtils();
    }

    private OpenIdAuthenticationMechanismDefinitionWrapper getOpenIdAuthenticationMechanismDefinition(HttpServletRequest request) {
        Properties props = mpp.getAuthMechProperties(OidcHttpAuthenticationMechanism.class);
        /*
         * Build the baseURL from the incoming HttpRequest as the redirectURL may contain baseURL variable, such as ${baseURL}/Callback
         */
        String baseURL = requestUtils.getBaseURL(request);
        return new OpenIdAuthenticationMechanismDefinitionWrapper((OpenIdAuthenticationMechanismDefinition) props.get(JakartaSec30Constants.OIDC_ANNOTATION), baseURL);
    }

    @SuppressWarnings("unchecked")
    protected ModulePropertiesProvider getModulePropertiesProvider() {
        Instance<ModulePropertiesProvider> modulePropertiesProviderInstance = getCDI().select(ModulePropertiesProvider.class);
        if (modulePropertiesProviderInstance != null) {
            return modulePropertiesProviderInstance.get();
        }
        return null;
    }

    // Protected to allow unit testing
    protected Utils getUtils() {
        return new Utils();
    }

    protected AuthorizationRequestUtils getRequestUtils() {
        return new AuthorizationRequestUtils();
    }

    @SuppressWarnings("rawtypes")
    @FFDCIgnore(IllegalStateException.class)
    protected CDI getCDI() {
        try {
            return CDI.current();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Client client = getClient(request);

        boolean alreadyAuthenticated = isAlreadyAuthenticated(request);

        if (isAuthenticationRequired(request, httpMessageContext, alreadyAuthenticated)) {
            status = processStartFlowResult(client.startFlow(request, response), httpMessageContext);
        } else if (isCallbackRequest(request)) {
            status = processCallback(client, request, response, httpMessageContext);
        }

        // Else if isAuthenticationSessionEstablished
        //   status = processTokenExpirationIfNeeded - client.processExpiredToken() / logout();

        return status;
    }

    // Protected to allow unit testing
    protected Client getClient(HttpServletRequest request) {
        return ClientManager.getClientFor(getOpenIdAuthenticationMechanismDefinition(request));
    }

    /**
     * Authentication is required when "when the caller tries to access a protected resource without being authenticated,
     * or when the caller explicitly initiates authentication, without being authenticated for the current request"
     *
     * @param alreadyAuthenticated
     */
    private boolean isAuthenticationRequired(HttpServletRequest request, HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return isProtectedResourceWithoutBeingAuthenticated(httpMessageContext, alreadyAuthenticated)
               || isProgrammaticAuthenticationWithoutBeingAuthenticated(httpMessageContext, alreadyAuthenticated);
    }

    private boolean isProtectedResourceWithoutBeingAuthenticated(HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return httpMessageContext.isProtected() && !alreadyAuthenticated;
    }

    private boolean isProgrammaticAuthenticationWithoutBeingAuthenticated(HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return httpMessageContext.isAuthenticationRequest() && !alreadyAuthenticated;
    }

    private boolean isAlreadyAuthenticated(HttpServletRequest request) {
        return request.getUserPrincipal() != null;
    }

    private AuthenticationStatus processStartFlowResult(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext) {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.REDIRECT_TO_PROVIDER.equals(authResult)) {
                status = httpMessageContext.redirect(providerAuthenticationResult.getRedirectUrl());
            } else if (AuthResult.SEND_401.equals(authResult)) {
                status = httpMessageContext.responseUnauthorized();
            }
        }

        return status;
    }

    /**
     * Section https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog states that
     * a callback is detected "when a request contains a state request parameter."
     */
    private boolean isCallbackRequest(HttpServletRequest request) {
        return request.getParameter(OpenIdConstant.STATE) != null;
    }

    private AuthenticationStatus processCallback(Client client, HttpServletRequest request, HttpServletResponse response,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        try {
            ProviderAuthenticationResult providerAuthenticationResult = client.continueFlow(request, response);
            status = processContinueFlowResult(providerAuthenticationResult, httpMessageContext);
        } catch (AuthenticationResponseException e) {
            status = httpMessageContext.notifyContainerAboutLogin(getCredentialValidationResultFromException(e));
        } catch (TokenRequestException e) {
            status = httpMessageContext.notifyContainerAboutLogin(CredentialValidationResult.INVALID_RESULT);
        }

        return status;
    }

    private CredentialValidationResult getCredentialValidationResultFromException(AuthenticationResponseException exception) {
        if (AuthenticationResponseException.ValidationResult.NOT_VALIDATED_RESULT == exception.getValidationResult()) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        } else {
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    private AuthenticationStatus processContinueFlowResult(ProviderAuthenticationResult providerAuthenticationResult,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.SUCCESS.equals(authResult)) {
                status = handleOidcLogin(providerAuthenticationResult, httpMessageContext);
            }
        }

        return status;
    }

    private AuthenticationStatus handleOidcLogin(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext) throws AuthenticationException {
        Credential credential = createOidcTokensCredential(providerAuthenticationResult);
        // TODO: "When available in the "Token Response", the optional fields "refresh_token" and "expires_in" must be stored internally." Store full credential in the subject.
        return validateCredentials(credential, httpMessageContext);
    }

    private Credential createOidcTokensCredential(ProviderAuthenticationResult providerAuthenticationResult) {
        Credential credential = null;

        Hashtable<String, Object> customProperties = providerAuthenticationResult.getCustomProperties();
        if (customProperties != null) {
            TokenResponse tokenResponse = (TokenResponse) customProperties.get(AuthorizationCodeFlow.AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE);
            if (tokenResponse != null) {
                // TODO: credential = new OidcTokensCredential(client, tokenResponse, userinfoResponse);
                credential = new Credential() {
                    {
                    }
                };
            }
        }

        return credential;
    }

    private AuthenticationStatus validateCredentials(Credential credential, HttpMessageContext httpMessageContext) throws AuthenticationException {
        int rspStatus;
        String issuer = JavaEESecConstants.DEFAULT_REALM; // TODO: Set to "iss" claim from the identity token.
        AuthenticationStatus status = utils.handleAuthenticate(getCDI(), issuer, credential, httpMessageContext.getClientSubject(), httpMessageContext);

        if (status == AuthenticationStatus.SUCCESS) {
            Map<String, Object> messageInfoMap = httpMessageContext.getMessageInfo().getMap();
            messageInfoMap.put("jakarta.servlet.http.authType", "JAKARTA_OIDC");
            messageInfoMap.put("jakarta.servlet.http.registerSession", Boolean.TRUE.toString());
            rspStatus = HttpServletResponse.SC_OK;
        } else if (status == AuthenticationStatus.NOT_DONE) {
            // set SC_OK, since if the target is not protected, it'll be processed.
            rspStatus = HttpServletResponse.SC_OK;
        } else {
            rspStatus = HttpServletResponse.SC_UNAUTHORIZED;
        }

        httpMessageContext.getResponse().setStatus(rspStatus);

        return status;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

}
