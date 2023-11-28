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
package io.openliberty.security.jakartasec.cdi.beans;

import static io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest.IS_CONTAINER_INITIATED_FLOW;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.Utils;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import io.openliberty.cdi40.internal.utils.CDI40Utils;
import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionHolder;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionWrapper;
import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.jakartasec.identitystore.OpenIdContextImpl;
import io.openliberty.security.jakartasec.identitystore.OpenIdContextUtils;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestUtils;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.ClientManager;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException;
import io.openliberty.security.oidcclientcore.http.OriginalResourceRequest;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;
import io.openliberty.security.oidcclientcore.token.JakartaOidcTokenRequest;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Default
@ApplicationScoped
public class OidcHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final TraceComponent tc = Tr.register(OidcHttpAuthenticationMechanism.class);

    private static final String AUTH_TYPE_PROPERTY = "jakarta.servlet.http.authType";
    private static final String AUTH_TYPE_JAKARTA_OIDC = "JAKARTA_OIDC";
    private static final String CHECKING_FOR_EXPIRED_TOKEN = "CHECKING_FOR_EXPIRED_TOKEN";
    private static final String JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT = "JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT";

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
        OpenIdAuthenticationMechanismDefinitionHolder openIdAuthenticationMechanismDefinitionHolder = (OpenIdAuthenticationMechanismDefinitionHolder) props.get(JakartaSec30Constants.OIDC_ANNOTATION);
        return new OpenIdAuthenticationMechanismDefinitionWrapper(openIdAuthenticationMechanismDefinitionHolder.getOpenIdAuthenticationMechanismDefinition(), baseURL);
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
    public AuthenticationStatus validateRequest(HttpServletRequest req,
                                                HttpServletResponse res,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();

        Client client = getClient(request);

        boolean alreadyAuthenticated = isAlreadyAuthenticated(request);

        if (isAuthenticationRequired(httpMessageContext, alreadyAuthenticated) && !containsStoredState(request, response, client)) {
            status = processStartFlow(client, httpMessageContext);
        } else if (isCallbackRequest(request)) {
            status = processCallback(client, httpMessageContext);
        } else if (alreadyAuthenticated) {
            status = checkForExpiredTokensAndProcessIfNeeded(client, request, response, httpMessageContext);
        } else if (!httpMessageContext.isProtected()) {
            status = AuthenticationStatus.NOT_DONE;
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
    private boolean isAuthenticationRequired(HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return isProtectedResourceWithoutBeingAuthenticated(httpMessageContext, alreadyAuthenticated)
               || isProgrammaticAuthenticationWithoutBeingAuthenticated(httpMessageContext, alreadyAuthenticated);
    }

    private boolean isProtectedResourceWithoutBeingAuthenticated(HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return httpMessageContext.isProtected() && !alreadyAuthenticated;
    }

    private boolean isProgrammaticAuthenticationWithoutBeingAuthenticated(HttpMessageContext httpMessageContext, boolean alreadyAuthenticated) {
        return isNewAuthentication(httpMessageContext.getAuthParameters()) || (httpMessageContext.isAuthenticationRequest() && !alreadyAuthenticated);
    }

    private boolean isNewAuthentication(AuthenticationParameters authParameters) {
        if (authParameters != null) {
            return authParameters.isNewAuthentication();
            // TODO: Clear current state (OpenIdContext, subject from cache, and anything in storage if state has been set) if new authentication was requested.
        }
        return false;
    }

    private boolean containsStoredState(HttpServletRequest request, HttpServletResponse response, Client client) {
        String state = request.getParameter(OpenIdConstant.STATE);
        if (state == null) {
            return false;
        }
        OidcClientConfig clientConfig = client.getOidcClientConfig();
        Storage storage = StorageFactory.instantiateStorage(request, response, clientConfig.isUseSession());
        return storage.get(OidcStorageUtils.getStateStorageKey(state)) != null;
    }

    private boolean isAlreadyAuthenticated(HttpServletRequest request) {
        return request.getUserPrincipal() != null;
    }

    private AuthenticationStatus processStartFlow(Client client, HttpMessageContext httpMessageContext) {
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();

        request.setAttribute(IS_CONTAINER_INITIATED_FLOW, !httpMessageContext.isAuthenticationRequest());

        AuthenticationStatus status = AuthenticationStatus.SEND_CONTINUE;
        try {
            ProviderAuthenticationResult providerAuthenticationResult = client.startFlow(request, response);
            status = processStartFlowResult(providerAuthenticationResult, httpMessageContext);
        } catch (Exception e) {
            Tr.error(tc, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        return status;
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
    @FFDCIgnore(AuthenticationResponseException.class)
    private AuthenticationStatus processCallback(Client client, HttpMessageContext httpMessageContext) throws AuthenticationException {
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();

        AuthenticationStatus status = AuthenticationStatus.SEND_CONTINUE;
        try {
            ProviderAuthenticationResult providerAuthenticationResult = client.continueFlow(request, response);
            status = processContinueFlowResult(providerAuthenticationResult, httpMessageContext, client);
        } catch (AuthenticationResponseException e) {
            switch (e.getValidationResult()) {
                case NOT_VALIDATED_RESULT:
                    status = AuthenticationStatus.NOT_DONE;
                    break;
                case INVALID_RESULT:
                    Tr.error(tc, e.getMessage());
                    status = AuthenticationStatus.SEND_CONTINUE;
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    break;
            }
        } catch (UnsupportedResponseTypeException | TokenRequestException e) {
            Tr.error(tc, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        if (status == AuthenticationStatus.SUCCESS) {
            OidcClientConfig clientConfig = client.getOidcClientConfig();
            if (clientConfig.isRedirectToOriginalResource()) {
                restoreOriginalResourceRequest(clientConfig, httpMessageContext);
            }
        }
        return status;
    }

    private void restoreOriginalResourceRequest(OidcClientConfig clientConfig, HttpMessageContext httpMessageContext) {
        Optional<HttpServletRequest> originalResourceRequest = getOriginalResourceRequest(clientConfig, httpMessageContext);
        if (originalResourceRequest.isPresent()) {
            httpMessageContext.setRequest(originalResourceRequest.get());
            httpMessageContext.getMessageInfo().setRequestMessage(originalResourceRequest.get());
        }
    }

    private Optional<HttpServletRequest> getOriginalResourceRequest(OidcClientConfig clientConfig, HttpMessageContext httpMessageContext) {
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();

        HttpServletRequest originalResourceRequest = null;
        if (hasPreviouslyStoredOriginalResourceRequest(request, response, clientConfig)) {
            originalResourceRequest = recreateOriginalResourceRequest(request, response, clientConfig.isUseSession());
        }

        return Optional.ofNullable(originalResourceRequest);
    }

    /**
     * Determine if the original request was stored based on if there is a stored method.
     * There will always be a stored method if the original request was stored.
     * If nothing was stored even though isRedirectToOriginalResource was set to true, then the request was likely caller-initiated.
     * Note: Cannot rely on HttpMessageContext#isAuthenticationRequest here, since that info is lost in the callback.
     */
    private boolean hasPreviouslyStoredOriginalResourceRequest(HttpServletRequest request, HttpServletResponse response, OidcClientConfig clientConfig) {
        Storage storage = StorageFactory.instantiateStorage(request, response, clientConfig.isUseSession());
        String state = request.getParameter(OpenIdConstant.STATE);
        String stateHash = io.openliberty.security.oidcclientcore.utils.Utils.getStrHashCode(state);
        String storedMethod = storage.get(OidcClientStorageConstants.WAS_OIDC_REQ_METHOD + stateHash);

        return storedMethod != null && !storedMethod.isEmpty();
    }

    protected OriginalResourceRequest recreateOriginalResourceRequest(HttpServletRequest request, HttpServletResponse response, boolean useSession) {
        return new OriginalResourceRequest(request, response, useSession);
    }

    private AuthenticationStatus processContinueFlowResult(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext,
                                                           Client client) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.SUCCESS.equals(authResult)) {
                status = handleOidcLogin(providerAuthenticationResult, httpMessageContext, client);
            } else if (AuthResult.REDIRECT.equals(authResult)) {
                status = httpMessageContext.redirect(providerAuthenticationResult.getRedirectUrl());
            }
        }

        return status;
    }

    private AuthenticationStatus handleOidcLogin(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext,
                                                 Client client) throws AuthenticationException {
        return createAndValidateOidcTokensCredential(providerAuthenticationResult, client, httpMessageContext);
    }

    AuthenticationStatus createAndValidateOidcTokensCredential(ProviderAuthenticationResult providerAuthenticationResult, Client client,
                                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();
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
                credential.setOpenIdContext(getOpenIdContext());
            }
        }

        return credential;
    }

    private AuthenticationStatus validateCredentials(OidcTokensCredential credential, HttpMessageContext httpMessageContext) throws AuthenticationException {
        int rspStatus;
        // TODO: Pass JavaEESecConstants.DEFAULT_REALM below as the issuer. OidcIdentityStore should return proper issuer in CredentialValidationResult (fix OidcIdentityStore to get issuer from metadata if not available in config for the token refresh case)
        String issuer = getIssuerFromIdentityToken();

        Subject clientSubject = httpMessageContext.getClientSubject();
        AuthenticationStatus status = utils.handleAuthenticate(getCDI(), issuer, credential, clientSubject, httpMessageContext);

        if (status == AuthenticationStatus.SUCCESS) {
            setOpenIdContextInSubject(clientSubject, credential.getOpenIdContext());

            Map<String, Object> messageInfoMap = httpMessageContext.getMessageInfo().getMap();
            messageInfoMap.put(AUTH_TYPE_PROPERTY, AUTH_TYPE_JAKARTA_OIDC);
            messageInfoMap.put("jakarta.servlet.http.registerSession", Boolean.TRUE.toString());

            rspStatus = HttpServletResponse.SC_OK;
        } else if (status == AuthenticationStatus.NOT_DONE) {
            // set SC_OK, since if the target is not protected, it'll be processed.
            rspStatus = HttpServletResponse.SC_OK;
        } else {
            rspStatus = HttpServletResponse.SC_UNAUTHORIZED;
            status = AuthenticationStatus.SEND_CONTINUE;
        }

        httpMessageContext.getResponse().setStatus(rspStatus);

        return status;
    }

    /**
     * Attempt to get the Issuer from the IdentityToken. If it can't be found, the default realm is returned.
     *
     * @return
     */
    private String getIssuerFromIdentityToken() {
        OpenIdContext openIdContext = OpenIdContextUtils.getOpenIdContextFromSubject();
        String issuer = JavaEESecConstants.DEFAULT_REALM;
        if (openIdContext == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The openIdContext is null, can't get the issuer, will be set to the default realm: " + issuer);
            }
        } else {
            IdentityToken idToken = openIdContext.getIdentityToken();
            if (idToken == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The IdentityToken is null, can't get the issuer, will be set to the default realm: " + issuer);
                }
            } else {
                Map<String, Object> claims = idToken.getClaims();
                if (claims == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The claims map on the IdentityToken is null, can't get the issuer, will be set to the default realm: " + issuer);
                    }
                } else {
                    String issuerFromMap = (String) claims.get(OpenIdConstant.ISSUER_IDENTIFIER);
                    if (issuerFromMap == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                     OpenIdConstant.ISSUER_IDENTIFIER + " returned null from the claims map, can't get the issuer, will be set to the default realm: " + issuer);
                        }
                    } else {
                        issuer = issuerFromMap;
                    }
                }
            }
        }
        return issuer;
    }

    @SuppressWarnings("unchecked")
    protected OpenIdContextImpl getOpenIdContext() {
        OpenIdContextImpl openIdContextImpl = null;
        Instance<OpenIdContext> openIdContextInstance = getCDI().select(OpenIdContext.class);

        if (openIdContextInstance != null) {
            OpenIdContext openIdContext = openIdContextInstance.get();

            openIdContextImpl = (OpenIdContextImpl) CDI40Utils.getContextualInstanceFromProxy(openIdContext);
        }
        return openIdContextImpl;
    }

    private void setOpenIdContextInSubject(Subject clientSubject, OpenIdContext openIdContext) {
        if (openIdContext != null) {
            clientSubject.getPrivateCredentials().add(openIdContext);
            Hashtable<String, Object> hashtable = utils.getSubjectExistingHashtable(clientSubject);
            if (hashtable != null) {
                String newCacheKey = getNewCustomCacheKeyValue(openIdContext);
                if (newCacheKey != null) {
                    hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, newCacheKey);
                }
            }
        }
    }

    private String getNewCustomCacheKeyValue(OpenIdContext openIdContext) {
        IdentityToken idToken = openIdContext.getIdentityToken();
        if (idToken != null) {
            return String.valueOf(idToken.hashCode());
        }
        AccessToken accessToken = openIdContext.getAccessToken();
        if (accessToken != null) {
            return String.valueOf(accessToken.hashCode());
        }
        return null;
    }

    private AuthenticationStatus checkForExpiredTokensAndProcessIfNeeded(Client client, HttpServletRequest request, HttpServletResponse response,
                                                                         HttpMessageContext httpMessageContext) throws AuthenticationException {
        ProviderAuthenticationResult providerAuthenticationResult;
        OpenIdContext openIdContext = OpenIdContextUtils.getOpenIdContextFromSubject();

        if (openIdContext == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The openIdContext from OpenIdContextUtils.getOpenIdContextFromSubject is null, the ProviderAuthenticationResult is set to failure");
            }
            providerAuthenticationResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            IdentityToken idToken = openIdContext.getIdentityToken();
            boolean isAccessTokenExpired = openIdContext.getAccessToken().isExpired();
            boolean isIdTokenExpired = false;
            String idTokenString = null;

            if (idToken != null) {
                isIdTokenExpired = idToken.isExpired();
                idTokenString = idToken.getToken();
            }

            request.setAttribute(CHECKING_FOR_EXPIRED_TOKEN, true);

            providerAuthenticationResult = client.checkForExpiredTokensAndProcessIfNeeded(request, response, isAccessTokenExpired, isIdTokenExpired, idTokenString,
                                                                                          getRefreshToken(openIdContext));
            request.removeAttribute(CHECKING_FOR_EXPIRED_TOKEN);
        }

        return processResultFromExpirationCheck(providerAuthenticationResult, client, httpMessageContext);
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

    private AuthenticationStatus processResultFromExpirationCheck(ProviderAuthenticationResult providerAuthenticationResult, Client client,
                                                                  HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        if (providerAuthenticationResult != null) {
            AuthResult authResult = providerAuthenticationResult.getStatus();

            if (AuthResult.REDIRECT_TO_PROVIDER.equals(authResult)) {
                status = httpMessageContext.redirect(providerAuthenticationResult.getRedirectUrl());
            } else if (AuthResult.SUCCESS.equals(authResult)) {
                status = updateOpenIdContextWithRefreshedTokens(providerAuthenticationResult, client, httpMessageContext);
            } else if (AuthResult.CONTINUE.equals(authResult)) {
                status = noProcessingNeededFromExpirationCheck(httpMessageContext);
            }
        }

        return status;
    }

    private AuthenticationStatus updateOpenIdContextWithRefreshedTokens(ProviderAuthenticationResult providerAuthenticationResult, Client client,
                                                                        HttpMessageContext httpMessageContext) throws AuthenticationException {
        return createAndValidateOidcTokensCredential(providerAuthenticationResult, client, httpMessageContext);
    }

    private AuthenticationStatus noProcessingNeededFromExpirationCheck(HttpMessageContext httpMessageContext) {
        httpMessageContext.getResponse().setStatus(HttpServletResponse.SC_OK);
        httpMessageContext.getMessageInfo().getMap().put(AUTH_TYPE_PROPERTY, AUTH_TYPE_JAKARTA_OIDC);

        return AuthenticationStatus.NOT_DONE;
    }

    @Override
    public void cleanSubject(HttpServletRequest req, HttpServletResponse resp, HttpMessageContext httpMessageContext) {
        HttpServletRequest request = httpMessageContext.getRequest();
        HttpServletResponse response = httpMessageContext.getResponse();

        Object checkingForExpiredTokenObject = request.getAttribute(CHECKING_FOR_EXPIRED_TOKEN);
        boolean checkingForExpiredToken = (checkingForExpiredTokenObject == null) ? false : (boolean) checkingForExpiredTokenObject;

        // Skip if invocation is due to a logout during a check for an expired token. LogoutHandler will process the rest of the logout.
        if (!checkingForExpiredToken) {
            String idTokenString = null;
            OpenIdContext openIdContext = OpenIdContextUtils.getOpenIdContextFromSubject();

            if (openIdContext != null) {
                IdentityToken idToken = openIdContext.getIdentityToken();
                if (idToken != null) {
                    idTokenString = idToken.getToken();
                }
            }

            Client client = getClient(request);
            ProviderAuthenticationResult providerAuthenticationResult = client.logout(request, response, idTokenString);
            request.setAttribute(JASPIC_PROVIDER_PERFORMED_REQUEST_LOGOUT, "true");

            processLogoutResult(providerAuthenticationResult, httpMessageContext);
        }
    }

    // Process only redirections. Error messages when failing to re-authenticate are already handled by JakartaOidcAuthorizationRequest.
    private void processLogoutResult(ProviderAuthenticationResult providerAuthenticationResult, HttpMessageContext httpMessageContext) {
        AuthResult authResult = providerAuthenticationResult.getStatus();

        if (AuthResult.REDIRECT_TO_PROVIDER.equals(authResult)) {
            httpMessageContext.redirect(providerAuthenticationResult.getRedirectUrl());
        }
    }

}
