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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class KeysetAwarePageImpl<T> implements KeysetAwarePage<T> {
    private static final TraceComponent tc = Tr.register(KeysetAwarePageImpl.class);

    private final Object[] args;
    private final boolean isForward;
    private final PageRequest<T> pageRequest;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

    @FFDCIgnore(Exception.class)
    KeysetAwarePageImpl(QueryInfo queryInfo, PageRequest<T> pageRequest, Object[] args) {

        this.args = args;
        this.queryInfo = queryInfo;
        this.pageRequest = pageRequest == null ? PageRequest.ofSize(100) : pageRequest;
        this.isForward = this.pageRequest.mode() != PageRequest.Mode.CURSOR_PREVIOUS;
        Optional<PageRequest.Cursor> keysetCursor = this.pageRequest.cursor();

        int maxPageSize = this.pageRequest.size();
        int firstResult = this.pageRequest.mode() == PageRequest.Mode.OFFSET //
                        ? RepositoryImpl.computeOffset(this.pageRequest) //
                        : 0;

        EntityManager em = queryInfo.entityInfo.builder.createEntityManager();
        try {
            String jpql = keysetCursor.isEmpty() ? queryInfo.jpql : //
                            isForward ? queryInfo.jpqlAfterKeyset : //
                                            queryInfo.jpqlBeforeKeyset;

            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryInfo.entityInfo.entityClass);
            queryInfo.setParameters(query, args);

            if (keysetCursor.isPresent())
                queryInfo.setKeysetParameters(query, keysetCursor.get());

            query.setFirstResult(firstResult);
            query.setMaxResults(maxPageSize + (maxPageSize == Integer.MAX_VALUE ? 0 : 1)); // extra position is for knowing whether to expect another page

            results = query.getResultList();

            // Keyset pagination involves reversing the ORDER BY to obtain the previous page, but the entries
            // on the page will also be in reverse order, so we need to reverse again to correct that
            if (!isForward)
                for (int size = results.size(), i = 0, j = size - (size > maxPageSize ? 2 : 1); i < j; i++, j--)
                    Collections.swap(results, i, j);
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
    public PageRequest.Cursor getKeysetCursor(int index) {
        if (index < 0 || index >= pageRequest.size())
            throw new IllegalArgumentException("index: " + index);

        T entity = results.get(index);

        final Object[] keyValues = new Object[queryInfo.sorts.size()];
        int k = 0;
        for (Sort<?> keyInfo : queryInfo.sorts)
            try {
                List<Member> accessors = queryInfo.entityInfo.attributeAccessors.get(keyInfo.property());
                Object value = entity;
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        value = ((Method) accessor).invoke(value);
                    else
                        value = ((Field) accessor).get(value);
                keyValues[k++] = value;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        return Cursor.forKeyset(keyValues);
    }

    @Override
    public boolean hasNext() {
        // The extra position is only available for identifying a next page if the current page was obtained in the forward direction
        int minToHaveNextPage = isForward ? (pageRequest.size() + (pageRequest.size() == Integer.MAX_VALUE ? 0 : 1)) : 1;
        return results.size() >= minToHaveNextPage;
    }

    @Override
    public boolean hasPrevious() {
        // The extra position is only available for identifying a previous page if the current page was obtained in the reverse direction
        int minToHavePreviousPage = isForward ? 1 : (pageRequest.size() + (pageRequest.size() == Integer.MAX_VALUE ? 0 : 1));
        return results.size() >= minToHavePreviousPage;
    }

    @Override
    public int numberOfElements() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? max : size;
    }

    @Override
    public PageRequest<T> pageRequest() {
        return pageRequest;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> PageRequest<E> pageRequest(Class<E> entityClass) {
        // KeysetAwareSlice/Page must always have the same type result as sort criteria per the API.
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
    public PageRequest<T> nextPageRequest() {
        if (!hasNext())
            return null; // TODO error

        PageRequest<T> p = pageRequest.page() == Long.MAX_VALUE ? pageRequest : pageRequest.page(pageRequest.page() + 1);
        return p.afterKeyset(queryInfo.getKeysetValues(results.get(Math.min(results.size(), pageRequest.size()) - 1)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> PageRequest<E> nextPageRequest(Class<E> entityClass) {
        // KeysetAwareSlice/Page must always have the same type result as sort criteria per the API.
        return (PageRequest<E>) nextPageRequest();
    }

    @Override
    public PageRequest<T> previousPageRequest() {
        if (!hasPrevious())
            return null; // TODO error

        // Decrement page number by 1 unless it would go below 1.
        PageRequest<T> p = pageRequest.page() == 1 ? pageRequest : pageRequest.page(pageRequest.page() - 1);
        return p.beforeKeyset(queryInfo.getKeysetValues(results.get(0)));
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
