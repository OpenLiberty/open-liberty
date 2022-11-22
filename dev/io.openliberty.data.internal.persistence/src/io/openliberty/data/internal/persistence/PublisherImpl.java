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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SubmissionPublisher;

import jakarta.data.repository.Limit;
import jakarta.data.repository.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Using Java's SubmissionPublisher to try this path in the simplest way possible.
 * A real implementation wouldn't do this.
 */
public class PublisherImpl<T> extends SubmissionPublisher<T> implements Runnable {
    private final Object[] args;
    private final Limit limit;
    private final Pageable pagination;
    private final QueryInfo queryInfo;

    PublisherImpl(QueryInfo queryInfo, ExecutorService executor, Limit limit, Pageable pagination, Object[] args) {
        super(executor, 200);

        this.queryInfo = queryInfo;
        this.limit = limit;
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
            queryInfo.setParameters(query, args);

            if (pagination != null) {
                for (Pageable p = pagination; p != null; p = p == null ? null : p.next()) {
                    // TODO Keyset pagination
                    int maxPageSize = p.size();
                    query.setFirstResult(RepositoryImpl.computeOffset(p.page(), maxPageSize));
                    query.setMaxResults(maxPageSize);
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
