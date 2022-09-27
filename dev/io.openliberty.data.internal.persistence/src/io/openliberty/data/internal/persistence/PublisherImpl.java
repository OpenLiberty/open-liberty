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

import jakarta.data.Param;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Using Java's SubmissionPublisher to try this path in the simplest way possible.
 * A real implementation wouldn't do this.
 */
public class PublisherImpl<T> extends SubmissionPublisher<T> implements Runnable {
    private final Object[] args;
    private final Method method;
    private final int numParams; // can differ from args.length due to Consumer/Pagination/Sort/Sorts parameters
    private final QueryInfo queryInfo;

    PublisherImpl(QueryInfo queryInfo, ExecutorService executor, Method method, int numParams, Object[] args) {
        super(executor, 200);

        this.queryInfo = queryInfo;
        this.method = method;
        this.numParams = numParams;
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

            // Could align with actual amounts requested if implementing a publisher
            // instead of using Java's SubmissionPublisher.
            for (int pageSize = 4, pageNum = 1;; pageNum++) {
                query.setFirstResult((pageNum - 1) * pageSize);
                query.setMaxResults(pageSize);
                List<T> results = query.getResultList();
                if (results.isEmpty())
                    break;
                else
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
