/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.varExpansion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class ConfigChecker {

    @Inject
    private Config config;

    public void assertConfigPropertyEquals(String name, String[] expectedValues) {
        String[] values = config.getOptionalValue(name, String[].class).orElse(new String[0]);
        assertArrayEquals("Incorrect value found for name: " + name, expectedValues, values);
    }

    public void assertConfigPropertyEquals(String name, List<String> expectedValues) {
        List<String> values = config.getOptionalValues(name, String.class).orElse(Collections.emptyList());
        assertEquals("Incorrect value found for name: " + name, expectedValues, values);
    }

}
