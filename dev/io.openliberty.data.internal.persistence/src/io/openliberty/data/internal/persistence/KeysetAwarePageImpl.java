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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.exceptions.DataException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class KeysetAwarePageImpl<T> implements KeysetAwarePage<T> {
    private static final TraceComponent tc = Tr.register(KeysetAwarePageImpl.class);

    private final Object[] args;
    private final boolean isForward;
    private final Pageable pagination;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

    @FFDCIgnore(Exception.class)
    KeysetAwarePageImpl(QueryInfo queryInfo, Pageable pagination, Object[] args) {

        this.args = args;
        this.queryInfo = queryInfo;
        this.pagination = pagination == null ? Pageable.ofSize(100) : pagination;
        this.isForward = this.pagination.mode() != Pageable.Mode.CURSOR_PREVIOUS;
        Pageable.Cursor keysetCursor = this.pagination.cursor();

        int maxPageSize = this.pagination.size();
        int firstResult = this.pagination.mode() == Pageable.Mode.OFFSET //
                        ? RepositoryImpl.computeOffset(this.pagination.page(), maxPageSize) //
                        : 0;

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            String jpql = keysetCursor == null ? queryInfo.jpql : //
                            isForward ? queryInfo.jpqlAfterKeyset : //
                                            queryInfo.jpqlBeforeKeyset;

            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            if (keysetCursor != null)
                if (queryInfo.paramNames.isEmpty() || queryInfo.paramNames.get(0) == null) // positional parameters
                    for (int i = 0; i < keysetCursor.size(); i++) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter ?" + (queryInfo.paramCount + i + 1));
                        // TODO detect if user provides a wrong-sized keyset? Or let JPA error surface?
                        query.setParameter(queryInfo.paramCount + i + 1, keysetCursor.getKeysetElement(i));
                    }
                else // named parameters
                    for (int i = 0; i < keysetCursor.size(); i++) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter :keyset" + (i + 1));
                        // TODO detect if user provides a wrong-sized keyset? Or let JPA error surface?
                        query.setParameter("keyset" + (queryInfo.paramCount + i + 1), keysetCursor.getKeysetElement(i));
                    }

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
        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
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
    public <C extends Collection<T>> C getContent(Supplier<C> collectionFactory) {
        C collection = collectionFactory.get();
        int size = results.size();
        int max = pagination.size();
        size = size > max ? max : size;
        for (int i = 0; i < size; i++)
            collection.add(results.get(i));
        return collection;
    }

    @Override
    public Pageable.Cursor getKeysetCursor(int index) {
        if (index < 0 || index >= pagination.size())
            throw new IllegalArgumentException("index: " + index);

        T entity = results.get(index);

        final Object[] keyValues = new Object[queryInfo.keyset.size()];
        int k = 0;
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.property());
                if (accessor instanceof Method)
                    keyValues[k++] = ((Method) accessor).invoke(entity);
                else
                    keyValues[k++] = ((Field) accessor).get(entity);
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
    public Pageable pageable() {
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
    public Pageable nextPageable() {
        // The extra position is only available for identifying a next page if the current page was obtained in the forward direction
        int minToHaveNextPage = isForward ? (pagination.size() + (pagination.size() == Integer.MAX_VALUE ? 0 : 1)) : 1;
        if (results.size() < minToHaveNextPage)
            return null;

        Object entity = results.get(Math.min(results.size(), pagination.size()) - 1);

        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.property());
                if (accessor instanceof Method)
                    keyValues.add(((Method) accessor).invoke(entity));
                else
                    keyValues.add(((Field) accessor).get(entity));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        Pageable p = pagination.page() == Long.MAX_VALUE ? pagination : pagination.page(pagination.page() + 1);
        return p.afterKeyset(keyValues.toArray());
    }

    @Override
    public Pageable previousPageable() {
        // The extra position is only available for identifying a previous page if the current page was obtained in the reverse direction
        int minToHavePreviousPage = isForward ? 1 : (pagination.size() + (pagination.size() == Integer.MAX_VALUE ? 0 : 1));
        if (results.size() < minToHavePreviousPage)
            return null;

        Object entity = results.get(0);

        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.property());
                if (accessor instanceof Method)
                    keyValues.add(((Method) accessor).invoke(entity));
                else
                    keyValues.add(((Field) accessor).get(entity));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        // Decrement page number by 1 unless it would go below 1.
        Pageable p = pagination.page() == 1 ? pagination : pagination.page(pagination.page() - 1);
        return p.beforeKeyset(keyValues.toArray());
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

        @SuppressWarnings("unchecked")
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
