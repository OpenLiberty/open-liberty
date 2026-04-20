/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.convertertools.sharedConvertersModule;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.fat.convertertools.sharedConverters.City;
import io.openliberty.mcp.internal.fat.convertertools.sharedConverters.Person;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools for testing that shared converters from EAR/lib are accessible when no custom converters are defined.
 * This module does NOT have any custom converters, so it uses only shared converters from EAR/lib.
 */
@ApplicationScoped
public class SharedConvertersModuleTools {

    @Tool(name = "testBuiltInStringConverter",
          title = "Test Built-in String Converter",
          description = "Tests that built-in String converter works in sharedConvertersModule")
    public String testBuiltInStringConverter(
            @ToolArg(name = "planet", defaultValue = "Mars", required = false) String planet) {
        return planet;
    }

    @Tool(name = "testBuiltInIntConverter",
          title = "Test Built-in Int Converter",
          description = "Tests that built-in Integer converter works in sharedConvertersModule")
    public int testBuiltInIntConverter(
            @ToolArg(name = "year", defaultValue = "2026", required = false) int year) {
        return year;
    }

    @Tool(name = "testCityConverter",
          title = "Test City Converter",
          description = "Tests City converter - sharedConvertersModule uses shared converter (no custom override)")
    public City testCityConverter(
            @ToolArg(name = "city", defaultValue = "Paris::France::2000000", required = false) City city) {
        return city;
    }

    @Tool(name = "testPersonConverter",
          title = "Test Person Converter",
          description = "Tests Person converter - uses shared converter from EAR/lib")
    public Person testPersonConverter(
            @ToolArg(name = "person", defaultValue = "Jane::25", required = false) Person person) {
        return person;
    }

    // Removed testCompanyConverter - EJB JAR isolation is already tested by testEJBJar()
}

// Made with Bob
