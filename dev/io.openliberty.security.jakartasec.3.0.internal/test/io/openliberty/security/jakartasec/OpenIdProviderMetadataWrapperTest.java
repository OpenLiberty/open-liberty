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

import static junit.framework.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/**
 * Verify that the {@link OpenIdProviderMetadataWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class OpenIdProviderMetadataWrapperTest {

    private static String EMPTY_DEFAULT = "";
    private static String AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
    private static String TOKEN_ENDPOINT = "tokenEndpoint";
    private static String USERINFO_ENDPOINT = "userinfoEndpoint";
    private static String END_SESSION_ENDPOINT = "endSessionEndpoint";
    private static String JWKS_URI = "jwksURI";
    private static String ISSUER = "issuer";
    private static String SUBJECT_TYPE_SUPPORTED = "subjectTypeSupported";
    private static String ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED = "idTokenSigningAlgorithmsSupported";
    private static String RESPONSE_TYPE_SUPPORTED = "responseTypeSupported";
    private static String SUBJECT_TYPE_SUPPORTED_DEFAULT = "public";
    private static String ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED_DEFAULT = "RS256";
    private static String RESPONSE_TYPE_SUPPORTED_DEFAULT = "code,id_token,token id_token";

    @Test
    public void testGetAuthorizationEndpoint() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getAuthorizationEndpoint());
    }

    @Test
    public void testGetTokenEndpoint() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getTokenEndpoint());
    }

    @Test
    public void testGetUserinfoEndpoint() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getUserinfoEndpoint());
    }

    @Test
    public void testGetEndSessionEndpoint() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getEndSessionEndpoint());
    }

    @Test
    public void testGetJwksURI() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getJwksURI());
    }

    @Test
    public void testGetIssuer() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(EMPTY_DEFAULT, wrapper.getIssuer());
    }

    @Test
    public void testGetSubjectTypeSupported() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(SUBJECT_TYPE_SUPPORTED_DEFAULT, wrapper.getSubjectTypeSupported());
    }

    @Test
    public void testGetIdTokenSigningAlgorithmsSupported() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED_DEFAULT, wrapper.getIdTokenSigningAlgorithmsSupported());
    }

    @Test
    public void testGetResponseTypeSupported() {
        OpenIdProviderMetadata providerMetadata = getInstanceofAnnotation(null);
        OpenIdProviderMetadataWrapper wrapper = new OpenIdProviderMetadataWrapper(providerMetadata);

        assertEquals(RESPONSE_TYPE_SUPPORTED_DEFAULT, wrapper.getResponseTypeSupported());
    }

    private OpenIdProviderMetadata getInstanceofAnnotation(final Map<String, Object> overrides) {
        OpenIdProviderMetadata annotation = new OpenIdProviderMetadata() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String authorizationEndpoint() {
                return (overrides != null && overrides.containsKey(AUTHORIZATION_ENDPOINT)) ? (String) overrides.get(AUTHORIZATION_ENDPOINT) : EMPTY_DEFAULT;
            }

            @Override
            public String tokenEndpoint() {
                return (overrides != null && overrides.containsKey(TOKEN_ENDPOINT)) ? (String) overrides.get(TOKEN_ENDPOINT) : EMPTY_DEFAULT;
            }

            @Override
            public String userinfoEndpoint() {
                return (overrides != null && overrides.containsKey(USERINFO_ENDPOINT)) ? (String) overrides.get(USERINFO_ENDPOINT) : EMPTY_DEFAULT;
            }

            @Override
            public String endSessionEndpoint() {
                return (overrides != null && overrides.containsKey(END_SESSION_ENDPOINT)) ? (String) overrides.get(END_SESSION_ENDPOINT) : EMPTY_DEFAULT;
            }

            @Override
            public String jwksURI() {
                return (overrides != null && overrides.containsKey(JWKS_URI)) ? (String) overrides.get(JWKS_URI) : EMPTY_DEFAULT;
            }

            @Override
            public String issuer() {
                return (overrides != null && overrides.containsKey(ISSUER)) ? (String) overrides.get(ISSUER) : EMPTY_DEFAULT;
            }

            @Override
            public String subjectTypeSupported() {
                return (overrides != null && overrides.containsKey(SUBJECT_TYPE_SUPPORTED)) ? (String) overrides.get(SUBJECT_TYPE_SUPPORTED) : SUBJECT_TYPE_SUPPORTED_DEFAULT;
            }

            @Override
            public String idTokenSigningAlgorithmsSupported() {
                return (overrides != null
                        && overrides.containsKey(ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED)) ? (String) overrides.get(ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED) : ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED_DEFAULT;
            }

            @Override
            public String responseTypeSupported() {
                return (overrides != null && overrides.containsKey(RESPONSE_TYPE_SUPPORTED)) ? (String) overrides.get(RESPONSE_TYPE_SUPPORTED) : RESPONSE_TYPE_SUPPORTED_DEFAULT;
            }

        };

        return annotation;
    }

}