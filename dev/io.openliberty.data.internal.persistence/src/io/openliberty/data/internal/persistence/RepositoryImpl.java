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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

import jakarta.data.Inheritance;
import jakarta.data.Limit;
import jakarta.data.Param;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R, E> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private final Class<E> defaultEntityClass; // repository methods can return subclasses in the case of @Inheritance
    private final PersistenceDataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    private final Class<R> repositoryInterface;

    public RepositoryImpl(PersistenceDataProvider provider, Class<R> repositoryInterface, Class<E> defaultEntityClass) {
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        this.defaultEntityClass = defaultEntityClass;
        boolean inheritance = defaultEntityClass.getAnnotation(Inheritance.class) != null ||
                              defaultEntityClass.getAnnotation(jakarta.persistence.Inheritance.class) != null;

        CompletableFuture<EntityInfo> defaultEntityInfoFuture = provider.futureEntityInfo(defaultEntityClass);

        for (Method method : repositoryInterface.getMethods()) {
            Class<?> returnType = method.getReturnType();
            Class<?> returnArrayType = returnType.getComponentType();

            Class<?> entityClass = returnArrayType == null ? returnType : returnArrayType;
            if (!inheritance || !defaultEntityClass.isAssignableFrom(entityClass)) // TODO allow other entity types from model
                entityClass = defaultEntityClass;

            CompletableFuture<EntityInfo> entityInfoFuture = entityClass.equals(defaultEntityClass) ? defaultEntityInfoFuture : provider.futureEntityInfo(entityClass);

            queries.put(method, entityInfoFuture.thenCombine(CompletableFuture.completedFuture(method), this::getQueryInfo));
        }
    }

    private QueryInfo getQueryInfo(EntityInfo entityInfo, Method method) {
        return new QueryInfo("SELECT o FROM Products o WHERE o.id=?1", 1, entityInfo, null, null, null, null, null); // TODO copy implementation to here
    }

    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CompletableFuture<QueryInfo> queryInfoFuture = queries.get(method);

        if (queryInfoFuture == null) {
            String methodName = method.getName();
            if (args == null) {
                if ("hashCode".equals(methodName))
                    return System.identityHashCode(proxy);
                else if ("toString".equals(methodName))
                    return repositoryInterface.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
            } else if (args.length == 1) {
                if ("equals".equals(methodName))
                    return proxy == args[0];
            }
            throw new UnsupportedOperationException(method.toString());
        }

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "invoke " + method.getName(), args);
        try {
            QueryInfo queryInfo = queryInfoFuture.join();

            boolean requiresTransaction;
            switch (queryInfo.type) {
                case SELECT:
                case COUNT:
                case EXISTS:
                    requiresTransaction = false;
                    break;
                default:
                    requiresTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus();
            }

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;
            try {
                if (requiresTransaction) {
                    suspendedLTC = provider.localTranCurrent.suspend();
                    provider.tranMgr.begin();
                }

                em = queryInfo.entityInfo.persister.createEntityManager();

                switch (queryInfo.type) {
                    case MERGE:
                        if (queryInfo.saveParamType.isArray()) {
                            ArrayList<Object> results = new ArrayList<>();
                            Object a = args[0];
                            int length = Array.getLength(a);
                            for (int i = 0; i < length; i++)
                                results.add(em.merge(Array.get(a, i)));
                            em.flush();
                            returnValue = results;
                        } else if (Iterable.class.isAssignableFrom(queryInfo.saveParamType)) {
                            ArrayList<Object> results = new ArrayList<>();
                            for (Object e : ((Iterable<?>) args[0]))
                                results.add(em.merge(e));
                            em.flush();
                            returnValue = results;
                        } else {
                            returnValue = em.merge(args[0]);
                            em.flush();
                        }

                        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
                        } else {
                            returnValue = returnType.isInstance(returnValue) ? returnValue : null;
                        }
                        break;
                    case SELECT:
                        TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
                        if (args != null) {
                            Parameter[] params = method.getParameters();
                            for (int i = 0; i < queryInfo.paramCount; i++) {
                                Param param = params[i].getAnnotation(Param.class);
                                if (param == null)
                                    query.setParameter(i + 1, args[i]);
                                else // named parameter
                                    query.setParameter(param.value(), args[i]);
                            }
                        }

                        if (queryInfo.pagination == null) {
                            Limit limit = method.getAnnotation(Limit.class);
                            if (limit != null)
                                query.setMaxResults(limit.value());
                        } else {
                            // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
                            query.setFirstResult((int) queryInfo.pagination.getSkip());
                            query.setMaxResults((int) queryInfo.pagination.getPageSize());
                        }

                        List<?> results = query.getResultList();

                        if (queryInfo.collector != null) {
                            // Collector is more useful on the other path, when combined with pagination
                            Object r = queryInfo.collector.supplier().get();
                            BiConsumer<Object, Object> accumulator = queryInfo.collector.accumulator();
                            for (Object item : results)
                                accumulator.accept(r, item);
                            returnValue = queryInfo.collector.finisher().apply(r);
                            if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType))
                                returnValue = CompletableFuture.completedFuture(returnValue);
                        } else if (queryInfo.consumer != null) {
                            for (Object result : results)
                                queryInfo.consumer.accept(result);
                            return CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType) //
                                            ? CompletableFuture.completedFuture(null) //
                                            : null;
                        } else if (queryInfo.entityInfo.type.equals(returnType)) {
                            returnValue = results.isEmpty() ? null : results.get(0);
                        } else if (returnType.isInstance(results)) {
                            returnValue = results;
                        } else if (queryInfo.returnArrayType != null) {
                            Object r = Array.newInstance(queryInfo.returnArrayType, results.size());
                            int i = 0;
                            for (Object o : results)
                                Array.set(r, i++, o);
                            returnValue = r;
                        } else if (Optional.class.equals(returnType)) {
                            returnValue = results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
                        } else if (Collection.class.isAssignableFrom(returnType)) {
                            try {
                                @SuppressWarnings("unchecked")
                                Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) returnType.getConstructor();
                                Collection<Object> list = c.newInstance();
                                list.addAll(results);
                                returnValue = list;
                            } catch (NoSuchMethodException x) {
                                throw new UnsupportedOperationException(returnType + " lacks public zero parameter constructor.");
                            }
                        } else if (Stream.class.isAssignableFrom(returnType)) {
                            Stream.Builder<Object> builder = Stream.builder();
                            for (Object result : results)
                                builder.accept(result);
                            returnValue = builder.build();
                        } else if (IntStream.class.isAssignableFrom(returnType)) {
                            IntStream.Builder builder = IntStream.builder();
                            for (Object result : results)
                                builder.accept((Integer) result);
                            returnValue = builder.build();
                        } else if (LongStream.class.isAssignableFrom(returnType)) {
                            LongStream.Builder builder = LongStream.builder();
                            for (Object result : results)
                                builder.accept((Long) result);
                            returnValue = builder.build();
                        } else if (DoubleStream.class.isAssignableFrom(returnType)) {
                            DoubleStream.Builder builder = DoubleStream.builder();
                            for (Object result : results)
                                builder.accept((Double) result);
                            returnValue = builder.build();
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            Limit limit = method.getAnnotation(Limit.class);
                            if (limit != null && limit.value() == 1) // single result
                                returnValue = CompletableFuture.completedFuture(results.isEmpty() ? null : results.get(0));
                            else // multiple
                                returnValue = CompletableFuture.completedFuture(results);
                        } else if (results.isEmpty()) {
                            returnValue = null;
                        } else if (results.size() == 1) {
                            // single result
                            returnValue = results.get(0);
                            if (returnValue != null && !returnType.isAssignableFrom(returnValue.getClass())) {
                                // TODO these conversions are not all safe
                                if (double.class.equals(returnType) || Double.class.equals(returnType))
                                    returnValue = ((Number) returnValue).doubleValue();
                                else if (float.class.equals(returnType) || Float.class.equals(returnType))
                                    returnValue = ((Number) returnValue).floatValue();
                                else if (long.class.equals(returnType) || Long.class.equals(returnType))
                                    returnValue = ((Number) returnValue).longValue();
                                else if (int.class.equals(returnType) || Integer.class.equals(returnType))
                                    returnValue = ((Number) returnValue).intValue();
                                else if (short.class.equals(returnType) || Short.class.equals(returnType))
                                    returnValue = ((Number) returnValue).shortValue();
                                else if (byte.class.equals(returnType) || Byte.class.equals(returnType))
                                    returnValue = ((Number) returnValue).byteValue();
                            }
                        } else { // TODO convert other return types?
                            returnValue = results;
                        }
                        break;
                    case UPDATE:
                    case DELETE:
                        jakarta.persistence.Query update = em.createQuery(queryInfo.jpql);
                        if (args != null) {
                            Parameter[] params = method.getParameters();
                            for (int i = 0; i < args.length; i++) {
                                Param param = params[i].getAnnotation(Param.class);
                                if (param == null)
                                    update.setParameter(i + 1, args[i]);
                                else // named parameter
                                    update.setParameter(param.value(), args[i]);
                            }
                        }

                        int updateCount = update.executeUpdate();

                        returnValue = toReturnValue(updateCount, returnType);
                        break;
                    default:
                        throw new UnsupportedOperationException(queryInfo.type.name());
                }

                failed = false;
            } finally {
                if (em != null)
                    em.close();

                if (requiresTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            provider.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.commit();
                    } finally {
                        if (suspendedLTC != null)
                            provider.localTranCurrent.resume(suspendedLTC);
                    }
                } else {
                    if (failed && Status.STATUS_ACTIVE == provider.tranMgr.getStatus())
                        provider.tranMgr.setRollbackOnly();
                }
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), returnValue);
            return returnValue;
        } catch (Throwable x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), x);
            throw x;
        }
    }

    private static final Object toReturnValue(int i, Class<?> returnType) {
        Object result;
        if (int.class.equals(returnType) || Integer.class.equals(returnType) || Number.class.equals(returnType))
            result = i;
        else if (long.class.equals(returnType) || Long.class.equals(returnType))
            result = Long.valueOf(i);
        else if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
            result = i != 0;
        else if (void.class.equals(returnType) || Void.class.equals(returnType))
            result = null;
        else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType))
            result = CompletableFuture.completedFuture(Long.valueOf(i)); // default for completion stages
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);

        return result;
    }
}