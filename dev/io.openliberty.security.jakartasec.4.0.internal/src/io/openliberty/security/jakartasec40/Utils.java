/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

import jakarta.security.enterprise.identitystore.IdentityStore.ValidationType;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;

/**
 * A class to assist with generating actual instances of an @InMemoryIdentityStoreDefinition,
 * given a simple map of name (String) to value (Object) values.
 *
 * Two separate static methods are created, one to allow the return of a @Credentials structure,
 * and the second to create the in memory identity store structure.
 *
 *
 */
public class Utils {

    public static final String CALLER_NAME_NAME = "callerName";
    public static final String GROUPS_NAME = "groups";
    public static final String PASSWORD_NAME = "password";

    public static InMemoryIdentityStoreDefinition.Credentials getInstanceOfCredentialsAnnotation(final Map<String, Object> overrides) {
        InMemoryIdentityStoreDefinition.Credentials annotation = new InMemoryIdentityStoreDefinition.Credentials() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String callerName() {
                return overrides == null ? "" : (String) overrides.getOrDefault(CALLER_NAME_NAME, "");
            }

            @Override
            public String[] groups() {
                return overrides == null ? JakartaSec40Constants.SPEC_DEFAULT_GROUPS : ((String[]) overrides.getOrDefault(GROUPS_NAME,
                                                                                                                          JakartaSec40Constants.SPEC_DEFAULT_GROUPS)).clone();
            }

            @Override
            public @Sensitive String password() {
                return overrides == null ? "" : (String) overrides.getOrDefault(PASSWORD_NAME, "");
            }

        };

        return annotation;
    }

    public static InMemoryIdentityStoreDefinition getInstanceOfInMemoryAnnotation(final Map<String, Object> overrides) {
        InMemoryIdentityStoreDefinition annotation = new InMemoryIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public int priority() {
                return overrides == null ? JakartaSec40Constants.SPEC_DEFAULT_PRIORITY : (Integer) overrides.getOrDefault(JavaEESecConstants.PRIORITY,
                                                                                                                          JakartaSec40Constants.SPEC_DEFAULT_PRIORITY);
            }

            @Override
            public String priorityExpression() {
                return overrides == null ? JakartaSec40Constants.SPEC_DEFAULT_PRIORITYEXPRESSION : (String) overrides.getOrDefault(JavaEESecConstants.PRIORITY_EXPRESSION,
                                                                                                                                   JakartaSec40Constants.SPEC_DEFAULT_PRIORITYEXPRESSION);
            }

            @Override
            public ValidationType[] useFor() {
                return overrides == null ? JakartaSec40Constants.SPEC_DEFAULT_USEFOR : ((ValidationType[]) overrides.getOrDefault(JavaEESecConstants.USE_FOR,
                                                                                                                                  JakartaSec40Constants.SPEC_DEFAULT_USEFOR)).clone();
            }

            @Override
            public String useForExpression() {
                return overrides == null ? JakartaSec40Constants.SPEC_DEFAULT_USEFOREXPRESSION : (String) overrides.getOrDefault(JavaEESecConstants.USE_FOR_EXPRESSION,
                                                                                                                                 JakartaSec40Constants.SPEC_DEFAULT_USEFOREXPRESSION);
            }

            @Override
            public Credentials[] value() {
                return overrides == null ? new Credentials[] {} : ((Credentials[]) overrides.getOrDefault(JakartaSec40Constants.VALUE,
                                                                                                          new Credentials[] {})).clone();
            }
        };
        return annotation;
    }
}
