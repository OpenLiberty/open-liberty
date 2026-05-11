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

/**
 * Capability that is specific to the version of Jakarta Data.
 */
public class Data_1_1 implements DataVersionCompatibility {

    /**
     * Annotations for repository query operations that accept a JPQL query.
     */
    private static final Set<Class<? extends Annotation>> JPQL_QUERY_ANNOS = //
                    // TODO add new Jakarta Persistence anno
                    Set.of(Query.class);

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

    @Override
    @Trivial
    public boolean atLeast(int major, int minor) {
        return major == 1 && minor <= 1;
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
    public Set<Class<? extends Annotation>> jpqlQueryAnnoTypes() {
        return JPQL_QUERY_ANNOS;
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