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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.exceptions.DataException;
import jakarta.data.repository.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PaginatedIterator<T> implements Iterator<T> {
    private final Object[] args;
    private int index;
    private Boolean hasNext;
    private List<T> page;
    private Pageable pagination;
    private final QueryInfo queryInfo;

    PaginatedIterator(QueryInfo queryInfo, Pageable pagination, Object[] args) {
        this.queryInfo = queryInfo;
        this.pagination = pagination == null ? Pageable.ofSize(100) : pagination;
        this.args = args;

        getPage();
    }

    @FFDCIgnore(Exception.class)
    private void getPage() {
        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            // TODO Keyset pagination
            // TODO possible overflow with both of these.
            long maxPageSize = pagination.size();
            query.setFirstResult((int) ((pagination.page() - 1) * maxPageSize));
            query.setMaxResults((int) maxPageSize);
            pagination = pagination.next();

            page = query.getResultList();
            index = -1;
            hasNext = !page.isEmpty();
        } catch (Exception x) {
            throw new DataException(x);
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
