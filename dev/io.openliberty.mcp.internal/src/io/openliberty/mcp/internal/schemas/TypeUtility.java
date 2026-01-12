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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;

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

    /**
     * Find the necessary type variable mappings required to generate schemas for children of {@code baseType}.
     * <p>
     * If {@code baseType} is a parameterized type, we first add any mappings for its type variables from the current context.
     * <p>
     * Next we walk the superclasses and superinterfaces of {@code baseType} and add any type variable assignments found.
     * <p>
     * For example, for a base type {@code Box<X> extends Map<String, X>}, we will first resolve {@code <X>} - say the result is {@code Foo} - and add {@code X -> Foo} to the map.
     * Then we will walk superinterfaces and add {@code K -> String} and {@code V -> Foo}. So the map returned will be {@code {X -> Foo, K -> String, V -> Foo}}.
     *
     * @param baseType the base type to create the type variable mappings for
     * @param previousContext the current schema generation context
     * @return the map of type variables to their values in the current context
     */
    public static Map<TypeVariable<?>, Type> createTypeVariableMap(Type baseType, SchemaGenerationContext previousContext) {
        Map<TypeVariable<?>, Type> baseTypeResolved = new HashMap<>();

        // Resolve any type variables in baseType itself
        typeVariables(baseType).forEach(v -> {
            baseTypeResolved.put(v, previousContext.resolveTypeVariable(v));
        });
        Map<TypeVariable<?>, Type> result = new HashMap<>(baseTypeResolved);

        searchGenericTree(baseType, result);
        return result;
    }

    /**
     * Similar to createTypeVariableMap but will only give mappings of type variable that exist in the type and not in parent hierarchy.
     *
     * @param baseType the base type to create the type variable mappings for
     * @param previousContext the current schema generation context
     * @return the map of type variables to their values in the current context
     */
    public static Map<TypeVariable<?>, Type> createCoreTypeVariableMap(Type baseType, SchemaGenerationContext previousContext) {
        Map<TypeVariable<?>, Type> baseTypeResolved = new HashMap<>();

        // Resolve any type variables in baseType itself
        typeVariables(baseType).forEach(v -> {
            baseTypeResolved.put(v, previousContext.resolveTypeVariable(v));
        });
        Map<TypeVariable<?>, Type> result = new HashMap<>(baseTypeResolved);

        searchGenericTree(baseType, result);
        result.keySet().removeAll(baseTypeResolved.keySet());
        return result;
    }

    /**
     * Returns a stream of type variables referenced by a type.
     * <p>
     * For a parameterized type {@code Holder<Z>.Box<X, List<T>>} this method would return a stream of {@code X, T, Z}.
     * <p>
     * For a generic array type {@code Map<X, Y>[]}, this method would return a stream of {@code X, Y}.
     * <p>
     * For any other type, this method returns an empty stream
     *
     * @param type the type
     * @return a stream of type variables used within {@code type}
     */
    private static Stream<TypeVariable<?>> typeVariables(Type type) {
        return walkTypeParameters(type).filter(TypeVariable.class::isInstance)
                                       .map(TypeVariable.class::cast);
    }

    /**
     * Returns a stream which recursively walks the type arguments of the given type.
     * <p>
     * For a parameterized type {@code Map<String, List<X>>}, this method would return a stream of {@code [Map<String, List<X>>, String, List<X>, X]}.
     * <p>
     * For a generic array type {@code List<X>[]}, this method would return a stream of {@code [List<X>[], List<X>, X]}.
     * <p>
     * For any other type, this method returns a stream containing just the type itself.
     *
     * @param type the starting type
     * @return the stream of types
     */
    private static Stream<Type> walkTypeParameters(Type type) {
        if (type instanceof ParameterizedType pType) {
            Stream<Type> parameterStream = Arrays.stream(pType.getActualTypeArguments()).flatMap(TypeUtility::walkTypeParameters);
            Stream<Type> result = Stream.concat(Stream.of(type), parameterStream);
            if (pType.getOwnerType() != null) {
                result = Stream.concat(result, walkTypeParameters(pType.getOwnerType()));
            }
            return result;
        } else if (type instanceof GenericArrayType gaType) {
            return Stream.concat(Stream.of(type), walkTypeParameters(gaType.getGenericComponentType()));
        } else {
            return Stream.of(type);
        }
    }

    /**
     * Searches through types inheritance and owner classes and maps type variable to actual types.
     * As current is the start it is both the child type and current type in the search
     *
     * @param current the starting type
     * @param genericMap map that will be updated with keys that can route from typevariable to the actual type
     */
    public static void searchAndUpdateGenericTree(Type current, Map<TypeVariable<?>, Type> genericMap) {
        searchGenericTree(current, genericMap);
    }

    /**
     * Recursively searches all interfaces, super classes and also owners if nested classes.
     * If any of the higher classes are parameterised then it will map the type variable to actual variables.
     *
     * @param current the starting type
     * @param genericMap map that will be updated with keys that can route from typevariable to the actual type
     */
    private static void searchGenericTree(Type current, Map<TypeVariable<?>, Type> genericMap) {
        Class<?> c;
        if (current instanceof ParameterizedType currentPt) {
            if (currentPt.getOwnerType() != null) {
                searchGenericTree(currentPt.getOwnerType(), genericMap);
            }
            populateGenericMap(currentPt, genericMap);
            c = (Class<?>) currentPt.getRawType();
        } else if (current instanceof Class<?> cls) {
            c = cls;
        } else {
            return;
        }
        for (Type iface : c.getGenericInterfaces()) {
            searchGenericTree(iface, genericMap);
        }
        Type superCls = c.getGenericSuperclass();
        if (superCls != null || superCls != Object.class) {
            searchGenericTree(superCls, genericMap);
        }
    }

    /**
     * For pTypeToResolve, add a mapping for any TypeVariables and actual type arguments are mapped which will link the type variable from the higher most pTypeToResolve to the
     * final childType (current in entry method call)
     *
     * @param pTypeToResolve the type above child type (higher in inheritance tree)
     * @param childType the type that extends a superclass implements an interface or belongs to an owner class.
     * @param genericMap map that will be updated with keys that can route from typevariable to the actual type
     */
    private static void populateGenericMap(ParameterizedType pTypeToResolve, Map<TypeVariable<?>, Type> genericMap) {
        Class<?> rawTypeToResolve = (Class<?>) pTypeToResolve.getRawType();
        Type[] typeArguments = pTypeToResolve.getActualTypeArguments();
        TypeVariable<?>[] typeParameters = rawTypeToResolve.getTypeParameters();

        for (int i = 0; i < typeParameters.length; i++) {
            Type resolvedArgument = genericMap.get(typeArguments[i]);
            if (resolvedArgument != null) {
                genericMap.put(typeParameters[i], resolvedArgument);
            } else {
                genericMap.put(typeParameters[i], typeArguments[i]);
            }
        }
    }

    public static boolean hasGenericParams(Type type) {
        if (type instanceof ParameterizedType pt) {
            boolean hasGeneric = false;
            for (Type arg : pt.getActualTypeArguments()) {
                hasGeneric |= hasGenericParams(arg);
            }
            return hasGeneric;
        } else if (type instanceof TypeVariable<?> || type instanceof WildcardType || type instanceof GenericArrayType) {
            return true;
        }
        return false;
    }

}
