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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.DataException;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PageImpl<T> implements Page<T> {
    private static final TraceComponent tc = Tr.register(PageImpl.class);

    private final Object[] args;
    private final Pageable pagination;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

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
            query.setMaxResults((int) maxPageSize + 1);

            results = query.getResultList();
        } catch (Exception x) {
            throw new DataException(x);
        } finally {
            em.close();
        }
    }

    /**
     * Query for count of total elements across all pages.
     *
     * @param jpql count query.
     */
    private long countTotalElements() {
        if (pagination.getPage() == 1L && results.size() <= pagination.getSize())
            return results.size();

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "query for count: " + queryInfo.jpqlCount);
            TypedQuery<Long> query = em.createQuery(queryInfo.jpqlCount, Long.class);
            queryInfo.setParameters(query, args);

            return query.getSingleResult();
        } catch (Exception x) {
            throw new DataException(x);
        } finally {
            em.close();
        }
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
    public long getNumber() {
        return pagination.getPage();
    }

    @Override
    public int getNumberOfElements() {
        int size = results.size();
        int max = (int) pagination.getSize(); // TODO fix data type to int in spec
        return size > max ? max : size;
    }

    @Override
    public Pageable getPageable() {
        return pagination;
    }

    @Override
    public long getTotalElements() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements;
    }

    @Override
    public long getTotalPages() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements / pagination.getSize() + (totalElements % pagination.getSize() > 0 ? 1 : 0);
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        int size = results.size();
        long max = pagination.getSize();
        return size > max ? new ResultIterator((int) max) : results.iterator();
    }

    @Override
    public Pageable nextPageable() {
        if (results.size() <= pagination.getSize())
            return null;

        return pagination.next();
    }

    @Override
    public Stream<T> stream() {
        return getContent().stream();
    }

    /**
     * Iterator that restricts the number of results to the specified amount.
     */
    @Trivial
    private class ResultIterator implements Iterator<T> {
        private int index;
        private final Iterator<T> iterator;
        private final int size;

        private ResultIterator(int size) {
            this.size = size;
            this.iterator = results.iterator();
        }

        @Override
        public boolean hasNext() {
            return index < size && iterator.hasNext();
        }

        @Override
        public T next() {
            if (index >= size)
                throw new NoSuchElementException("Element at index " + index);
            T result = iterator.next();
            index++;
            return result;
        }
    }

    /**
     * List that restricts the number of results to the specified amount.
     */
    @Trivial
    private class ResultList extends AbstractList<T> implements RandomAccess {
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
