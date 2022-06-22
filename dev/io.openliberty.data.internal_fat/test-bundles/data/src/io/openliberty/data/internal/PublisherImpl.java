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
import java.util.List;
import java.util.concurrent.SubmissionPublisher;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import io.openliberty.data.Param;

/**
 * Using Java's SubmissionPublisher to try this path in the simplest way possible.
 * A real implementation wouldn't do this.
 */
public class PublisherImpl<T> extends SubmissionPublisher<T> implements Runnable {
    private final String jpql;
    private final Method method;
    private final Object[] args;
    private final QueryHandler<T> queryHandler;

    PublisherImpl(String jpql, QueryHandler<T> queryHandler, Method method, Object[] args) {
        this.jpql = jpql;
        this.queryHandler = queryHandler;
        this.method = method;
        this.args = args;

        queryHandler.persistence.executor.submit(this);
    }

    @Override
    public void run() {
        EntityManager em = null;
        try {
            em = queryHandler.punit.createEntityManager();
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryHandler.entityClass);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < args.length; i++) {
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
            close();
        } catch (Throwable x) {
            closeExceptionally(x);
        } finally {
            if (em != null)
                em.close();
        }
    }
}
