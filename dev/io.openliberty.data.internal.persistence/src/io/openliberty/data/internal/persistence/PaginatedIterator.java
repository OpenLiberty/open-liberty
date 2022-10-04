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
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.data.Pageable;
import jakarta.data.Param;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PaginatedIterator<T> implements Iterator<T> {
    private final Object[] args;
    private int index;
    private Boolean hasNext;
    private final Method method;
    private final int numParams; // can differ from args.length due to Consumer/Pagination/Sort/Sorts parameters
    private List<T> page;
    private Pageable pagination;
    private final QueryInfo queryInfo;

    PaginatedIterator(QueryInfo queryInfo, Pageable pagination,
                      Method method, int numParams, Object[] args) {
        this.queryInfo = queryInfo;
        this.pagination = pagination == null ? Pageable.of(1, 100) : pagination;
        this.method = method;
        this.numParams = numParams;
        this.args = args;

        getPage();
    }

    private void getPage() {
        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < numParams; i++) {
                    Param param = params[i].getAnnotation(Param.class);
                    if (param == null)
                        query.setParameter(i + 1, args[i]);
                    else // named parameter
                        query.setParameter(param.value(), args[i]);
                }
            }
            // TODO possible overflow with both of these.
            long maxPageSize = pagination.getSize();
            query.setFirstResult((int) ((pagination.getPage() - 1) * maxPageSize));
            query.setMaxResults((int) maxPageSize);
            pagination = pagination.next();

            page = query.getResultList();
            index = -1;
            hasNext = !page.isEmpty();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean hasNext() {
        if (hasNext == null)
            if (index + 1 < page.size())
                hasNext = true;
            else if (pagination == null) // no more pages
                hasNext = false;
            else
                getPage();

        return hasNext;
    }

    @Override
    public T next() {
        if (!hasNext())
            throw new NoSuchElementException();

        hasNext = null;
        return page.get(++index);
    }
}
