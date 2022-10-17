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
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.DataException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetPageable;
import jakarta.data.repository.KeysetPageable.Cursor;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 */
public class KeysetAwarePageImpl<T> implements KeysetAwarePage<T> {
    private static final TraceComponent tc = Tr.register(KeysetAwarePageImpl.class);

    private final boolean isForward;
    private final Pageable pagination;
    private final QueryInfo queryInfo;
    private final List<T> results;

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
                    query.setParameter(queryInfo.paramCount + i + 1, keysetCursor.getKeysetElement(i));
                }

            query.setFirstResult(firstResult);
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
    public Cursor getKeysetCursor(int index) {
        throw new UnsupportedOperationException(); // TODO
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
    public KeysetPageable next() {
        Object entity;
        if (isForward) {
            if (results.size() <= pagination.getSize())
                return null;
            entity = results.get((int) pagination.getSize() - 1);
        } else
            throw new UnsupportedOperationException(); // TODO

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

        return isForward ? pagination.next().afterKeyset(keyValues.toArray()) //
                        : null; // TODO pagination.previous().beforeKeyset(keyValues.toArray());
    }

    @Override
    public KeysetPageable previous() {
        throw new UnsupportedOperationException(); // TODO
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
