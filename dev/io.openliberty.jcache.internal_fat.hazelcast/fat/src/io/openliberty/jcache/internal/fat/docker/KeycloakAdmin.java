/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat.docker;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * This class is a collection of methods used to access a {@link KeycloakContainer}'s administrative
 * REST API.
 */
@SuppressWarnings("restriction")
public class KeycloakAdmin {

    private static final Class<?> CLASS = KeycloakAdmin.class;

    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_ID = "id";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    public static final String ADMIN_CLIENT_ID = "admin-cli";

    public static final Map<String, String> BEARER_TOKENS_BY_REALM = new HashMap<String, String>();

    private final KeycloakContainer keycloak;

    /**
     * Instantiate a new {@link KeycloakAdmin} instance.
     *
     * @param keycloak The {@link KeycloakContainer} the new instance will connect to.
     */
    public KeycloakAdmin(KeycloakContainer keycloak) {
        this.keycloak = keycloak;
    }

    /**
     * Add a bearer token for the admin user on the default realm to the HTTP request.
     *
     * @param request The request to add the token to.
     * @throws Exception If there was an error adding the bearer token.
     */
    private void addAdminBearerToken(HttpRequestBase request) throws Exception {
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminCliBearerToken()));
    }

    /**
     * Build an HTTP delete request using the admin user.
     *
     * @param endpoint The endpoint to send the request to.
     * @return The HTTP delete request.
     * @throws Exception If there was an error building the request.
     */
    private HttpDelete buildHttpDeleteRequest(String endpoint) throws Exception {
        HttpDelete request = new HttpDelete(endpoint);
        addAdminBearerToken(request);
        return request;
    }

    /**
     * Build an HTTP get request using the admin user.
     *
     * @param endpoint The endpoint to send the request to.
     * @param params   Parameters to add to the request.
     * @return The HTTP get request.
     * @throws Exception If there was an error building the request.
     */
    private HttpGet buildHttpGetRequest(String endpoint, String... params) throws Exception {
        URIBuilder builder = new URIBuilder(endpoint);
        if (params != null && params.length > 0) {
            for (int idx = 0; idx < params.length; idx = idx + 2) {
                builder.addParameter(params[idx], params[idx + 1]);
            }
        }

        HttpGet request = new HttpGet(builder.build());
        addAdminBearerToken(request);

        return request;
    }

    /**
     * Build an HTTP post request using the admin user.
     *
     * @param endpoint    The endpoint to send the request to.
     * @param jsonContent The JSON content to send as part of the post.
     * @return The HTTP post request.
     * @throws Exception If there was an error building the request.
     */
    private HttpPost buildHttpPostRequest(String endpoint, String jsonContent) throws Exception {
        HttpPost request = new HttpPost(endpoint);
        addAdminBearerToken(request);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return request;
    }

    /**
     * Create a user in the specified realm
     *
     * @param realm    The Keycloak realm to create the user in.
     * @param username The username for the user.
     * @param password The password for the user.
     * @throws Exception If there was an error creating the user.
     */
    public void createUser(String realm, String username, String password) throws Exception {
        final String METHOD_NAME = "createUser";

        /*
         * Create JSON for the user.
         */
        JsonObject body = Json.createObjectBuilder()
                        .add("username", username)
                        .add("enabled", "true")
                        .add("email", username + "@liberty.org")
                        .add("credentials",
                             Json.createArrayBuilder()
                                             .add(Json.createObjectBuilder().add("type", "password").add("value", password)))
                        .build();

        /*
         * Create the request.
         */
        HttpPost request = new HttpPost(getUsersRestEndpoint(realm));
        addAdminBearerToken(request);
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        /*
         * Create an HTTP client and execute the request.
         */
        Log.info(CLASS, METHOD_NAME, "Requesting to create user at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create new user.");
                }
            }
        }
    }

    /**
     * Delete a client from the specified realm.
     *
     * @param realm The Keycloak realm to delete the client from.
     * @param id    The id of the client to delete (not the clientId).
     * @return A boolean indicating whether the client was deleted.
     * @throws Exception If there was an error deleting the client.
     */
    public boolean deleteClient(String realm, String id) throws Exception {
        final String METHOD_NAME = "deleteClient";

        /*
         * Create the request.
         */
        HttpDelete request = buildHttpDeleteRequest(getClientsRestEndpoint(realm) + "/" + id);

        /*
         * Create an HTTP client and execute the request.
         */
        Log.info(CLASS, METHOD_NAME, "Requesting to delete client at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
            }
        }
    }

    /**
     * Delete a user from the specified realm.
     *
     * @param realm    The Keycloak realm to delete the user from.
     * @param username The username of the user to delete.
     * @return A boolean indicating whether the user was deleted.
     * @throws Exception If there was an error deleting the user.
     */
    public boolean deleteUser(String realm, String username) throws Exception {
        final String METHOD_NAME = "deleteUser";

        JsonArray users = getUsers(realm, username);
        if (users.size() != 1) {
            throw new Exception("Should have returned 1, and only 1 user. Instead returned: " + users);
        }
        String userId = users.getJsonObject(0).getString(KEY_ID);

        /*
         * Create the request.
         */
        HttpDelete request = buildHttpDeleteRequest(getUsersRestEndpoint(realm) + "/" + userId);

        /*
         * Create an HTTP client and execute the request.
         */
        Log.info(CLASS, METHOD_NAME, "Requesting to delete user at " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return HttpStatus.SC_NO_CONTENT == response.getStatusLine().getStatusCode();
            }
        }
    }

    /**
     * Get a bearer token for the Keycloak admin cli for the admin user on the default realm.
     *
     * @return The bearer token for the admin user.
     * @throws Exception If there was an error getting the bearer token.
     */
    private String getAdminCliBearerToken() throws Exception {
        final String METHOD_NAME = "getAdminCliBearerToken";

        String bearerToken = null;
        /*
         * Build the request.
         */
        HttpPost request = new HttpPost(getBearerTokenEndpoint(KeycloakContainer.DEFAULT_REALM));
        request.setEntity(new StringEntity("username=" + KeycloakContainer.ADMIN_USER + "&password=" + KeycloakContainer.ADMIN_PASS
                                           + "&grant_type=password&client_id=" + ADMIN_CLIENT_ID, ContentType.APPLICATION_FORM_URLENCODED));

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Requesting bearer token from " + request.getURI());
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to retrieve bearer token for " + ADMIN_CLIENT_ID
                                                            + ".");
                }

                /*
                 * Get the access token.
                 */
                bearerToken = KeycloakUtils.getJsonObject(KeycloakUtils.getStringResponse(response)).getString(KEY_ACCESS_TOKEN);
            }
        }

        Log.info(CLASS, METHOD_NAME, "Retrieved bearer token: " + bearerToken);
        return bearerToken;
    }

    /**
     * Get the REST endpoint for the Keycloak server.
     *
     * @return The REST endpoint.
     */
    private String getAdminRESTEndpoint() {
        return keycloak.getRootHttpsEndpoint() + "/admin/realms";
    }

    /**
     * Get the REST endpoint for retrieving a bearer token.
     *
     * @param realm The Keycloak realm to get the endpoint for.
     * @return The REST endpoint for retrieving a bearer token.
     */
    private String getBearerTokenEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Get the registered client from Keycloak.
     *
     * @param realm    The Keycloak realm to get the client from.
     * @param clientId The clientId of the registered client to retrieve.
     * @return The JSON object representing the client.
     * @throws Exception If there was an error retrieving the client.
     */
    public JsonObject getClient(String realm, String clientId) throws Exception {
        final String METHOD_NAME = "getClient";

        /*
         * Create the request.
         */
        HttpGet request = buildHttpGetRequest(getClientsRestEndpoint(realm), KEY_CLIENT_ID, clientId);

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Requesting clients from " + request.getURI());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    return null;
                }

                /*
                 * Return the client as a JsonObject.
                 */
                JsonArray clients = KeycloakUtils.getJsonArray(KeycloakUtils.getStringResponse(response));

                if (clients.size() != 1) {
                    return null;
                }
                return clients.getJsonObject(0);
            }
        }
    }

    /**
     * Get the REST endpoint for client resources.
     *
     * @param realm The Keycloak realm to get the endpoint for.
     * @return The REST endpoint for client resources.
     */
    private String getClientsRestEndpoint(String realm) {
        return getAdminRESTEndpoint() + "/" + realm + "/clients";
    }

    /**
     * Get registered users for the keycloak realm.
     *
     * @param realm    The realm to get the users from.
     * @param username The username or pattern to get the users for.
     * @return The JSON array of users.
     * @throws Exception If there was an error retrieving the users from the Keycloak realm.
     */
    public JsonArray getUsers(String realm, String username) throws Exception {
        final String METHOD_NAME = "getUsers";

        /*
         * Create the request.
         */
        HttpGet request = buildHttpGetRequest(getUsersRestEndpoint(realm), "username", username);

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Requesting users from " + request.getURI());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    return null;
                }
                return KeycloakUtils.getJsonArray(KeycloakUtils.getStringResponse(response));
            }
        }
    }

    /**
     * Get the REST endpoint for user resources.
     *
     * @param realm The Keycloak realm to get the endpoint for.
     * @return The REST endpoint for user resources.
     */
    private String getUsersRestEndpoint(String realm) {
        return getAdminRESTEndpoint() + "/" + realm + "/users";
    }

    /**
     * Register a OAuth2 client to the Keycloak server.
     *
     * @param libertyServer The Liberty server to register as the OAuth client.
     * @param libertySp     The Liberty service provider. This is the ID of the samlWebSso20 element.
     * @param keycloakRealm The Keycloak realm to add the SAML client to.
     * @return The client ID from the new client's Keycloak configuration.
     * @throws Exception if there was an error registering the client.
     */
    public String registerOAuth20Client(LibertyServer libertyServer, String clientId, String realm, String oauth2LoginId) throws Exception {
        final String METHOD_NAME = "registerOAuth20Client";

        /*
         * We will use a wildcard redirect URI to represent the following possibilities:
         * - oidcLogin: https://localhost:9443/ibm/api/social-login/redirect/keycloakLogin
         * - openidConnectClient: https://localhost:9443/oidcclient/redirect/keycloakLogin
         */
        String redirectHttpsUri = "https://" + libertyServer.getHostname() + ":" + libertyServer.getHttpDefaultSecurePort() + "/*";

        /*
         * Create a client representation to create the OAuth20 client.
         */
        JsonArray redirectUris = Json.createArrayBuilder().add(redirectHttpsUri).build();
        JsonObject clientRep = Json.createObjectBuilder()
                        .add("clientId", clientId)
                        .add("protocol", "openid-connect")
                        .add("redirectUris", redirectUris)
                        .add("clientAuthenticatorType", "client-secret")
                        .add("publicClient", false)
                        .build();

        String id = null;
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {

            /*
             * Create an HTTP client.
             */
            HttpPost postRequest = buildHttpPostRequest(getAdminRESTEndpoint() + "/" + realm + "/clients", clientRep.toString());

            /*
             * Send the request to Keycloak to convert the SP metadata to Keycloak configuration.
             */
            String clientJson = null;
            Log.info(CLASS, METHOD_NAME, "Posting client representation to " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create client " + clientId + ".");
                }

                /*
                 * Get the response entity.
                 */
                clientJson = KeycloakUtils.getStringResponse(response);
            }

            id = getClient(realm, clientId).getString(KEY_ID);
        }

        return id;
    }

    /**
     * Get the client secret for the specified client and realm.
     *
     * @param realm    The realm the client is on.
     * @param clientId The clientId for the client to get the secret of (not the 'id').
     * @return The client secret.
     * @throws Exception if there was an unforeseen error getting the client secret.
     */
    public String getClientSecret(String realm, String clientId) throws Exception {
        final String METHOD_NAME = "getClientSecret";

        String id = getClient(realm, clientId).getString(KEY_ID);

        /*
         * Get the client secret for the client.
         */
        String uri = getAdminRESTEndpoint() + "/" + realm + "/clients/" + id + "/client-secret";
        HttpGet getRequest = buildHttpGetRequest(uri);
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Retrieving client secret from " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to retrieve secret for client " + clientId);
                }

                /*
                 * Get the response entity.
                 *
                 * {"type":"secret","value":"abcdefgh..."}
                 */
                return KeycloakUtils.getJsonObject(KeycloakUtils.getStringResponse(response)).getString("value");
            }
        }
    }

    /**
     * Register a SAML client to the Keycloak server. The Liberty server must be running so that the SAML SP metadata file can be retrieved.
     *
     * @param libertyServer The Liberty server to register as the SAML client. This server must be running.
     * @param libertySp     The Liberty service provider. This is the ID of the samlWebSso20 element.
     * @param keycloakRealm The Keycloak realm to add the SAML client to.
     * @return The client ID from the new client's Keycloak configuration.
     * @throws Exception if there was an error registering the client.
     */
    public String registerSamlClient(LibertyServer libertyServer, String libertySp, String keycloakRealm) throws Exception {
        final String METHOD_NAME = "registerSamlClient";
        String spMetadataUrl = "https://" + libertyServer.getHostname() + ":" + libertyServer.getHttpDefaultSecurePort() + "/ibm/saml20/" + libertySp + "/samlmetadata";

        /*
         * Get the SP metadata from the Liberty server.
         */
        HttpGet getRequest = buildHttpGetRequest(spMetadataUrl);
        String spMetadata = null;
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Retrieving SAML SP metadata from " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to retrieve SP metadata from server. Is the server running?");
                }

                /*
                 * Get the response entity.
                 */
                spMetadata = KeycloakUtils.getStringResponse(response);
            }
        }

        /*
         * Create an HTTP client.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {

            /*
             * Create the request to convert the SP metadata.
             */
            HttpPost postRequest = buildHttpPostRequest(getAdminRESTEndpoint() + "/" + keycloakRealm + "/client-description-converter", spMetadata);

            /*
             * Send the request to Keycloak to convert the SP metadata to Keycloak configuration.
             */
            String clientJson = null;
            Log.info(CLASS, METHOD_NAME, "Send metadata descriptor conversion from " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to convert SP metadata.");
                }

                /*
                 * Get the response entity.
                 */
                clientJson = KeycloakUtils.getStringResponse(response);
            }

            /*
             * Create the request to register the client from the converted SP metadata.
             */
            postRequest = buildHttpPostRequest(getClientsRestEndpoint(keycloakRealm), clientJson);
            Log.info(CLASS, METHOD_NAME, "Requesting to register client at " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create new client.");
                }
            }

            /*
             * Return the client ID.
             */
            return KeycloakUtils.getJsonObject(clientJson).getString(KEY_CLIENT_ID);
        }
    }

    /**
     * Get the OAuth2 user authorization endpoint.
     *
     * @param realm The realm to get the endpoint for.
     * @return The string representing the endpoint.
     */
    public String getOAuth20AuthorizationEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/openid-connect/auth";
    }

    /**
     * Get the OAuth2 token endpoint.
     *
     * @param realm The realm to get the endpoint for.
     * @return The string representing the endpoint.
     */
    public String getOAuth20TokenEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /**
     * Get the OAuth2 user API endpoint.
     *
     * @param realm The realm to get the endpoint for.
     * @return The string representing the endpoint.
     */
    public String getOAuth20UserApiEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/openid-connect/userinfo";
    }

    /**
     * Get the OIDC discovery endpoint.
     *
     * @param realm The realm to get the dnedpoint for.
     * @return The string representing the endpoint.
     */
    public String getOidcDiscoveryEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/.well-known/openid-configuration";
    }

    /**
     * Create a new realm in Keycloak.
     *
     * @param realm The realm to create.
     * @throws Exception if there was an unforeseen error.
     */
    public void createRealm(String realm) throws Exception {
        final String METHOD_NAME = "createRealm";

        /*
         * Create JSON for the realm.
         */
        JsonObject body = Json.createObjectBuilder()
                        .add("id", realm)
                        .add("realm", realm)
                        .add("displayName", "Keycloak")
                        .add("displayNameHtml", "<div class=\"kc-logo-text\"><span>Keycloak</span></div>")
                        .add("enabled", "true")
                        .build();

        /*
         * Create the request.
         */
        HttpPost request = new HttpPost(getAdminRESTEndpoint());
        addAdminBearerToken(request);
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        /*
         * Create an HTTP client and execute the request.
         */
        Log.info(CLASS, METHOD_NAME, "Requesting to create realm " + realm + " at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create new realm. ");
                }
            }
        }
    }

    /**
     * Create and exception base on an unexpected HTTP response.
     *
     * @param response The response received.
     * @param msg      A message to prepend to the message.
     * @return The new Exception.
     * @throws Exception The Exception that can be thrown.
     */
    private static Exception createResponseException(CloseableHttpResponse response, String msg) throws Exception {
        String errorMsg = msg + " HTTP status code: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

        String contents = KeycloakUtils.getStringResponse(response);
        if (contents != null) {
            msg += "\n\n" + contents;
        }

        return new Exception(errorMsg);
    }
}
