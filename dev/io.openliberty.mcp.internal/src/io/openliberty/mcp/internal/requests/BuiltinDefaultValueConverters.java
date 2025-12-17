/*******************************************************************************
 * Copyright (c) contributors to https://github.com/quarkiverse/quarkus-mcp-server
 * Copyright (c) 2025 IBM Corporation and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/runtime/BuiltinDefaultValueConverters.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.lang.reflect.Type;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class BuiltinDefaultValueConverters {

    public static final Map<Type, DefaultValueConverter<?>> CONVERTERS = Map.of(
                                                                                Boolean.class, new BooleanConverter(),
                                                                                Byte.class, new ByteConverter(),
                                                                                Short.class, new ShortConverter(),
                                                                                Integer.class, new IntegerConverter(),
                                                                                Long.class, new LongConverter(),
                                                                                Float.class, new FloatConverter(),
                                                                                Double.class, new DoubleConverter(),
                                                                                Character.class, new CharacterConverter(),
                                                                                String.class, new StringConverter());

    public static class BooleanConverter implements DefaultValueConverter<Boolean> {

        @Override
        public Boolean convert(String defaultValue) {
            return Boolean.valueOf(defaultValue);
        }

    }

    public static class ByteConverter implements DefaultValueConverter<Byte> {

        @Override
        public Byte convert(String defaultValue) {
            return Byte.valueOf(defaultValue);
        }

    }

    public static class ShortConverter implements DefaultValueConverter<Short> {

        @Override
        public Short convert(String defaultValue) {
            return Short.valueOf(defaultValue);
        }

    }

    public static class IntegerConverter implements DefaultValueConverter<Integer> {

        @Override
        public Integer convert(String defaultValue) {
            return Integer.valueOf(defaultValue);
        }

    }

    public static class LongConverter implements DefaultValueConverter<Long> {

        @Override
        public Long convert(String defaultValue) {
            return Long.valueOf(defaultValue);
        }

    }

    public static class FloatConverter implements DefaultValueConverter<Float> {

        @Override
        public Float convert(String defaultValue) {
            return Float.valueOf(defaultValue);
        }

    }

    public static class DoubleConverter implements DefaultValueConverter<Double> {

        @Override
        public Double convert(String defaultValue) {
            return Double.valueOf(defaultValue);
        }

    }

    public static class CharacterConverter implements DefaultValueConverter<Character> {
        private static final TraceComponent tc = Tr.register(CharacterConverter.class);

        @Override
        public Character convert(String defaultValue) {
            if (defaultValue == null || defaultValue.length() != 1)
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0021E.defaultvalue.character.invalid.length", defaultValue));
            return Character.valueOf(defaultValue.charAt(0));
        }

    }

    public static class StringConverter implements DefaultValueConverter<String> {

        @Override
        public String convert(String defaultValue) {
            return defaultValue;
        }

    }

}
