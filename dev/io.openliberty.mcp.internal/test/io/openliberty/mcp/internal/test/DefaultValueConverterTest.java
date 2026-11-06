/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import io.openliberty.mcp.internal.ConverterRegistry;
import io.openliberty.mcp.internal.ToolRegistry.ToolArgumentImpl;
import io.openliberty.mcp.internal.requests.DefaultValueResolver;
import io.openliberty.mcp.internal.testutils.TestUtils;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;

public class DefaultValueConverterTest {
    public record City(String name, String country, int population, boolean isCapital) {};

    public record Person(String name, int age) {};

    public static class CityConverter implements DefaultValueConverter<City> {

        /**
         * Converts a default value string in the format "Name::Country::Population::IsCapital" to a {@link City} object
         * Example string: "Manchester::England::8000::false"
         */
        @Override
        public City convert(String defaultValue) {
            String[] fields = defaultValue.split("::");
            if (fields.length != 4) {
                throw new IllegalArgumentException();
            }
            String name = fields[0];
            String country = fields[1];
            int population = Integer.parseInt(fields[2]);
            boolean isCapital = Boolean.parseBoolean(fields[3]);
            return new City(name, country, population, isCapital);
        }

    }

    private static ConverterRegistry testConverterRegistry;

    @BeforeClass
    public static void setup() {
        testConverterRegistry = TestUtils.createTestConverterRegistry();
        testConverterRegistry.addConverter(City.class, new CityConverter());
    }

    @Test
    public void testArgumentDefaultValueIntTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("year", "Integer value", false, Integer.class, "2025");
        assertThat(DefaultValueResolver.resolveDefaultValue("testTool", argMetadata, testConverterRegistry), equalTo(2025));
    }

    @Test
    public void testArgumentDefaultValueStringTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("planet", "String value", false, String.class, "Jupiter");
        assertThat(DefaultValueResolver.resolveDefaultValue("defaultValueString", argMetadata, testConverterRegistry), equalTo("Jupiter"));
    }

    @Test
    public void testArgumentDefaultValueCharTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("initial", "Char value", false, Character.class, "H");
        assertThat(DefaultValueResolver.resolveDefaultValue("defaultValueChar", argMetadata, testConverterRegistry), equalTo('H'));
    }

    @Test
    public void testArgumentDefaultValueInvalidCharTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("initial", "Char value", false, Character.class, "HH");
        assertThrows(() -> DefaultValueResolver.resolveDefaultValue("defaultValueInvalidChar", argMetadata, testConverterRegistry),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0020E: The default value of the initial argument of the defaultValueInvalidChar MCP tool cannot be converted to the class java.lang.Character type. The value is HH. The error is java.lang.IllegalArgumentException: CWMCM0021E: A character default value must be exactly one character, but was HH."));
    }

    @Test
    public void testArgumentDefaultValueInvalidLongTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("count", "Long value", false, Long.class, "notANumber");
        assertThrows(() -> DefaultValueResolver.resolveDefaultValue("defaultValueInvalidLong", argMetadata, testConverterRegistry),
                     exception()
                                .ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0020E: The default value of the count argument of the defaultValueInvalidLong MCP tool cannot be converted to the class java.lang.Long type. The value is notANumber. The error is java.lang.NumberFormatException: For input string: \"notANumber\""));
    }

    @Test
    public void testArgumentDefaultValueBooleanTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("bool", "Bool value", false, Boolean.class, "true");
        assertThat(DefaultValueResolver.resolveDefaultValue("defaultValueBool", argMetadata, testConverterRegistry), equalTo(true));
    }

    @Test
    public void testArgumentDefaultValueCustomObjectTypeConversion() {
        ToolArgument argMetadata = new ToolArgumentImpl("city", "City value", false, City.class, "Manchester::England::8000::false");
        assertThat(DefaultValueResolver.resolveDefaultValue("defaultValueObj", argMetadata, testConverterRegistry),
                   equalTo(new City("Manchester", "England", 8000, false)));
    }

    @Test
    public void testArgumentDefaultValueWithoutConverter() {
        ToolArgument argMetadata = new ToolArgumentImpl("person", "Person value", false, Person.class, "Joe::35");
        assertThrows(() -> DefaultValueResolver.resolveDefaultValue("defaultValueObjWithoutConverter", argMetadata, testConverterRegistry),
                     exception().ofType(IllegalArgumentException.class));
    }

    @Test
    public void testArgumentNullDefaultValueReturnsNull() {
        // null defaultValue on a programmatically registered argument must not cause a NullPointerException in DefaultValueResolver.
        ToolArgument argMetadata = new ToolArgumentImpl("value", "String value", false, String.class, null);
        assertThat(DefaultValueResolver.resolveDefaultValue("nullDefaultValue", argMetadata, testConverterRegistry), equalTo(null));
    }

}
