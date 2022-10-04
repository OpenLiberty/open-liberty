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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.Utils;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionWrapper;
import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestUtils;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.ClientManager;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.JakartaOidcTokenRequest;
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
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
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
        } else if (alreadyAuthenticated) {
            status = processExpiredTokenResult(processExpiredToken(client, request, response, httpMessageContext), httpMessageContext);
        }

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

    /**
     * Use SEND_CONTINUE rather than SEND_FAILURE for failures since JaspiServiceImpl converts a SEND_FAILURE to AuthenticationResult(AuthResult.RETURN, detail)
     * and the WebContainerSecurityCollaboratorImpl allows access to unprotected resources for AuthResult.RETURN. SEND_CONTINUE will prevent this by properly
     * returning a 401 and not continue to the redirectUri.
     */
    private AuthenticationStatus processCallback(Client client, HttpServletRequest request, HttpServletResponse response,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_CONTINUE;

        try {
            ProviderAuthenticationResult providerAuthenticationResult = client.continueFlow(request, response);
            status = processContinueFlowResult(providerAuthenticationResult, httpMessageContext, request, response, client);
        } catch (AuthenticationResponseException | TokenRequestException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        return status;
    }

    private AuthenticationStatus processContinueFlowResult(ProviderAuthenticationResult providerAuthenticationResult,
                                                           HttpMessageContext httpMessageContext, HttpServletRequest request, HttpServletResponse response,
                                                           Client client) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.SUCCESS.equals(authResult)) {
                status = handleOidcLogin(providerAuthenticationResult, httpMessageContext, request, response, client);
            }
        }

        return status;
    }

    private AuthenticationStatus handleOidcLogin(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext,
                                                 HttpServletRequest request, HttpServletResponse response, Client client) throws AuthenticationException {
        OidcTokensCredential credential = createOidcTokensCredential(providerAuthenticationResult, request, response, client);

        return validateCredentials(credential, httpMessageContext);
    }

    private OidcTokensCredential createOidcTokensCredential(ProviderAuthenticationResult providerAuthenticationResult, HttpServletRequest request, HttpServletResponse response,
                                                            Client client) {
        OidcTokensCredential credential = null;

        Hashtable<String, Object> customProperties = providerAuthenticationResult.getCustomProperties();
        if (customProperties != null) {
            TokenResponse tokenResponse = (TokenResponse) customProperties.get(JakartaOidcTokenRequest.AUTH_RESULT_CUSTOM_PROP_TOKEN_RESPONSE);
            if (tokenResponse != null) {
                credential = new OidcTokensCredential(tokenResponse, client, request, response);
            }
        }

        return credential;
    }

    private AuthenticationStatus validateCredentials(OidcTokensCredential credential, HttpMessageContext httpMessageContext) throws AuthenticationException {
        int rspStatus;
        String issuer = JavaEESecConstants.DEFAULT_REALM; // TODO: Set to "iss" claim from the identity token.
        Subject clientSubject = httpMessageContext.getClientSubject();
        AuthenticationStatus status = utils.handleAuthenticate(getCDI(), issuer, credential, clientSubject, httpMessageContext);

        if (status == AuthenticationStatus.SUCCESS) {
            setOpenIdContextInSubject(clientSubject, credential.getOpenIdContext());

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

    private void setOpenIdContextInSubject(Subject clientSubject, OpenIdContext openIdContext) {
        if (openIdContext != null) {
            clientSubject.getPrivateCredentials().add(openIdContext);
        }
    }

    private AuthenticationStatus processExpiredTokenResult(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext) {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.REDIRECT_TO_PROVIDER.equals(authResult)) {
                status = httpMessageContext.redirect(providerAuthenticationResult.getRedirectUrl());
            } else if (AuthResult.SUCCESS.equals(authResult)) {
                status = AuthenticationStatus.SUCCESS;
            }
        }

        return status;
    }

    private ProviderAuthenticationResult processExpiredToken(Client client, HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {
        OpenIdContext openIdContext = getOpenIdContextFromSubject(httpMessageContext);

        if (openIdContext == null) {
            // TODO add debug. should not be here.
            return new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        IdentityToken idToken = openIdContext.getIdentityToken();
        boolean isAccessTokenExpired = openIdContext.getAccessToken().isExpired();
        boolean isIdTokenExpired = idToken.isExpired();
        String idTokenString = idToken.getToken();
        String refreshTokenString = getRefreshToken(openIdContext);
        return client.processExpiredToken(request, response, isAccessTokenExpired, isIdTokenExpired, idTokenString, refreshTokenString);

    }

    private OpenIdContext getOpenIdContextFromSubject(HttpMessageContext httpMessageContext) {
        Subject clientSubject = httpMessageContext.getClientSubject();
        Set<OpenIdContext> creds = clientSubject.getPrivateCredentials(OpenIdContext.class);
        for (OpenIdContext openIdContext : creds) {
            // there should only be one OpenIdContext in the clientSubject.getPrivateCredentials(OpenIdContext.class) set.
            return openIdContext;
        }
        return null;
    }

    private String getRefreshToken(OpenIdContext openIdContext) {
        Optional<RefreshToken> optionalRefreshToken = openIdContext.getRefreshToken();
        if (optionalRefreshToken.isPresent()) {
            RefreshToken refreshToken = optionalRefreshToken.get();
            if (refreshToken != null) {
                return refreshToken.getToken();
            }
        }
        return null;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

}
