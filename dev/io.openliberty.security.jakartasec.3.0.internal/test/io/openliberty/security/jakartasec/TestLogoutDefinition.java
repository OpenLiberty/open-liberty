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
package io.openliberty.security.jakartasec;

import static io.openliberty.security.jakartasec.JakartaSec30Constants.EMPTY_DEFAULT;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;

public class TestLogoutDefinition {

    protected static String NOTIFY_PROVIDER = "notifyProvider";
    protected static String NOTIFY_PROVIDER_EXPRESSION = "notifyProviderExpression";
    protected static String REDIRECT_URI = "redirectURI";
    protected static String ACCESS_TOKEN_EXPIRY = "accessTokenExpiry";
    protected static String ACCESS_TOKEN_EXPIRY_EXPRESSION = "accessTokenExpiryExpression";
    protected static String IDENTITY_TOKEN_EXPIRY = "identityTokenExpiry";
    protected static String IDENTITY_TOKEN_EXPIRY_EXPRESSION = "identityTokenExpiryExpression";

    protected static LogoutDefinition getInstanceofAnnotation(final Map<String, Object> overrides) {
        LogoutDefinition annotation = new LogoutDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean notifyProvider() {
                return (overrides != null && overrides.containsKey(NOTIFY_PROVIDER)) ? (Boolean) overrides.get(NOTIFY_PROVIDER) : false;
            }

            @Override
            public String notifyProviderExpression() {
                return (overrides != null && overrides.containsKey(NOTIFY_PROVIDER_EXPRESSION)) ? (String) overrides.get(NOTIFY_PROVIDER_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String redirectURI() {
                return (overrides != null && overrides.containsKey(REDIRECT_URI)) ? (String) overrides.get(REDIRECT_URI) : EMPTY_DEFAULT;
            }

            @Override
            public boolean accessTokenExpiry() {
                return (overrides != null && overrides.containsKey(ACCESS_TOKEN_EXPIRY)) ? (Boolean) overrides.get(ACCESS_TOKEN_EXPIRY) : false;
            }

            @Override
            public String accessTokenExpiryExpression() {
                return (overrides != null && overrides.containsKey(ACCESS_TOKEN_EXPIRY_EXPRESSION)) ? (String) overrides.get(ACCESS_TOKEN_EXPIRY_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean identityTokenExpiry() {
                return (overrides != null && overrides.containsKey(IDENTITY_TOKEN_EXPIRY)) ? (Boolean) overrides.get(IDENTITY_TOKEN_EXPIRY) : false;
            }

            @Override
            public String identityTokenExpiryExpression() {
                return (overrides != null && overrides.containsKey(IDENTITY_TOKEN_EXPIRY_EXPRESSION)) ? (String) overrides.get(IDENTITY_TOKEN_EXPIRY_EXPRESSION) : EMPTY_DEFAULT;
            }

        };

        return annotation;
    }

}