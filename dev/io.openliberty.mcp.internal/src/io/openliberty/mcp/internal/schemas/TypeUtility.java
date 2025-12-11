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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility methods for inspecting types
 */
public class TypeUtility {

    /**
     * The key and value type of a {@code Map}
     *
     * @param key the key type
     * @param value the value type
     */
    public record MapTypes(Type key, Type value) {};

    /**
     * Given a type which extends {@code Map<K, V>}, find the parameter types {@code K} and {@code V}.
     * <p>
     * If {@code rootType} is a parameterized type, then {@code K} and {@code V} may be type variables of {@code rootType}.
     *
     * @param rootType the type that extends {@code Map}
     * @return the parameter types of {@code Map}
     */
    public static MapTypes getMapTypes(Type rootType) {
        List<Type> route = getRouteToType(rootType, Map.class);
        Type[] resolvedParameters = resolveTypeArguments(route);
        return new MapTypes(resolvedParameters[0], resolvedParameters[1]);
    }

    /**
     * Given a type which extends {@code Collection<T>}, find the parameter type {@code T}.
     * <p>
     * If {@code rootType} is a parameterized type, then {@code T} may be type variables of {@code rootType}.
     *
     * @param rootType the type that extends {@code Collection}
     * @return the parameter type of {@code Collection}
     */
    public static Type getCollectionType(Type rootType) {
        List<Type> route = getRouteToType(rootType, Collection.class);
        Type[] resolvedParameters = resolveTypeArguments(route);
        return resolvedParameters[0];
    }

    /**
     * Given a type which extends {@code Optional<T>}, find the parameter type {@code T}.
     * <p>
     * If {@code rootType} is a parameterized type, then {@code T} may be type variables of {@code rootType}.
     *
     * @param rootType the type that extends {@code Optional}
     * @return the parameter type of {@code Optional}
     */
    public static Type getOptionalType(Type rootType) {
        List<Type> route = getRouteToType(rootType, Optional.class);
        Type[] resolvedParameters = resolveTypeArguments(route);
        return resolvedParameters[0];
    }

    /**
     * Get the actual values for the type variables of the first type in {@code typeList}, after translating them through every type in {@code typeList}
     * <p>
     * E.g. if {@code typeList} contains {@code [Collection<T>, List<T>, ArrayList<String>]} then this method will return {@code [String]}.
     *
     * @param typeList the list of types to resolve against
     * @return the resolved type arguments of the first element of {@code typeList}
     */
    public static Type[] resolveTypeArguments(List<Type> typeList) {
        Type typeToResolve = typeList.get(0);
        if (!(typeToResolve instanceof ParameterizedType)) {
            return new Type[] {};
        }

        ParameterizedType pTypeToResolve = (ParameterizedType) typeToResolve;
        Class<?> rawTypeToResolve = (Class<?>) pTypeToResolve.getRawType();
        Type[] startingTypeParameters = rawTypeToResolve.getTypeParameters();
        Type[] currentTypes = Arrays.copyOf(startingTypeParameters, startingTypeParameters.length, Type[].class);

        for (Type t : typeList) {
            if (!(t instanceof ParameterizedType)) {
                continue;
            }
            ParameterizedType pt = (ParameterizedType) t;
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            Type[] typeParameters = ((Class<?>) pt.getRawType()).getTypeParameters();
            for (int i = 0; i < currentTypes.length; i++) {
                for (int j = 0; j < typeParameters.length; j++) {
                    if (currentTypes[i] == typeParameters[j]) {
                        currentTypes[i] = actualTypeArguments[j];
                    }
                }
            }
        }

        return currentTypes;
    }

    /**
     * Finds a list of types from {@code target} to {@code start} where each element is a direct subtype of the element before it.
     * <p>
     * Generic types are retained in the list.
     * <p>
     * {@code getRouteToType(ArrayList<String>, Collection.class)} would return {@code [Collection<E>, List<E>, ArrayList<String>]}
     *
     * @param start the type to start searching from
     * @param target the type being searched for, must be a supertype of {@code start}
     * @return a subtype chain from {@code target} to {@code start}
     * @throws IllegalArgumentException if {@code target} is not a supertype of {@code start}
     */
    public static List<Type> getRouteToType(Type start, Class<?> target) {
        Deque<Type> route = new ArrayDeque<>();
        boolean successful = buildRouteToType(start, target, route);
        if (!successful) {
            throw new IllegalArgumentException("Type " + start + " does not extend " + target);
        }
        return List.copyOf(route);
    }

    private static boolean buildRouteToType(Type current, Class<?> target, Deque<Type> routeSoFar) {
        Class<?> c;
        if (current instanceof ParameterizedType p) {
            c = (Class<?>) p.getRawType();
        } else if (current instanceof Class<?> cls) {
            c = cls;
        } else {
            return false;
        }

        if (c == target) {
            routeSoFar.addFirst(current);
            return true;
        }

        routeSoFar.addFirst(current);
        for (Type iface : c.getGenericInterfaces()) {
            if (buildRouteToType(iface, target, routeSoFar))
                return true;
        }
        Type superCls = c.getGenericSuperclass();
        if (superCls != null && buildRouteToType(superCls, target, routeSoFar)) {
            return true;
        }

        // We didn't find Map in our supertypes
        routeSoFar.removeFirst();
        return false;
    }
}
