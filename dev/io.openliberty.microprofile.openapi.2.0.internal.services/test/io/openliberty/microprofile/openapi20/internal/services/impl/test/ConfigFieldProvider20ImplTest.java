/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.services.impl.test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.services.ConfigField;
import io.openliberty.microprofile.openapi20.internal.services.impl.ConfigFieldProvider20Impl;
import io.smallrye.openapi.api.OpenApiConfig;

public class ConfigFieldProvider20ImplTest {

    /**
     * Methods in OpenApiConfig which should not have a corresponding entry in the ConfigField enum
     */
    public static final Collection<String> METHODS_TO_IGNORE = Arrays.asList("pathServers",
                                                                             "operationServers",
                                                                             "patternOf",
                                                                             "asCsvSet",
                                                                             "doAllowNakedPathParameter");

    /**
     * Test that the ConfigSerializer covers all methods of OpenApiConfig
     * <p>
     * This should only fail if the version of smallrye-open-api is updated and the ConfigFieldProvider is not updated to match.
     */
    @Test
    public void testCoverage() {
        HashSet<String> configFieldMethods = new HashSet<>();
        for (ConfigField field : new ConfigFieldProvider20Impl().getConfigFields()) {
            configFieldMethods.add(field.getMethod());
        }

        HashSet<String> missingFields = new HashSet<>();
        for (Method m : OpenApiConfig.class.getMethods()) {
            if (!METHODS_TO_IGNORE.contains(m.getName()) && !configFieldMethods.remove(m.getName())) {
                missingFields.add(m.getName());
            }
        }

        assertThat("OpenApiConfig methods missing from ConfigField enum", missingFields, is(empty()));
        assertThat("ConfigField entries which don't have a matching method in OpenApiConfig", configFieldMethods, is(empty()));
    }
}
