/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class InjectionUtils {

    public static Class<?> getActualType(Type genericType) {

        return getActualType(genericType, 0);
    }

    public static Class<?> getActualType(Type genericType, int pos) {

        if (genericType == null) {
            return null;
        }
        if (genericType == Object.class) {
            return (Class<?>) genericType;
        }

        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            if (genericType instanceof TypeVariable) {
                genericType = getType(((TypeVariable<?>) genericType).getBounds(), pos);
            } else if (genericType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) genericType;
                Type[] bounds = wildcardType.getLowerBounds();
                if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                }
                genericType = getType(bounds, pos);
            } else if (genericType instanceof GenericArrayType) {
                genericType = ((GenericArrayType) genericType).getGenericComponentType();
            }
            Class<?> cls = null;
            if (!(genericType instanceof ParameterizedType)) {
                cls = (Class<?>) genericType;
            } else {
                cls = (Class<?>) ((ParameterizedType) genericType).getRawType();
            }
            return cls.isArray() ? cls.getComponentType() : cls;

        }
        ParameterizedType paramType = (ParameterizedType) genericType;
        Type t = getType(paramType.getActualTypeArguments(), pos);
        return t instanceof Class ? (Class<?>) t : getActualType(t, 0);
    }

    public static Type getType(Type[] types, int pos) {
        if (pos >= types.length) {
            throw new RuntimeException("No type can be found at position " + pos);
        }
        return types[pos];
    }

}
