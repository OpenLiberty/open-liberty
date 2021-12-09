/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

public class ClientRegistrationHelper {

    private boolean isHash = false;

    public ClientRegistrationHelper(boolean isHash) {
        this.isHash = isHash;
    }

    public void setHash(boolean isHash) {
        this.isHash = isHash;
    }

    public boolean isHash() {
        return isHash;
    }

    public OidcBaseClient getSampleOidcBaseClient(String providerName) {
        OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();
        testOidcBaseClient.setComponentId(providerName);
        return testOidcBaseClient;
    }

    public OidcBaseClient getSampleOidcBaseClient() {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.REDIRECT_URI_1));
        redirectUris.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.REDIRECT_URI_2));

        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.GRANT_TYPES_1));
        grantTypes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.GRANT_TYPES_2));

        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.RESPONSE_TYPES_1));
        responseTypes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.RESPONSE_TYPES_2));

        JsonArray postLogoutRedirectUris = new JsonArray();
        postLogoutRedirectUris.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.POST_LOGOUT_REDIRECT_URI_1));
        postLogoutRedirectUris.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.POST_LOGOUT_REDIRECT_URI_2));

        JsonArray trustedUriPrefixes = new JsonArray();
        trustedUriPrefixes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.TRUSTED_URI_PREFIX_1));
        trustedUriPrefixes.add(new JsonPrimitive(AbstractOidcRegistrationBaseTest.TRUSTED_URI_PREFIX_2));

        String hashClientSecret = null;

        if (isHash) {
            hashClientSecret = HashSecretUtils.hashSecret(AbstractOidcRegistrationBaseTest.CLIENT_SECRET, AbstractOidcRegistrationBaseTest.CLIENT_ID, true, AbstractOidcRegistrationBaseTest.SALT, AbstractOidcRegistrationBaseTest.ALG, HashSecretUtils.DEFAULT_ITERATIONS, HashSecretUtils.DEFAULT_KEYSIZE);
        }

        OidcBaseClient testOidcBaseClient = new OidcBaseClient(AbstractOidcRegistrationBaseTest.CLIENT_ID, isHash ? hashClientSecret : AbstractOidcRegistrationBaseTest.CLIENT_SECRET, redirectUris, AbstractOidcRegistrationBaseTest.CLIENT_NAME, AbstractOidcRegistrationBaseTest.COMPONENT_ID, AbstractOidcRegistrationBaseTest.IS_ENABLED);

        // Use modifiers to set remaining properties
        testOidcBaseClient.setClientIdIssuedAt(AbstractOidcRegistrationBaseTest.CLIENT_ID_ISSUED_AT);
        testOidcBaseClient.setRegistrationClientUri(AbstractOidcRegistrationBaseTest.REGISTRATION_CLIENT_URI);
        testOidcBaseClient.setClientSecretExpiresAt(AbstractOidcRegistrationBaseTest.CLIENT_SECRET_EXPIRES_AT);
        testOidcBaseClient.setTokenEndpointAuthMethod(AbstractOidcRegistrationBaseTest.TOKEN_ENDPOINT_AUTH_METHOD);
        testOidcBaseClient.setScope(AbstractOidcRegistrationBaseTest.SCOPE);
        testOidcBaseClient.setGrantTypes(grantTypes);
        testOidcBaseClient.setResponseTypes(responseTypes);
        testOidcBaseClient.setApplicationType(AbstractOidcRegistrationBaseTest.APPLICATION_TYPE);
        testOidcBaseClient.setSubjectType(AbstractOidcRegistrationBaseTest.SUBJECT_TYPE);
        testOidcBaseClient.setPostLogoutRedirectUris(postLogoutRedirectUris);
        testOidcBaseClient.setPreAuthorizedScope(AbstractOidcRegistrationBaseTest.PREAUTHORIZED_SCOPE);
        testOidcBaseClient.setIntrospectTokens(AbstractOidcRegistrationBaseTest.INTROSPECT_TOKENS);
        testOidcBaseClient.setTrustedUriPrefixes(trustedUriPrefixes);
        testOidcBaseClient.setFunctionalUserId(AbstractOidcRegistrationBaseTest.FUNCTIONAL_USER_ID);
        testOidcBaseClient.setSalt(AbstractOidcRegistrationBaseTest.SALT);
        testOidcBaseClient.setAlgorithm(AbstractOidcRegistrationBaseTest.ALG);

        return testOidcBaseClient;
    }

    public List<OidcBaseClient> getsampleOidcBaseClients(int numOfSampleOidcBaseClients) {
        return getsampleOidcBaseClients(numOfSampleOidcBaseClients, AbstractOidcRegistrationBaseTest.COMPONENT_ID);
    }

    public List<OidcBaseClient> getsampleOidcBaseClients(int numOfSampleOidcBaseClients, String providerName) {
        List<OidcBaseClient> testOidcBaseClients = new ArrayList<OidcBaseClient>();

        for (int i = 0; i < numOfSampleOidcBaseClients; i++) {
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient(providerName);
            testOidcBaseClient.setClientId(String.valueOf(i));
            testOidcBaseClients.add(testOidcBaseClient);
        }

        return testOidcBaseClients;
    }

    public void assertEqualsOidcBaseClients(OidcBaseClient client1, OidcBaseClient client2) {
        // Ensure object return matches object seeded
        assertEquals(client1.getComponentId(), client2.getComponentId());
        assertEquals(client1.getClientId(), client2.getClientId());

        String clientSecret = client1.getClientSecret();
        if (isHash) { // The database and custom stores store the client secret as a oneway hash
            clientSecret = HashSecretUtils.hashSecret(clientSecret, client1.getClientId(), true, AbstractOidcRegistrationBaseTest.SALT, AbstractOidcRegistrationBaseTest.ALG, HashSecretUtils.DEFAULT_ITERATIONS, HashSecretUtils.DEFAULT_KEYSIZE);
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

    public List<OidcBaseClient> getOidcBaseClientsList(Collection<OidcBaseClient> oidcBaseClientsColl) {
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

    public void assertUnImplementedException(OidcServerException e) {
        assertEquals(e.getErrorCode(), OIDCConstants.ERROR_SERVER_ERROR);
        assertEquals(e.getHttpStatus(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

}
