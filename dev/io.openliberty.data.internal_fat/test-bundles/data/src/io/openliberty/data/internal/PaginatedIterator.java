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
package io.openliberty.data.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import io.openliberty.data.Pagination;
import io.openliberty.data.Param;

/**
 */
public class PaginatedIterator<T> implements Iterator<T> {
    private final Object[] args;
    private int index;
    private Boolean hasNext;
    private final String jpql;
    private final Method method;
    private List<T> page;
    private Pagination pagination;
    private final QueryHandler<T> queryHandler;

    PaginatedIterator(String jpql, Pagination pagination, QueryHandler<T> queryHandler, Method method, Object[] args) {
        this.jpql = jpql;
        this.pagination = pagination == null ? Pagination.page(1).size(100) : pagination;
        this.queryHandler = queryHandler;
        this.method = method;
        this.args = args;

        getPage();
    }

    private void getPage() {
        EntityManager em = queryHandler.punit.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryHandler.entityClass);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < args.length; i++)
                    if (i == args.length - 1 && args[i] instanceof Pagination) {
                        break; // final argument can be a Pagination
                    } else {
                        Param param = params[i].getAnnotation(Param.class);
                        if (param == null)
                            query.setParameter(i + 1, args[i]);
                        else // named parameter
                            query.setParameter(param.value(), args[i]);
                    }
            }
            // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
            query.setFirstResult((int) pagination.getSkip());
            query.setMaxResults((int) pagination.getPageSize());
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
