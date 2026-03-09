/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.defaultValueApp;

import java.util.logging.Logger;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

@ApplicationScoped
public class DefaultValueApp {

    private static final Logger LOG = Logger.getLogger(DefaultValueApp.class.getName());

    @Tool(name = "testToolArgStringDefaultValue", title = "ToolArg String Default Value", description = "Test tool defaults to default value when argument not provided")
    public String testToolArgStringDefaultValue(@ToolArg(name = "planet", description = "planet you live in", required = false, defaultValue = "Jupiter") String planet) {
        return planet;
    }

    @Tool(name = "testToolArgIntDefaultValue", title = "ToolArg Int Default Value", description = "Test tool defaults to default value when argument not provided")
    public int testToolArgIntDefaultValue(@ToolArg(name = "year", description = "current year", required = false, defaultValue = "2025") int year) {
        return year;
    }

    @Tool(name = "testToolArgCustomTypeDefaultValue", title = "ToolArg Custom Type Default Value", description = "Test tool defaults to default value when argument not provided")
    public City testToolArgCustomTypeDefaultValue(@ToolArg(name = "city", description = "City object value", required = false,
                                                           defaultValue = "Manchester::England::8000::false") City city) {
        return city;
    }

    @Tool(name = "testToolArgCustomTypeDefaultValueWithInheritedConverter", title = "Create a House",
          description = "Test tool defaults to default value when argument not provided")
    public House testObjectWithInheritedConverterResponse(@ToolArg(name = "house", description = "House object value", required = false,
                                                                   defaultValue = "5::London Road") House house) {
        return house;
    }

    @Tool(name = "testMultipleToolArgsOneDefaultValue", title = "testMultipleToolArgsOneDefaultValue", description = "MultipleToolArgsOneDefaultValue")
    public String testMultipleToolArgsOneDefaultValue(@ToolArg(name = "planet", description = "planet you live in", required = false, defaultValue = "Jupiter") String planet,
                                                      @ToolArg(name = "year", description = "current year") int year) {
        return "Planet " + planet + " was created in the year " + year;
    }

    @Tool(name = "testDependentBeanCustomConverter", title = "ToolArg Default Value with Dependent Bean Custom Converter",
          description = "Test custom converter is not destroyed if registered with a dependent bean")
    public Person testDependentBeanCustomConverter(@ToolArg(name = "person", description = "Person object value", required = false,
                                                            defaultValue = "Joe::25") Person person) {
        return person;
    }

    public static record House(int number, String street) {};

    public static record Person(String name, int age) {};

    public static record City(String name, String country, int population, boolean isCapital) {};

    public static abstract class AbstractConverter<T> implements DefaultValueConverter<T> {

        @Override
        public abstract T convert(String defaultValue);
    }

    @ApplicationScoped
    public static class HouseConverter extends AbstractConverter<House> {
        /**
         * Converts a default value string in the format "Number::Street" to a {@link House} object
         * Example string: "5::London Road"
         */
        @Override
        public House convert(String defaultValue) {
            String[] fields = defaultValue.split("::");
            if (fields.length != 2) {
                throw new IllegalArgumentException();
            }
            int number = Integer.parseInt(fields[0]);
            String street = fields[1];
            return new House(number, street);
        }
    }

    @ApplicationScoped
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
            LOG.info("[CityConverter] City converter with LOWER priority used");
            return new City(name, country, population, isCapital);
        }

    }

    @ApplicationScoped
    @Priority(5)
    public static class PriorityCityConverter implements DefaultValueConverter<City> {

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

            LOG.info("[PriorityCityConverter] City converter with HIGHER priority used");
            return new City(name, country, population, isCapital);
        }

    }

    @Dependent
    public static class PersonConverterDependentBean implements DefaultValueConverter<Person> {

        /**
         * Converts a default value string in the format "Name::Age" to a {@link City} object
         * Example string: "Joe::25"
         */
        @Override
        public Person convert(String defaultValue) {
            String[] fields = defaultValue.split("::");
            if (fields.length != 2) {
                throw new IllegalArgumentException();
            }
            String name = fields[0];
            int age = Integer.parseInt(fields[1]);
            return new Person(name, age);
        }

        @PreDestroy
        public void cleanup() {
            LOG.info("[PersonConverterDependentBean] PreDestroy called");
        }

    }

    @ApplicationScoped
    public static class InvalidNoParameterTypeCustomConverter implements DefaultValueConverter {

        @Override
        public Object convert(String defaultValue) {
            return 0;
        }

    }

    @Dependent
    public static class InvalidGenericConverter<T> implements DefaultValueConverter<T> {

        @Override
        public T convert(String defaultValue) {
            return (T) defaultValue;
        }

    }

}
