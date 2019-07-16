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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 *
 */
public class OidcBaseClientScopeReducerTest extends AbstractOidcRegistrationBaseTest {
    private static final String SCOPE1 = "openid";
    private static final String SCOPE2 = "general";
    private static final String SCOPE3 = "profile";
    private static final String CLIENT_SCOPES = SCOPE1 + " " + SCOPE2 + " " + SCOPE3;
    private static final String CLIENT_PREAUTHORIZED_SCOPE = SCOPE3;
    private static final String[] VALID_REQUEST_SCOPES = { SCOPE1, SCOPE2, SCOPE3 };
    private static final String VALID_REQUEST_PREAUTHORIZED_SCOPE = CLIENT_PREAUTHORIZED_SCOPE;
    private static final String INVALID_REQUEST_CASE_SENSITIVE_SCOPE = SCOPE3.toUpperCase();
    private static final String INVALID_SCOPE = "invalid";

    @Test
    public void testBadConstructor() {
        boolean expectedErrorOccured = false;
        OidcBaseClient nullClient = null;
        try {
            OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(nullClient);
        } catch (IllegalArgumentException e) {
            expectedErrorOccured = true;
        }

        assertTrue("OidcBaseClientScopeReducer constructor should have thrown IllegalArgumentException because of null client value parameter.", expectedErrorOccured);
    }

    @Test
    public void testNullScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope(null);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer initialized with client that has null scope should have returned false to containing anything.", reducer.hasClientScope(SCOPE1));
    }

    @Test
    public void testBlankScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope("");

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer initialized with client that has blank scope should have returned false to containing anything.", reducer.hasClientScope(SCOPE1));
    }

    @Test
    public void testNullPreAuthorizedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope(null);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer initialized with client that has null preAuthorized scope should have returned false to containing anything.",
                    reducer.hasClientPreAuthorizedScope(SCOPE3));
    }

    @Test
    public void testBlankPreAuthorizedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope("");

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer initialized with client that has blank preAuthorized scope should have returned false to containing anything.",
                    reducer.hasClientPreAuthorizedScope(SCOPE3));
    }

    @Test
    public void testClientScopeWithLeadingTrailingSpaces() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope(" " + CLIENT_SCOPES + " ");

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        for (String scope : VALID_REQUEST_SCOPES) {
            assertTrue("Reducer should have returned true. Ensure sample client contains requested scope value, otherwise error in implementation.",
                       reducer.hasClientScope(scope));
        }

    }

    @Test
    public void testValidRequestedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope(CLIENT_SCOPES);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        for (String scope : VALID_REQUEST_SCOPES) {
            assertTrue("Reducer should have returned true. Ensure sample client contains requested scope value, otherwise error in implementation.",
                       reducer.hasClientScope(scope));
        }

    }

    @Test
    public void testClientPreAuthorizedScopeWithLeadingTrailingSpaces() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope(" " + CLIENT_PREAUTHORIZED_SCOPE + " ");

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertTrue("Reducer should have returned true. Ensure sample client contains requested preAuthorized scope value, otherwise error in implementation.",
                   reducer.hasClientPreAuthorizedScope(VALID_REQUEST_PREAUTHORIZED_SCOPE));
    }

    @Test
    public void testValidRequestedPreAuthorizedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope(CLIENT_PREAUTHORIZED_SCOPE);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertTrue("Reducer should have returned true. Ensure sample client contains requested preAuthorized scope value, otherwise error in implementation.",
                   reducer.hasClientPreAuthorizedScope(VALID_REQUEST_PREAUTHORIZED_SCOPE));
    }

    @Test
    public void testInvalidScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope(CLIENT_SCOPES);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer should have returned false because the request scope is invalid and not in the client's scope.",
                    reducer.hasClientScope(INVALID_SCOPE));
    }

    @Test
    public void testInvalidPreAuthorizedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope(CLIENT_PREAUTHORIZED_SCOPE);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer should have returned false because the request scope is invalid and not in the client's preAuthorized scope.",
                    reducer.hasClientPreAuthorizedScope(INVALID_SCOPE));
    }

    @Test
    public void testCaseSensitiveScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setScope(CLIENT_SCOPES);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer should have returned false because the request scope is invalid because of case sensitivity.",
                    reducer.hasClientScope(INVALID_REQUEST_CASE_SENSITIVE_SCOPE));
    }

    @Test
    public void testCaseSensitivePreAuthorizedScope() {
        OidcBaseClient sampleClient = getSampleOidcBaseClient();
        sampleClient.setPreAuthorizedScope(CLIENT_SCOPES);

        OidcBaseClientScopeReducer reducer = new OidcBaseClientScopeReducer(sampleClient);

        assertFalse("Reducer should have returned false because the request scope is invalid because of case sensitivity.",
                    reducer.hasClientPreAuthorizedScope(INVALID_REQUEST_CASE_SENSITIVE_SCOPE));
    }
}
