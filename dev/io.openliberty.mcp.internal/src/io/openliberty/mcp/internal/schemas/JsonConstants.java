/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

/**
 * Constants for creating JSON schemas, mostly keywords
 */
public class JsonConstants {

    public static final String TYPE = "type";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_OBJECT = "object";
    public static final String REF = "$ref";
    public static final String DEFS = "$defs";
    public static final String REQUIRED = "required";
    public static final String PROPERTIES = "properties";
    public static final String DESCRIPTION = "description";
    public static final String ENUM = "enum";
    public static final String ITEMS = "items";
    public static final String ARRAY = "array";
    public static final String PROPERTY_NAMES = "propertyNames";
    public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String ALL_OF = "allOf";

    public static final Set<Type> JSON_STRING_TYPES = Set.of(String.class, Character.class, char.class);
    public static final Set<Type> JSON_INT_TYPES = Set.of(Integer.class, int.class,
                                                          Long.class, long.class,
                                                          Short.class, short.class,
                                                          Byte.class, byte.class,
                                                          BigInteger.class);
    public static final Set<Type> JSON_BOOLEAN_TYPES = Set.of(Boolean.class, boolean.class);
    public static final Set<Type> JSON_NUMBER_TYPES = Set.of(Number.class,
                                                             Float.class, float.class,
                                                             Double.class, double.class,
                                                             BigDecimal.class);

}
