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
import java.util.Arrays;
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
import jakarta.data.page.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class KeysetAwarePageImpl<T> implements KeysetAwarePage<T> {
    private static final TraceComponent tc = Tr.register(KeysetAwarePageImpl.class);

    private final Object[] args;
    private final boolean isForward;
    private final Pageable<T> pagination;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

    @FFDCIgnore(Exception.class)
    KeysetAwarePageImpl(QueryInfo queryInfo, Pageable<T> pagination, Object[] args) {

        this.args = args;
        this.queryInfo = queryInfo;
        this.pagination = pagination == null ? Pageable.ofSize(100) : pagination;
        this.isForward = this.pagination.mode() != Pageable.Mode.CURSOR_PREVIOUS;
        Optional<Pageable.Cursor> keysetCursor = this.pagination.cursor();

        int maxPageSize = this.pagination.size();
        int firstResult = this.pagination.mode() == Pageable.Mode.OFFSET //
                        ? RepositoryImpl.computeOffset(this.pagination) //
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
        int max = pagination.size();
        return size > max ? new ResultList(max) : results;
    }

    @Override
    public Pageable.Cursor getKeysetCursor(int index) {
        if (index < 0 || index >= pagination.size())
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

        return new Cursor(keyValues);
    }

    @Override
    public long number() {
        return pagination.page();
    }

    @Override
    public int numberOfElements() {
        int size = results.size();
        int max = pagination.size();
        return size > max ? max : size;
    }

    @Override
    public Pageable<T> pageable() {
        return pagination;
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
        return totalElements / pagination.size() + (totalElements % pagination.size() > 0 ? 1 : 0);
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        int size = results.size();
        int max = pagination.size();
        return size > max ? new ResultIterator(max) : results.iterator();
    }

    @Override
    public Pageable<T> nextPageable() {
        // The extra position is only available for identifying a next page if the current page was obtained in the forward direction
        int minToHaveNextPage = isForward ? (pagination.size() + (pagination.size() == Integer.MAX_VALUE ? 0 : 1)) : 1;
        if (results.size() < minToHaveNextPage)
            return null;

        Pageable<T> p = pagination.page() == Long.MAX_VALUE ? pagination : pagination.page(pagination.page() + 1);
        return p.afterKeyset(queryInfo.getKeysetValues(results.get(Math.min(results.size(), pagination.size()) - 1)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Pageable<E> nextPageable(Class<E> entityClass) {
        // KeysetAwareSlice/Page must always have the same type result as sort criteria per the API.
        return (Pageable<E>) nextPageable();
    }

    @Override
    public Pageable<T> previousPageable() {
        // The extra position is only available for identifying a previous page if the current page was obtained in the reverse direction
        int minToHavePreviousPage = isForward ? 1 : (pagination.size() + (pagination.size() == Integer.MAX_VALUE ? 0 : 1));
        if (results.size() < minToHavePreviousPage)
            return null;

        // Decrement page number by 1 unless it would go below 1.
        Pageable<T> p = pagination.page() == 1 ? pagination : pagination.page(pagination.page() - 1);
        return p.beforeKeyset(queryInfo.getKeysetValues(results.get(0)));
    }

    @Override
    public Stream<T> stream() {
        return content().stream();
    }

    /**
     * Keyset cursor
     */
    @Trivial
    private static class Cursor implements Pageable.Cursor {
        private final Object[] keyValues;

        private Cursor(Object[] keyValues) {
            this.keyValues = keyValues;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null
                                && getClass() == o.getClass()
                                && Arrays.equals(keyValues, ((Cursor) o).keyValues);
        }

        @Override
        public Object getKeysetElement(int index) {
            return keyValues[index];
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(keyValues);
        }

        @Override
        public int size() {
            return keyValues.length;
        }

        @Override
        public String toString() {
            return new StringBuilder(47) //
                            .append("KeysetAwarePageImpl.Cursor@").append(Integer.toHexString(hashCode())) //
                            .append(" with ").append(keyValues.length).append(" keys") //
                            .toString();
        }
    };

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
