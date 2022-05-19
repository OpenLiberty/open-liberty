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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.data.Data;
import io.openliberty.data.Query;

public class QueryHandler<T> implements InvocationHandler {
    private static enum QueryType {
        DELETE, INSERT, SELECT, UPDATE
    };

    private final Class<T> beanClass;
    private final Data data;
    private final List<Class<?>> dataClasses;
    private final DataPersistence persistence;
    private final PersistenceServiceUnit punit;

    @SuppressWarnings("unchecked")
    public QueryHandler(Bean<T> bean) {
        beanClass = (Class<T>) bean.getBeanClass();
        data = beanClass.getAnnotation(Data.class);

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        Entry<PersistenceServiceUnit, List<Class<?>>> persistenceInfo = //
                        persistence.getPersistenceInfo(data.dataStore(), beanClass.getClassLoader());
        if (persistenceInfo == null)
            throw new RuntimeException("Persistence layer unavailable for " + data);
        punit = persistenceInfo.getKey();
        dataClasses = persistenceInfo.getValue();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (args == null || args.length == 0) {
            if ("hashCode".equals(methodName))
                return System.identityHashCode(proxy);
            else if ("toString".equals(methodName))
                return beanClass.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        System.out.println("Handler invoke " + method);

        Object returnValue;
        QueryType queryType;
        boolean requiresTransaction;

        Query dataQuery = method.getAnnotation(Query.class);
        String jpql = dataQuery == null ? null : dataQuery.value();

        // TODO Actual implementation is lacking so we are cheating by
        // temporarily sending in the JPQL directly and assuming SELECT queries only:
        if (jpql == null) {
            queryType = QueryType.INSERT;
            requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
        } else {
            queryType = QueryType.SELECT;
            requiresTransaction = false;
        }

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        try {
            if (requiresTransaction) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                persistence.tranMgr.begin();
            }

            em = punit.createEntityManager();

            switch (queryType) {
                case DELETE:
                    throw new UnsupportedOperationException("not implemented");
                case INSERT:
                    em.persist(args[0]);
                    em.flush();
                    returnValue = null;
                    break;
                case SELECT:
                    Class<?> returnType = method.getReturnType();
                    Class<?> resultType = dataClasses.contains(returnType) ? returnType : dataClasses.get(0); // TODO what if multiple? Specify on method-level anno?

                    TypedQuery<?> query = em.createQuery(jpql, resultType);
                    for (int i = 0; i < args.length; i++)
                        query.setParameter(i + 1, args[i]);
                    List<?> results = query.getResultList();

                    if (resultType.equals(returnType))
                        returnValue = results.isEmpty() ? null : results.iterator().next();
                    // TODO could have other return types
                    else if (List.class.equals(method.getReturnType()))
                        returnValue = results;
                    else
                        throw new UnsupportedOperationException(methodName + " with return type " + returnType);
                    break;
                case UPDATE:
                    throw new UnsupportedOperationException("not implemented");
                default:
                    throw new UnsupportedOperationException(queryType.name());
            }

            failed = false;
        } finally {
            if (em != null)
                em.close();

            if (requiresTransaction) {
                try {
                    int status = persistence.tranMgr.getStatus();
                    if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                        persistence.tranMgr.rollback();
                    else if (status != Status.STATUS_NO_TRANSACTION)
                        persistence.tranMgr.commit();
                } finally {
                    if (suspendedLTC != null)
                        persistence.localTranCurrent.resume(suspendedLTC);
                }
            } else {
                if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus())
                    persistence.tranMgr.setRollbackOnly();
            }
        }
        return returnValue;
    }
}