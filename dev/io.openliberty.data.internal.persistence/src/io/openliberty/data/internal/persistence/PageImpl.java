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

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.DataException;
import jakarta.data.Page;
import jakarta.data.repository.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PageImpl<T> implements Page<T> {
    private final Pageable pagination;
    private final List<T> results;

    PageImpl(QueryInfo queryInfo, Pageable pagination, Object[] args) {
        this.pagination = pagination == null ? Pageable.of(1, 100) : pagination;

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            // TODO possible overflow with both of these.
            long maxPageSize = pagination.getSize();
            query.setFirstResult((int) ((pagination.getPage() - 1) * maxPageSize));
            query.setMaxResults((int) maxPageSize + 1);

            results = query.getResultList();
        } catch (Exception x) {
            throw new DataException(x);
        } finally {
            em.close();
        }
    }

    @Override
    public Stream<T> get() {
        return getContent().stream(); // TODO Is there a more efficient way to do this?
    }

    @Override
    public List<T> getContent() {
        int size = results.size();
        long max = pagination.getSize();
        return size > max ? new ResultList((int) max) : results;
    }

    @Override
    public <C extends Collection<T>> C getContent(Supplier<C> collectionFactory) {
        C collection = collectionFactory.get();
        long size = results.size();
        long max = pagination.getSize();
        size = size > max ? max : size;
        for (int i = 0; i < size; i++)
            collection.add(results.get(i));
        return collection;
    }

    @Override
    public long getPage() {
        return pagination.getPage();
    }

    @Override
    public Pageable getPageable() {
        return pagination;
    }

    @Override
    public Pageable next() {
        if (results.size() <= pagination.getSize())
            return null;

        return pagination.next();
    }

    @Override
    public long size() {
        long size = results.size();
        long max = pagination.getSize();
        return size > max ? max : size;
    }

    /**
     * Restricts the number of results to the specified amount.
     */
    @Trivial
    private class ResultList extends AbstractList<T> {
        private final int size;

        private ResultList(int size) {
            this.size = size;
        }

        @Override
        public T get(int index) {
            if (index < size)
                return results.get(index);
            else
                throw new IndexOutOfBoundsException(index);
        }

        @Override
        public int size() {
            return size;
        }
    }
}
