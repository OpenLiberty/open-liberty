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
package com.ibm.ws.security.oauth20.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 * Unit test case to verify behavior integrity of OidcBaseClient bean
 */
public class OidcBaseClientTest extends AbstractOidcRegistrationBaseTest {

    private static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    @Test
    public void testOidcBaseClientConstructor() {
        String methodName = "testOidcBaseClientConstructor";

        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive(REDIRECT_URI_1));
        redirectUris.add(new JsonPrimitive(REDIRECT_URI_2));

        try {
            OidcBaseClient testOidcBaseClient =
                            new OidcBaseClient(CLIENT_ID,
                                            CLIENT_SECRET,
                                            redirectUris,
                                            CLIENT_NAME,
                                            COMPONENT_ID,
                                            IS_ENABLED);

            //Assert expected values
            assertEquals(CLIENT_ID, testOidcBaseClient.getClientId());
            assertEquals(CLIENT_SECRET, testOidcBaseClient.getClientSecret());
            assertEquals(redirectUris, testOidcBaseClient.getRedirectUris());
            assertEquals(CLIENT_NAME, testOidcBaseClient.getClientName());
            assertEquals(COMPONENT_ID, testOidcBaseClient.getComponentId());
            assertEquals(IS_ENABLED, testOidcBaseClient.isEnabled());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testOidcBaseClientAccessorModifiers() {
        String methodName = "testOidcBaseClientAccessorModifiers";

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

        try {
            OidcBaseClient testOidcBaseClient = new OidcBaseClient("", "", new JsonArray(), "", "", false);

            //Use modifiers to set all properties
            testOidcBaseClient.setClientId(CLIENT_ID);
            testOidcBaseClient.setClientSecret(CLIENT_SECRET);
            testOidcBaseClient.setRedirectUris(redirectUris);
            testOidcBaseClient.setClientName(CLIENT_NAME);
            testOidcBaseClient.setComponentId(COMPONENT_ID);
            testOidcBaseClient.setEnabled(IS_ENABLED);
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

            //Assert expected values with accessors
            assertEquals(CLIENT_ID, testOidcBaseClient.getClientId());
            assertEquals(CLIENT_SECRET, testOidcBaseClient.getClientSecret());
            assertEquals(redirectUris, testOidcBaseClient.getRedirectUris());
            assertEquals(CLIENT_NAME, testOidcBaseClient.getClientName());
            assertEquals(COMPONENT_ID, testOidcBaseClient.getComponentId());
            assertEquals(IS_ENABLED, testOidcBaseClient.isEnabled());
            assertEquals(CLIENT_ID_ISSUED_AT, testOidcBaseClient.getClientIdIssuedAt());
            assertEquals(REGISTRATION_CLIENT_URI, testOidcBaseClient.getRegistrationClientUri());
            assertEquals(CLIENT_SECRET_EXPIRES_AT, testOidcBaseClient.getClientSecretExpiresAt());
            assertEquals(TOKEN_ENDPOINT_AUTH_METHOD, testOidcBaseClient.getTokenEndpointAuthMethod());
            assertEquals(SCOPE, testOidcBaseClient.getScope());
            assertEquals(grantTypes, testOidcBaseClient.getGrantTypes());
            assertEquals(responseTypes, testOidcBaseClient.getResponseTypes());
            assertEquals(APPLICATION_TYPE, testOidcBaseClient.getApplicationType());
            assertEquals(SUBJECT_TYPE, testOidcBaseClient.getSubjectType());
            assertEquals(postLogoutRedirectUris, testOidcBaseClient.getPostLogoutRedirectUris());
            assertEquals(PREAUTHORIZED_SCOPE, testOidcBaseClient.getPreAuthorizedScope());
            assertEquals(INTROSPECT_TOKENS, testOidcBaseClient.isIntrospectTokens());
            assertEquals(trustedUriPrefixes, testOidcBaseClient.getTrustedUriPrefixes());
            assertEquals(FUNCTIONAL_USER_ID, testOidcBaseClient.getFunctionalUserId());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testOidcBaseClientDeepCopy() {
        String methodName = "testOidcBaseClientDeepCopy";
        try {
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();

            OidcBaseClient deepCopyTestOidcBaseClient = testOidcBaseClient.getDeepCopy();

            String randomString = "random";
            long randomLong = -100L;

            JsonArray randomJsonArray = new JsonArray();
            randomJsonArray.add(new JsonPrimitive("randomPrimitive"));

            //Modify original reference
            testOidcBaseClient.setClientId(randomString);
            testOidcBaseClient.setClientSecret(randomString);
            testOidcBaseClient.setRedirectUris(randomJsonArray);
            testOidcBaseClient.setClientName(randomString);
            testOidcBaseClient.setComponentId(randomString);
            testOidcBaseClient.setEnabled(!IS_ENABLED);
            testOidcBaseClient.setClientIdIssuedAt(randomLong);
            testOidcBaseClient.setRegistrationClientUri(randomString);
            testOidcBaseClient.setClientSecretExpiresAt(randomLong);
            testOidcBaseClient.setTokenEndpointAuthMethod(randomString);
            testOidcBaseClient.setScope(randomString);
            testOidcBaseClient.setGrantTypes(randomJsonArray);
            testOidcBaseClient.setResponseTypes(randomJsonArray);
            testOidcBaseClient.setApplicationType(randomString);
            testOidcBaseClient.setSubjectType(randomString);
            testOidcBaseClient.setPostLogoutRedirectUris(randomJsonArray);
            testOidcBaseClient.setPreAuthorizedScope(randomString);
            testOidcBaseClient.setIntrospectTokens(!INTROSPECT_TOKENS);
            testOidcBaseClient.setTrustedUriPrefixes(randomJsonArray);
            testOidcBaseClient.setFunctionalUserId(randomString);

            //Assert that modified deep copy values don't equal the original copy values
            assertFalse(deepCopyTestOidcBaseClient.getClientId().equals(testOidcBaseClient.getClientId()));
            assertFalse(deepCopyTestOidcBaseClient.getClientSecret().equals(testOidcBaseClient.getClientSecret()));
            assertFalse(deepCopyTestOidcBaseClient.getRedirectUris() == testOidcBaseClient.getRedirectUris());
            assertFalse(deepCopyTestOidcBaseClient.getClientName().equals(testOidcBaseClient.getClientName()));
            assertFalse(deepCopyTestOidcBaseClient.getComponentId().equals(testOidcBaseClient.getComponentId()));
            assertFalse(deepCopyTestOidcBaseClient.isEnabled() == testOidcBaseClient.isEnabled());
            assertFalse(deepCopyTestOidcBaseClient.getClientIdIssuedAt() == testOidcBaseClient.getClientIdIssuedAt());
            assertFalse(deepCopyTestOidcBaseClient.getRegistrationClientUri().equals(testOidcBaseClient.getRegistrationClientUri()));
            assertFalse(deepCopyTestOidcBaseClient.getClientSecretExpiresAt() == testOidcBaseClient.getClientSecretExpiresAt());
            assertFalse(deepCopyTestOidcBaseClient.getTokenEndpointAuthMethod().equals(testOidcBaseClient.getTokenEndpointAuthMethod()));
            assertFalse(deepCopyTestOidcBaseClient.getScope().equals(testOidcBaseClient.getScope()));
            assertFalse(deepCopyTestOidcBaseClient.getGrantTypes() == testOidcBaseClient.getGrantTypes());
            assertFalse(deepCopyTestOidcBaseClient.getResponseTypes() == testOidcBaseClient.getResponseTypes());
            assertFalse(deepCopyTestOidcBaseClient.getApplicationType().equals(testOidcBaseClient.getApplicationType()));
            assertFalse(deepCopyTestOidcBaseClient.getSubjectType().equals(testOidcBaseClient.getSubjectType()));
            assertFalse(deepCopyTestOidcBaseClient.getPostLogoutRedirectUris() == testOidcBaseClient.getPostLogoutRedirectUris());
            assertFalse(deepCopyTestOidcBaseClient.getPreAuthorizedScope().equals(testOidcBaseClient.getPreAuthorizedScope()));
            assertFalse(deepCopyTestOidcBaseClient.isIntrospectTokens() == testOidcBaseClient.isIntrospectTokens());
            assertFalse(deepCopyTestOidcBaseClient.getTrustedUriPrefixes() == testOidcBaseClient.getTrustedUriPrefixes());
            assertFalse(deepCopyTestOidcBaseClient.getFunctionalUserId().equals(testOidcBaseClient.getFunctionalUserId()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testOidcBaseClientConfidential() {
        String methodName = "testOidcBaseClientConfidential";
        try {
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();

            //With sampleOidcBaseClient, clientSecret is not null or empty
            assertTrue(testOidcBaseClient.isConfidential());

            testOidcBaseClient.setClientSecret(null);
            assertFalse(testOidcBaseClient.isConfidential());

            testOidcBaseClient.setClientSecret("");
            assertFalse(testOidcBaseClient.isConfidential());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testOidcBaseClientToString() {
        String methodName = "testOidcBaseClientToString";
        try {
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();
            String toStringValue = testOidcBaseClient.toString();

            assertTrue(toStringValue.contains("_componentId=" + COMPONENT_ID));
            assertTrue(toStringValue.contains("_clientId=" + CLIENT_ID));
            assertTrue(toStringValue.contains("_clientSecret=" + CLIENT_SECRET));
            assertTrue(toStringValue.contains("_displayName=" + CLIENT_NAME));
            assertTrue(toStringValue.contains("_redirectURIs=" + REDIRECT_URI_1 + " " + REDIRECT_URI_2));
            assertTrue(toStringValue.contains("_isEnabled=" + IS_ENABLED));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
