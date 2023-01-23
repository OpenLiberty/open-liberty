/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec;

import static io.openliberty.security.jakartasec.JakartaSec30Constants.EMPTY_DEFAULT;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

public class TestOpenIdProviderMetadataDefinition {

    protected static String AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
    protected static String TOKEN_ENDPOINT = "tokenEndpoint";
    protected static String USERINFO_ENDPOINT = "userinfoEndpoint";
    protected static String END_SESSION_ENDPOINT = "endSessionEndpoint";
    protected static String JWKS_URI = "jwksURI";
    protected static String ISSUER = "issuer";
    protected static String SUBJECT_TYPE_SUPPORTED = "subjectTypeSupported";
    protected static String ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED = "idTokenSigningAlgorithmsSupported";
    protected static String RESPONSE_TYPE_SUPPORTED = "responseTypeSupported";
    protected static String SUBJECT_TYPE_SUPPORTED_DEFAULT = JakartaSec30Constants.SUBJECT_TYPE_SUPPORTED_DEFAULT;
    protected static String RESPONSE_TYPE_SUPPORTED_DEFAULT = JakartaSec30Constants.RESPONSE_TYPE_SUPPORTED_DEFAULT;

    protected static OpenIdProviderMetadata getInstanceofAnnotation(final Map<String, Object> overrides) {
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
                        && overrides.containsKey(ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED)) ? (String) overrides.get(ID_TOKEN_SIGNING_ALGORITHMS_SUPPORTED) : OpenIdConstant.DEFAULT_JWT_SIGNED_ALGORITHM;
            }

            @Override
            public String responseTypeSupported() {
                return (overrides != null && overrides.containsKey(RESPONSE_TYPE_SUPPORTED)) ? (String) overrides.get(RESPONSE_TYPE_SUPPORTED) : RESPONSE_TYPE_SUPPORTED_DEFAULT;
            }

        };

        return annotation;
    }
}