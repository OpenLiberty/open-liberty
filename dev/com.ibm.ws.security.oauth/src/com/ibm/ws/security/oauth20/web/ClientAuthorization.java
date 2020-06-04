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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.OAuthResultImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuth20BadParameterException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientScopeReducer;
import com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class ClientAuthorization {

    private static TraceComponent tc = Tr.register(ClientAuthorization.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    private static final Set<String> requiredAttributes = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] { OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.CLIENT_SECRET,
                    OAuth20Constants.RESPONSE_TYPE,
                    OAuth20Constants.STATE,
                    OAuth20Constants.SCOPE })));

    public OAuthResult validateAuthorization(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response) throws OidcServerException {

        OAuthResult retVal = null;
        AttributeList attrList = new AttributeList();

        OidcBaseClient client = null;
        String grantType = null;

        try {
            // validate username
            validateUsername(request, attrList);

            // Per section 4.1.2.1 of the OAuth 2.0 spec (RFC6749), the state parameter must be included in any error response if it was
            // originally provided in the request. Adding it to the attribute list here will ensure it is propagated to any failure response.
            String[] stateParams = request.getParameterValues(OAuth20Constants.STATE);
            if (stateParams != null) {
                attrList.setAttribute(OAuth20Constants.STATE, OAuth20Constants.ATTRTYPE_PARAM_QUERY, stateParams);
            }

            // validate client id
            client = validateClientId(request, attrList, provider);
            String clientId = client.getClientId();

            // validate redirect uri
            validateRedirectUri(request, attrList, client);

            // validate response type
            String responseType = validateResponseTypeAndReturn(request, provider, clientId);

            // validate grant type
            grantType = getRequestedGrantType(request, responseType);
            validateGrantTypes(provider, request, clientId, responseType);

            // validate response and grant type combination
            if (!(isValidResponseTypeForAuthorizationCodeGrantType(responseType, grantType) || isValidResponseTypeForImplicitGrantType(responseType, grantType))) {
                throw new OAuth20InvalidResponseTypeException("security.oauth20.error.invalid.responsetype", responseType);
            }
            attrList.setAttribute(OAuth20Constants.RESPONSE_TYPE, OAuth20Constants.ATTRTYPE_PARAM_QUERY, new String[] { responseType });

            // validate state
            if (stateParams != null && stateParams.length > 1) {
                throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.STATE);
            }

            // validate scopes
            validateScopes(request, attrList, client, clientId);
        } catch (OAuth20Exception e) {
            retVal = new OAuthResultImpl(OAuthResultImpl.STATUS_FAILED, attrList, e);
        }
        if (retVal == null) {
            retVal = validateMisc(request, client, grantType, attrList);
        }
        return retVal;
    }

    @FFDCIgnore({ OAuth20BadParameterException.class })
    public OAuthResult validateMisc(HttpServletRequest request, OidcBaseClient client,
            String grantType, AttributeList attrList) throws OidcServerException {
        try {
            if (OAuth20Constants.GRANT_TYPE_IMPLICIT.equals(grantType)) {
                // ignore resource parameter if it's not an implicit request.
                // For code type, the resource parameter shows at token_endpoint
                OAuth20ProviderUtils.validateResource(request, attrList, client);
            }

            // add extended properties in the request
            Enumeration<?> e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                if (!requiredAttributes.contains(name)) {
                    attrList.setAttribute(name, OAuth20Constants.ATTRTYPE_REQUEST, request.getParameterValues(name));
                }
            }
        } catch (OAuth20BadParameterException e) {
            WebUtils.throwOidcServerException(request, e);
        }

        return new OAuthResultImpl(OAuthResult.STATUS_OK, attrList);
    }

    /**
     * Sets the username attribute in the given attribute list to the name of the user principal in the request.
     *
     * @param request
     * @param attrList
     */
    private void validateUsername(HttpServletRequest request, AttributeList attrList) {
        Principal principal = request.getUserPrincipal();
        String username = principal == null ? null : principal.getName();
        attrList.setAttribute(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { username });
    }

    /**
     * Validates that the given request contains a single, non-empty client_id parameter, and that the specified provider
     * contains a client with that client_id.
     *
     * @param request
     * @param attrList
     * @param provider
     * @return
     * @throws OAuth20Exception
     */
    private OidcBaseClient validateClientId(HttpServletRequest request, AttributeList attrList, OAuth20Provider provider) throws OAuth20Exception {

        String[] clientIdParams = request.getParameterValues(OAuth20Constants.CLIENT_ID);
        if (clientIdParams == null) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.CLIENT_ID, null);
        }
        attrList.setAttribute(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY, clientIdParams);
        if (clientIdParams.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.CLIENT_ID);
        }
        String clientId = clientIdParams[0];
        if (clientId == null || clientId.length() == 0) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.CLIENT_ID, null);
        }
        OidcBaseClient client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
        if (client == null || !client.isEnabled()) {
            throw new OAuth20InvalidClientException("security.oauth20.error.invalid.client", clientId, false);
        }
        return client;
    }

    /**
     * Validates that only one registered redirect URI is provided and that it is properly formatted. If validation is
     * successful, the registered redirect URI is added to the attribute list.
     *
     * @param attrList
     * @param registeredUris
     * @param client
     * @throws OAuth20Exception
     */
    private void validateRegisteredRedirectUri(AttributeList attrList, JsonArray registeredUris, OidcBaseClient client) throws OAuth20Exception {
        if (registeredUris == null || registeredUris.size() != 1 || client.getAllowRegexpRedirects()) {
            // No, or multiple, registered URIs configured
            // or if regexp matching is in use, then we can't proceed unless we have something to match against.
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.REDIRECT_URI, null);
        } else {
            if (!OidcOAuth20Util.validateRedirectUris(registeredUris, false)) {
                throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.registered.redirecturi",
                        OidcOAuth20Util.getSpaceDelimitedString(registeredUris), null);
            }
            // Use the registered redirect URI
            String[] registeredRedirectUri = { registeredUris.get(0).getAsString() };
            attrList.setAttribute(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY, registeredRedirectUri);
        }
    }

    /**
     * Validates the redirect URI provided in the request (if present) against the registered redirect URIs in the
     * specified client. Checks for duplicate redirect_uri parameters and valid redirect URI formats. If a redirect URI
     * was not provided in the request, this validates that only one valid redirect URI is registered in the client. If
     * a redirect URI was provided in the request, this validates that it is properly formatted and that it matches one
     * of the redirect URIs registered with the client.
     *
     * @param request
     * @param attrList
     * @param client
     * @throws OAuth20Exception
     */
    private void validateRedirectUri(HttpServletRequest request, AttributeList attrList, OidcBaseClient client) throws OAuth20Exception {

        String[] redirectUriParams = request.getParameterValues(OAuth20Constants.REDIRECT_URI);
        if (redirectUriParams != null) {
            attrList.setAttribute(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY, redirectUriParams);
        }
        if (redirectUriParams != null && redirectUriParams.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.REDIRECT_URI);
        }
        String redirectUri = redirectUriParams == null ? null : redirectUriParams[0];
        JsonArray registeredUris = client.getRedirectUris();
        if (redirectUri == null || redirectUri.length() == 0) {
            // redirectUri is not specified; use the registered redirect uri
            validateRegisteredRedirectUri(attrList, registeredUris, client);
        } else {
            // redirect uri must be valid and match the registered value if it is already set
            if (!OAuth20Util.validateRedirectUri(redirectUri)) {
                throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.redirecturi", redirectUri, null);
            }

            if (registeredUris == null || registeredUris.size() == 0
                    || !OidcOAuth20Util.jsonArrayContainsString(registeredUris, redirectUri, client.getAllowRegexpRedirects())) {
                throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.redirecturi.mismatch",
                        redirectUri, OidcOAuth20Util.getSpaceDelimitedString(registeredUris), null);
            }
        }
    }

    /**
     * Sets the scope attribute in the provided attribute list to the intersection of the set of requested scopes and the
     * scopes registered with the provided client. OIDC requests will be checked for the required scope parameter, and
     * the specified client will be checked for at least one registered scope.
     *
     * @param request
     * @param attrList
     * @param client
     * @param clientId
     * @throws OAuth20Exception
     */
    private void validateScopes(HttpServletRequest request, AttributeList attrList, OidcBaseClient client, String clientId) throws OAuth20Exception {
        if (request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME) != null) {
            checkForMissingScopeInTheRequest(request);
            checkForEmptyRegisteredScopeSet(client, clientId);
        }
        // For OIDC requests, since we check the registered scopes in the client configuration above,
        // we do not need to set isEmptyScopeSetAllowed to false on oidc cases here.
        String[] scopes = getReducedScopes(client, request, clientId, true);

        if (scopes != null) {
            attrList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_REQUEST, scopes);
        }
    }

    /**
     * responseType must equal "code" and grantType must equal "authorization_code".
     *
     * @param responseType
     * @param grantType
     * @return
     */
    protected boolean isValidResponseTypeForAuthorizationCodeGrantType(String responseType, String grantType) {
        if (responseType == null || grantType == null) {
            return false;
        }
        return (OAuth20Constants.RESPONSE_TYPE_CODE.equals(responseType) && grantType.equals(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
    }

    /**
     * responseType must contain "token" and/or "id_token" as separate values and grantType must equal "implicit".
     * For example, a valid combination could be responseType="token other" and grantType="implicit", while an
     * invalid combination would be responseType="mytoken" and grantType="implicit".
     *
     * @param responseType
     * @param grantType
     * @return
     */
    protected boolean isValidResponseTypeForImplicitGrantType(String responseType, String grantType) {
        if (responseType == null || grantType == null) {
            return false;
        }
        boolean containsValidRt = false;
        String[] rtParts = responseType.split(" ");
        for (String rt : rtParts) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking individual response type: " + rt);
            }
            if (rt.equals(OAuth20Constants.RESPONSE_TYPE_TOKEN) || rt.equals(OIDCConstants.RESPONSE_TYPE_ID_TOKEN)) {
                containsValidRt = true;
                break;
            }
        }
        return (containsValidRt && grantType.equals(OAuth20Constants.GRANT_TYPE_IMPLICIT));
    }

    /**
     * Verifies that the provided client has at least one scope registered.
     *
     * @param client
     * @param clientId
     * @throws OAuth20InvalidScopeException
     */
    protected void checkForEmptyRegisteredScopeSet(OidcBaseClient client, String clientId) throws OAuth20InvalidScopeException {
        if (client != null) {
            String registeredScopes = client.getScope();
            if (registeredScopes == null || registeredScopes.trim().length() == 0) {
                // no registered scopes
                throw new OAuth20InvalidScopeException("security.oauth20.error.missing.registered.scope", null, clientId);
            }
        }
    }

    /**
     * Ensures that at least one scope parameter is included in the provided request.
     *
     * @param request
     * @throws OAuth20InvalidScopeException
     */
    protected void checkForMissingScopeInTheRequest(HttpServletRequest request) throws OAuth20InvalidScopeException {
        String[] scopeParams = request.getParameterValues(OAuth20Constants.SCOPE);
        if (scopeParams == null) {
            throw new OAuth20InvalidScopeException("security.oauth20.error.missing.scope", "OpenID Connect request");
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "IDC Request scopeParams:" + OAuth20Util.arrayToSpaceString(scopeParams));
        }
    }

    public OAuthResult validateAndHandle2LegsScope(OAuth20Provider provider,
            HttpServletRequest request,
            HttpServletResponse response,
            String clientId) {

        AttributeList attrList = new AttributeList();
        OAuthResult retVal = new OAuthResultImpl(OAuthResult.STATUS_OK, attrList);
        try {
            String[] grantTypeParams = request.getParameterValues(OAuth20Constants.GRANT_TYPE);
            if (!validateGrantTypes(provider, request, clientId)) {
                throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", grantTypeParams[0]);
            }

            String grantType = (grantTypeParams == null) ? null : grantTypeParams[0];
            if (!(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) ||
                    OAuth20Constants.GRANT_TYPE_JWT.equals(grantType) || OAuth20Constants.GRANT_TYPE_PASSWORD.equals(grantType))) {
                // Grant type is either authorization_code, refresh_token, or resource_owner (implicit shouldn't reach this flow)
                // No need to perform pre-authorized scope handling
                return retVal;
            }

            if (request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME) == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "This is an OAuth20 request");
                }
                attrList.setAttribute(OAuth20Constants.REQUEST_FEATURE,
                        OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST,
                        new String[] { OAuth20Constants.REQUEST_FEATURE_OAUTH2 });

                if (OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) ||
                        OAuth20Constants.GRANT_TYPE_PASSWORD.equals(grantType)) {
                    // client_credentials and password go back to the old behavior
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Since this is OAuth20 request, client_credetinals and resource_owner will go back to the old behavior");
                    }

                    // We need to get the scope from request since from now on we will only check
                    // OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST on client_credientials and password
                    String[] scopeParams = request.getParameterValues(OAuth20Constants.SCOPE);
                    if (scopeParams != null && scopeParams.length > 1) {
                        throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.SCOPE);
                    }
                    if (scopeParams != null && scopeParams.length == 1) {
                        String scopeStr = scopeParams[0];
                        String[] scopes = scopeStr.split(" ");
                        attrList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, getUniqueArray(scopes));
                        request.setAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, attrList);
                    }
                    return retVal;
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "This is an OIDC request");
                }
                attrList.setAttribute(OAuth20Constants.REQUEST_FEATURE,
                        OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST,
                        new String[] { OAuth20Constants.REQUEST_FEATURE_OIDC });
                checkForMissingScopeInTheRequest(request);
            }

            // validate scope
            OidcBaseClient client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
            if (client == null) {
                throw new OAuth20InvalidClientException("security.oauth20.error.invalid.client", clientId, false);
            }
            String[] reducedScopes = getReducedScopes(client, request, clientId, false);
            checkForEmptyScopeList(reducedScopes, request, client, clientId);

            if (isClientAutoAuthorized(provider, request, clientId)) {
                attrList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, getUniqueArray(reducedScopes));
                request.setAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, attrList);
            } else {
                // Request and/or client is not auto authorized
                String preAuthzScopes = client.getPreAuthorizedScope();
                if (preAuthzScopes == null || preAuthzScopes.isEmpty()) {
                    Tr.error(tc, "JWT_SERVER_NO_PRE_AUTHORIZED_SCOPE_ERR", clientId);
                    String msg = Tr.formatMessage(tc, "JWT_SERVER_NO_PRE_AUTHORIZED_SCOPE_ERR", clientId);
                    throw new OAuth20Exception(OAuth20Exception.INVALID_SCOPE, msg, null);
                }
                // Make sure all scopes in the reduced list are listed as pre-authorized by the client
                OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(client);
                for (int i = 0; i < reducedScopes.length; i++) {
                    String scope = reducedScopes[i];
                    if (scope != null && scope.length() > 0) {
                        if (!reducer.hasClientPreAuthorizedScope(scope)) {
                            Tr.error(tc, "JWT_SERVER_SCOPE_NOT_PRE_AUTHORIZED_ERR", scope, clientId);
                            String msg = Tr.formatMessage(tc, "JWT_SERVER_SCOPE_NOT_PRE_AUTHORIZED_EXTERNAL_ERR", clientId);
                            throw new OAuth20Exception(OAuth20Exception.INVALID_SCOPE, msg, null);
                        }
                    }
                }
                attrList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, reducedScopes);
                request.setAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, attrList);
            }

        } catch (OAuth20Exception e) {
            retVal = new OAuthResultImpl(OAuthResultImpl.STATUS_FAILED, attrList, e);
        }

        return retVal;
    }

    /**
     * Returns the value of the first scope parameter in the request. Returns null if no scope parameter values were found.
     *
     * @param request
     * @return
     */
    public String getRequestedScopes(HttpServletRequest request) {
        String[] scopeParams = request.getParameterValues(OAuth20Constants.SCOPE);
        if (scopeParams != null) {
            return scopeParams[0];
        }
        return null;
    }

    /**
     * Validates that all of the requested grant types are within the intersection of grant types registered with the
     * specified client and grant types allowed by the specified provider.
     *
     * @param provider
     * @param request
     * @param clientId
     * @return
     * @throws OAuth20Exception
     */
    public boolean validateGrantTypes(OAuth20Provider provider, HttpServletRequest request, String clientId) throws OAuth20Exception {
        return validateGrantTypes(provider, request, clientId, null);
    }

    /**
     * Validates that all of the requested grant types are within the intersection of grant types registered with the
     * specified client and grant types allowed by the specified provider.
     *
     * @param provider
     * @param request
     * @param clientId
     * @param responseType
     * @return
     * @throws OAuth20Exception
     */
    public boolean validateGrantTypes(OAuth20Provider provider, HttpServletRequest request, String clientId, String responseType) throws OAuth20Exception {

        String requestedGrantType = getRequestedGrantType(request, responseType);
        if (requestedGrantType == null) {
            return false;
        }

        boolean isGrantTypeValid = false;
        String[] grantTypes = requestedGrantType.split(" ");
        Set<String> reducedGrantTypes = getReducedGrantTypes(provider, clientId);
        for (int i = 0; i < grantTypes.length; i++) {
            String gtype = grantTypes[i].trim();
            if (gtype != null && gtype.length() > 0) {
                if (!isRequestedGrantTypeRegistered(gtype, reducedGrantTypes)) {
                    throw new OAuth20InvalidGrantTypeException("security.oauth20.error.invalid.granttype", gtype);
                } else {
                    isGrantTypeValid = true;
                }
            }
        }
        return isGrantTypeValid;
    }

    /**
     * If the specified provider contains a client with the given clientId, returns the set of all grant types registered
     * with that client.
     *
     * @param provider
     * @param clientId
     * @return
     * @throws OAuth20Exception
     */
    private Set<String> getRegisteredGrantTypes(OAuth20Provider provider, String clientId) throws OAuth20Exception {
        Set<String> gtSet = new HashSet<String>();
        if (provider != null) {
            OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
            if (clientProvider != null) {
                OidcOAuth20Client client = clientProvider.get(clientId);
                if (client != null) {
                    JsonArray arrayJson = client.getGrantTypes();
                    if (arrayJson != null) {
                        for (int i = 0; i < arrayJson.size(); i++) {
                            gtSet.add(arrayJson.get(i).getAsString());
                        }
                    }
                }
            }
        }
        return gtSet;
    }

    /**
     * Returns the set of all grant types allowed by the specified provider.
     *
     * @param provider
     * @return
     * @throws OAuth20Exception
     */
    private Set<String> getGrantTypesAllowed(OAuth20Provider provider) throws OAuth20Exception {

        Set<String> gtSet = new HashSet<String>();
        if (provider != null) {
            String[] grantTypes = provider.getGrantTypesAllowed();
            if (grantTypes != null) {
                for (int i = 0; i < grantTypes.length; i++) {
                    gtSet.add(grantTypes[i].toString());
                }
            }
        }
        return gtSet;
    }

    private Set<String> getSetIntersection(Set<String> set1, Set<String> set2) throws OAuth20Exception {
        Set<String> intersection = new HashSet<String>(set1);
        intersection.retainAll(set2);
        return intersection;
    }

    /**
     * Returns the intersection of the grant types registered with the specified client and the grant types allowed by
     * the provider.
     *
     * @param provider
     * @param clientId
     * @return
     * @throws OAuth20Exception
     */
    public Set<String> getReducedGrantTypes(OAuth20Provider provider, String clientId) throws OAuth20Exception {

        Set<String> registeredGrantTypes = getRegisteredGrantTypes(provider, clientId);
        Set<String> grantTypesAllowed = getGrantTypesAllowed(provider);

        return getSetIntersection(registeredGrantTypes, grantTypesAllowed);
    }

    private boolean isRequestedGrantTypeRegistered(String requested, Set<String> registered) {
        if (registered == null || registered.isEmpty()) {
            // If no registered grant types, then return true
            return true;
        }
        return registered.contains(requested);
    }

    /**
     * Removes any duplicates from the provided array and returns a new array with only the unique values.
     *
     * @param scopes
     * @return
     */
    public String[] getUniqueArray(String[] scopes) {
        Set<String> scopeSet = new HashSet<String>();
        for (int i = 0; scopes != null && i < scopes.length; i++) {
            String scope = scopes[i];
            scopeSet.add(scope);
        }
        return scopeSet.toArray(new String[scopeSet.size()]);
    }

    /**
     * Returns the intersection of the set of requested scopes and the scopes registered with the provided client.
     *
     * @param client
     * @param request
     * @param clientId
     * @param isEmptyScopeSetAllowed
     * @return
     * @throws OAuth20Exception
     */
    public String[] getReducedScopes(OidcOAuth20Client client, HttpServletRequest request, String clientId, boolean isEmptyScopeSetAllowed) throws OAuth20Exception {
        String[] scopeParams = request.getParameterValues(OAuth20Constants.SCOPE);
        // TODO: Check the spec to see if scope must be unique
        if (scopeParams != null && scopeParams.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.SCOPE);
        }
        if (scopeParams == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Scope parameter was not found");
            }
            return null;
        }

        OidcBaseClientScopeReducer reducer = null;

        if (client != null) {
            reducer = new OidcBaseClientScopeReducer((OidcBaseClient) client);
        } else {
            throw new OAuth20InvalidClientException("security.oauth20.error.invalid.client", clientId, false);
        }

        String scopeStr = scopeParams[0];
        String[] scopes = scopeStr.split(" ");

        Set<String> scopeSet = new HashSet<String>();
        for (int i = 0; i < scopes.length; i++) {
            String scope = scopes[i].trim();
            if (scope != null && scope.length() > 0) {
                // Perform special validation of characters in a scope
                if (OAuth20Util.validateScopeString(scope)) {
                    if (isScopeRegistered(reducer, scope, isEmptyScopeSetAllowed)) {
                        scopeSet.add(scope);
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "The requested scope [" + scope + "] is not registered and is therefore ignored.");
                        }
                    }
                } else {
                    throw new OAuth20BadParameterFormatException("security.oauth20.error.parameter.format", OAuth20Constants.SCOPE, scope);
                }
            }
        }
        return scopeSet.toArray(new String[0]);
    }

    public String getRegisteredScopes(OidcOAuth20Client client) {
        if (client != null) {
            return client.getScope();
        }
        return null;
    }

    /**
     * Determine if two URLs match, taking implicit ports into account. java.net.URL can do this but it also resolves
     * hostnames, which is low-performance and can cause problems for virtual hosts.
     *
     * @param a
     * @param b
     * @return
     */
    protected boolean urlsMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        boolean result = false;
        if (a.startsWith("https:") && a.contains(":443/")) {
            a = a.replace(":443/", "/");
        }
        if (b.startsWith("https:") && b.contains(":443/")) {
            b = b.replace(":443/", "/");
        }
        if (a.startsWith("http:") && a.contains(":80/")) {
            a = a.replace(":80/", "/");
        }
        if (b.startsWith("http:") && b.contains(":80/")) {
            b = b.replace(":80/", "/");
        }

        result = a.equals(b);

        return result;
    }

    /**
     * Determines if the provider allows for auto authorization, if the request is requesting auto authorization,
     * and if the client_id included in the request is listed as an auto authorized client by the provider.
     *
     * @param provider
     * @param request
     * @return
     */
    protected boolean isClientAutoAuthorized(OAuth20Provider provider, HttpServletRequest request) {
        String clientId = request.getParameter(OAuth20Constants.CLIENT_ID);
        return isClientAutoAuthorized(provider, request, clientId);
    }

    /**
     * Determines if the provider allows for auto authorization, if the request is requesting auto authorization,
     * and if the specified client is listed as an auto authorized client by the provider.
     *
     * @param provider
     * @param request
     * @param clientId
     * @return
     */
    boolean isClientAutoAuthorized(OAuth20Provider provider, HttpServletRequest request, String clientId) {
        /*
         * Check:
         * - autoauthorization is enabled
         * - autoauthorization param is set to true
         * - client is whitelisted
         */
        String autoAuthzName = provider.getAutoAuthorizeParam();
        if (autoAuthzName == null || autoAuthzName.isEmpty()) {
            return false;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Auto authorize param configured, checking if set to true");
        }
        String autoAuthzValue = request.getParameter(autoAuthzName);
        boolean autoAuthorize = provider.isAutoAuthorize();

        if (!autoAuthorize && !"true".equalsIgnoreCase(autoAuthzValue)) {
            return false;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Auto authorize param is true, loading whitelisted clients");
        }

        String[] allowedClients = provider.getAutoAuthorizeClients();

        if (allowedClients == null || allowedClients.length < 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authauthz param enabled but no whitelisted clients, strange to see an autoauthz request.");
            }
            return false;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking if client [" + clientId + "] is whitelisted");
        }
        // Check if the client is whitelisted
        for (String client : allowedClients) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking against whitelisted client: " + client);
            }
            if (clientId != null && clientId.equals(client)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the specified scope is registered.
     *
     * @param reducer
     * @param scope
     * @param isEmptyScopeSetAllowed If true and the reducer contains an empty registered scope set, the scope is considered registered.
     *  If false and the reducer contains an empty registered scope set, the scope is NOT considered registered.
     * @return
     * @throws OAuth20Exception
     */
    protected boolean isScopeRegistered(OidcBaseClientScopeReducer reducer, String scope, boolean isEmptyScopeSetAllowed) throws OAuth20Exception {
        boolean registered = false;
        if (reducer.isNullEmptyScope()) {
            registered = isEmptyScopeSetAllowed ? true : false;
        } else if (reducer.hasClientScope(scope)) {
            registered = true;
        }
        return registered;
    }

    /**
     * Returns whether all of the provided scopes are listed as pre-authorized scopes by the given client.
     *
     * @param provider
     * @param clientId
     * @param scopes
     * @return False if no scopes are provided, if there are no pre-authorized scopes listed by the client, or if any of
     *  the provided scopes do not appear in the pre-authorized scopes list. Returns true if no client provider could be
     *  found or if all of the scopes provided are contained in the pre-authorized scopes list.
     * @throws OAuth20Exception
     */
    public boolean isPreAuthorizedScope(OAuth20Provider provider, String clientId, String[] scopes) throws OAuth20Exception {
        if (scopes == null || scopes.length == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null or no scopes provided");
            }
            return false;
        }

        if (provider == null) {
            return false;
        }
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        if (clientProvider == null) {
            return false;
        }

        OidcOAuth20Client client = clientProvider.get(clientId);
        if (client == null) {
            throw new OAuth20InvalidClientException("security.oauth20.error.invalid.client", clientId, false);
        }

        String preAuthzScopes = client.getPreAuthorizedScope();
        if (preAuthzScopes == null || preAuthzScopes.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No pre-authorized scopes found in the client configuration");
            }
            return false;
        }
        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer((OidcBaseClient) client);
        for (int i = 0; i < scopes.length; i++) {
            String scope = scopes[i].trim();
            if (scope != null && scope.length() > 0) {
                if (!reducer.hasClientPreAuthorizedScope(scope)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Scope [" + scope + "] was not a client pre-authorized scope");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param provider
     * @param clientId
     * @return
     * @throws OAuth20Exception
     */
    public Set<String> getRegisteredClientResponseTypes(OAuth20Provider provider, String clientId) throws OAuth20Exception {

        Set<String> gtSet = new HashSet<String>();
        if (provider != null) {
            OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
            if (clientProvider != null) {
                OidcOAuth20Client client = clientProvider.get(clientId);
                if (client != null) {
                    JsonArray arrayJson = client.getResponseTypes();
                    if (arrayJson != null) {
                        for (int i = 0; i < arrayJson.size(); i++) {
                            gtSet.add(arrayJson.get(i).getAsString());
                        }
                    }
                }
            }
        }
        return gtSet;
    }

    /**
     * Validates that the request contains a single, non-empty response_type parameter. The parameter value must be
     * non-empty and must be one of the response types registered with the specified client. The specified client must
     * have at least one registered response type.
     *
     * @param request
     * @param provider
     * @param clientId
     * @return The responseType that this request will use
     * @throws OAuth20Exception
     */
    public String validateResponseTypeAndReturn(HttpServletRequest request, OAuth20Provider provider, String clientId) throws OAuth20Exception {

        String[] responseTypeParams = request.getParameterValues(OAuth20Constants.RESPONSE_TYPE);
        if (responseTypeParams == null) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESPONSE_TYPE, null);
        }
        // only one response_type parameter allowed in the request
        if (responseTypeParams.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.RESPONSE_TYPE);
        }
        String responseType = responseTypeParams[0];
        if (responseType == null || responseType.length() == 0) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OAuth20Constants.RESPONSE_TYPE, null);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Got response type from request: [" + responseType + "]");
        }

        Set<String> clientResponseTypes = getRegisteredClientResponseTypes(provider, clientId);

        String[] requestedResponseTypes = responseType.split(" ");
        for (String reqResponseType : requestedResponseTypes) {
            // Ensure that each requested response type is supported by the server configuration
            Boolean found = false;
            for (String clientResponseType : clientResponseTypes) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Checking requested response type [" + reqResponseType + "] against registered response type: [" + clientResponseType + "]");
                }
                String[] clientRtParts = clientResponseType.split(" ");
                for (String clientRt : clientRtParts) {
                    if (clientRt.equals(reqResponseType)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                throw new OAuth20InvalidResponseTypeException("security.oauth20.error.invalid.responsetype", responseType);
            }
        }

        return responseType;
    }

    /**
     * Gets the grant type from the request. If no grant type was provided in the request, the specified response type is
     * used to determine the grant type.
     *
     * @param request
     * @param responseType
     * @return
     * @throws OAuth20Exception
     */
    public String getRequestedGrantType(HttpServletRequest request, String responseType) throws OAuth20Exception {

        String[] grantTypeParams = request.getParameterValues(OAuth20Constants.GRANT_TYPE);
        if (grantTypeParams == null) {
            if (responseType == null) {
                return "unknown";
            }
            if (responseType.equals(OAuth20Constants.RESPONSE_TYPE_CODE)) {
                return OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;
            }
            if (isValidResponseTypeForImplicitGrantType(responseType, OAuth20Constants.GRANT_TYPE_IMPLICIT)) {
                return OAuth20Constants.GRANT_TYPE_IMPLICIT;
            }
            return "unknown";
        }
        if (grantTypeParams.length > 1) {
            Tr.error(tc, "JWT_SERVER_DUPLICATED_PARAMETERS_ERR", OAuth20Constants.GRANT_TYPE);
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", OAuth20Constants.GRANT_TYPE);
        }

        return grantTypeParams[0];
    }

    /**
     * @param scopes
     * @param oauthResult
     * @param request
     * @param provider
     * @param clientId
     * @return
     */
    public OAuthResult checkForEmptyScopeSetAfterConsent(String[] scopes, OAuthResult oauthResult, HttpServletRequest request, OAuth20Provider provider, String clientId) {
        try {
            OidcBaseClient client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
            checkForEmptyScopeList(scopes, request, client, clientId);
        } catch (OAuth20Exception oe) {
            if (oauthResult != null) {
                return new OAuthResultImpl(OAuthResult.STATUS_FAILED, oauthResult.getAttributeList(), oe);
            } else {
                return new OAuthResultImpl(OAuthResult.STATUS_FAILED, new AttributeList(), oe);
            }
        }
        return oauthResult;
    }

    /**
     * Throws an exception if the provided scopes array is empty. The exception includes the requested scopes and the
     * registered scopes of the specified client.
     *
     * @param scopes
     * @param request
     * @param provider
     * @param clientId
     * @throws OAuth20Exception
     */
    private void checkForEmptyScopeList(String[] scopes, HttpServletRequest request, OidcBaseClient client, String clientId) throws OAuth20Exception {
        if (scopes == null || scopes.length == 0) {
            String requestedScopeStr = getRequestedScopes(request);
            String[] requestedScopes = null;
            if (requestedScopeStr != null) {
                requestedScopes = requestedScopeStr.split(" ");
            }
            String registeredScopeStr = getRegisteredScopes(client);
            String[] registeredScopes = null;
            if (registeredScopeStr != null) {
                registeredScopes = registeredScopeStr.split(" ");
            }
            throw new OAuth20InvalidScopeException("security.oauth20.error.empty.scope", requestedScopes, registeredScopes, clientId);
        }
    }

    /**
     * Returns the set of common scopes between the requested scopes and the scopes registered with the client within
     * the specified provider.
     *
     * @param provider
     * @param request
     * @param clientId
     * @param isEmptyScopeSetAllowed
     * @return
     * @throws OAuth20Exception
     */
    public String[] getReducedScopes(OAuth20Provider provider, HttpServletRequest request, String clientId, boolean isEmptyScopeSetAllowed) throws OAuth20Exception {
        OidcBaseClient client = OAuth20ProviderUtils.getOidcOAuth20Client(provider, clientId);
        return getReducedScopes(client, request, clientId, isEmptyScopeSetAllowed);
    }

}