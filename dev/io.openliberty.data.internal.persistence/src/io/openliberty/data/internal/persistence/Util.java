/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * A location for helper methods that do not require any state.
 */
public class Util {
    /**
     * Commonly used result types that are not entities.
     */
    static final Set<Class<?>> NON_ENTITY_RESULT_TYPES = new HashSet<>();

    /**
     * Primitive types for numeric values.
     */
    static final Set<Class<?>> PRIMITIVE_NUMERIC_TYPES = //
                    Set.of(long.class, int.class, short.class, byte.class,
                           double.class, float.class);

    /**
     * List of Jakarta Data operation annotations for use in NLS messages.
     */
    static final String OP_ANNOS = "Delete, Find, Insert, Query, Save, Update";

    /**
     * Return types for deleteBy that distinguish delete-only from find-and-delete.
     */
    static final Set<Class<?>> RETURN_TYPES_FOR_DELETE_ONLY = //
                    Set.of(void.class, Void.class,
                           boolean.class, Boolean.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           Number.class);

    /**
     * Valid types for repository method parameters that specify sort criteria.
     */
    static final Set<Class<?>> SORT_PARAM_TYPES = //
                    Set.of(Order.class, Sort.class, Sort[].class);

    /**
     * Valid types for repository method parameters after the query parameters.
     */
    static final Set<Class<?>> SPECIAL_PARAM_TYPES = //
                    Set.of(Limit.class, Order.class, PageRequest.class,
                           Sort.class, Sort[].class);

    /**
     * Valid types for when a repository method computes an update count
     */
    static final Set<Class<?>> UPDATE_COUNT_TYPES = //
                    Set.of(boolean.class, Boolean.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           void.class, Void.class,
                           Number.class);

    /**
     * Valid types for jakarta.persistence.Version, except for
     * java.sql.Timestamp, which is not a valid type in Jakarta Data.
     */
    public static final Set<Class<?>> VERSION_TYPES = //
                    Set.of(Instant.class,
                           int.class, Integer.class,
                           LocalDateTime.class,
                           long.class, Long.class,
                           short.class, Short.class);

    /**
     * Mapping of Java primitive class to wrapper class.
     */
    private static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = //
                    Map.of(boolean.class, Boolean.class,
                           byte.class, Byte.class,
                           char.class, Character.class,
                           double.class, Double.class,
                           float.class, Float.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           short.class, Short.class,
                           void.class, Void.class);

    /**
     * Returns true if it is certain the class cannot be an entity
     * because it is one of the common non-entity result types
     * or it is an enumeration, interface, or abstract class.
     * Otherwise, returns false.
     *
     * @param c class of result.
     * @return true if a result of the type might be an entity, otherwise false.
     */
    @Trivial
    public static boolean cannotBeEntity(Class<?> c) {
        int modifiers;
        return NON_ENTITY_RESULT_TYPES.contains(c) ||
               c.isEnum() ||
               Modifier.isInterface(modifiers = c.getModifiers()) ||
               Modifier.isAbstract(modifiers);
    }

    /**
     * Identifies whether a method is annotated with a Jakarta Data annotation
     * that performs and operation, such as Query, Find, or Save. This method is
     * for use by error reporting only, so it does not need to be very efficient.
     *
     * @param method repository method.
     * @return if the repository method has an annotation indicating an operation.
     */
    @Trivial
    static final boolean hasOperationAnno(Method method) {
        Set<Class<? extends Annotation>> operationAnnos = //
                        Set.of(Delete.class,
                               Insert.class,
                               Find.class,
                               Query.class,
                               Save.class,
                               Update.class);

        for (Annotation anno : method.getAnnotations())
            if (operationAnnos.contains(anno.annotationType()))
                return true;

        return false;
    }

    /**
     * Indicates if the specified class is a wrapper for the primitive class.
     *
     * @param primitive primitive class.
     * @param cl        another class that might be a wrapper class for the primitive class.
     * @return true if the class is the wrapper class for the primitive class, otherwise false.
     */
    static final boolean isWrapperClassFor(Class<?> primitive, Class<?> cl) {
        return primitive == long.class && cl == Long.class ||
               primitive == int.class && cl == Integer.class ||
               primitive == float.class && cl == Float.class ||
               primitive == double.class && cl == Double.class ||
               primitive == char.class && cl == Character.class ||
               primitive == byte.class && cl == Byte.class ||
               primitive == boolean.class && cl == Boolean.class ||
               primitive == short.class && cl == Short.class;
    }

    static {
        for (Entry<Class<?>, Class<?>> e : WRAPPER_CLASSES.entrySet()) {
            NON_ENTITY_RESULT_TYPES.add(e.getKey()); // primitive classes
            NON_ENTITY_RESULT_TYPES.add(e.getValue()); // wrapper classes
        }
        NON_ENTITY_RESULT_TYPES.add(BigDecimal.class);
        NON_ENTITY_RESULT_TYPES.add(BigInteger.class);
        NON_ENTITY_RESULT_TYPES.add(Object.class);
        NON_ENTITY_RESULT_TYPES.add(String.class);
    }

    /**
     * Returns some of the more commonly used return types that are valid
     * for a life cycle method.
     *
     * @param singularClassName        simple class name of the entity
     * @param hasSingularEntityParam   if the life cycle method entity parameter is
     *                                     singular (not an Iterable or array)
     * @param includeBooleanAndNumeric whether to include boolean and numeric types
     *                                     as valid.
     * @return some of the more commonly used return types that are valid for a
     *         life cycle method.
     */
    static List<String> lifeCycleReturnTypes(String singularClassName,
                                             boolean hasSingularEntityParam,
                                             boolean includeBooleanAndNumeric) {
        List<String> validReturnTypes = new ArrayList<>();
        if (includeBooleanAndNumeric) {
            validReturnTypes.add("boolean");
            validReturnTypes.add("int");
            validReturnTypes.add("long");
        }

        validReturnTypes.add("void");

        if (hasSingularEntityParam) {
            validReturnTypes.add(singularClassName);
        } else {
            validReturnTypes.add(singularClassName + "[]");
            validReturnTypes.add("List<" + singularClassName + ">");
        }

        return validReturnTypes;
    }

    /**
     * Returns the wrapper class if a primitive class, otherwise the same class.
     *
     * @param c class that is possibly a primitive class.
     * @return wrapper class for a primitive, otherwise the same class that was supplied as a parameter.
     */
    @Trivial
    static final Class<?> wrapperClassIfPrimitive(Class<?> c) {
        Class<?> w = WRAPPER_CLASSES.get(c);
        return w == null ? c : w;
    }
}
