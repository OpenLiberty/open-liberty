/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oidc.common;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.test.common.CommonTestClass;

/**
 * Base test for 'client registration' related test cases to extends
 *
 */
@Ignore
// Ignore this class, since it's not actually a test
public abstract class AbstractOidcRegistrationBaseTest extends CommonTestClass {

    public static String _testName = "";

    public static final String FIELD_CLIENT_SECRET = "client_secret";
    public static final String FIELD_APPLICATION_TYPE = "application_type";
    public static final String FIELD_SUBJECT_TYPE = "subject_type";

    /**
     * Expected OidcBaseClient values
     */
    public static final String COMPONENT_ID = "OP";
    public static final String CLIENT_ID = "123456789";
    public static final String CLIENT_SECRET = "secret";
    public static final String CLIENT_NAME = "test client";

    public static final String REDIRECT_URI_1 = "http://www.redirect1.com";
    public static final String REDIRECT_URI_2 = "http://www.redirect2.com";

    public static final boolean IS_ENABLED = true;
    public static final long CLIENT_ID_ISSUED_AT = 12345L;
    public static final String REGISTRATION_CLIENT_URI = "https://localhost:8020/oidc/endpoint/OP/registration/" + CLIENT_ID;
    public static final long CLIENT_SECRET_EXPIRES_AT = 0;
    public static final String TOKEN_ENDPOINT_AUTH_METHOD = "none";
    public static final String SCOPE = "openid general profile";

    public static final String GRANT_TYPES_1 = "authorization_code";
    public static final String GRANT_TYPES_2 = "implicit";

    public static final String RESPONSE_TYPES_1 = "code";
    public static final String RESPONSE_TYPES_2 = "token";

    public static final String APPLICATION_TYPE = "web";
    public static final String SUBJECT_TYPE = "public";

    public static final String POST_LOGOUT_REDIRECT_URI_1 = "http://www.logout1.com";
    public static final String POST_LOGOUT_REDIRECT_URI_2 = "http://www.logout2.com";

    public static final String PREAUTHORIZED_SCOPE = "profile";
    public static final boolean INTROSPECT_TOKENS = true;

    public static final String TRUSTED_URI_PREFIX_1 = "http://www.trusted1.com/";
    public static final String TRUSTED_URI_PREFIX_2 = "http://www.trusted2.com/";

    public static final String FUNCTIONAL_USER_ID = "testuser";

    public static final String SALT = "HStFDlTCZ4rJQThbSddZG5q0Ovf2ozDlrJtT1AYiUhM=";
    public static final String ALG = "PBKDF2WithHmacSHA512";
    public static boolean isHash = false;

    public static OidcBaseClient getSampleOidcBaseClient(String providerName) {
        OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();
        testOidcBaseClient.setComponentId(providerName);
        return testOidcBaseClient;
    }

    public static OidcBaseClient getSampleOidcBaseClient() {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive(REDIRECT_URI_1));
        redirectUris.add(new JsonPrimitive(REDIRECT_URI_2));

        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(GRANT_TYPES_1));
        grantTypes.add(new JsonPrimitive(GRANT_TYPES_2));

        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive(RESPONSE_TYPES_1));
        responseTypes.add(new JsonPrimitive(RESPONSE_TYPES_2));

        JsonArray postLogoutRedirectUris = new JsonArray();
        postLogoutRedirectUris.add(new JsonPrimitive(POST_LOGOUT_REDIRECT_URI_1));
        postLogoutRedirectUris.add(new JsonPrimitive(POST_LOGOUT_REDIRECT_URI_2));

        JsonArray trustedUriPrefixes = new JsonArray();
        trustedUriPrefixes.add(new JsonPrimitive(TRUSTED_URI_PREFIX_1));
        trustedUriPrefixes.add(new JsonPrimitive(TRUSTED_URI_PREFIX_2));

        String hashClientSecret = null;

        if (isHash) {
            hashClientSecret = HashSecretUtils.hashSecret(CLIENT_SECRET, CLIENT_ID, true, SALT, ALG, HashSecretUtils.DEFAULT_ITERATIONS, HashSecretUtils.DEFAULT_KEYSIZE);
        }

        OidcBaseClient testOidcBaseClient = new OidcBaseClient(CLIENT_ID, isHash ? hashClientSecret : CLIENT_SECRET, redirectUris, CLIENT_NAME, COMPONENT_ID, IS_ENABLED);

        // Use modifiers to set remaining properties
        testOidcBaseClient.setClientIdIssuedAt(CLIENT_ID_ISSUED_AT);
        testOidcBaseClient.setRegistrationClientUri(REGISTRATION_CLIENT_URI);
        testOidcBaseClient.setClientSecretExpiresAt(CLIENT_SECRET_EXPIRES_AT);
        testOidcBaseClient.setTokenEndpointAuthMethod(TOKEN_ENDPOINT_AUTH_METHOD);
        testOidcBaseClient.setScope(SCOPE);
        testOidcBaseClient.setGrantTypes(grantTypes);
        testOidcBaseClient.setResponseTypes(responseTypes);
        testOidcBaseClient.setApplicationType(APPLICATION_TYPE);
        testOidcBaseClient.setSubjectType(SUBJECT_TYPE);
        testOidcBaseClient.setPostLogoutRedirectUris(postLogoutRedirectUris);
        testOidcBaseClient.setPreAuthorizedScope(PREAUTHORIZED_SCOPE);
        testOidcBaseClient.setIntrospectTokens(INTROSPECT_TOKENS);
        testOidcBaseClient.setTrustedUriPrefixes(trustedUriPrefixes);
        testOidcBaseClient.setFunctionalUserId(FUNCTIONAL_USER_ID);
        testOidcBaseClient.setSalt(SALT);
        testOidcBaseClient.setAlgorithm(ALG);

        return testOidcBaseClient;
    }

    public static List<OidcBaseClient> getsampleOidcBaseClients(int numOfSampleOidcBaseClients) {
        return getsampleOidcBaseClients(numOfSampleOidcBaseClients, COMPONENT_ID);
    }

    public static List<OidcBaseClient> getsampleOidcBaseClients(int numOfSampleOidcBaseClients, String providerName) {
        List<OidcBaseClient> testOidcBaseClients = new ArrayList<OidcBaseClient>();

        for (int i = 0; i < numOfSampleOidcBaseClients; i++) {
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient(providerName);
            testOidcBaseClient.setClientId(String.valueOf(i));
            testOidcBaseClients.add(testOidcBaseClient);
        }

        return testOidcBaseClients;
    }

    public static void assertEqualsOidcBaseClients(OidcBaseClient client1, OidcBaseClient client2) {
        // Ensure object return matches object seeded
        assertEquals(client1.getComponentId(), client2.getComponentId());
        assertEquals(client1.getClientId(), client2.getClientId());

        String clientSecret = client1.getClientSecret();
        if (isHash) { // The database and custom stores store the client secret as a oneway hash
            clientSecret = HashSecretUtils.hashSecret(clientSecret, client1.getClientId(), true, SALT, ALG, HashSecretUtils.DEFAULT_ITERATIONS, HashSecretUtils.DEFAULT_KEYSIZE);
        }
        assertEquals(clientSecret, client2.getClientSecret());
        assertEquals(client1.getRedirectUris(), client2.getRedirectUris());
        assertEquals(client1.getClientName(), client2.getClientName());
        assertEquals(client1.isEnabled(), client2.isEnabled());
        assertEquals(client1.getTokenEndpointAuthMethod(), client2.getTokenEndpointAuthMethod());
        assertEquals(client1.getScope(), client2.getScope());
        assertEquals(client1.getGrantTypes(), client2.getGrantTypes());
        assertEquals(client1.getResponseTypes(), client2.getResponseTypes());
        assertEquals(client1.getApplicationType(), client2.getApplicationType());
        assertEquals(client1.getSubjectType(), client2.getSubjectType());
        assertEquals(client1.getPostLogoutRedirectUris(), client2.getPostLogoutRedirectUris());
        assertEquals(client1.getPreAuthorizedScope(), client2.getPreAuthorizedScope());
        assertEquals(client1.isIntrospectTokens(), client2.isIntrospectTokens());
        assertEquals(client1.getTrustedUriPrefixes(), client2.getTrustedUriPrefixes());
        assertEquals(client1.getFunctionalUserId(), client2.getFunctionalUserId());
    }

    public static List<OidcBaseClient> getOidcBaseClientsList(Collection<OidcBaseClient> oidcBaseClientsColl) {
        List<OidcBaseClient> clients = new ArrayList<OidcBaseClient>();
        if (oidcBaseClientsColl != null && oidcBaseClientsColl.size() > 0) {
            for (OidcBaseClient client : oidcBaseClientsColl) {
                clients.add(client);
            }
        }

        return clients;
    }

    public Map<String, OidcBaseClient> getOidcBaseClientMap(Collection<OidcBaseClient> oidcBaseClientsColl) {
        Map<String, OidcBaseClient> oidcBaseClientsMap = new TreeMap<String, OidcBaseClient>();
        for (OidcBaseClient client : oidcBaseClientsColl) {
            oidcBaseClientsMap.put(client.getClientId(), client);
        }

        return oidcBaseClientsMap;
    }

    protected void assertUnImplementedException(OidcServerException e) {
        assertEquals(e.getErrorCode(), OIDCConstants.ERROR_SERVER_ERROR);
        assertEquals(e.getHttpStatus(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected static void setHash(boolean hash) {
        isHash = hash;
    }
}
