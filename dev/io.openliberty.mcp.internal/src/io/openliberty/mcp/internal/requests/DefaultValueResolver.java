/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;

/**
 * Resolves an argument default value for a tool method parameter
 */
public class DefaultValueResolver {

    private static final TraceComponent tc = Tr.register(DefaultValueResolver.class);
    private static Map<Class<?>, Object> TYPE_DEFAULTS_MAP = Map.of(
                                                                    boolean.class, false,
                                                                    char.class, '\0',
                                                                    byte.class, (byte) 0,
                                                                    short.class, (short) 0,
                                                                    int.class, 0,
                                                                    long.class, 0L,
                                                                    float.class, 0f,
                                                                    double.class, 0d);

    /**
     * Resolve the default value for a missing argument
     *
     * If defaultValue is set, convert it using DefaultValueConverter
     * If type is Optional, return Optional.empty()
     * If type is primitive, return primitive default
     * Otherwise return null
     *
     * @param argMetadata the tool argument metadata
     * @return the default value, converted to the argument type
     * @throws IllegalArgumentException if default value conversion fails
     */
    public static Object resolveDefaultValue(ToolArgument argMetadata) {
        if (!argMetadata.defaultValue().isEmpty()) {
            try {
                return convertDefaultValue(argMetadata.defaultValue(), argMetadata.name(), argMetadata.type());
            } catch (Exception e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0020E.defaultvalue.conversion.error",
                                                                    null,
                                                                    argMetadata.name(),
                                                                    argMetadata.type(),
                                                                    argMetadata.defaultValue(), e),
                                                   e);
            }
        }

        if (isOptionalType(argMetadata.type())) {
            return Optional.empty();
        }

        if (isPrimitiveType(argMetadata.type())) {
            return getPrimitiveDefault(argMetadata.type());
        }

        return null;
    }

    private static boolean isPrimitiveType(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isPrimitive();
        }
        return false;
    }

    public static boolean isOptionalType(Type type) {
        if (type instanceof Class<?>) {
            return Optional.class.equals(type);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return Optional.class.equals(parameterizedType.getRawType());
        }
        return false;
    }

    private static Object getPrimitiveDefault(Type type) {
        return TYPE_DEFAULTS_MAP.get(type);
    }

    private static Object convertDefaultValue(String defaultValue, String argName, Type type) {
        Type boxedType = TypeUtility.box(type);

        DefaultValueConverter<?> converter = BuiltinDefaultValueConverters.CONVERTERS.get(boxedType);

        if (converter == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0017E.missing.toolarg.defaultvalue.converter",
                                                                null,
                                                                argName,
                                                                type));
        }

        return converter.convert(defaultValue);
    }
}
