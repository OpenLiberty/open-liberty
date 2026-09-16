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
package io.openliberty.data.internal.v1_1;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.DataVersionCompatibility;
import io.openliberty.data.internal.QueryInfo;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.cdi.RepositoryProducer;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Assign;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.First;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Select;
import jakarta.data.repository.Update;
import jakarta.data.repository.stateful.Detach;
import jakarta.data.repository.stateful.Merge;
import jakarta.data.repository.stateful.Persist;
import jakarta.data.repository.stateful.Refresh;
import jakarta.data.repository.stateful.Remove;
import jakarta.data.restrict.Restriction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
public class Data_1_1 implements DataVersionCompatibility {
    /**
     * The class jakarta.persistence.EntityAgent.CreationOption[]
     * if Jakarta Persistence 4.0+ is used. Otherwise null (a temporary
     * special case to allow experimentation with EclipseLink's Persistence
     * 3.2 implementation). TODO remove the special case
     */
    private static final Class<?> EA_CREATION_OPTION_ARRAY_CLASS;

    /**
     * The class jakarta.persistence.EntityManager.CreationOption[]
     * if Jakarta Persistence 4.0+ is used. Otherwise null (a temporary
     * special case to allow experimentation with EclipseLink's Persistence
     * 3.2 implementation). TODO remove the special case
     */
    private static final Class<?> EM_CREATION_OPTION_ARRAY_CLASS;

    /**
     * Empty array of jakarta.persistence.EntityAgent.CreationOption[]
     * if Jakarta Persistence 4.0+ is used. Otherwise null (a temporary
     * special case to allow experimentation with EclipseLink's Persistence
     * 3.2 implementation). TODO remove the special case
     */
    private static final Object EMPTY_EA_CREATION_OPTIONS;

    /**
     * Empty array of jakarta.persistence.EntityManager.CreationOption[]
     * if Jakarta Persistence 4.0+ is used. Otherwise null (a temporary
     * special case to allow experimentation with EclipseLink's Persistence
     * 3.2 implementation). TODO remove the special case
     */
    private static final Object EMPTY_EM_CREATION_OPTIONS;

    /**
     * jakarta.persistence.query.JakartaQuery.class
     */
    static final Class<? extends Annotation> JAKARTA_QUERY_CLASS;

    /**
     * The value() method of the JakartaQuery annotation.
     */
    static final Method JAKARTA_QUERY_VALUE;

    /**
     * Annotations that represent lifecycle operations that are allowed for
     * methods of a stateful repository.
     */
    private static final Set<Class<? extends Annotation>> LIFECYCLE_ANNOS_STATEFUL = //
                    Set.of(Detach.class,
                           Merge.class,
                           Persist.class,
                           Refresh.class,
                           Remove.class);

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
     * Annotations that represent lifecycle operations.
     */
    private static final List<Class<? extends Annotation>> LIFECYCLE_ANNOS = //
                    Stream.concat(LIFECYCLE_ANNOS_STATEFUL.stream(),
                                  LIFECYCLE_ANNOS_STATELESS.stream()) //
                                    .toList();

    /**
     * jakarta.persistence.query.NativeQuery.class
     */
    static final Class<? extends Annotation> NATIVE_QUERY_CLASS;

    /**
     * The value() method of the NativeQuery annotation.
     */
    static final Method NATIVE_QUERY_VALUE;

    /**
     * Annotations for repository query operations that accept a JPQL or SQL query.
     */
    private static final Set<Class<? extends Annotation>> QUERY_LANGUAGE_ANNOS;

    /**
     * jakarta.persistence.query.Query.class
     */
    static final Class<? extends Annotation> QUERY_OPTIONS_CLASS;

    /**
     * Classes that are valid as return types of resource accessor methods for a
     * stateful repository.
     */
    private static final Set<Class<?>> RESOURCE_ACCESSOR_CLASSES_STATEFUL = //
                    Set.of(Connection.class,
                           DataSource.class,
                           EntityManager.class);

    /**
     * Classes that are valid as return types of resource accessor methods for a
     * stateless repository.
     */
    private static final Set<Class<?>> RESOURCE_ACCESSOR_CLASSES_STATELESS = //
                    RESOURCE_ACCESSOR_CLASSES_STATEFUL; // TODO 1.1 entity agent

    /**
     * Types that are valid as repository method special parameters.
     */
    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = //
                    Set.of(Limit.class,
                           Order.class,
                           Sort.class,
                           Sort[].class,
                           PageRequest.class,
                           Restriction.class);

    static {
        // TODO remove this temporary toleration for the persistence-3.2 feature
        ClassLoader cl = EntityManager.class.getClassLoader();
        boolean jpa32 = EntityManager.class.getClasses().length == 0;
        if (jpa32) {
            EA_CREATION_OPTION_ARRAY_CLASS = null;
            EM_CREATION_OPTION_ARRAY_CLASS = null;
            EMPTY_EA_CREATION_OPTIONS = null;
            EMPTY_EM_CREATION_OPTIONS = null;
            JAKARTA_QUERY_CLASS = null;
            JAKARTA_QUERY_VALUE = null;
            NATIVE_QUERY_CLASS = null;
            NATIVE_QUERY_VALUE = null;
            QUERY_OPTIONS_CLASS = null;
            QUERY_LANGUAGE_ANNOS = Set.of(Query.class);
        } else { // Persistence 4.0
            try {
                Class<?> CreationOption_EA = cl //
                                .loadClass("jakarta.persistence.EntityAgent$CreationOption");
                EA_CREATION_OPTION_ARRAY_CLASS = CreationOption_EA.arrayType();
                EMPTY_EA_CREATION_OPTIONS = Array.newInstance(CreationOption_EA, 0);

                Class<?> CreationOption_EM = cl //
                                .loadClass("jakarta.persistence.EntityManager$CreationOption");
                EM_CREATION_OPTION_ARRAY_CLASS = CreationOption_EM.arrayType();
                EMPTY_EM_CREATION_OPTIONS = Array.newInstance(CreationOption_EM, 0);

                JAKARTA_QUERY_CLASS = (Class<? extends Annotation>) cl //
                                .loadClass("jakarta.persistence.query.JakartaQuery");
                JAKARTA_QUERY_VALUE = JAKARTA_QUERY_CLASS.getMethod("value");

                NATIVE_QUERY_CLASS = (Class<? extends Annotation>) cl //
                                .loadClass("jakarta.persistence.query.NativeQuery");
                NATIVE_QUERY_VALUE = NATIVE_QUERY_CLASS.getMethod("value");

                QUERY_LANGUAGE_ANNOS = Set.of(JAKARTA_QUERY_CLASS,
                                              NATIVE_QUERY_CLASS,
                                              Query.class);

                QUERY_OPTIONS_CLASS = (Class<? extends Annotation>) cl //
                                .loadClass("jakarta.persistence.query.QueryOptions");
            } catch (ClassNotFoundException | NoSuchMethodException x) {
                throw new ExceptionInInitializerError(x);
            }
        }
    }

    @Override
    @Trivial
    public boolean atLeast(int major, int minor) {
        return major == 1 && minor <= 1;
    }

    @Override
    @Trivial
    public AutoCloseable createEntityAgent(EntityManagerFactory emf) {
        AutoCloseable agent;
        try {
            // TODO once JPA 3.2 is no longer tolerated with data-1.1,
            // agent = emf.createEntityAgent();
            agent = (AutoCloseable) emf.getClass() //
                            .getMethod("createEntityAgent",
                                       EA_CREATION_OPTION_ARRAY_CLASS) //
                            .invoke(emf,
                                    new Object[] { EMPTY_EA_CREATION_OPTIONS });
        } catch (IllegalAccessException | NoSuchMethodException x) {
            throw new RuntimeException(x); // should be impossible
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException rx)
                throw rx;
            throw new DataException(x.getCause());
        }

        return agent;
    }

    @Override
    @Trivial
    public EntityManager createEntityManager(EntityManagerFactory emf) {
        EntityManager em;

        try {
            if (EM_CREATION_OPTION_ARRAY_CLASS == null)
                // em = emf.createEntityManager()
                em = (EntityManager) emf.getClass() //
                                .getMethod("createEntityManager") //
                                .invoke(emf);
            else
                // em = emf.createEntityManager(CreationOption...)
                em = (EntityManager) emf.getClass() //
                                .getMethod("createEntityManager",
                                           EM_CREATION_OPTION_ARRAY_CLASS) //
                                .invoke(emf,
                                        new Object[] { EMPTY_EM_CREATION_OPTIONS });
        } catch (IllegalAccessException | NoSuchMethodException x) {
            throw new RuntimeException(x); // should be impossible
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException rx)
                throw rx;
            throw new DataException(x.getCause());
        }

        return em;
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
        return new QueryInfo_1_1( //
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
        return method.getAnnotation(Count.class);
    }

    @Override
    @Trivial
    public Class<?> getEntityClass(Find find) {
        return find.value();
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return method.getAnnotation(Exists.class);
    }

    @Override
    @Trivial
    public Integer getFirstAnnotationValue(Method method) {
        First first = method.getAnnotation(First.class);
        return first == null ? null : first.value();
    }

    @Override
    @Trivial
    public String[] getSelections(AnnotatedElement element) {
        Select[] selects = element.getAnnotationsByType(Select.class);
        if (selects.length == 0)
            return NO_SELECTIONS;
        String[] values = new String[selects.length];
        for (int i = 0; i < selects.length; i++)
            values[i] = selects[i].value();
        return values;
    }

    @Override
    @Trivial
    public boolean isRestriction(Object param) {
        return param instanceof Restriction;
    }

    @Override
    @Trivial
    public boolean isSpecialParamValid(Class<?> paramType,
                                       QueryType queryType) {
        return switch (queryType) {
            case FIND -> true;
            case FIND_AND_DELETE -> !PageRequest.class.equals(paramType);
            case COUNT, EXISTS, QM_DELETE -> Restriction.class.equals(paramType);
            case QM_UPDATE -> false; // TODO FUTURE same as QM_DELETE
            default -> false;
        };
    }

    @Override
    @Trivial
    public Collection<Class<? extends Annotation>> lifeCycleAnnoTypes(Boolean stateful) {
        return stateful == null //
                        ? LIFECYCLE_ANNOS //
                        : stateful //
                                        ? LIFECYCLE_ANNOS_STATEFUL //
                                        : LIFECYCLE_ANNOS_STATELESS;
    }

    @Override
    @Trivial
    public String paramAnnosForUpdate() {
        // TODO 1.1
        return By.class.getSimpleName() + ", " +
               Add.class.getSimpleName() + ", " +
               Assign.class.getSimpleName() + ", " +
               Divide.class.getSimpleName() + ", " +
               Multiply.class.getSimpleName() + ", " +
               SubtractFrom.class.getSimpleName();
    }

    @Override
    @Trivial
    public String persistenceFeatureName() {
        return "persistence-4.0";
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> queryLanguageAnnoTypes() {
        return QUERY_LANGUAGE_ANNOS;
    }

    @Override
    @Trivial
    public Set<Class<?>> resourceAccessorTypes(boolean stateful) {
        return stateful ? RESOURCE_ACCESSOR_CLASSES_STATEFUL //
                        : RESOURCE_ACCESSOR_CLASSES_STATELESS;
    }

    @Override
    @Trivial
    public String specialParamsForFind() {
        return "Limit, Order, Sort, Sort[], PageRequest, Restriction";
    }

    @Override
    @Trivial
    public String specialParamsForFindAndDelete() {
        return "Limit, Order, Sort, Sort[], Restriction";
    }

    @Override
    @Trivial
    public Set<Class<?>> specialParamTypes() {
        return SPECIAL_PARAM_TYPES;
    }

}