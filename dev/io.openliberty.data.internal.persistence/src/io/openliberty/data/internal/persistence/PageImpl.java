/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class PageImpl<T> implements Page<T> {
    private static final TraceComponent tc = Tr.register(PageImpl.class);

    private final Object[] args;
    private final PageRequest<?> pageRequest;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

    @FFDCIgnore(Exception.class)
    PageImpl(QueryInfo queryInfo, PageRequest<T> pageRequest, Object[] args) {
        this.queryInfo = queryInfo;
        this.pageRequest = pageRequest == null ? PageRequest.ofSize(100) : pageRequest;
        this.args = args;

        // BasicRepository.findAll(PageRequest) requires NullPointerException when PageRequest is null.
        // TODO Should this apply in general?
        if (pageRequest == null && queryInfo.paramCount == 0 && queryInfo.method.getParameterCount() == 1
            && PageRequest.class.equals(queryInfo.method.getParameterTypes()[0]))
            throw new NullPointerException("PageRequest: null");

        EntityManager em = queryInfo.entityInfo.builder.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.entityClass);
            queryInfo.setParameters(query, args);

            int maxPageSize = pageRequest.size();
            query.setFirstResult(RepositoryImpl.computeOffset(pageRequest));
            query.setMaxResults(maxPageSize + (maxPageSize == Integer.MAX_VALUE ? 0 : 1));

            results = query.getResultList();
        } catch (Exception x) {
            throw RepositoryImpl.failure(x);
        } finally {
            em.close();
        }
    }

    /**
     * Query for count of total elements across all pages.
     *
     * @param jpql count query.
     */
    @FFDCIgnore(Exception.class)
    private long countTotalElements() {
        if (pageRequest.page() == 1L && results.size() <= pageRequest.size() && pageRequest.size() < Integer.MAX_VALUE)
            return results.size();

        EntityManager em = queryInfo.entityInfo.builder.createEntityManager();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "query for count: " + queryInfo.jpqlCount);
            TypedQuery<Long> query = em.createQuery(queryInfo.jpqlCount, Long.class);
            queryInfo.setParameters(query, args);

            return query.getSingleResult();
        } catch (Exception x) {
            throw RepositoryImpl.failure(x);
        } finally {
            em.close();
        }
    }

    @Override
    public List<T> content() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? new ResultList(max) : results;
    }

    @Override
    public boolean hasNext() {
        return results.size() > pageRequest.size() || // additional result was read beyond the max page size
               pageRequest.size() == Integer.MAX_VALUE && results.size() == pageRequest.size();
    }

    @Override
    public int numberOfElements() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? max : size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageRequest<T> pageRequest() {
        return (PageRequest<T>) pageRequest;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> PageRequest<E> pageRequest(Class<E> entityClass) {
        return (PageRequest<E>) pageRequest;
    }

    @Override
    public long totalElements() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements;
    }

    @Override
    public long totalPages() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements / pageRequest.size() + (totalElements % pageRequest.size() > 0 ? 1 : 0);
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? new ResultIterator(max) : results.iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageRequest<T> nextPageRequest() {
        if (!hasNext())
            return null;

        return (PageRequest<T>) pageRequest.next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> PageRequest<E> nextPageRequest(Class<E> entityClass) {
        if (!hasNext())
            return null;

        return (PageRequest<E>) pageRequest.next();
    }

    @Override
    public Stream<T> stream() {
        return content().stream();
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
