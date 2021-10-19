/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.cache;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import io.openliberty.microprofile.openapi20.cache.ConfigSerializer.ConfigField;
import io.smallrye.openapi.api.OpenApiConfig;

public class ConfigSerializerTest {

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
     * This should only fail if the version of smallrye-open-api is updated and ConfigSerializer is not updated to match.
     */
    @Test
    public void testCoverage() {
        HashSet<String> configFieldNames = new HashSet<>();
        for (ConfigField field : ConfigSerializer.ConfigField.values()) {
            configFieldNames.add(field.name);
        }

        HashSet<String> missingFields = new HashSet<>();
        for (Method m : OpenApiConfig.class.getMethods()) {
            if (!METHODS_TO_IGNORE.contains(m.getName()) && !configFieldNames.remove(m.getName())) {
                missingFields.add(m.getName());
            }
        }

        assertThat("OpenApiConfig methods missing from ConfigField enum", missingFields, is(empty()));
        assertThat("ConfigField entries which don't have a matching method in OpenApiConfig", configFieldNames, is(empty()));
    }
}
