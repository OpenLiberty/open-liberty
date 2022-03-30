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
     * Add a bearer token for the admin user to the HTTP request.
     *
     * @param realm   The Keycloak realm to get the bearer token from.
     * @param request The request to add the token to.
     * @throws Exception If there was an error adding the bearer token.
     */
    private void addAdminBearerToken(String realm, HttpRequestBase request) throws Exception {
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminCliBearerToken(realm)));
    }

    /**
     * Build an HTTP delete request using the admin user.
     *
     * @param endpoint The endpoint to send the request to.
     * @param realm    The Keycloak realm to get the admin user's bearer token from.
     * @return The HTTP delete request.
     * @throws Exception If there was an error building the request.
     */
    private HttpDelete buildHttpDeleteRequest(String endpoint, String realm) throws Exception {
        HttpDelete request = new HttpDelete(endpoint);
        addAdminBearerToken(realm, request);
        return request;
    }

    /**
     * Build an HTTP get request using the admin user.
     *
     * @param endpoint The endpoint to send the request to.
     * @param realm    The Keycloak realm to get the admin user's bearer token from.
     * @param params   Parameters to add to the request.
     * @return The HTTP get request.
     * @throws Exception If there was an error building the request.
     */
    private HttpGet buildHttpGetRequest(String endpoint, String realm, String... params) throws Exception {
        URIBuilder builder = new URIBuilder(endpoint);
        if (params != null && params.length > 0) {
            for (int idx = 0; idx < params.length; idx = idx + 2) {
                builder.addParameter(params[idx], params[idx + 1]);
            }
        }

        HttpGet request = new HttpGet(builder.build());
        addAdminBearerToken(realm, request);

        return request;
    }

    /**
     * Build an HTTP post request using the admin user.
     *
     * @param endpoint    The endpoint to send the request to.
     * @param realm       The Keycloak realm to get the admin user's bearer token from.
     * @param jsonContent The JSON content to send as part of the post.
     * @return The HTTP post request.
     * @throws Exception If there was an error building the request.
     */
    private HttpPost buildHttpPostRequest(String endpoint, String realm, String jsonContent) throws Exception {
        HttpPost request = new HttpPost(endpoint);
        addAdminBearerToken(realm, request);
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
        addAdminBearerToken(realm, request);
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        /*
         * Create an HTTP client and execute the request.
         */
        Log.info(CLASS, METHOD_NAME, "Requesting to create user at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to create new user. HTTP status code: " + response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    /**
     * Delete a client from the specified realm.
     *
     * @param realm    The Keycloak realm to delete the client from.
     * @param clientId The clientId of the client to delete.
     * @return A boolean indicating whether the client was deleted.
     * @throws Exception If there was an error deleting the client.
     */
    public boolean deleteClient(String realm, String clientId) throws Exception {
        final String METHOD_NAME = "deleteClient";

        /*
         * Need the client's ID (not clientId) to delete.
         */
        String id = getClient(realm, clientId).getString(KEY_ID);

        /*
         * Create the request.
         */
        HttpDelete request = buildHttpDeleteRequest(getClientsRestEndpoint(realm) + "/" + id, realm);

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
        HttpDelete request = buildHttpDeleteRequest(getUsersRestEndpoint(realm) + "/" + userId, realm);

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
     * Get a bearer token for the Keycloak admin cli for the admin user.
     *
     * @param realm The Keycloak realm to get the bearer token for.
     * @return The bearer token for the admin user.
     * @throws Exception If there was an error getting the bearer token.
     */
    private String getAdminCliBearerToken(String realm) throws Exception {
        final String METHOD_NAME = "getAdminCliBearerToken";

        String bearerToken = BEARER_TOKENS_BY_REALM.get(realm);

        if (bearerToken == null) {
            /*
             * Build the request.
             */
            HttpPost request = new HttpPost(getBearerTokenEndpoint(realm));
            request.setEntity(new StringEntity("username=" + KeycloakContainer.ADMIN_USER + "&password=" + KeycloakContainer.ADMIN_PASS
                                               + "&grant_type=password&client_id=" + ADMIN_CLIENT_ID, ContentType.APPLICATION_FORM_URLENCODED));

            /*
             * Create an HTTP client and execute the request.
             */
            try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
                Log.info(CLASS, METHOD_NAME, "Requesting bearer token from " + request.getURI());
                try (CloseableHttpResponse response = httpClient.execute(request)) {

                    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                        throw new Exception("Unable to retrieve bearer token for " + ADMIN_CLIENT_ID
                                            + ". HTTP status code: " + response.getStatusLine().getStatusCode());
                    }

                    /*
                     * Get the access token.
                     */
                    bearerToken = KeycloakUtils.getJsonObject(KeycloakUtils.getStringResponse(response)).getString(KEY_ACCESS_TOKEN);
//                    BEARER_TOKENS_BY_REALM.put(realm, bearerToken); TODO Don't cache... Token lifetime is too short.
                }
            }
        }

        return bearerToken;
    }

    /**
     * Get the REST endpoint for the Keycloak server.
     *
     * @return The REST endpoint.
     */
    private String getAdminRESTEndpoint() {
        return keycloak.getRootHttpEndpoint() + "/admin/realms";
    }

    /**
     * Get the REST endpoint for retrieving a bearer token.
     *
     * @param realm The Keycloak realm to get the endpoint for.
     * @return The REST endpoint for retrieving a bearer token.
     */
    private String getBearerTokenEndpoint(String realm) {
        return keycloak.getRootHttpEndpoint() + "/realms/" + realm + "/protocol/openid-connect/token";
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
        HttpGet request = buildHttpGetRequest(getClientsRestEndpoint(realm), realm, KEY_CLIENT_ID, clientId);

        /*
         * Create an HTTP client and execute the request.
         */
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Requesting users from " + request.getURI());
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
        HttpGet request = buildHttpGetRequest(getUsersRestEndpoint(realm), realm, "username", username);

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
        HttpGet getRequest = buildHttpGetRequest(spMetadataUrl, keycloakRealm);
        String spMetadata = null;
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, METHOD_NAME, "Retrieving SAML SP metadata from " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to retrieve SP metadata from server. Is the server running? HTTP status code: "
                                        + response.getStatusLine().getStatusCode());
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
            HttpPost postRequest = buildHttpPostRequest(getAdminRESTEndpoint() + "/" + keycloakRealm + "/client-description-converter", keycloakRealm, spMetadata);

            /*
             * Send the request to Keycloak to convert the SP metadata to Keycloak configuration.
             */
            String clientJson = null;
            Log.info(CLASS, METHOD_NAME, "Send metadata descriptor conversion from " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to convert SP metadata. HTTP status code: "
                                        + response.getStatusLine().getStatusCode());
                }

                /*
                 * Get the response entity.
                 */
                clientJson = KeycloakUtils.getStringResponse(response);
            }

            /*
             * Create the request to register the client from the converted SP metadata.
             */
            postRequest = buildHttpPostRequest(getClientsRestEndpoint(keycloakRealm), keycloakRealm, clientJson);
            Log.info(CLASS, METHOD_NAME, "Requesting to register client at " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw new Exception("Unable to create new client. HTTP status code: "
                                        + response.getStatusLine().getStatusCode());
                }
            }

            /*
             * Return the client ID.
             */
            return KeycloakUtils.getJsonObject(clientJson).getString(KEY_CLIENT_ID);
        }
    }
}
