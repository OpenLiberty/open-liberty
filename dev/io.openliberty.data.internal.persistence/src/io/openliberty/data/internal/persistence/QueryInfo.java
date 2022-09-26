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

import java.util.function.Consumer;
import java.util.stream.Collector;

import jakarta.data.Pagination;

/**
 */
class QueryInfo {
    static enum Type {
        COUNT, DELETE, EXISTS, MERGE, SELECT, UPDATE
    }

    final Collector<Object, Object, Object> collector;
    final Consumer<Object> consumer;
    final EntityInfo entityInfo;
    final String jpql;
    final Pagination pagination;
    final int paramCount;
    final Class<?> returnArrayType;
    final Class<?> saveParamType;
    final Type type;

    QueryInfo(String jpql, int paramCount, EntityInfo entityInfo, Pagination pagination,
              Collector<Object, Object, Object> collector, Consumer<Object> consumer,
              Class<?> saveParamType, Class<?> returnArrayType) {
        this.jpql = jpql;
        this.paramCount = paramCount;
        this.entityInfo = entityInfo;
        this.pagination = pagination;
        this.collector = collector;
        this.consumer = consumer;
        this.returnArrayType = returnArrayType;
        this.saveParamType = saveParamType;

        if (jpql == null) {
            type = Type.MERGE;
        } else {
            String q = jpql.toUpperCase();
            if (q.startsWith("SELECT"))
                type = Type.SELECT;
            else if (q.startsWith("UPDATE"))
                type = Type.UPDATE;
            else if (q.startsWith("DELETE"))
                type = Type.DELETE;
            else
                throw new UnsupportedOperationException(jpql);
        }
    }

    QueryInfo(String jpql, int paramCount, EntityInfo entityInfo, Pagination pagination,
              Collector<Object, Object, Object> collector, Consumer<Object> consumer,
              Class<?> saveParamType, Class<?> returnArrayType, Type type) {
        this.jpql = jpql;
        this.paramCount = paramCount;
        this.entityInfo = entityInfo;
        this.pagination = pagination;
        this.collector = collector;
        this.consumer = consumer;
        this.returnArrayType = returnArrayType;
        this.saveParamType = saveParamType;
        this.type = type;
    }
}
