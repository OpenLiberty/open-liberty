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

    final EntityInfo entityInfo;
    final String jpql;
    final Class<?> returnArrayType;
    final Class<?> saveParamType;
    final Type type;

    QueryInfo(Type type, String jpql, EntityInfo entityInfo,
              Class<?> saveParamType, Class<?> returnArrayType) {
        this.jpql = jpql;
        this.entityInfo = entityInfo;
        this.returnArrayType = returnArrayType;
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
