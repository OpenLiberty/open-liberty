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

/**
 */
class QueryInfo {
    static enum Type {
        COUNT, DELETE, EXISTS, MERGE, SELECT, UPDATE
    }

    /**
     * Information about the type of entity to which the query pertains.
     */
    final EntityInfo entityInfo;

    /**
     * Generated JPQL for the query. Null if a save operation.
     */
    final String jpql;

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
    final Class<?> saveParamType;

    /**
     * Categorization of query type.
     */
    final Type type;

    QueryInfo(Type type, String jpql, EntityInfo entityInfo,
              Class<?> saveParamType, Class<?> returnArrayType, Class<?> returnParamType) {
        this.jpql = jpql;
        this.entityInfo = entityInfo;
        this.returnArrayType = returnArrayType;
        this.returnTypeParam = returnParamType;
        this.saveParamType = saveParamType;

        if (type == null) {
            String q = jpql.toUpperCase();
            if (q.startsWith("SELECT"))
                this.type = Type.SELECT;
            else if (q.startsWith("UPDATE"))
                this.type = Type.UPDATE;
            else if (q.startsWith("DELETE"))
                this.type = Type.DELETE;
            else
                throw new UnsupportedOperationException(jpql);
        } else {
            this.type = type;
        }
    }
}
