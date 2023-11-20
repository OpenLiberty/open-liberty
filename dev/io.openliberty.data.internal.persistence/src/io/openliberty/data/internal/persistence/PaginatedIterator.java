/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.page.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PaginatedIterator<T> implements Iterator<T> {
    private final TraceComponent tc = Tr.register(PaginatedIterator.class);

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
    @Trivial
    private void getPage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "getPage " + pagination.page());

        Optional<Pageable.Cursor> keysetCursor = pagination.cursor();
        int maxPageSize = pagination.size();
        int startAt = keysetCursor.isEmpty() ? RepositoryImpl.computeOffset(pagination) : 0;
        String jpql = keysetCursor.isEmpty() ? queryInfo.jpql : //
                        pagination.mode() == Pageable.Mode.CURSOR_NEXT ? queryInfo.jpqlAfterKeyset : //
                                        queryInfo.jpqlBeforeKeyset;

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryInfo.entityInfo.entityClass);
            queryInfo.setParameters(query, args);

            if (keysetCursor.isPresent())
                queryInfo.setKeysetParameters(query, keysetCursor.get());

            if (startAt > 0)
                query.setFirstResult(startAt);

            query.setMaxResults(maxPageSize);

            page = query.getResultList();
            index = -1;
            hasNext = !page.isEmpty();
            if (hasNext && pagination.mode() == Pageable.Mode.CURSOR_PREVIOUS)
                Collections.reverse(page);
            if (page.size() == maxPageSize) {
                if (keysetCursor.isEmpty()) {
                    pagination = pagination.next();
                } else if (pagination.mode() == Pageable.Mode.CURSOR_NEXT) {
                    Pageable next = pagination.page() == Long.MAX_VALUE ? pagination : pagination.page(pagination.page() + 1);
                    pagination = next.afterKeyset(queryInfo.getKeysetValues(page.get(page.size() - 1)));
                } else { // CURSOR_PREVIOUS
                    // Decrement page number by 1 unless it would go below 1.
                    Pageable prev = pagination.page() == 1 ? pagination : pagination.page(pagination.page() - 1);
                    pagination = prev.beforeKeyset(queryInfo.getKeysetValues(page.get(0)));
                }
            } else {
                pagination = null;
            }
        } catch (Exception x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(this, tc, "getPage", x);
            throw RepositoryImpl.failure(x);
        } finally {
            em.close();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "getPage size " + page.size());
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
