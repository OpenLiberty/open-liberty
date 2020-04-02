/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.RateLimiter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

public class ClientAuthentication {
    private static TraceComponent tc = Tr.register(ClientAuthentication.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    private static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";
    private static final String PROVIDER_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";

    private static final ArrayList<EndpointType> endpointTypeForInvalidClientList = new ArrayList<EndpointType>(10);
    static {
        // authorize, token, introspect, revoke,
        // discovery, userinfo, registration, check_session_iframe,
        // end_session, coverage_map, proxy
        endpointTypeForInvalidClientList.add(EndpointType.authorize);
        endpointTypeForInvalidClientList.add(EndpointType.token);
        endpointTypeForInvalidClientList.add(EndpointType.introspect);
        endpointTypeForInvalidClientList.add(EndpointType.revoke);
        endpointTypeForInvalidClientList.add(EndpointType.app_password);
        endpointTypeForInvalidClientList.add(EndpointType.app_token);
    }

    @SuppressWarnings("serial")
    private class ClientAuthenticationDataException extends Exception {
    }

    @FFDCIgnore(ClientAuthenticationDataException.class)
    public boolean verify(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response, EndpointType endpointType) throws IOException, ServletException, OidcServerException {
        boolean verified = false;

        // Extract username/password from Authorization header if using basic auth, otherwise obtain client_id and client_secret parameters
        ClientAuthnData data = null;
        try {
            data = new ClientAuthnData(request, response);
        } catch (OAuth20DuplicateParameterException e) {
            handleDuplicateParameterException(e, response);
            return false;
        }

        String grantType = null;
        try {
            grantType = checkForRepeatedOrEmptyParameter(request, OAuth20Constants.GRANT_TYPE);
        } catch (OAuth20DuplicateParameterException e) {
            handleDuplicateParameterException(e, response);
            return false;
        }

        String authScheme = getAuthenticationScheme(request);

        try {
            verified = isClientAuthenticationDataValid(provider, request, response, endpointType, data, grantType, authScheme);
        } catch (ClientAuthenticationDataException e) {
            return false;
        }

        if (verified) {
            request.setAttribute("authenticatedClient", data.getUserName());
        } else {
            sendErrorAndLogMessageForInvalidClient(request, response, endpointType, data, authScheme);
            return false;
        }

        // Verify the resource owner credentials for grant type of "password"
        if (OAuth20Constants.GRANT_TYPE_PASSWORD.equals(grantType) && !provider.isSkipUserValidation()) {
            verified = isResourceOwnerCredentialValid(provider, request, response, endpointType, data, authScheme);
        }

        return verified;
    }

    private boolean isClientAuthenticationDataValid(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response, EndpointType endpointType, ClientAuthnData data, String grantType, String authScheme) throws OidcServerException, ClientAuthenticationDataException {
        boolean isClientAuthenticationDataValid = false;
        if (data.hasAuthnData()) {
            isClientAuthenticationDataValid = isProvidedClientAuthenticationDataValid(provider, response, endpointType, data, grantType, authScheme);
        } else {
            // User name was null or empty
            if (!data.isBasicAuth()) {
                // Not using basic auth, which means the client_id parameter must have been missing
                String uri = request.getRequestURI();
                sendErrorAndLogMessage(response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        Constants.ERROR_CODE_INVALID_REQUEST,
                        null,
                        MESSAGE_BUNDLE,
                        "OAUTH_INVALID_CLIENT",
                        "CWWKS1406E: The " + endpointType.toString() + " request had an invalid client credential. The request URI was {" + uri + "}.",
                        new Object[] { endpointType.toString(), uri },
                        "security.oauth20.error.missing.parameter",
                        new Object[] { OAuth20Constants.CLIENT_ID });
                throw new ClientAuthenticationDataException();
            }
        }
        return isClientAuthenticationDataValid;
    }

    private boolean isProvidedClientAuthenticationDataValid(OAuth20Provider provider, HttpServletResponse response, EndpointType endpointType, ClientAuthnData data, String grantType, String authScheme) throws ClientAuthenticationDataException, OidcServerException {
        boolean isClientAuthenticationDataValid;
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        if (clientProvider == null) {
            int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            if (authScheme != null) {
                // Per the section 5.2 of the OAuth 2.0 spec, if returning invalid_client and the client attempted to authenticate via the Authorization
                // request header, the response must be a 401 and include the "WWW-Authenticate" header matching the authentication scheme used by the client.
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            }
            sendErrorAndLogMessage(response,
                    statusCode,
                    Constants.ERROR_CODE_INVALID_CLIENT,
                    authScheme,
                    PROVIDER_BUNDLE,
                    "security.oauth20.error.missing.client.provider",
                    "CWOAU0070E: A client provider was not found for the OAuth provider.",
                    new Object[] {}, null, null);
            throw new ClientAuthenticationDataException();
        }

        String password = data.getPassWord();
        if (password == null && !data.isBasicAuth()) {
            // Per the section 2.3.1 of the OAuth 2.0 spec, when accepting client credentials in the request-body, the "client MAY omit the
            // [client_secret] parameter if the client secret is an empty string."
            // For the basic auth case, a null password means a password was not included, not that the password was empty.
            password = "";
        }
        // adding check to see whether the client config specifies that it is a public client
        boolean clientSpecifiesPublic = isPublicClient(clientProvider, data);

        if (provider.isAllowPublicClients() || clientSpecifiesPublic) {
            isClientAuthenticationDataValid = isValidPublicClient(response, password, clientProvider, data, endpointType, grantType, authScheme);
        } else {
            // Only allow confidential clients; client credentials must be validated
            isClientAuthenticationDataValid = clientProvider.validateClient(data.getUserName(), password);
        }

        // Make sure this client is actually enabled
        if (isClientAuthenticationDataValid) {
            isClientAuthenticationDataValid = clientProvider.get(data.getUserName()).isEnabled();
            if (!isClientAuthenticationDataValid && tc.isDebugEnabled()) {
                Tr.debug(tc, "Client " + data.getUserName() + " is not enabled so cannot be verified");
            }
        } else {
            RateLimiter.limit();
        }
        return isClientAuthenticationDataValid;
    }

    /**
     * @param clientProvider
     * @param data
     * @return
     */
    private boolean isPublicClient(OidcOAuth20ClientProvider clientProvider, ClientAuthnData data) {
        OidcBaseClient client = null;
        try {
            client = clientProvider.get(data.getUserName());
        } catch (OidcServerException e) {

        }
        if (client != null && client.isPublicClient()) {
            return true;
        }
        return false;
    }

    private boolean isValidPublicClient(HttpServletResponse response, String password, OidcOAuth20ClientProvider clientProvider, ClientAuthnData data, EndpointType endpointType, String grantType, String authScheme) throws OidcServerException, ClientAuthenticationDataException {
        boolean isClientAuthenticationDataValid;
        // Validate client credentials if present, otherwise only check for existence of client
        if (password != null && password.length() > 0) {
            isClientAuthenticationDataValid = clientProvider.validateClient(data.getUserName(), password);
        } else {
            // Requests sent without a password/client_secret and clients with empty passwords will be treated as public clients
            if (grantTypeRequiresConfidentialClient(grantType)) {
                int errorCode = HttpServletResponse.SC_BAD_REQUEST;
                if (authScheme != null) {
                    // Per the section 5.2 of the OAuth 2.0 spec, if returning invalid_client and the client attempted to authenticate via the Authorization
                    // request header, the response must be a 401 and include the "WWW-Authenticate" header matching the authentication scheme used by the client.
                    errorCode = HttpServletResponse.SC_UNAUTHORIZED;
                }
                // Public client used but grantType requires confidential client
                sendErrorAndLogMessage(response,
                        errorCode,
                        Constants.ERROR_CODE_INVALID_CLIENT,
                        authScheme,
                        PROVIDER_BUNDLE,
                        "security.oauth20.error.granttype.requires.confidential.client",
                        "CWOAU0071E: A public client attempted to access the " + endpointType.toString() + " endpoint using the " + grantType + " grant type. The client_id is: " + data.getUserName(),
                        new Object[] { endpointType.toString(), grantType, data.getUserName() }, null, null);
                throw new ClientAuthenticationDataException();
            }
            // Grant type does not require confidential client, only check for existence of public client
            isClientAuthenticationDataValid = clientProvider.exists(data.getUserName());
        }
        return isClientAuthenticationDataValid;
    }

    private void sendErrorAndLogMessageForInvalidClient(HttpServletRequest request, HttpServletResponse response, EndpointType endpointType, ClientAuthnData data, String authScheme) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ClientAuthentication with invalid_client. endpointType: " + endpointType);
        }
        if (endpointTypeForInvalidClientList.contains(endpointType)) {

            int errorCode = HttpServletResponse.SC_BAD_REQUEST;
            if (authScheme != null) {
                // Per the section 5.2 of the OAuth 2.0 spec, if returning invalid_client and the client attempted to authenticate via the Authorization
                // request header, the response must be a 401 and include the "WWW-Authenticate" header matching the authentication scheme used by the client.
                errorCode = HttpServletResponse.SC_UNAUTHORIZED;
            }

            String uri = request.getRequestURI();

            sendErrorAndLogMessage(response,
                    errorCode,
                    Constants.ERROR_CODE_INVALID_CLIENT,
                    authScheme,
                    MESSAGE_BUNDLE,
                    "OAUTH_INVALID_CLIENT",
                    "CWWKS1406E: The " + endpointType.toString() + " request had an invalid client credential. The request URI was {" + uri + "}.",
                    new Object[] { endpointType.toString(), uri },
                    "security.oauth20.endpoint.client.auth.error",
                    new Object[] { data.getUserName() });
        } else {
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED,
                    Constants.ERROR_CODE_INVALID_CLIENT, null, authScheme);
            Tr.error(tc, "security.oauth20.endpoint.client.auth.error", new Object[] { data.getUserName() });
        }
    }

    private boolean isResourceOwnerCredentialValid(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response, EndpointType endpointType, ClientAuthnData data, String authScheme) throws OidcServerException {
        boolean isCredentialValid = false;
        appPasswordMisConfigurationCheck(provider, data);
        try {
            if (provider.isPasswordGrantRequiresAppPassword()) {
                isCredentialValid = validateResourceOwnerCredentialWithAppPassword(request, response, endpointType, provider, data);
            } else {
                isCredentialValid = validateResourceOwnerCredential(provider, request, response, endpointType);
            }

        } catch (OAuth20DuplicateParameterException e) {
            handleDuplicateParameterException(e, response);
            return false;
        } catch (OAuth20MissingParameterException e) {
            handleMissingParameterException(e, response);
            return false;
        }
        if (isCredentialValid) {
            String user = EndpointUtils.getParameter(request, OAuth20Constants.RESOURCE_OWNER_USERNAME);
            String client = data.getUserName();
            if (EndpointUtils.reachedTokenLimit(provider, request, user, client)) {
                isCredentialValid = false;
                createTokenLimitReachedError(request, response, client, endpointType, authScheme);
            }
        } else {
            sendErrorForInvalidResourceOwnerCredentials(request, response, endpointType, authScheme, provider.isPasswordGrantRequiresAppPassword());
        }
        return isCredentialValid;
    }

    static boolean appPasswordMisConfigEvaluated = false;

    /** if client and provider are configured in an incompatible way regarding app passwords, emit a single warning message.
     *  We'll only check this once no matter how many clients and providers there are, but that will hopefully
     *  help if anyone makes this configuration mistake, which can be difficult to diagnose.
     * @param provider
     * @param data
     */
    void appPasswordMisConfigurationCheck(OAuth20Provider provider, ClientAuthnData data) {
        if (appPasswordMisConfigEvaluated == true) {
            return;
        }
        OidcBaseClient client = null;
        boolean providerWantsAppPassword = provider.isPasswordGrantRequiresAppPassword();
        boolean clientSupportsAppPassword = false;
        String clientId = data.getUserName(); // might be client id, or real user name
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        if (clientProvider != null & clientId != null) {
            try {
                client = clientProvider.get(clientId);
            } catch (OidcServerException e) {
            } // ffdc

        }
        if (client != null && client.isAppPasswordAllowed()) {
            clientSupportsAppPassword = true;
        }
        if (clientSupportsAppPassword && !providerWantsAppPassword) {
            Tr.warning(tc, "security.oauth20.apppassword.config.c1p0.warning", new Object[] { client.getClientId(), provider.getID() });
        }
        if (!clientSupportsAppPassword && providerWantsAppPassword) {
            Tr.warning(tc, "security.oauth20.apppassword.config.c0p1.warning", new Object[] { client.getClientId(), provider.getID() });
        }
        appPasswordMisConfigEvaluated = true;
    }

    /**
     * @param request
     * @param response
     * @param client
     * @param endpointType
     * @param authScheme
     * @return
     */
    private void createTokenLimitReachedError(HttpServletRequest request, HttpServletResponse response, String client, EndpointType endpointType, String authScheme) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ClientAuthentication with too many token requests. endpointType: " + endpointType);
        }
        if (endpointTypeForInvalidClientList.contains(endpointType)) {
            int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            WebUtils.sendErrorJSON(response, statusCode, Constants.ERROR_CODE_INVALID_REQUEST, null);
        }
    }

    private void sendErrorForInvalidResourceOwnerCredentials(HttpServletRequest request, HttpServletResponse response, EndpointType endpointType, String authScheme, boolean isAppPasswordCheck) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ClientAuthentication with invalid_resource_owner_credentials. endpointType: " + endpointType);
        }
        if (endpointTypeForInvalidClientList.contains(endpointType)) {
            // Don't have to check for duplicate parameter since that was already done when validating the credentials
            String userName = request.getParameter(OAuth20Constants.RESOURCE_OWNER_USERNAME);

            int errorCode = HttpServletResponse.SC_BAD_REQUEST;
            if (authScheme != null) {
                // Per the section 5.2 of the OAuth 2.0 spec, if returning invalid_client and the client attempted to authenticate via the Authorization
                // request header, the response must be a 401 and include the "WWW-Authenticate" header matching the authentication scheme used by the client.
                errorCode = HttpServletResponse.SC_UNAUTHORIZED;
            }

            String message = "security.oauth20.endpoint.resowner.auth.error";
            String defaultMsg = "CWOAU0069E: The resource owner could not be verified. Either the resource owner: " + userName + " or password is incorrect.";
            if (isAppPasswordCheck) {
                message = "security.oauth20.endpoint.resowner.apppassword.error";
                defaultMsg = "CWOAU0074E: The application password exchange request for user [{0}] could not be completed because the application password could not be verified. The password is either incorrect, expired, deleted, or is supplied with the wrong client credentials.";
            }
            sendErrorAndLogMessage(response,
                    errorCode,
                    Constants.ERROR_CODE_INVALID_CLIENT,
                    authScheme,
                    PROVIDER_BUNDLE,
                    message,
                    defaultMsg,
                    new Object[] { userName }, null, null);
        } else {
            WebUtils.sendErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED,
                    Constants.ERROR_CODE_INVALID_CLIENT, null, authScheme);
        }
    }

    protected boolean grantTypeRequiresConfidentialClient(String grantType) {
        /*
         * Grant type: client_credentials
         * Reasoning: Per section 4.4 of the OAuth 2.0 spec (RFC 6749), "The client credentials grant type MUST only be used by confidential clients."
         *
         * Grant type: jwt-bearer
         * Reasoning: Per section 2.1 of the OAuth JWT Assertion Profiles spec (RFC 7523), authentication of the client is optional. According to
         * section 3.1 of the same spec, "Whether or not client authentication is needed in conjunction with a JWT authorization grant, as
         * well as the supported types of client authentication, are policy decisions at the discretion of the authorization server."
         * The implementers here have made the judgement call of allowing only confidential clients to authenticate.
         */
        return (OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS.equalsIgnoreCase(grantType))
                || (OAuth20Constants.GRANT_TYPE_JWT.equalsIgnoreCase(grantType));
    }

    /**
     * Takes the resource owner credentials specified in the request and validates them against the user registry.
     *
     * @param request
     * @param response
     * @param type
     * @return
     * @throws OidcServerException
     * @throws OAuth20DuplicateParameterException
     * @throws OAuth20MissingParameterException
     */
    protected boolean validateResourceOwnerCredential(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response, EndpointType type) throws OidcServerException, OAuth20DuplicateParameterException, OAuth20MissingParameterException {
        boolean valid = false;
        String userName = "";

        try {
            UserRegistry reg = getUserRegistry();
            userName = checkForRepeatedOrEmptyParameter(request, OAuth20Constants.RESOURCE_OWNER_USERNAME);
            if (userName == null) {
                // Per section 4.3.2 of the OAuth 2.0 spec (RFC 6749), the "username" parameter is required
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESOURCE_OWNER_USERNAME, null);
            }
            String passWord = checkForRepeatedOrEmptyParameter(request, OAuth20Constants.RESOURCE_OWNER_PASSWORD);
            if (passWord == null) {
                // Per section 4.3.2 of the OAuth 2.0 spec (RFC 6749), the "password" parameter is required
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESOURCE_OWNER_PASSWORD, null);
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "validateResourceOwnerCredential for Username " + userName);
            }

            if (reg.checkPassword(userName, passWord) != null) {
                valid = true;
            }

            // switch the username if needed. Oauth20ComponentImpl.buildTokenAttributeList will pick this up.
            if (valid && provider.isROPCPreferUserSecurityName()) {
                String userSecName = reg.getUserSecurityName(userName);
                if (!userSecName.equals(userName)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "setting attribute to override user name to " + userSecName);
                    }
                    request.setAttribute(OAuth20Constants.RESOURCE_OWNER_OVERRIDDEN_USERNAME, userSecName);
                }
            }

        } catch (OAuth20DuplicateParameterException e) {
            throw e;
        } catch (OAuth20MissingParameterException e) {
            throw e;
        } catch (Exception e1) {
            Tr.error(tc, "security.oauth20.endpoint.resowner.auth.error", userName);
            throw new OidcServerException("invalid_resource_owner_credential", OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_BAD_REQUEST, e1);
        }

        return valid;

    }

    /**
     * Takes the resource owner credentials specified in the request and validates them against the user registry.
     *
     * @param request
     * @param response
     * @param type
     * @param provider
     * @return
     * @throws OidcServerException
     * @throws OAuth20DuplicateParameterException
     * @throws OAuth20MissingParameterException
     */
    protected boolean validateResourceOwnerCredentialWithAppPassword(HttpServletRequest request, HttpServletResponse response, EndpointType type, OAuth20Provider provider, ClientAuthnData clientAuthnData) throws OidcServerException, OAuth20DuplicateParameterException, OAuth20MissingParameterException {
        boolean valid = false;
        String userName = "";

        try {
            userName = checkForRepeatedOrEmptyParameter(request, OAuth20Constants.RESOURCE_OWNER_USERNAME);
            if (userName == null) {
                // Per section 4.3.2 of the OAuth 2.0 spec (RFC 6749), the "username" parameter is required
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESOURCE_OWNER_USERNAME, null);
            }
            String appPassWord = checkForRepeatedOrEmptyParameter(request, OAuth20Constants.RESOURCE_OWNER_PASSWORD);
            if (appPassWord == null) {
                // Per section 4.3.2 of the OAuth 2.0 spec (RFC 6749), the "password" parameter is required
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESOURCE_OWNER_PASSWORD, null);
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "validateResourceOwnerCredential for Username " + userName);
            }

            if (checkAppPassword(userName, appPassWord, provider, clientAuthnData) != null) {
                valid = true;
            }
        } catch (OAuth20DuplicateParameterException e) {
            throw e;
        } catch (OAuth20MissingParameterException e) {
            throw e;
        } catch (Exception e1) {
            Tr.error(tc, "security.oauth20.endpoint.resowner.apppassword.error", userName);
            throw new OidcServerException("invalid_resource_owner_credential", OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_BAD_REQUEST, e1);
        }
        return valid;

    }

    /**
     * @return the token, but only if its type is app_password.
     */
    private Object checkAppPassword(String userName, String appPassword, OAuth20Provider provider, ClientAuthnData clientAuthnData) {
        String lookupKey = appPassword;
        // if (!provider.isLocalStoreUsed()) {
        // lookupKey = EndpointUtils.computeTokenHash(null, appPassword, OAuth20Constants.GRANT_TYPE_APP_PASSWORD);
        // }
        String encode = provider.getAccessTokenEncoding();
        if (OAuth20Constants.PLAIN_ENCODING.equals(encode)) { // must be app-password or app-token
            lookupKey = EndpointUtils.computeTokenHash(lookupKey);
        } else {
            lookupKey = EndpointUtils.computeTokenHash(lookupKey, encode);
        }

        OAuth20Token accessToken = provider.getTokenCache().get(lookupKey); // it's also null when the token expires
        if (accessToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Access token was not found in the provider's token cache");
            }
            return null;
        }
        accessToken = assertTokenIsUsedByAllowedClient(accessToken, clientAuthnData, provider);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "checkAppPassword obtained access token " + accessToken);
        }
        /*
         * if (accessToken != null && accessToken.getGrantType().equals(OAuth20Constants.APP_PASSWORD)) {
         * if (tc.isDebugEnabled()) {
         * Tr.debug(tc, "checkAppPassword has found access token " + accessToken);
         * }
         * return accessToken;
         * }
         */
        if (accessToken == null) {
            return null;
        }
        if (accessToken.getGrantType().equals(OAuth20Constants.APP_PASSWORD)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "checkAppPassword obtained access token for an app password " + accessToken);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "checkAppPassword access token is not for app password, return null");
            }
            return null;
        }
        if (!accessToken.getUsername().equals(userName)) { // username of request had to match username of app-pw.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "UserName from token request: " + userName +
                        " does not match userName of app password: " + accessToken.getUsername() + ", return null");
            }
            Tr.error(tc, "security.oauth20.endpoint.resowner.apppassword.error", new Object[] { userName }); // CWOAU0074E
            return null;
        }

        return accessToken;

    }

    private OAuth20Token assertTokenIsUsedByAllowedClient(OAuth20Token accessToken, ClientAuthnData clientAuthnData, OAuth20Provider provider) {
        String clientIdFromAuthnData = clientAuthnData.getUserName();
        String[] usedBy = accessToken.getUsedBy();
        if (usedBy != null) {
            List<String> usedByList = Arrays.asList(usedBy);
            if (!usedByList.contains(clientIdFromAuthnData)) {
                Tr.error(tc, "security.oauth20.apppassword.exchange.wrongclient", new Object[] { usedBy[0], clientIdFromAuthnData }); // CWOAU0077E
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client ID [" + clientIdFromAuthnData + "] not found in used_by list of allowed clients for this token " + usedByList);
                }
                return null;
            }
        } else {
            // Associate this access token exclusively with this client
            Map<String, String[]> extensionProperties = accessToken.getExtensionProperties();
            if (extensionProperties != null) {
                extensionProperties.put(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_BY, new String[] { clientIdFromAuthnData });
                if (!provider.isLocalStoreUsed()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "persist the token : " + accessToken.getId() + " to database after adding usedBy ext with the client : " + clientIdFromAuthnData);
                    }
                    OAuth20EnhancedTokenCache cache = provider.getTokenCache();
                    cache.removeByHash(accessToken.getId()); // some db's can't handle insert on existing record.
                    cache.addByHash(accessToken.getId(), accessToken, accessToken.getLifetimeSeconds());
                }
            }
        }
        return accessToken;
    }

    protected UserRegistry getUserRegistry() throws WSSecurityException {
        return com.ibm.wsspi.security.registry.RegistryHelper.getUserRegistry(null);
    }

    /**
     * Handles the duplicate parameter exception by sending a 400 response with "invalid_request" as the error code. The
     * exception message is logged by the server and included as the value of the error_description.
     *
     * @param e
     * @param response
     */
    private void handleDuplicateParameterException(OAuth20DuplicateParameterException e, HttpServletResponse response) {
        WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_REQUEST, e.getMessage(), null);
        Tr.error(tc, e.getMessage());
    }

    /**
     * Handles the missing parameter exception by sending a 400 response with "invalid_request" as the error code. The
     * exception message is logged by the server and included as the value of the error_description.
     *
     * @param e
     * @param response
     */
    private void handleMissingParameterException(OAuth20MissingParameterException e, HttpServletResponse response) {
        WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST, Constants.ERROR_CODE_INVALID_REQUEST, e.getMessage(), null);
        Tr.error(tc, e.getMessage());
    }

    /**
     * Uses the WebUtils.sendErrorJSON() method to send a JSON response back and logs the specified error message.
     * If logMsgKey is not null, the message with that key and the provided logMsgArgs will be logged instead of the
     * message mapped to webMsgKey.
     *
     * @param response
     * @param statusCode
     * @param errorCode OAuth 2.0 spec-defined "error" response parameter value
     * @param authScheme Authentication scheme included in the Authorization header of the request
     * @param webMsgBundleName Trace bundle name
     * @param webMsgKey
     * @param webDefaultMsg
     * @param webMsgArgs
     * @param logMsgKey
     * @param logMsgArgs
     */
    private void sendErrorAndLogMessage(HttpServletResponse response, int statusCode, String errorCode, String authScheme,
            String webMsgBundleName, String webMsgKey, String webDefaultMsg, Object[] webMsgArgs,
            String logMsgKey, Object[] logMsgArgs) {

        String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                webMsgBundleName,
                webMsgKey,
                webMsgArgs,
                webDefaultMsg);

        WebUtils.sendErrorJSON(response, statusCode, errorCode, errorMsg, authScheme);

        if (logMsgKey != null) {
            // Different message should be logged than the message sent in the web response
            Tr.error(tc, logMsgKey, logMsgArgs);
        } else {
            // Log the same message as the web response
            Tr.error(tc, webMsgKey, webMsgArgs);
        }
    }

    /**
     * Extracts the authentication scheme from the Authentication header of the request, if such a header exists.
     *
     * @param request
     * @return
     */
    private String getAuthenticationScheme(HttpServletRequest request) {
        String authzHeader = request.getHeader(ClientAuthnData.Authorization_Header);
        if (authzHeader != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Got Authorization header: " + authzHeader);
            }
            String[] headerSplit = authzHeader.split(" ");
            if (headerSplit.length > 0) {
                String authScheme = headerSplit[0].trim();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got authentication scheme: " + authScheme);
                }
                return authScheme;
            }
        }
        return null;
    }

    /**
     * Throws an exception if multiple values exist for the given parameter name. Otherwise returns the value of the
     * parameter (null if the parameter doesn't exist or has an empty value). These cases are covered by sections 3.1
     * and 3.2 of the OAuth 2.0 spec (RFC6749).
     *
     * Sections 3.1 and 3.2 [RFC6749]:
     * "Parameters sent without a value MUST be treated as if they were omitted from the request... Request and response
     * parameters MUST NOT be included more than once."
     *
     * @param request
     * @param parameter
     * @return
     * @throws OAuth20DuplicateParameterException
     */
    @Sensitive
    private String checkForRepeatedOrEmptyParameter(HttpServletRequest request, String parameter) throws OAuth20DuplicateParameterException {
        String[] paramArray = request.getParameterValues(parameter);
        if (paramArray != null && paramArray.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", parameter);
        }
        // TODO Parameter can sometimes be a client secret or password, so currently treat everything as sensitive
        if (paramArray == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No values found for parameter: " + parameter);
            }
            return null;
        }
        String paramValue = paramArray[0];
        if (paramValue.isEmpty()) {
            return null;
        }
        return paramValue;
    }

}
