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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.data.Data;
import io.openliberty.data.Entity;
import io.openliberty.data.Query;
import io.openliberty.data.Repository;

public class QueryHandler<T> implements InvocationHandler {
    private static enum QueryType {
        DELETE, MERGE, SELECT, UPDATE
    };

    private final Class<T> beanClass;
    private final Data data;
    private final Entity entity;
    private final Set<Class<?>> entityClassesAvailable; // TODO is this information needed?
    private final String entityName;
    private final DataPersistence persistence;
    private final PersistenceServiceUnit punit;

    @SuppressWarnings("unchecked")
    public QueryHandler(Bean<T> bean, Entity entity) {
        beanClass = (Class<T>) bean.getBeanClass();
        data = beanClass.getAnnotation(Data.class);
        this.entity = entity;
        entityName = entity.value().getSimpleName();

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        Entry<PersistenceServiceUnit, Set<Class<?>>> persistenceInfo = //
                        persistence.getPersistenceInfo(data.value(), beanClass.getClassLoader());
        if (persistenceInfo == null)
            throw new RuntimeException("Persistence layer unavailable for " + data);
        punit = persistenceInfo.getKey();
        entityClassesAvailable = persistenceInfo.getValue();
    }

    private String getRepositoryQuery(String methodName, Object[] args, Class<?>[] paramTypes) {
        if (args == null) {
            if ("count".equals(methodName))
                return "SELECT COUNT(o) FROM " + entityName + " o";
        } else if (args.length == 1) {
            if ("save".equals(methodName))
                return null; // default handling covers this
            if (Iterable.class.equals(paramTypes[0])) {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + entityName + " o WHERE o." + entity.id() + " IN ?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + entityName + " o WHERE o." + entity.id() + " IN ?1";
            } else {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + entityName + " o WHERE o." + entity.id() + "=?1";
                else if ("existsById".equals(methodName))
                    return "SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END FROM " + entityName + " o WHERE o." + entity.id() + "=?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + entityName + " o WHERE o." + entity.id() + "=?1";
            }
        }
        throw new UnsupportedOperationException("Repository method " + methodName + " with parameters " + Arrays.toString(paramTypes));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if (args == null) {
            if ("hashCode".equals(methodName))
                return System.identityHashCode(proxy);
            else if ("toString".equals(methodName))
                return beanClass.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        System.out.println("Handler invoke " + method);

        Class<?> returnType = method.getReturnType();
        Object returnValue;
        QueryType queryType;
        boolean requiresTransaction;

        Query dataQuery = method.getAnnotation(Query.class);
        String jpql = dataQuery == null ? null : dataQuery.value();

        // Repository built-in methods
        if (jpql == null && Repository.class.equals(method.getDeclaringClass()))
            jpql = getRepositoryQuery(methodName, args, method.getParameterTypes());

        // TODO Actual implementation is lacking so we are cheating by
        // temporarily sending in the JPQL directly:
        if (jpql == null) {
            queryType = QueryType.MERGE;
            requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
        } else {
            String q = jpql.toUpperCase();
            if (q.startsWith("SELECT")) {
                queryType = QueryType.SELECT;
                requiresTransaction = false;
            } else if (q.startsWith("UPDATE")) {
                queryType = QueryType.UPDATE;
                requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
            } else if (q.startsWith("DELETE")) {
                queryType = QueryType.DELETE;
                requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
            } else {
                throw new UnsupportedOperationException(jpql);
            }
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
                case MERGE:
                    if (entityClassesAvailable.contains(args[0].getClass()) ||
                        entityClassesAvailable.contains(method.getParameterTypes()[0])) {
                        em.merge(args[0]);
                        em.flush();
                        returnValue = returnType.isInstance(args[0]) ? args[0] : null;
                    } else if (Iterable.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        ArrayList<Object> results = new ArrayList<>();
                        for (Object e : ((Iterable<?>) args[0])) {
                            em.merge(e);
                            results.add(e);
                        }
                        em.flush();
                        returnValue = returnType.isInstance(results) ? results : null;
                    } else {
                        throw new UnsupportedOperationException(method.toString());
                    }
                    break;
                case SELECT:
                    Class<?> returnArrayType = returnType.getComponentType();
                    Class<?> resultType;
                    if (returnArrayType == null)
                        if (Iterable.class.isAssignableFrom(returnType) ||
                            Optional.class.equals(returnType))
                            resultType = entity.value();
                        else
                            resultType = returnType;
                    else
                        resultType = returnArrayType;

                    TypedQuery<?> query = em.createQuery(jpql, resultType);
                    if (args != null)
                        for (int i = 0; i < args.length; i++)
                            query.setParameter(i + 1, args[i]);

                    List<?> results = query.getResultList();

                    if (resultType.equals(returnType))
                        returnValue = results.isEmpty() ? null : results.iterator().next();
                    else if (returnType.isInstance(results))
                        returnValue = results;
                    else if (Optional.class.equals(returnType))
                        returnValue = results.isEmpty() ? Optional.empty() : Optional.of(results.iterator().next());
                    else // TODO convert return type
                        throw new UnsupportedOperationException(methodName + " with return type " + returnType);
                    break;
                case UPDATE:
                case DELETE:
                    jakarta.persistence.Query update = em.createQuery(jpql);
                    if (args != null)
                        for (int i = 0; i < args.length; i++)
                            update.setParameter(i + 1, args[i]);

                    int updateCount = update.executeUpdate();

                    returnValue = toReturnValue(updateCount, returnType);
                    break;
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

    private static final Object toReturnValue(int i, Class<?> returnType) {
        if (int.class.equals(returnType) || Integer.class.equals(returnType) || Number.class.equals(returnType))
            return i;
        else if (long.class.equals(returnType) || Long.class.equals(returnType))
            return Long.valueOf(i);
        else if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
            return i != 0;
        else if (void.class.equals(returnType) || Void.class.equals(returnType))
            return null;
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);
    }
}