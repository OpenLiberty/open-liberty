/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;

import io.openliberty.mcp.messaging.Cancellation;

public enum SpecialArgumentType {
    CANCELLATION(Cancellation.class),
    UNSUPPORTED(Object.class);

    private final Class<?> typeClass;

    SpecialArgumentType(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public static Resolution fromClass(Type type) {
        Class<?> clazz = extractClass(type);
        if (clazz == null) {
            return new Resolution(UNSUPPORTED, Object.class);
        }
        for (SpecialArgumentType specialArgType : values()) {
            if (specialArgType.typeClass.equals(clazz)) {
                return new Resolution(specialArgType, clazz);
            }
        }
        return new Resolution(UNSUPPORTED, clazz);
    }

    private static Class<?> extractClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        return null;
    }

    public record Resolution(SpecialArgumentType specialArgsType, Class<?> actualClass) {
        @Override
        public String toString() {
            if (specialArgsType == UNSUPPORTED) {
                return "UNSUPPORTED(" + actualClass.getName() + ")";
            }
            return specialArgsType.name() + "(" + actualClass.getName() + ")";
        }
    }
}
