/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

public class TestOpenIdAuthenticationMechanismDefinition {

    public static final String PROVIDER_URI = "providerURI";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String REDIRECT_URI = "redirectURI";
    public static final String REDIRECT_TO_ORIGINAL_RESOURCE = "redirectToOriginalResource";
    public static final String REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION = "redirectToOriginalResourceExpression";
    public static final String SCOPE = "scope";
    public static final String SCOPE_EXPRESSION = "scopeExpression";
    public static final String RESPONSE_TYPE = "responseType";
    public static final String RESPONSE_MODE = "responseMode";
    public static final String PROMPT = "prompt";
    public static final String PROMPT_EXPRESSION = "promptExpression";
    public static final String DISPLAY = "display";
    public static final String DISPLAY_EXPRESSION = "displayExpression";
    public static final String USE_NONCE = "useNonce";
    public static final String USE_NONCE_EXPRESSION = "useNonceExpression";
    public static final String USE_SESSION = "useSession";
    public static final String USE_SESSION_EXPRESSION = "useSessionExpression";
    public static final String EXTRA_PARAMETERS = "extraParameters";
    public static final String EXTRA_PARAMETERS_EXPRESSION = "extraParametersExpression";
    public static final String JWKS_CONNECT_TIMEOUT = "jwksConnectTimeout";
    public static final String JWKS_CONNECT_TIMEOUT_EXPRESSION = "jwksConnectTimeoutExpression";
    public static final String JWKS_READ_TIMEOUT = "jwksReadTimeout";
    public static final String JWKS_READ_TIMEOUT_EXPRESSION = "jwksReadTimeoutExpression";
    public static final String TOKEN_AUTO_REFRESH = "tokenAutoRefresh";
    public static final String TOKEN_AUTO_REFRESH_EXPRESSION = "tokenAutoRefreshExpression";
    public static final String TOKEN_MIN_VALIDITY = "tokenMinValidity";
    public static final String TOKEN_MIN_VALIDITY_EXPRESSION = "tokenMinValidityExpression";
    public static final String EMPTY_DEFAULT = "";
    public static final int TIMEOUT_DEFAULT = 500;
    public static final int TOKEN_MIN_VALIDITY_DEFAULT = 10000;
    public static final String[] SCOPE_DEFAULT = new String[] { OpenIdConstant.OPENID_SCOPE,
                                                                OpenIdConstant.EMAIL_SCOPE,
                                                                OpenIdConstant.PROFILE_SCOPE };
    private static final String PROVIDER_METADATA = "providerMetadata";

    public static OpenIdAuthenticationMechanismDefinition getInstanceofAnnotation(final Map<String, Object> overrides) {
        OpenIdAuthenticationMechanismDefinition annotation = new OpenIdAuthenticationMechanismDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String providerURI() {
                return (overrides != null && overrides.containsKey(PROVIDER_URI)) ? (String) overrides.get(PROVIDER_URI) : EMPTY_DEFAULT;
            }

            @Override
            public OpenIdProviderMetadata providerMetadata() {
                return (overrides != null
                        && overrides.containsKey(PROVIDER_METADATA)) ? TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation((Map<String, Object>) overrides.get(PROVIDER_METADATA)) : TestOpenIdProviderMetadataDefinition.getInstanceofAnnotation(null);
            }

            @Override
            public String clientId() {
                return (overrides != null && overrides.containsKey(CLIENT_ID)) ? (String) overrides.get(CLIENT_ID) : EMPTY_DEFAULT;
            }

            @Override
            public String clientSecret() {
                return (overrides != null && overrides.containsKey(CLIENT_SECRET)) ? (String) overrides.get(CLIENT_SECRET) : EMPTY_DEFAULT;
            }

            @Override
            public ClaimsDefinition claimsDefinition() {
                return TestClaimsDefinition.getInstanceofAnnotation(null);
            }

            @Override
            public LogoutDefinition logout() {
                return TestLogoutDefinition.getInstanceofAnnotation(null);
            }

            @Override
            public String redirectURI() {
                return (overrides != null && overrides.containsKey(REDIRECT_URI)) ? (String) overrides.get(REDIRECT_URI) : "${baseURL}/Callback";
            }

            @Override
            public boolean redirectToOriginalResource() {
                return (overrides != null && overrides.containsKey(REDIRECT_TO_ORIGINAL_RESOURCE)) ? (Boolean) overrides.get(REDIRECT_TO_ORIGINAL_RESOURCE) : false;
            }

            @Override
            public String redirectToOriginalResourceExpression() {
                return (overrides != null
                        && overrides.containsKey(REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION)) ? (String) overrides.get(REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String[] scope() {
                return (overrides != null && overrides.containsKey(SCOPE)) ? (String[]) overrides.get(SCOPE) : SCOPE_DEFAULT;
            }

            @Override
            public String scopeExpression() {
                return (overrides != null && overrides.containsKey(SCOPE_EXPRESSION)) ? (String) overrides.get(SCOPE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String responseType() {
                return (overrides != null && overrides.containsKey(RESPONSE_TYPE)) ? (String) overrides.get(RESPONSE_TYPE) : OpenIdConstant.CODE;
            }

            @Override
            public String responseMode() {
                return (overrides != null && overrides.containsKey(RESPONSE_MODE)) ? (String) overrides.get(RESPONSE_MODE) : EMPTY_DEFAULT;
            }

            @Override
            public PromptType[] prompt() {
                return (overrides != null && overrides.containsKey(PROMPT)) ? (PromptType[]) overrides.get(PROMPT) : new PromptType[] {};
            }

            @Override
            public String promptExpression() {
                return (overrides != null && overrides.containsKey(PROMPT_EXPRESSION)) ? (String) overrides.get(PROMPT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public DisplayType display() {
                return (overrides != null && overrides.containsKey(DISPLAY)) ? (DisplayType) overrides.get(DISPLAY) : DisplayType.PAGE;
            }

            @Override
            public String displayExpression() {
                return (overrides != null && overrides.containsKey(DISPLAY_EXPRESSION)) ? (String) overrides.get(DISPLAY_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean useNonce() {
                return (overrides != null && overrides.containsKey(USE_NONCE)) ? (Boolean) overrides.get(USE_NONCE) : true;
            }

            @Override
            public String useNonceExpression() {
                return (overrides != null && overrides.containsKey(USE_NONCE_EXPRESSION)) ? (String) overrides.get(USE_NONCE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean useSession() {
                return (overrides != null && overrides.containsKey(USE_SESSION)) ? (Boolean) overrides.get(USE_SESSION) : true;
            }

            @Override
            public String useSessionExpression() {
                return (overrides != null && overrides.containsKey(USE_SESSION_EXPRESSION)) ? (String) overrides.get(USE_SESSION_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String[] extraParameters() {
                return (overrides != null && overrides.containsKey(EXTRA_PARAMETERS)) ? (String[]) overrides.get(EXTRA_PARAMETERS) : new String[] {};
            }

            @Override
            public String extraParametersExpression() {
                return (overrides != null && overrides.containsKey(EXTRA_PARAMETERS_EXPRESSION)) ? (String) overrides.get(EXTRA_PARAMETERS_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int jwksConnectTimeout() {
                return (overrides != null && overrides.containsKey(JWKS_CONNECT_TIMEOUT)) ? (Integer) overrides.get(JWKS_CONNECT_TIMEOUT) : TIMEOUT_DEFAULT;
            }

            @Override
            public String jwksConnectTimeoutExpression() {
                return (overrides != null && overrides.containsKey(JWKS_CONNECT_TIMEOUT_EXPRESSION)) ? (String) overrides.get(JWKS_CONNECT_TIMEOUT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int jwksReadTimeout() {
                return (overrides != null && overrides.containsKey(JWKS_READ_TIMEOUT)) ? (Integer) overrides.get(JWKS_READ_TIMEOUT) : TIMEOUT_DEFAULT;
            }

            @Override
            public String jwksReadTimeoutExpression() {
                return (overrides != null && overrides.containsKey(JWKS_READ_TIMEOUT_EXPRESSION)) ? (String) overrides.get(JWKS_READ_TIMEOUT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean tokenAutoRefresh() {
                return (overrides != null && overrides.containsKey(TOKEN_AUTO_REFRESH)) ? (Boolean) overrides.get(TOKEN_AUTO_REFRESH) : false;
            }

            @Override
            public String tokenAutoRefreshExpression() {
                return (overrides != null && overrides.containsKey(TOKEN_AUTO_REFRESH_EXPRESSION)) ? (String) overrides.get(TOKEN_AUTO_REFRESH_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int tokenMinValidity() {
                return (overrides != null && overrides.containsKey(TOKEN_MIN_VALIDITY)) ? (Integer) overrides.get(TOKEN_MIN_VALIDITY) : TOKEN_MIN_VALIDITY_DEFAULT;
            }

            @Override
            public String tokenMinValidityExpression() {
                return (overrides != null && overrides.containsKey(TOKEN_MIN_VALIDITY_EXPRESSION)) ? (String) overrides.get(TOKEN_MIN_VALIDITY_EXPRESSION) : EMPTY_DEFAULT;
            }

        };

        return annotation;
    }

}
