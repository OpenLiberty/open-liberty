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
    private final EntityInfo entityInfo;
    private int index;
    private Boolean hasNext;
    private final String jpql;
    private final Method method;
    private final int numParams; // can differ from args.length due to Pagination and Sort/Sorts
    private List<T> page;
    private Pagination pagination;

    PaginatedIterator(String jpql, Pagination pagination, EntityInfo entityInfo,
                      Method method, int numParams, Object[] args) {
        this.jpql = jpql;
        this.pagination = pagination == null ? Pagination.page(1).size(100) : pagination;
        this.entityInfo = entityInfo;
        this.method = method;
        this.numParams = numParams;
        this.args = args;

        getPage();
    }

    private void getPage() {
        EntityManager em = entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, entityInfo.type);
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
