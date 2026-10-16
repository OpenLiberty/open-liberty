/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class ConfigChecker {

    @Inject
    private Config config;

    public void assertConfigPropertyEquals(String name, String expectedValue) {
        String value = config.getOptionalValue(name, String.class).orElse("not there");
        assertEquals("Incorrect value found for name: " + name, expectedValue, value);
    }

    public void assertConfigPropertyEquals(String name, String[] expectedValues) {
        String[] values = config.getOptionalValue(name, String[].class).orElse(new String[0]);
        assertArrayEquals("Incorrect value found for name: " + name, expectedValues, values);
    }

    public void assertConfigPropertyContains(String name, String expectedValue) {
        String value = config.getOptionalValue(name, String.class).orElse("not there");
        assertTrue("Incorrect value found  for name: " + name, value.contains(expectedValue));
    }

    public void assertConfigPropertyNotPresent(String name) {
        Optional<String> value = config.getOptionalValue(name, String.class);
        assertFalse("Found " + value.get() + " for name " + name + " but expected an empty Optional", value.isPresent());
    }
}
