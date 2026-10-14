/*******************************************************************************
 * Copyright (c) 2024,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.v1_0;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Set;

import javax.sql.DataSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.DataVersionCompatibility;
import io.openliberty.data.internal.QueryInfo;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.cdi.RepositoryProducer;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
public class Data_1_0 implements DataVersionCompatibility {
    /**
     * Persistence 3.2 method:
     * EntityManager.createQuery(String qlString)
     */
    static final Method EM_createQuery_ql;

    /**
     * Persistence 3.2 method:
     * EntityManager.createQuery(String qlString, Class<T> resultClass)
     */
    static final Method EM_createQuery_ql_rc;

    /**
     * Persistence 3.2 method:
     * EntityManagerFactory.createEntityManager()
     */
    private static final Method EMF_createEntityManager;

    /**
     * Annotations that represent lifecycle operations that are allowed for
     * methods of a stateful repository.
     */
    private static final Set<Class<? extends Annotation>> LIFECYCLE_ANNOS_STATEFUL = //
                    Set.of();

    /**
     * Annotations that represent lifecycle operations that are allowed for
     * methods of a stateless repository.
     */
    private static final Set<Class<? extends Annotation>> LIFECYCLE_ANNOS_STATELESS = //
                    Set.of(Delete.class,
                           Insert.class,
                           Update.class,
                           Save.class);

    /**
     * Annotations for repository query operations that accept a JPQL query.
     */
    private static final Set<Class<? extends Annotation>> QUERY_LANGUAGE_ANNOS = //
                    Set.of(Query.class);

    /**
     * Classes that are valid as return types of resource accessor methods for a
     * stateful repository.
     */
    private static final Set<Class<?>> RESOURCE_ACCESSOR_CLASSES_STATEFUL = //
                    Set.of(Connection.class,
                           DataSource.class,
                           EntityManager.class);

    /**
     * Types that are valid as repository method special parameters.
     */
    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = //
                    Set.of(Limit.class, Order.class, PageRequest.class,
                           Sort.class, Sort[].class);

    static {
        try {
            // Jakarta Persistence 3.2 methods:
            EM_createQuery_ql = EntityManager.class //
                            .getMethod("createQuery", String.class);
            EM_createQuery_ql_rc = EntityManager.class //
                            .getMethod("createQuery", String.class, Class.class);
            EMF_createEntityManager = EntityManagerFactory.class //
                            .getMethod("createEntityManager");
        } catch (NoSuchMethodException x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    @Override
    @Trivial
    public boolean atLeast(int major, int minor) {
        return major == 1 && minor == 0;
    }

    @Override
    @Trivial
    public AutoCloseable createEntityAgent(EntityManagerFactory emf) {
        throw new UnsupportedOperationException();
    }

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    @Trivial
    public EntityManager createEntityManager(EntityManagerFactory emf) {
        try {
            return (EntityManager) EMF_createEntityManager.invoke(emf);
        } catch (IllegalAccessException x) {
            throw new RuntimeException(x); // should never occur
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException rx)
                throw rx;
            else
                throw new RuntimeException(x);
        }
    }

    @Override
    @Trivial
    public QueryInfo createQueryInfo(RepositoryProducer<?> repositoryProducer,
                                     Class<?> repositoryInterface,
                                     Method method,
                                     QueryType methodType,
                                     Annotation methodTypeAnno,
                                     Class<?> entityParamType,
                                     boolean isOptional,
                                     Class<?> returnArrayType,
                                     Class<?> multiType,
                                     Class<?> singleType,
                                     Class<?> singleTypeElementType) {
        return new QueryInfo_1_0( //
                        repositoryProducer, //
                        repositoryInterface, //
                        method, //
                        methodType, //
                        methodTypeAnno, //
                        entityParamType, //
                        isOptional, //
                        returnArrayType, //
                        multiType, //
                        singleType, //
                        singleTypeElementType);
    }

    @Override
    @Trivial
    public Annotation getCountAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public Class<?> getEntityClass(Find find) {
        return void.class;
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public Integer getFirstAnnotationValue(Method method) {
        return null;
    }

    @Override
    @Trivial
    public String[] getSelections(AnnotatedElement element) {
        return NO_SELECTIONS;
    }

    @Override
    @Trivial
    public boolean isRestriction(Object param) {
        return false;
    }

    @Override
    @Trivial
    public boolean isSpecialParamValid(Class<?> paramType,
                                       QueryType queryType) {
        return switch (queryType) {
            case FIND -> true;
            case FIND_AND_DELETE -> !PageRequest.class.equals(paramType);
            default -> false;
        };
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> lifeCycleAnnoTypes(Boolean stateful) {
        return Boolean.TRUE.equals(stateful) //
                        ? LIFECYCLE_ANNOS_STATEFUL //
                        : LIFECYCLE_ANNOS_STATELESS;
    }

    @Override
    @Trivial
    public String paramAnnosForUpdate() {
        return By.class.getName();
    }

    @Override
    @Trivial
    public String persistenceFeatureName() {
        return "persistence-3.2";
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> queryLanguageAnnoTypes() {
        return QUERY_LANGUAGE_ANNOS;
    }

    @Override
    @Trivial
    public Set<Class<?>> resourceAccessorTypes(Boolean stateful) {
        return RESOURCE_ACCESSOR_CLASSES_STATEFUL;
    }

    @Override
    @Trivial
    public String specialParamsForFind() {
        return "Limit, PageRequest, Order, Sort, Sort[]";
    }

    @Override
    @Trivial
    public String specialParamsForFindAndDelete() {
        return "Limit, Order, Sort, Sort[]";
    }

    @Override
    @Trivial
    public Set<Class<?>> specialParamTypes() {
        return SPECIAL_PARAM_TYPES;
    }

}