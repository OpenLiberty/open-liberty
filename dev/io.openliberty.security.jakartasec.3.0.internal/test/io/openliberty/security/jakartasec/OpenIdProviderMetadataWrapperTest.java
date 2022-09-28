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
package io.openliberty.security.jakartasec;

import static io.openliberty.security.jakartasec.JakartaSec30Constants.EMPTY_DEFAULT;
import static junit.framework.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/**
 * Verify that the {@link OpenIdProviderMetadataWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class OpenIdProviderMetadataWrapperTest {

    private Map<String, Object> overrides;

    private static final String STRING_EL_EXPRESSION = "#{'blah'.concat('blah')}";
    private static final String EVALUATED_EL_EXPRESSION_STRING_RESULT = "blahblah";

    @Before
    public void setUp() {
        overrides = new HashMap<String, Object>();
    }

    @Test
    public void testGetAuthorizationEndpoint() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getAuthorizationEndpoint());
    }

    @Test
    public void testGetAuthorizationEndpoint_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.AUTHORIZATION_ENDPOINT, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getAuthorizationEndpoint());
    }

    @Test
    public void testGetTokenEndpoint() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getTokenEndpoint());
    }

    @Test
    public void testGetTokenEndpoint_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.TOKEN_ENDPOINT, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getTokenEndpoint());
    }

    @Test
    public void testGetUserinfoEndpoint() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getUserinfoEndpoint());
    }

    @Test
    public void testGetUserinfoEndpoint_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.USERINFO_ENDPOINT, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getUserinfoEndpoint());
    }

    @Test
    public void testGetEndSessionEndpoint() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getEndSessionEndpoint());
    }

    @Test
    public void testGetEndSessionEndpoint_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.END_SESSION_ENDPOINT, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getEndSessionEndpoint());
    }

    @Test
    public void testGetJwksURI() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getJwksURI());
    }

    @Test
    public void testGetJwksURI_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.JWKS_URI, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getJwksURI());
    }

    @Test
    public void testGetIssuer() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getIssuer());
    }

    @Test
    public void testGetIssuer_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.ISSUER, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getIssuer());
    }

    @Test
    public void testGetSubjectTypeSupported() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(TestOpenIdProviderMetadataDefinition.SUBJECT_TYPE_SUPPORTED_DEFAULT, wrapper.getSubjectTypeSupported());
    }

    @Test
    public void testGetSubjectTypeSupported_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.SUBJECT_TYPE_SUPPORTED, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getSubjectTypeSupported());
    }

    @Test
    public void testGetIdTokenSigningAlgorithmsSupported() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(TestOpenIdProviderMetadataDefinition.ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED_DEFAULT, wrapper.getIdTokenSigningAlgorithmsSupported());
    }

    @Test
    public void testGetIdTokenSigningAlgorithmsSupported_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getIdTokenSigningAlgorithmsSupported());
    }

    @Test
    public void testGetResponseTypeSupported() {
        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(TestOpenIdProviderMetadataDefinition.RESPONSE_TYPE_SUPPORTED_DEFAULT, wrapper.getResponseTypeSupported());
    }

    @Test
    public void testGetResponseTypeSupported_EL() {
        overrides.put(TestOpenIdProviderMetadataDefinition.RESPONSE_TYPE_SUPPORTED, STRING_EL_EXPRESSION);

        OpenIdProviderMetadata providerMetadata = TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(overrides);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getResponseTypeSupported());
    }

}