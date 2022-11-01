/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.repository.Sort;
import jakarta.persistence.Query;

/**
 */
class QueryInfo {
    static enum Type {
        COUNT, DELETE, EXISTS, MERGE, SELECT, UPDATE
    }

    /**
     * Information about the type of entity to which the query pertains.
     */
    EntityInfo entityInfo;

    /**
     * Indicates if the query has a WHERE clause.
     * This is accurate only for generated or partially provided queries.
     */
    boolean hasWhere;

    /**
     * JPQL for the query. Null if a save operation.
     */
    String jpql;

    /**
     * JPQL for a find query after a keyset. Otherwise null.
     */
    String jpqlAfterKeyset;

    /**
     * JPQL for a find query before a keyset. Otherwise null.
     */
    String jpqlBeforeKeyset;

    /**
     * For counting the total number of results across all pages.
     * Null if pagination is not used or only slices are used.
     */
    String jpqlCount;

    /**
     * Keyset consisting of key names and sort direction.
     */
    List<Sort> keyset;

    /**
     * Value from findFirst#By, or 1 for findFirstBy, otherwise 0.
     */
    long maxResults;

    /**
     * Repository method to which this query information pertains.
     */
    final Method method;

    /**
     * Number of parameters to the JPQL query.
     */
    int paramCount = Integer.MIN_VALUE; // initialize to undefined

    /**
     * Names that specified by the <code>Param</code> annotation for each query parameter.
     * Null for positions of parameters that lack the annotation. (These will use ?1, ?2, ...)
     */
    List<String> paramNames = Collections.emptyList();

    /**
     * Indicates that parameters are supplied to the repository method
     * as entity or Iterable of entity and need conversion to entity id
     * or list of entity id.
     * This is currently only used for delete(entity) and delete(Iterable of entities).
     */
    boolean paramsNeedConversionToId;

    /**
     * Array element type if the repository method returns an array, such as,
     * <code>Product[] findByNameLike(String namePattern);</code>
     * or if its parameterized type is an array, such as,
     * <code>CompletableFuture&lt;Product[]&gt; findByNameLike(String namePattern);</code>
     * Otherwise null.
     */
    final Class<?> returnArrayType;

    /**
     * Type parameter of the repository method return value.
     * Null if the return type is not parameterized or is generic.
     * This is useful in cases such as
     * <code>&#64;Query(...) Optional&lt;Float&gt; priceOf(String productId)</code>
     * and
     * <code>CompletableFuture&lt;Stream&lt;Product&gt&gt; findByNameLike(String namePattern)</code>
     */
    final Class<?> returnTypeParam;

    /**
     * Type of the first parameter to a save operation. Null if not a save operation.
     */
    Class<?> saveParamType;

    /**
     * Categorization of query type.
     */
    Type type;

    /**
     * Construct partially complete query information.
     */
    QueryInfo(Method method, Class<?> returnArrayType, Class<?> returnTypeParam) {
        this.method = method;
        this.returnArrayType = returnArrayType;
        this.returnTypeParam = returnTypeParam;
    }

    /**
     * Sets query parameters from repository method arguments.
     *
     * @param query the query
     * @param args  repository method arguments
     * @throws Exception if an error occurs
     */
    void setParameters(Query query, Object... args) throws Exception {
        for (int i = 0, count = paramNames.size(); i < paramCount; i++) {
            Object arg = paramsNeedConversionToId ? //
                            toEntityId(args[i], entityInfo.keyAccessor) : //
                            args[i];
            String paramName = count > i ? paramNames.get(i) : null;
            if (paramName == null)
                query.setParameter(i + 1, arg);
            else // named parameter
                query.setParameter(paramName, arg);
        }
    }

    /**
     * Converts a repository method parameter that is an entity or iterable of entities
     * into an entity id or list of entity ids.
     *
     * @param value       value of the repository method parameter.
     * @param keyAccessor accessor method or field for the entity id.
     * @return entity id or list of entity ids.
     */
    private static final Object toEntityId(Object value, Member keyAccessor) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> entityClass = keyAccessor.getDeclaringClass();
        if (value instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            if (keyAccessor instanceof Method) {
                Method keyAccessorMethod = (Method) keyAccessor;
                for (Object p : (Iterable<?>) value)
                    if (entityClass.isInstance(p))
                        list.add(keyAccessorMethod.invoke(p));
                    else
                        list.add(p);
            } else { // Field
                Field keyAccessorField = (Field) keyAccessor;
                for (Object p : (Iterable<?>) value)
                    if (entityClass.isInstance(p))
                        list.add(keyAccessorField.get(p));
                    else
                        list.add(p);
            }
            value = list;
        } else if (entityClass.isInstance(value)) { // single entity
            if (keyAccessor instanceof Method)
                value = ((Method) keyAccessor).invoke(value);
            else // Field
                value = ((Field) keyAccessor).get(value);
        }
        return value;
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder("QueryInfo@").append(Integer.toHexString(hashCode())).append(':').append(method).toString();
    }

    /**
     * Copy of query information, but with updated JPQL.
     */
    QueryInfo withJPQL(String jpql) {
        QueryInfo q = new QueryInfo(method, returnArrayType, returnTypeParam);
        q.entityInfo = entityInfo;
        q.hasWhere = hasWhere;
        q.jpql = jpql;
        q.jpqlAfterKeyset = jpqlAfterKeyset;
        q.jpqlBeforeKeyset = jpqlBeforeKeyset;
        q.jpqlCount = jpqlCount;
        q.keyset = keyset;
        q.maxResults = maxResults;
        q.paramCount = paramCount;
        q.paramNames = paramNames;
        q.paramsNeedConversionToId = paramsNeedConversionToId;
        q.saveParamType = saveParamType;
        q.type = type;
        return q;
    }
}
