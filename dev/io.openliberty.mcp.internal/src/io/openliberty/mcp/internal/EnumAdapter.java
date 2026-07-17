/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * Adapts an enum type to a String
 * <p>
 * Note this must be abstract because Jsonb needs to be able to extract the types
 * from class definition, which it can't do if it has type variables.
 *
 * @param <T> the enum type
 */
public abstract class EnumAdapter<T extends Enum<T>> implements JsonbAdapter<T, String> {
    private String[] stringValues;
    private T[] enumConstants;

    /**
     * Create an enum adapter for the given type
     *
     * @param enumClass the enum class
     * @param stringifyFunction function to convert an enum value into its string representation
     */
    public EnumAdapter() {
        Class<T> enumClass = getEnumClass();
        enumConstants = enumClass.getEnumConstants();
        stringValues = new String[enumConstants.length];

        for (T value : enumClass.getEnumConstants()) {
            String string = stringify(value);
            stringValues[value.ordinal()] = string;
        }
    }

    protected abstract String stringify(T value);

    protected abstract Class<T> getEnumClass();

    @Override
    public T adaptFromJson(String name) {
        for (int i = 0; i < stringValues.length; i++) {
            if (stringValues[i].equals(name)) {
                return enumConstants[i];
            }
        }
        return null;
    }

    @Override
    public String adaptToJson(T value) {
        return stringValues[value.ordinal()];
    }
}
