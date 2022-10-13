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

import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;

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
     * Generated JPQL for the query. Null if a save operation.
     */
    String jpql;

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
    int paramCount;

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
        q.jpql = jpql;
        q.entityInfo = this.entityInfo;
        q.maxResults = this.maxResults;
        q.paramCount = this.paramCount;
        q.paramsNeedConversionToId = this.paramsNeedConversionToId;
        q.saveParamType = this.saveParamType;
        q.type = this.type;
        return q;
    }
}
