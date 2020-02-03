/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.OAuthConstants;
import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientSerializer;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientValidator;
import com.ibm.ws.security.oauth20.util.Base64;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

/**
 *
 */
public class RegistrationEndpointServices extends AbstractOidcEndpointServices {
    private enum ClientSecretAction {
        NEW, RETAIN, CLEAR
    }

    private static final int DEFAULT_CLIENT_SECRET_LENGTH = 60; // Specified length of generated client secrets
    public static final String ROLE_REQUIRED = "clientManager"; // Role required to access services
    public static final String UNAUTHORIZED_HEADER_VALUE = "Basic realm=\"" + ROLE_REQUIRED + "\""; // Basic Auth challenge header value
    protected static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";
    private static TraceComponent tc = Tr.register(RegistrationEndpointServices.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private Enumeration<Locale> locales = null;
    // Regular expression to detect incoming client registration request pattern
    private static final String REGEX_REGISTRATION_CLIENTID = "^" + OAuth20RequestFilter.REGEX_COMPONENT_ID + OAuth20RequestFilter.REGEX_REGISTRATION + "$";

    // Configured GSON (De)Serializer for OidcBaseClient objects (Thread Safe according to documentation)
    public static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(OidcBaseClient.class, new OidcBaseClientSerializer())
            .create();

    protected RegistrationEndpointServices() {
    }

    /**
     * Main method that routes all the different incoming client registration requests.
     *
     * According to specification, Administrator-Managed OAuth 2.0 Client Registration must be secured.
     * In this implementation, the security validation is managed in the parent services that is delegating
     * to this method.
     * https://w3-connections.ibm.com/wikis/home?lang=en-us#!/wiki/W90ca708d8d15_46d1_b0b9_31a4b4c82d4f/page/Administrator-Managed%20OAuth%202.0%20Client%20Registration%20Protocol
     *
     * @param provider
     * @param request
     * @param response
     * @throws OidcServerException
     * @throws IOException
     */
    protected void handleEndpointRequest(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws OidcServerException, IOException {
        locales = request.getLocales();
        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET) || request.getMethod().equalsIgnoreCase(HTTP_METHOD_HEAD)) {
            processHeadOrGet(provider, request, response);
        } else if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_POST)) {
            processPost(provider, request, response);
        } else if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_PUT)) {
            processPut(provider, request, response);
        } else if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_DELETE)) {
            processDelete(provider, request, response);
        } else {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_UNSUPPORTED_METHOD", new Object[] { request.getMethod(), "Registration Endpoint Service" });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void processHeadOrGet(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws OidcServerException, IOException {
        // Validates that a JSON response is acceptable
        validateJsonAcceptable(request);

        // Obtain Client Provider
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();

        // If clientId is specified at end of /registration/* return that registered clientId's values
        String clientId = extractClientId(request.getPathInfo());
        if (!OidcOAuth20Util.isNullEmpty(clientId)) {
            processHeadOrGetSingleClient(clientId, clientProvider, request, response);
        } else {
            processHeadOrGetAllClients(clientProvider, request, response);
        }
    }

    private void processHeadOrGetSingleClient(String clientId, OidcOAuth20ClientProvider clientProvider, HttpServletRequest request, HttpServletResponse response)
            throws IOException, OidcServerException {
        OidcBaseClient client = clientProvider.get(clientId);
        if (client == null) {
            // CWWKS1424E
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_CLIENTID_NOT_FOUND", new Object[] { clientId });
            Tr.event(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT, HttpServletResponse.SC_NOT_FOUND);

        }

        // Remove empty initialized JSON Arrays
        omitEmptyArrays(client);

        // Calculate eTag
        String eTag = computeETag(client);

        // Calculate and add Registration URI
        processClientRegistationUri(client, request);

        // Set Headers
        setCommonResponseHeaders(eTag, response, true /**Set application/json CT **/
        );

        // Check for conditional method execution
        OidcServerException preconditionException = checkConditionalExecution(request, true, true, eTag, null);
        if (preconditionException != null) {
            response.setStatus(preconditionException.getHttpStatus());
            response.flushBuffer();
            return;
        }

        // Set response body for GET requests
        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET)) {
            decodeClientName(client);
            byte[] b = GSON.toJson(client).toString().getBytes("UTF-8");
            response.getOutputStream().write(b);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
        return;
    }

    private void processHeadOrGetAllClients(OidcOAuth20ClientProvider clientProvider, HttpServletRequest request, HttpServletResponse response)
            throws IOException, OidcServerException {
        // If no clientId is specified, return collection of registered clients
        JsonArray clientsAsJSON = GSON.toJsonTree(clientProvider.getAll(request)).getAsJsonArray();

        // Remove empty initialized JSON Arrays
        omitEmptyArrays(clientsAsJSON);

        // If decode client names are done here, the client names going back to the response will still be the
        // encoded names. Decoding has to be done in OidcBaseClientValidator first before it converts to a Json array.
        // decodeClientNames(clientsAsJSON);

        // Calculate eTag
        String eTag = computeETag(clientsAsJSON);

        // Set Headers
        setCommonResponseHeaders(eTag, response, true /**Set application/json CT **/
        );

        // Check for conditional method execution
        OidcServerException preconditionException = checkConditionalExecution(request, true, true, eTag, null);
        if (preconditionException != null) {
            response.setStatus(preconditionException.getHttpStatus());
            response.flushBuffer();
            return;
        }

        // Has to move the setStatus call before the write to avoid the following warning
        // SRVE8115W: WARNING: Cannot set status. Response already committed.
        response.setStatus(HttpServletResponse.SC_OK);
        // Set response body for GET request
        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET)) {
            JsonObject responseBody = new JsonObject();

            responseBody.add("data", GSON.toJsonTree(clientsAsJSON));
            // Write out in UTF-8 bytes format
            response.getOutputStream().write(responseBody.toString().getBytes("UTF-8"));
        }

        response.flushBuffer();
    }

    private void processPost(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws OidcServerException, IOException {
        // Validates that a JSON response is acceptable
        validateJsonAcceptable(request);

        // Verify valid request Content-Type
        validateContentType(request, CT_APPLICATION_JSON);

        // POST is only allowed at the direct servlet path /registration
        String clientId = extractClientId(request.getPathInfo());
        if (!OidcOAuth20Util.isNullEmpty(clientId)) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_INVALID_REQUEST_PATH");
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }

        // Parse and obtain client
        OidcBaseClient newClient = getOidcBaseClientFromRequestBody(request);

        if (newClient == null) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_REGISTRATION_REQUEST_MISSING_CLIENT");
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate client
        OidcBaseClient validatedClient = OidcBaseClientValidator.getInstance(newClient).validateCreateUpdate();

        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();

        // Check if clientId is specified
        // If so verify no collision with existing
        // If not, generate and set one
        processClientId(validatedClient, clientProvider);

        // If no client_name specified, copy raw from client_id
        processClientName(validatedClient);

        // Generate a client_secret if conditions are met
        // Must be handled after OidcBaseClientValidator.validate()
        // which sets default conditions.
        processNewClientSecret(validatedClient);

        // Calculate and set registration_client_uri
        processClientRegistationUri(validatedClient, request);

        OidcBaseClient savedClientWithDefaults = OidcBaseClientValidator.getInstance(clientProvider.put(validatedClient)).setDefaultsForOmitted();
        // return decoded client name for the response
        decodeClientName(savedClientWithDefaults);

        // Remove empty initialized JSON Arrays
        omitEmptyArrays(savedClientWithDefaults);

        // Set Headers
        setCommonResponseHeaders(computeETag(savedClientWithDefaults), response, true /**Set application/json CT **/
        );

        // Write out in UTF-8 bytes format
        byte[] b = OidcOAuth20Util.GSON_RAW.toJson(savedClientWithDefaults).toString().getBytes("UTF-8");
        response.getOutputStream().write(b);
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.flushBuffer();
    }

    private void processPut(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws OidcServerException, IOException {
        // Validates that a JSON response is acceptable
        validateJsonAcceptable(request);

        // Verify valid request Content-Type
        validateContentType(request, CT_APPLICATION_JSON);

        // PUT is only allowed when a clientId as part of the request URI to the registration servlet
        String clientId = validateRequestContainsClientId(request);

        // Initialize DB Connection
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();

        // Validate that the clientId exists in the database.
        // OidcServerException thrown if user does not exist
        OidcOAuth20Client existingClient = validateClientIdExists(clientId, clientProvider);

        // Parse and obtain client
        OidcBaseClient newClient = getOidcBaseClientFromRequestBody(request);

        // Override client_id parameter obtained from body, with the one from the request URI
        newClient.setClientId(clientId);

        // Validate client and set defaults if necessary
        OidcBaseClient validatedClient = OidcBaseClientValidator.getInstance(newClient).validateCreateUpdate();

        // Copy applicable output params from existing client back to new client
        // client_id_issued_at && client_secret_expires_at
        copyExistingOutputParams(validatedClient, existingClient);

        // If no client_name specified, copy raw from client_id
        processClientName(validatedClient);

        ClientSecretAction clientSecretAction = processUpdateClientSecret(validatedClient, existingClient);

        // If password is newly generated, save to tmpNewSecret, for use in response.
        String tmpNewSecret = null;
        if (clientSecretAction == ClientSecretAction.NEW) {
            tmpNewSecret = validatedClient.getClientSecret();

            // Hash client secret to be stored in DB
            validatedClient.setClientSecret(PasswordUtil.passwordEncode(tmpNewSecret));
        }

        // Calculate and set registration_client_uri
        processClientRegistationUri(validatedClient, request);

        response.setHeader(CT, CT_APPLICATION_JSON_AND_UTF8);

        // Remove empty initialized JSON Arrays
        omitEmptyArrays((OidcBaseClient) existingClient);

        // Update the client, assuming etag matches
        String currEtag = computeETag((OidcBaseClient) existingClient);

        // Check for conditional method execution
        OidcServerException preconditionException = checkConditionalExecution(request, false, true, currEtag, null);

        // No issues were found
        if (preconditionException == null) {
            OidcBaseClient savedClientWithDefaults = OidcBaseClientValidator.getInstance(clientProvider.update(validatedClient)).setDefaultsForOmitted();

            // Remove empty initialized JSON Arrays
            omitEmptyArrays(savedClientWithDefaults);

            // Calculate eTag and add headers
            String eTag = computeETag(savedClientWithDefaults);
            response.addHeader(HDR_ETAG, String.format("\"%s\"", eTag)); //$NON-NLS-1$

            // return decoded client name for the response
            decodeClientName(savedClientWithDefaults);

            if (clientSecretAction == ClientSecretAction.RETAIN && !OidcOAuth20Util.isNullEmpty(savedClientWithDefaults.getClientSecret())) {
                // Mask client_secret with (*) because no new secret was generated
                byte[] b = GSON.toJson(savedClientWithDefaults).toString().getBytes("UTF-8");
                response.getOutputStream().write(b);
            } else {
                // If 'NEW' client secret was generated or 'CLEAR', send unhashed secret back
                savedClientWithDefaults.setClientSecret(tmpNewSecret);

                byte[] b = OidcOAuth20Util.GSON_RAW.toJson(savedClientWithDefaults).toString().getBytes("UTF-8");
                response.getOutputStream().write(b);
            }

            // Return response
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            if (preconditionException.isComplete()) {
                throw preconditionException;
            }

            response.setStatus(preconditionException.getHttpStatus());
        }

        response.flushBuffer();
    }

    private void processDelete(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws IOException, OidcServerException {
        // If no clientId is specified at end of /registration/* return error 400 response
        String clientId = extractClientId(request.getPathInfo());
        if (OidcOAuth20Util.isNullEmpty(clientId)) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_MISSING_CLIENTID", new Object[] { request.getMethod(), OAuth20Constants.CLIENT_ID });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }

        // Initialize DB Connection
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        OidcBaseClient currClient = clientProvider.get(clientId);

        // If clientId does not exist in database, return error 404 response
        if (currClient == null) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_INVALID_CLIENTID", new Object[] { request.getMethod(), OAuth20Constants.CLIENT_ID, clientId });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT, HttpServletResponse.SC_NOT_FOUND);

        }

        // Remove empty initialized JSON Arrays
        omitEmptyArrays(currClient);

        // Check eTag before deleting
        String eTag = computeETag(currClient);
        OidcServerException preconditionException = checkConditionalExecution(request, false, true, eTag, null);
        if (preconditionException != null) {
            response.setStatus(preconditionException.getHttpStatus());
            response.flushBuffer();
            return;
        }

        clientProvider.delete(clientId);

        // TODO: Revoke access token for clientId

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.flushBuffer();
    }

    private OidcBaseClient getOidcBaseClientFromRequestBody(HttpServletRequest request) throws IOException, OidcServerException {
        try {
            return GSON.fromJson(request.getReader(), OidcBaseClient.class);
        } catch (JsonParseException e) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_MALFORMED_REQUEST");
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);

        }
    }

    private String extractClientId(String pathInfo) {
        Pattern PATH_RE = Pattern.compile(REGEX_REGISTRATION_CLIENTID);
        Matcher m = PATH_RE.matcher(pathInfo);
        if (m.matches()) {
            String clientId = m.group(2);

            if (clientId != null) {
                return trimSlashes(clientId);
            }
        }

        return null;
    }

    private void processClientId(OidcBaseClient client, OidcOAuth20ClientProvider clientProvider) throws OidcServerException {
        String clientId = client.getClientId();
        if (!OidcOAuth20Util.isNullEmpty(clientId)) {
            // Throw error if client_id already exists
            if (clientProvider.exists(clientId)) {
                BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_CLIENTID_EXISTS", new Object[] { clientId });
                Tr.error(tc, errorMsg.getServerErrorMessage());
                throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            // Generate and set a unique client_id
            String generatedClientId = generateUUID();
            while (clientProvider.exists(generatedClientId)) {
                generatedClientId = generateUUID();
            }

            client.setClientId(generatedClientId);
        }

        // Set client_id_issued_at whether client_id is specified or not
        client.setClientIdIssuedAt(System.currentTimeMillis() / 1000);
    }

    /**
     * This method is called from post and put methods. The client name is in decoded format. Need to
     * encode it before saving it to the backend storage.
     * @param client
     */
    private void processClientName(OidcBaseClient client) {
        String clientId = client.getClientId();
        if (OidcOAuth20Util.isNullEmpty(client.getClientName()) && !OidcOAuth20Util.isNullEmpty(clientId)) {
            client.setClientName(clientId);
        } else {
            // encode the client name
            encodeClientName(client);
        }
    }

    private void processNewClientSecret(OidcBaseClient client) throws OidcServerException {
        // If token ep auth method is set to basic, with no secret specified, generate one
        if ((OidcOAuth20Util.isNullEmpty(client.getTokenEndpointAuthMethod())
                || client.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC)
                || client.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_POST))
                && OidcOAuth20Util.isNullEmpty(client.getClientSecret())) {
            client.setClientSecret(OAuthUtil.getRandom(DEFAULT_CLIENT_SECRET_LENGTH));
            return;
        }

        // If token ep auth method is set to none, make sure no secret is specified
        if (!OidcOAuth20Util.isNullEmpty(client.getTokenEndpointAuthMethod())
                && client.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_NONE)
                && !OidcOAuth20Util.isNullEmpty(client.getClientSecret())) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_PUBLIC_CLIENT_CREATE_FAILURE");
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);

        }
    }

    private ClientSecretAction processUpdateClientSecret(OidcBaseClient incomingClient, OidcOAuth20Client existingClient) throws OidcServerException {
        ClientSecretAction action = null;

        if (OidcOAuth20Util.isNullEmpty(incomingClient.getClientSecret())) { // If incoming client_secret IS empty
            action = setSecretAndGetActionForEmptyUpdatedClientSecret(incomingClient, existingClient);
        } else { // If incoming client_secret IS NOT empty
            action = setSecretAndGetActionForNonEmptyUpdatedClientSecret(incomingClient, existingClient);
        }

        if (action == null) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_INVALID_CONFIG");
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        }

        return action;
    }

    private ClientSecretAction setSecretAndGetActionForEmptyUpdatedClientSecret(OidcBaseClient incomingClient, OidcOAuth20Client existingClient) {
        ClientSecretAction action = null;
        if (OidcOAuth20Util.isNullEmpty(incomingClient.getTokenEndpointAuthMethod())
                || incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC)
                || incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_POST)) {
            // Generating new secret
            action = ClientSecretAction.NEW;
            incomingClient.setClientSecret(OAuthUtil.getRandom(DEFAULT_CLIENT_SECRET_LENGTH));
        } else if (incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_NONE)) {
            if (!OidcOAuth20Util.isNullEmpty(existingClient.getClientSecret())) {
                action = ClientSecretAction.CLEAR;
                // Incoming client secret is already empty, no need to clear it again
            } else {
                action = ClientSecretAction.RETAIN;
                // Incoming client secret is empty, client is public, existing client secret is empty
            }
        }
        return action;
    }

    private ClientSecretAction setSecretAndGetActionForNonEmptyUpdatedClientSecret(OidcBaseClient incomingClient, OidcOAuth20Client existingClient) throws OidcServerException {
        ClientSecretAction action = null;
        if (OidcOAuth20Util.isNullEmpty(incomingClient.getTokenEndpointAuthMethod())
                || incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC)
                || incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_POST)) {
            if (incomingClient.getClientSecret().equals("*")) {
                if (OidcOAuth20Util.isNullEmpty(existingClient.getClientSecret())) {
                    BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_CLIENT_SECRET_UPDATE_FAILURE");
                    Tr.error(tc, errorMsg.getServerErrorMessage());
                    throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);

                } else {
                    // Retaining previously set secret
                    action = ClientSecretAction.RETAIN;
                    incomingClient.setClientSecret(existingClient.getClientSecret());
                }
            } else {
                action = ClientSecretAction.NEW;
                // Incoming client secret contains new value to be saved
            }
        } else if (incomingClient.getTokenEndpointAuthMethod().equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_NONE)) {
            action = getActionForNonEmptyClientSecretForTokenEndptAuthMethodNone(incomingClient, existingClient);
        }
        return action;
    }

    private ClientSecretAction getActionForNonEmptyClientSecretForTokenEndptAuthMethodNone(OidcBaseClient incomingClient, OidcOAuth20Client existingClient) throws OidcServerException {
        ClientSecretAction action = null;
        // OAuth clients whose token endpoint authorization method is "none" are assumed to be public clients and must NOT
        // have a client secret configured. If the client has a non-empty secret assigned, that's an error.
        if (incomingClient.getClientSecret().equals("*")) {
            if (!OidcOAuth20Util.isNullEmpty(existingClient.getClientSecret())) {
                throwErrorForInvalidPublicClientUpdate();
            } else {
                // Retaining previously set secret
                action = ClientSecretAction.RETAIN;
                incomingClient.setClientSecret(existingClient.getClientSecret());
            }
        } else {
            throwErrorForInvalidPublicClientUpdate();
        }
        return action;
    }

    private void throwErrorForInvalidPublicClientUpdate() throws OidcServerException {
        BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_PUBLIC_CLIENT_UPDATE_FAILURE");
        Tr.error(tc, errorMsg.getServerErrorMessage());
        throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
    }

    public static void processClientRegistationUri(OidcOAuth20Client client, HttpServletRequest request) {
        String registrationClientUri = computeRegistrationUri(request, client.getClientId());
        client.setRegistrationClientUri(registrationClientUri);
    }

    private static String computeRegistrationUri(HttpServletRequest request, String registeredId) {
        String registrationUri = trimTrailingSlash(request.getRequestURL().toString());
        if (!registrationUri.endsWith(registeredId)) {
            registrationUri = registrationUri + FORWARD_SLASH + registeredId;
        }

        return registrationUri;
    }

    private OidcOAuth20Client validateClientIdExists(String clientId, OidcOAuth20ClientProvider clientProvider) throws OidcServerException {
        OidcOAuth20Client client = clientProvider.get(clientId);
        if (client == null) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_CLIENTID_NOT_FOUND", new Object[] { clientId });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_CLIENT, HttpServletResponse.SC_NOT_FOUND);
        }

        return client;
    }

    private String validateRequestContainsClientId(HttpServletRequest request) throws OidcServerException {
        String clientId = extractClientId(request.getPathInfo());
        if (OidcOAuth20Util.isNullEmpty(clientId)) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_MISSING_CLIENTID", new Object[] { request.getMethod(), OAuth20Constants.CLIENT_ID });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);

        }

        return clientId;
    }

    private void copyExistingOutputParams(OidcBaseClient newClient, OidcOAuth20Client existingClient) {
        newClient.setClientIdIssuedAt(existingClient.getClientIdIssuedAt());
        newClient.setClientSecretExpiresAt(existingClient.getClientSecretExpiresAt());
    }

    /**
     * Computes the etag for specified client.
     *
     * @param client The client to compute an etag from. Must not be <code>null</code>.
     *
     * @return etag The computed etag. Never <code>null</code>.
     * @throws IOException Thrown on metadata read errors.
     */
    private String computeETag(final OidcBaseClient client) throws IOException {
        JsonArray clientArray = new JsonArray();
        clientArray.add(OidcOAuth20Util.getJsonObj(client));
        String eTag = computeETag(clientArray);
        return eTag;
    }

    /**
     * Compute an ETag from a list of consumer objects represented as JSON objects.
     *
     * @param results
     *            the list of JSON consumer objects
     * @return the ETag for the list of objects
     * @throws IOException
     */
    private String computeETag(JsonArray results) throws IOException {
        // Compute an MD5 hash of the JSON serialization of the results.
        // In order to come up with a consistent value, the result list must be processed
        // in the same order each time, so order by consumer name, which is required and
        // which must be unique.
        Comparator<JsonObject> comparator = new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject object1, JsonObject object2) {
                String name1 = object1.get(OIDCConstants.CLIENT_ID).getAsString();
                String name2 = object2.get(OIDCConstants.CLIENT_ID).getAsString();
                return name1.compareTo(name2);
            }
        };

        List<JsonObject> list = OidcOAuth20Util.getListOfJsonObjects(results);
        Collections.sort(list, comparator);
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
        } catch (NoSuchAlgorithmException e) {
            // should never happen since all Java implementations must support MD5
            throw new RuntimeException(e);
        }

        for (JsonObject object : list) {
            // Remove registrationUri and clientSecret from eTag calculation
            String tmpRegistrationUri = null;
            JsonElement registrationClientUriEle = object.get(OIDCConstants.OIDC_CLIENTREG_REGISTRATION_CLIENT_URI);
            if (registrationClientUriEle != null && !registrationClientUriEle.getAsString().isEmpty()) {
                tmpRegistrationUri = registrationClientUriEle.getAsString();
                object.remove(OIDCConstants.OIDC_CLIENTREG_REGISTRATION_CLIENT_URI);
            }

            String tmpClientSecret = null;
            JsonElement clientSecretEle = object.get("client_secret");
            if (clientSecretEle != null && !clientSecretEle.getAsString().isEmpty()) {
                tmpClientSecret = clientSecretEle.getAsString();
                object.remove("client_secret");
            }

            // Update the digest with this client
            StringWriter writer = new StringWriter();
            (new Gson()).toJson(object, writer);
            digest.update(Base64Coder.getBytes(writer.toString()));

            // Add back registrationUri and clientSecret
            if (tmpRegistrationUri != null) {
                object.add(OIDCConstants.OIDC_CLIENTREG_REGISTRATION_CLIENT_URI, new JsonPrimitive(tmpRegistrationUri));
            }

            if (tmpClientSecret != null) {
                object.add("client_secret", new JsonPrimitive(tmpClientSecret));
            }
        }

        byte[] digestBytes = digest.digest();
        return Base64.encode(digestBytes);
    }

    private void setCommonResponseHeaders(String eTag, HttpServletResponse response, boolean setContentType) {
        response.setHeader(OAuthConstants.HEADER_CACHE_CONTROL, HDR_VALUE_PRIVATE);

        if (!OidcOAuth20Util.isNullEmpty(eTag)) {
            response.addHeader(HDR_ETAG, String.format("\"%s\"", eTag)); //$NON-NLS-1$
        }

        if (setContentType) {
            response.setHeader(CT, CT_APPLICATION_JSON_AND_UTF8);
        }
    }

    public static void omitEmptyArrays(JsonArray clients) {
        if (clients == null || clients.size() == 0) {
            return;
        }

        List<JsonObject> clientObjs = OidcOAuth20Util.getListOfJsonObjects(clients);

        if (clientObjs == null || clientObjs.size() == 0) {
            return;
        }

        for (JsonObject clientObj : clientObjs) {
            OidcBaseClient client = GSON.fromJson(clientObj, OidcBaseClient.class);

            if (OidcOAuth20Util.isNullEmpty(client.getRedirectUris())) {
                clientObj.remove(OIDCConstants.OIDC_CLIENTREG_REDIRECT_URIS);
            }

            if (OidcOAuth20Util.isNullEmpty(client.getPostLogoutRedirectUris())) {
                clientObj.remove(OIDCConstants.OIDC_CLIENTREG_POST_LOGOUT_URIS);
            }

            if (OidcOAuth20Util.isNullEmpty(client.getTrustedUriPrefixes())) {
                clientObj.remove(OIDCConstants.JSA_CLIENTREG_TRUSTED_URI_PREFIXES);
            }

            if (OidcOAuth20Util.isNullEmpty(client.getFunctionalUserGroupIds())) {
                clientObj.remove(OIDCConstants.JSA_CLIENTREG_FUNCTIONAL_USER_GROUP_IDS);
            }
        }
    }

    /**
     * Method to remove initialize empty arrays from JSON
     * by setting them to null.
     *
     * This method is used before returning the response JSON
     *
     * @param client
     */
    public static void omitEmptyArrays(OidcBaseClient client) {
        if (client == null) {
            return;
        }

        if (OidcOAuth20Util.isNullEmpty(client.getRedirectUris())) {
            client.setRedirectUris(null);
        }

        if (OidcOAuth20Util.isNullEmpty(client.getPostLogoutRedirectUris())) {
            client.setPostLogoutRedirectUris(null);
        }

        if (OidcOAuth20Util.isNullEmpty(client.getTrustedUriPrefixes())) {
            client.setTrustedUriPrefixes(null);
        }

        if (OidcOAuth20Util.isNullEmpty(client.getFunctionalUserGroupIds())) {
            client.setFunctionalUserGroupIds(null);
        }
    }

    private static void decodeClientNames(JsonArray clients) {
        if (clients == null || clients.size() == 0) {
            return;
        }

        List<JsonObject> clientObjs = OidcOAuth20Util.getListOfJsonObjects(clients);

        if (clientObjs == null || clientObjs.size() == 0) {
            return;
        }

        for (JsonObject clientObj : clientObjs) {
            OidcBaseClient client = GSON.fromJson(clientObj, OidcBaseClient.class);

            decodeClientName(client);
        }
    }

    private static void encodeClientName(OidcBaseClient client) {
        if (client.getClientName() != null) {
            client.setClientName(WebUtils.encode(client.getClientName(), null, "UTF-8"));
        }
    }

    private static void decodeClientName(OidcBaseClient client) {
        try {
            if (client.getClientName() != null) {
                client.setClientName(URLDecoder.decode(client.getClientName(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            // keep the existing client name
        }
    }
}
