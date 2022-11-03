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
import java.util.List;
import java.util.function.Supplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.DataException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetPageable;
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

    KeysetAwarePageImpl(QueryInfo queryInfo, Pageable pagination, Object[] args) {
        KeysetPageable.Cursor keysetCursor = null;
        // TODO possible overflow with both of these.
        int firstResult;
        long maxPageSize = pagination.getSize();

        if (pagination instanceof KeysetPageable) {
            KeysetPageable keysetPagination = (KeysetPageable) pagination;
            this.isForward = keysetPagination.getMode() == KeysetPageable.Mode.NEXT;
            this.pagination = pagination;
            keysetCursor = keysetPagination.getCursor();
            firstResult = 0;
        } else {
            this.isForward = true;
            this.pagination = pagination == null ? Pageable.of(1, 100) : pagination;
            firstResult = (int) ((pagination.getPage() - 1) * maxPageSize);
        }

        this.args = args;
        this.queryInfo = queryInfo;

        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            String jpql = keysetCursor == null ? queryInfo.jpql : //
                            isForward ? queryInfo.jpqlAfterKeyset : //
                                            queryInfo.jpqlBeforeKeyset;

            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            if (keysetCursor != null)
                for (int i = 0; i < keysetCursor.size(); i++) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set keyset parameter ?" + (queryInfo.paramCount + i + 1));
                    // TODO detect if user provides a wrong-sized keyset? Or let JPA error surface?
                    query.setParameter(queryInfo.paramCount + i + 1, keysetCursor.getKeysetElement(i));
                }

            query.setFirstResult(firstResult);
            query.setMaxResults((int) maxPageSize + 1); // extra position is for knowing whether to expect another page

            results = query.getResultList();

            // Keyset pagination involves reversing the ORDER BY to obtain the previous page, but the entries
            // on the page will also be in reverse order, so we need to reverse again to correct that
            if (!isForward)
                for (int size = results.size(), i = 0, j = size - (size > maxPageSize ? 2 : 1); i < j; i++, j--)
                    Collections.swap(results, i, j);
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
    public Cursor getKeysetCursor(int index) {
        if (index < 0 || index >= pagination.getSize())
            throw new IllegalArgumentException("index: " + index);

        T entity = results.get(index);

        final Object[] keyValues = new Object[queryInfo.keyset.size()];
        int k = 0;
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.getProperty());
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
    public long getNumber() {
        return pagination.getPage();
    }

    @Override
    public int getNumberOfElements() {
        int size = results.size();
        int max = (int) pagination.getSize(); // TODO correct spec interface
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
    public KeysetPageable nextPageable() {
        // The extra position is only available for identifying a next page if the current page was obtained in the forward direction
        int minToHaveNextPage = isForward ? ((int) pagination.getSize() + 1) : 1;
        if (results.size() < minToHaveNextPage)
            return null;

        Object entity = results.get(Math.min(results.size(), (int) pagination.getSize()) - 1);

        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.getProperty());
                if (accessor instanceof Method)
                    keyValues.add(((Method) accessor).invoke(entity));
                else
                    keyValues.add(((Field) accessor).get(entity));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        return pagination.next().afterKeyset(keyValues.toArray());
    }

    @Override
    public KeysetPageable previousPageable() {
        // The extra position is only available for identifying a previous page if the current page was obtained in the reverse direction
        int minToHavePreviousPage = isForward ? 1 : ((int) pagination.getSize() + 1);
        if (results.size() < minToHavePreviousPage)
            return null;

        Object entity = results.get(0);

        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort keyInfo : queryInfo.keyset)
            try {
                Member accessor = queryInfo.entityInfo.attributeAccessors.get(keyInfo.getProperty());
                if (accessor instanceof Method)
                    keyValues.add(((Method) accessor).invoke(entity));
                else
                    keyValues.add(((Field) accessor).get(entity));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        // Decrement page number by 1 unless it would go below 1.
        Pageable p = pagination.getPage() == 1 ? pagination : Pageable.of(pagination.getPage() - 1, pagination.getSize());
        return p.beforeKeyset(keyValues.toArray());
    }

    /**
     * Keyset cursor
     */
    @Trivial
    private static class Cursor implements KeysetPageable.Cursor {
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
