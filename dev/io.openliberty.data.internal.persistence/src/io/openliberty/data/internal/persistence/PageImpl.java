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

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.data.DataException;
import jakarta.data.Page;
import jakarta.data.repository.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PageImpl<T> implements Page<T> {
    private final Object[] args;
    private final Pageable pagination;
    private final QueryInfo queryInfo;
    private final List<T> results;

    PageImpl(QueryInfo queryInfo, Pageable pagination, Object[] args) {
        this.queryInfo = queryInfo;
        this.pagination = pagination == null ? Pageable.of(1, 100) : pagination;
        this.args = args;

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            // TODO possible overflow with both of these.
            long maxPageSize = pagination.getSize();
            query.setFirstResult((int) ((pagination.getPage() - 1) * maxPageSize));
            query.setMaxResults((int) maxPageSize);

            results = query.getResultList();
        } catch (Exception x) {
            throw new DataException(x);
        } finally {
            em.close();
        }
    }

    @Override
    public Stream<T> get() {
        return results.stream();
    }

    @Override
    public Stream<T> getContent() {
        return results.stream();
    }

    @Override
    public <C extends Collection<T>> C getContent(Supplier<C> collectionFactory) {
        C collection = collectionFactory.get();
        collection.addAll(results);
        return collection;
    }

    @Override
    public Pageable getPagination() {
        return pagination;
    }

    @Override
    public Page<T> next() {
        if (results.isEmpty())
            return null;

        PageImpl<T> next = new PageImpl<T>(queryInfo, pagination.next(), args);

        if (next.results.isEmpty())
            return null;
        return next;
    }
}
