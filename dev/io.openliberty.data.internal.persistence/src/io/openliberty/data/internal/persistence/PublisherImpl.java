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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;

import jakarta.data.repository.Limit;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Using Java's SubmissionPublisher to try this path in the simplest way possible.
 * A real implementation wouldn't do this.
 */
public class PublisherImpl<T> extends SubmissionPublisher<T> implements Runnable {
    private final Object[] args;
    private final Limit limit;
    private final Method method;
    private final int numParams; // can differ from args.length due to Consumer/Pagination/Sort/Sorts parameters
    private final Pageable pagination;
    private final QueryInfo queryInfo;

    PublisherImpl(QueryInfo queryInfo, ExecutorService executor, Limit limit, Pageable pagination, Method method, int numParams, Object[] args) {
        super(executor, 200);

        this.queryInfo = queryInfo;
        this.limit = limit;
        this.method = method;
        this.numParams = numParams;
        this.pagination = pagination;
        this.args = args;

        executor.submit(this);
    }

    @Override
    public void run() {
        EntityManager em = null;
        try {
            em = queryInfo.entityInfo.persister.createEntityManager();
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
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

            if (pagination != null) {
                for (Pageable p = pagination; p != null; p = p == null ? null : p.next()) {
                    // TODO possible overflow with both of these.
                    long maxPageSize = p.getSize();
                    query.setFirstResult((int) ((p.getPage() - 1) * maxPageSize));
                    query.setMaxResults((int) p.getSize());
                    List<T> results = query.getResultList();
                    if (results.isEmpty())
                        p = null;
                    else
                        for (T result : results)
                            submit(result);
                }
            } else {
                // Could align with actual amounts requested if implementing a publisher
                // instead of using Java's SubmissionPublisher.

                long startAt = limit == null ? 0 : (limit.startAt() - 1);
                long maxResults = limit == null ? queryInfo.maxResults : limit.maxResults();

                // TODO possible overflow with both of these.
                if (maxResults > 0)
                    query.setMaxResults((int) maxResults);
                if (startAt > 0)
                    query.setFirstResult((int) startAt);

                List<T> results = query.getResultList();
                for (T result : results)
                    submit(result);
            }
        } catch (Throwable x) {
            closeExceptionally(x);
        } finally {
            if (em != null)
                em.close();
        }
    }
}
