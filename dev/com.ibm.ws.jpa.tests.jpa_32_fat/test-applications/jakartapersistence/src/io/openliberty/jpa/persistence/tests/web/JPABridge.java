/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.web;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * Reflection bridge that allows test code compiled against JPA 3.2 to run
 * correctly under both JPA 3.2 (EclipseLink) and JPA 4.0 (Hibernate 8).
 *
 * <p>The problem: JPA 4.0 removed {@code EntityManager.createQuery(String)}
 * (untyped, returns {@code Query}) and {@code EntityManager.createQuery(CriteriaQuery<T>)}.
 * WAR bytecode compiled against the JPA 3.2 API carries method descriptors that
 * refer to those old signatures, causing {@code NoSuchMethodError} at runtime
 * when the container loads the WAR against a JPA 4.0 provider.
 *
 * <p>This helper looks up the correct method via reflection at runtime so that
 * a single compiled WAR works on both API versions.
 */
public final class JPABridge {

    /** Cache: EM implementation class → {@code createStatement(String)} Method (JPA 4.0) */
    private static final ConcurrentHashMap<Class<?>, Method> CREATE_STATEMENT_CACHE = new ConcurrentHashMap<>();
    /** Cache: EM implementation class → {@code createQuery(CriteriaSelect)} Method (JPA 4.0) */
    private static final ConcurrentHashMap<Class<?>, Method> CREATE_QUERY_CRITERIA_CACHE = new ConcurrentHashMap<>();

    private JPABridge() {}

    /**
     * Creates a DML (UPDATE/DELETE) query from a JPQL string.
     *
     * <ul>
     *   <li>JPA 4.0: delegates to {@code EntityHandler.createStatement(String)} which returns
     *       {@code MutationQuery} (a subtype of {@code Query}).</li>
     *   <li>JPA 3.2: delegates to {@code EntityManager.createQuery(String)} directly.</li>
     * </ul>
     *
     * @param em   the entity manager
     * @param jpql the JPQL string (UPDATE or DELETE)
     * @return a {@link Query} ready for {@code executeUpdate()}
     */
    @SuppressWarnings("unchecked")
    public static Query createStatement(EntityManager em, String jpql) {
        Method m = CREATE_STATEMENT_CACHE.computeIfAbsent(em.getClass(), cls -> {
            try {
                // JPA 4.0: EntityHandler.createStatement(String) -> MutationQuery
                return cls.getMethod("createStatement", String.class);
            } catch (NoSuchMethodException e) {
                return null; // JPA 3.2 path
            }
        });
        if (m != null) {
            try {
                return (Query) m.invoke(em, jpql);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("JPABridge.createStatement failed", e);
            }
        }
        // JPA 3.2 fallback
        return em.createQuery(jpql);
    }

    /**
     * Creates a typed SELECT query from a {@link CriteriaQuery}.
     *
     * <ul>
     *   <li>JPA 4.0: {@code CriteriaQuery} implements {@code CriteriaSelect}, so we call
     *       {@code EntityManager.createQuery(CriteriaSelect)} via reflection.</li>
     *   <li>JPA 3.2: delegates to {@code EntityManager.createQuery(CriteriaQuery)} directly.</li>
     * </ul>
     *
     * @param em            the entity manager
     * @param criteriaQuery the criteria query
     * @param <T>           the result type
     * @return a {@link TypedQuery}
     */
    @SuppressWarnings("unchecked")
    public static <T> TypedQuery<T> createQuery(EntityManager em, CriteriaQuery<T> criteriaQuery) {
        Method m = CREATE_QUERY_CRITERIA_CACHE.computeIfAbsent(em.getClass(), cls -> {
            // Try to find createQuery(CriteriaSelect) — the JPA 4.0 signature.
            // CriteriaQuery implements CriteriaSelect in JPA 4.0, so the CriteriaQuery
            // instance is a valid argument.
            for (Class<?> iface : cls.getInterfaces()) {
                try {
                    Method candidate = findCreateQueryCriteriaSelect(iface);
                    if (candidate != null) return candidate;
                } catch (Exception ignore) {
                }
            }
            try {
                return findCreateQueryCriteriaSelect(cls);
            } catch (Exception ignore) {
                return null;
            }
        });
        if (m != null) {
            try {
                return (TypedQuery<T>) m.invoke(em, criteriaQuery);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("JPABridge.createQuery(CriteriaQuery) via CriteriaSelect failed", e);
            }
        }
        // JPA 3.2 fallback
        return em.createQuery(criteriaQuery);
    }

    /**
     * Walks the class/interface hierarchy to find a {@code createQuery} method
     * whose single parameter type is named {@code CriteriaSelect} (JPA 4.0).
     */
    private static Method findCreateQueryCriteriaSelect(Class<?> cls) {
        if (cls == null) return null;
        for (Method method : cls.getDeclaredMethods()) {
            if ("createQuery".equals(method.getName()) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if ("jakarta.persistence.criteria.CriteriaSelect".equals(paramType.getName())) {
                    return method;
                }
            }
        }
        for (Class<?> iface : cls.getInterfaces()) {
            Method found = findCreateQueryCriteriaSelect(iface);
            if (found != null) return found;
        }
        return findCreateQueryCriteriaSelect(cls.getSuperclass());
    }
}
