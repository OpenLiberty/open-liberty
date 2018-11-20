/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Implements the logic to find a method named using {@link Fallback#fallbackMethod()}
 *
 * @see #findMatchingMethod(Method, String)
 */
public class MethodFinder {

    /**
     * Recursively search the class hierarchy of the class which declares {@code originalMethod} to find a method named {@code methodName} with the same signature as
     * {@code originalMethod}
     *
     * @return The matching method, or {@code null} if one could not be found
     */
    public static Method findMatchingMethod(Method originalMethod, String methodName) {
        Class<?> originalClass = originalMethod.getDeclaringClass();

        return AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public Method run() {
                return findMatchingMethod(originalMethod, methodName, originalClass, new ResolutionContext(), originalClass);
            }
        });
    }

    /**
     * Recursively search the class hierarchy of {@code clazzToCheck} to find a method named {@code methodName} with the same signature as {@code originalMethod}
     *
     * @param originalMethod the original method
     * @param methodName the name of the method to search for
     * @param classToSearch the class to search
     * @param ctx the resolution context
     * @param originalClass the class which declared {@code originalMethod}
     * @return a method named {@code methodName}, in the class hierarchy of {@code clazzToCheck}, which matches the signature of {@code originalMethod}, or {@code null} if one
     *         cannot be found
     */
    private static Method findMatchingMethod(Method originalMethod, String methodName, Type classToSearch, ResolutionContext ctx, Class<?> originalClass) {
        // Check self
        Method result = findMethodOnClass(originalMethod, methodName, classToSearch, ctx, originalClass);

        // Check interfaces
        if (result == null) {
            for (Type iface : getClass(classToSearch).getGenericInterfaces()) {
                ctx.push(iface);
                result = findMatchingMethod(originalMethod, methodName, iface, ctx, originalClass);
                ctx.pop();
                if (result != null) {
                    break;
                }
            }
        }

        // Check superclass
        if (result == null) {
            Type superclass = getClass(classToSearch).getGenericSuperclass();
            if (superclass != null) {
                ctx.push(superclass);
                result = findMatchingMethod(originalMethod, methodName, superclass, ctx, originalClass);
                ctx.pop();
            }
        }

        return result;
    }

    /**
     * Search {@code clazzToCheck} to find a method named {@code methodName} with the same signature as {@code originalMethod}
     *
     * @param originalMethod the original method
     * @param methodName the name of the method to search for
     * @param classToSearch the class to search
     * @param ctx the resolution context
     * @param originalClass the class which declared {@code originalMethod}
     * @return a method named {@code methodName}, declared by {@code clazzToCheck}, which matches the signature of {@code originalMethod}, or {@code null} if one
     *         cannot be found
     */
    private static Method findMethodOnClass(Method originalMethod, String methodName, Type classToSearch, ResolutionContext ctx, Class<?> originalClass) {
        methodLoop: for (Method m : getClass(classToSearch).getDeclaredMethods()) {
            if (!methodName.equals(m.getName())) {
                continue;
            }

            final int modifiers = m.getModifiers();

            if (Modifier.isPrivate(modifiers)) {
                if (!classToSearch.equals(originalClass)) {
                    // Ignore private methods outside the original class
                    continue;
                }
            } else if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
                if (!getClass(classToSearch).getPackage().equals(originalClass.getPackage())) {
                    // Ignore package-scoped methods outside of the original class's package
                    continue;
                }
            }

            Type[] typesToMatch = originalMethod.getGenericParameterTypes();
            Type[] types = m.getGenericParameterTypes();
            if (typesToMatch.length != types.length) {
                continue;
            }

            for (int i = 0; i < typesToMatch.length; i++) {
                if (!typesEquivalent(typesToMatch[i], types[i], ctx)) {
                    continue methodLoop;
                }
            }

            if (!typesEquivalent(originalMethod.getGenericReturnType(), m.getGenericReturnType(), ctx)) {
                continue;
            }

            return m;
        }

        return null;
    }

    /**
     * Computes whether {@code type} is equivalent to {@code typeToMatch}
     * <p>
     * If {@code type} is a type variable, or is a ParameterizedType which has type variables, or is a GenericArrayType, {@code ctx} will be used to resolve any type variables,
     * if possible.
     * <p>
     * The type variable resolution is not done for {@code typeToMatch}.
     * <p>
     * This method will delegate to {@link #typesEquivalent(ParameterizedType, ParameterizedType, ResolutionContext)} and
     * {@link #typesEquivalent(Type, GenericArrayType, ResolutionContext)}
     *
     * @param typeToMatch the type to match against
     * @param type the type to check, type variable resolution will be performed for this type
     * @param ctx the resolution context to use to perform type variable resolution
     * @return {@code true} if {@code type} is equivalent to {@code typeToMatch} after type resolution, otherwise {@code false}
     */
    private static boolean typesEquivalent(Type typeToMatch, Type type, ResolutionContext ctx) {
        type = ctx.resolve(type);
        if (typeToMatch instanceof ParameterizedType) {
            if (type instanceof ParameterizedType) {
                return typesEquivalent((ParameterizedType) typeToMatch, (ParameterizedType) type, ctx);
            } else {
                return false;
            }
        } else {
            if (type instanceof ParameterizedType) {
                return false;
            }
        }

        if (typeToMatch instanceof WildcardType) {
            if (type instanceof WildcardType) {
                return typesEquivalent((WildcardType) typeToMatch, (WildcardType) type, ctx);
            } else {
                return false;
            }
        } else {
            if (type instanceof WildcardType) {
                return false;
            }
        }

        if (type instanceof GenericArrayType) {
            return typesEquivalent(typeToMatch, (GenericArrayType) type, ctx);
        }

        return typeToMatch.equals(type);
    }

    /**
     * Computes whether two ParameterizedTypes are equivalent.
     * <p>
     * This method will check the raw types and then recursively compare each of the type arguments using {@link #typesEquivalent(Type, Type, ResolutionContext)}
     *
     * @param typeToMatch the type to match against
     * @param type the type to check, type variable resolution will be performed for this type
     * @param ctx the resolution context to use to perform type variable resolution
     * @return {@code true} if {@code type} is equivalent to {@code typeToMatch} after type resolution, otherwise {@code false}
     */
    private static boolean typesEquivalent(ParameterizedType typeToMatch, ParameterizedType type, ResolutionContext ctx) {
        if (!typeToMatch.getRawType().equals(type.getRawType())) {
            return false;
        }

        Type[] typesA = typeToMatch.getActualTypeArguments();
        Type[] typesB = type.getActualTypeArguments();

        if (typesA.length != typesB.length) {
            return false;
        }

        for (int i = 0; i < typesA.length; i++) {
            if (!typesEquivalent(typesA[i], ctx.resolve(typesB[i]), ctx)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Computes whether a type is equivalent to a GenericArrayType.
     * <p>
     * This method will check that {@code typeToMatch} is either a {@link GenericArrayType} or an array and then recursively compare the component types of both arguments using
     * {@link #typesEquivalent(Type, Type, ResolutionContext)}.
     *
     * @param typeToMatch the type to match against
     * @param type the type to check, type variable resolution will be performed for this type
     * @param ctx the resolution context to use to perform type variable resolution
     * @return {@code true} if {@code type} is equivalent to {@code typeToMatch} after type resolution, otherwise {@code false}
     */
    private static boolean typesEquivalent(Type typeToMatch, GenericArrayType type, ResolutionContext ctx) {
        if (typeToMatch instanceof GenericArrayType) {
            GenericArrayType aGat = (GenericArrayType) typeToMatch;
            return typesEquivalent(aGat.getGenericComponentType(),
                                   ctx.resolve(type.getGenericComponentType()),
                                   ctx);
        }

        if (typeToMatch instanceof Class) {
            Class<?> aClazz = (Class<?>) typeToMatch;
            if (aClazz.isArray()) {
                return typesEquivalent(aClazz.getComponentType(), ctx.resolve(type.getGenericComponentType()), ctx);
            }
        }

        return false;
    }

    /**
     * Computes whether two WildcardTypes are equivalent.
     * <p>
     * This method will recursively check that the upper and lower wildcard bounds are equivalent
     *
     * @param typeToMatch the type to match against
     * @param type the type to check, type variable resolution will be performed for this type
     * @param ctx the resolution context to use to perform type variable resolution
     * @return {@code true} if {@code type} is equivalent to {@code typeToMatch} after type resolution, otherwise {@code false}
     */
    private static boolean typesEquivalent(WildcardType typeToMatch, WildcardType type, ResolutionContext ctx) {
        if (typeToMatch.getUpperBounds().length != type.getUpperBounds().length) {
            return false;
        }

        if (typeToMatch.getLowerBounds().length != type.getLowerBounds().length) {
            return false;
        }

        for (int i = 0; i < typeToMatch.getUpperBounds().length; i++) {
            if (!typesEquivalent(typeToMatch.getUpperBounds()[i], ctx.resolve(type.getUpperBounds()[i]), ctx)) {
                return false;
            }
        }

        for (int i = 0; i < typeToMatch.getLowerBounds().length; i++) {
            if (!typesEquivalent(typeToMatch.getLowerBounds()[i], ctx.resolve(type.getLowerBounds()[i]), ctx)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convert a {@link Type} to a {@link Class}
     * <p>
     * Class instances are cast, ParameterizedType instances return the result of {@link ParameterizedType#getRawType()}
     *
     * @param type the type to convert
     * @return the class
     * @throws RuntimeException if Type does not represent something that can be converted to a {@link Class}
     */
    private static Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        }
        throw new RuntimeException("Could not convert " + type + " to a class, it is a " + type.getClass());
    }

    /**
     * Stores a list of types that can be used to resolve TypeVariables
     * <p>
     * As we recursively navigate down the class hierarchy, a {@link Type} representing each class should be {@link #push(Type)}ed into the context. When we navigate back out of
     * that class, it should be {@link #pop()}ed out of the context.
     * <p>
     * At any point during search through the hierarchy, we can use {@link #resolve(Type)} to resolve any type variables using the context at that point.
     * <p>
     * To do this, we walk back up through the types that have been {@link #push(Type)}ed, most recent first. If the type we're trying to resolve is a {@link TypeVariable} and it
     * appears as one of the generic type parameters, we replace it with the actual type parameter.
     */
    private static class ResolutionContext {
        private final Deque<Type> types = new ArrayDeque<>();

        public void push(Type type) {
            types.push(type);
        }

        public void pop() {
            types.pop();
        }

        public Type resolve(Type var) {
            if (!(var instanceof TypeVariable<?>)) {
                return var;
            }

            for (Type type : types) {
                if (!(type instanceof ParameterizedType)) {
                    continue;
                }

                ParameterizedType pType = (ParameterizedType) type;
                Type result = var;
                TypeVariable<?>[] typeParams = MethodFinder.getClass(pType).getTypeParameters();
                for (int i = 0; i < typeParams.length; i++) {
                    if (var.equals(typeParams[i])) {
                        result = pType.getActualTypeArguments()[i];
                        break;
                    }
                }

                if (result instanceof TypeVariable) {
                    var = result;
                } else {
                    return result;
                }
            }
            return var;
        }
    }

}
