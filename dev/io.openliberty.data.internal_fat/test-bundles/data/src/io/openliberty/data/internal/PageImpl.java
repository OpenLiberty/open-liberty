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
package io.openliberty.data.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import io.openliberty.data.Page;
import io.openliberty.data.Pagination;
import io.openliberty.data.Param;

/**
 */
public class PageImpl<T> implements Page<T> {
    private final EntityInfo entityInfo;
    private final String jpql;
    private final Method method;
    private final int numParams; // can differ from args.length due to Pagination and Sort/Sorts
    private final Object[] args;
    private final Pagination pagination;
    private final List<T> results;

    PageImpl(String jpql, Pagination pagination, EntityInfo entityInfo,
             Method method, int numParams, Object[] args) {
        this.entityInfo = entityInfo;
        this.jpql = jpql;
        this.pagination = pagination == null ? Pagination.page(1).size(100) : pagination;
        this.method = method;
        this.numParams = numParams;
        this.args = args;

        EntityManager em = entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, entityInfo.type);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < numParams; i++) {
                    Param param = params[i].getAnnotation(Param.class);
                    if (param == null)
                        query.setParameter(i + 1, args[i]);
                    else // named parameter
                        query.setParameter(param.value(), args[i]);
                }
            }
            // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
            query.setFirstResult((int) pagination.getSkip());
            query.setMaxResults((int) pagination.getPageSize());

            results = query.getResultList();
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
    public Pagination getPagination() {
        return pagination;
    }

    @Override
    public Page<T> next() {
        if (results.isEmpty())
            return null;

        PageImpl<T> next = new PageImpl<T>(jpql, pagination.next(), entityInfo, method, numParams, args);

        if (next.results.isEmpty())
            return null;
        return next;
    }
}
