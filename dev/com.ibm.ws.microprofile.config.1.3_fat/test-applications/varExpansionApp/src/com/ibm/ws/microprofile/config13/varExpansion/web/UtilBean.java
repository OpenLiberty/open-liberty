/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config13.varExpansion.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@ApplicationScoped
public class UtilBean {

    private final Config config;

    public UtilBean() {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        config = builder.build();
    }

    public void getAndCheckVarValue(String key, String expectedValue) {
        String value = config.getOptionalValue(key, String.class).orElse("not there");
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValue + ", Found: " + value);
        assertEquals("Incorrect value found", expectedValue, value);
    }

    public void getAndCheckVarValue(String key, String[] expectedValues) {
        String[] values = config.getOptionalValue(key, String[].class).orElse(new String[0]);
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + java.util.Arrays.toString(expectedValues) + ", Found: " + java.util.Arrays.toString(values));
        assertArrayEquals("Incorrect value found", expectedValues, values);
    }

    public void getAndCheckVarValue(String key, List<String> expectedValues) {
        List<String> values = config.getOptionalValue(key, String[].class)
                        .map(java.util.Arrays::asList)
                        .orElse(java.util.Collections.emptyList());
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValues + ", Found: " + values);
        assertEquals("Incorrect value found", expectedValues, values);
    }

    public void getAndCheckVarValueContains(String key, String expectedValue) {
        String value = config.getOptionalValue(key, String.class).orElse("not there");
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValue + ", Found: " + value);
        assertTrue("Incorrect value found", value.contains(expectedValue));
    }
}
