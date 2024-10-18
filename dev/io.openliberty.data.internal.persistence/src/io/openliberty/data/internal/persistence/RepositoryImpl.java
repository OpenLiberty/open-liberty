/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;
import static jakarta.data.repository.By.ID;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.QueryInfo.Type;
import io.openliberty.data.internal.persistence.cdi.DataExtension;
import io.openliberty.data.internal.persistence.cdi.FutureEMBuilder;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataConnectionException;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Inheritance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final ThreadLocal<Deque<AutoCloseable>> defaultMethodResources = new ThreadLocal<>();

    private static final List<String> LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES = //
                    List.of(Insert.class.getSimpleName(),
                            Save.class.getSimpleName(),
                            Update.class.getSimpleName());

    private final AtomicBoolean isDisposed = new AtomicBoolean();
    final CompletableFuture<EntityInfo> primaryEntityInfoFuture;
    final DataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    final Class<R> repositoryInterface;
    final EntityValidator validator;

    @FFDCIgnore(CompletionException.class)
    public RepositoryImpl(DataProvider provider,
                          DataExtension extension,
                          FutureEMBuilder futureEMBuilder,
                          Class<R> repositoryInterface,
                          Class<?> primaryEntityClass,
                          Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
        EntityManagerBuilder builder;
        try {
            builder = futureEMBuilder.join();
        } catch (CompletionException x) {
            // The CompletionException does not have the current stack. Replace it.
            Throwable cause = x.getCause();
            if (cause != null)
                x = new CompletionException(cause.getMessage(), cause);
            throw x;
        }

        // EntityManagerBuilder implementations guarantee that the future
        // in the following map will be completed even if an error occurs
        this.primaryEntityInfoFuture = primaryEntityClass == null //
                        ? null //
                        : builder.entityInfoMap.computeIfAbsent(primaryEntityClass,
                                                                EntityInfo::newFuture);
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        Object validation = provider.validationService;
        this.validator = validation == null //
                        ? null //
                        : EntityValidator.newInstance(validation, repositoryInterface);

        // reusable instance for supplying this instance to completion stages:
        CompletableFuture<RepositoryImpl<?>> thisCF = queriesPerEntityClass.isEmpty() //
                        ? null //
                        : CompletableFuture.completedFuture(this);

        List<CompletableFuture<EntityInfo>> entityInfoFutures = new ArrayList<>();
        List<QueryInfo> entitylessQueryInfos = null;

        for (Entry<Class<?>, List<QueryInfo>> entry : queriesPerEntityClass.entrySet()) {
            Class<?> entityClass = entry.getKey();

            if (Query.class.equals(entityClass)) {
                entitylessQueryInfos = entry.getValue();
            } else {
                boolean inheritance = entityClass.getAnnotation(Inheritance.class) != null; // TODO what do we need to do this with?

                CompletableFuture<EntityInfo> entityInfoFuture = //
                                builder.entityInfoMap.computeIfAbsent(entityClass,
                                                                      EntityInfo::newFuture);
                entityInfoFutures.add(entityInfoFuture);

                for (QueryInfo queryInfo : entry.getValue()) {
                    if (queryInfo.type == QueryInfo.Type.RESOURCE_ACCESS) {
                        queryInfo.validateParams = validator != null &&
                                                   validator.isValidatable(queryInfo.method)[1];
                        queries.put(queryInfo.method,
                                    CompletableFuture.completedFuture(queryInfo));
                    } else {
                        queries.put(queryInfo.method,
                                    entityInfoFuture.thenCombine(thisCF,
                                                                 queryInfo::init));
                    }
                }
            }
        }

        if (entitylessQueryInfos != null) {
            if (entityInfoFutures.isEmpty()) {
                for (QueryInfo queryInfo : entitylessQueryInfos) {
                    MappingException x = exc(MappingException.class,
                                             "CWWKD1001.no.primary.entity",
                                             queryInfo.method,
                                             repositoryInterface.getName(),
                                             "DataRepository<EntityClass, EntityIdClass>");
                    queries.put(queryInfo.method, CompletableFuture.failedFuture(x));
                }
            }

            CompletableFuture<?>[] futures = entityInfoFutures //
                            .toArray(new CompletableFuture<?>[entityInfoFutures.size()]);
            CompletableFuture<Map<String, CompletableFuture<EntityInfo>>> allEntityInfo = //
                            CompletableFuture.allOf(futures) //
                                            .handle((VOID, x) -> x) //
                                            .thenCombine(CompletableFuture.completedFuture(entityInfoFutures),
                                                         this::allEntityInfoAsMap);
            for (QueryInfo queryInfo : entitylessQueryInfos) {
                queries.put(queryInfo.method, allEntityInfo //
                                .thenCombine(CompletableFuture.completedFuture(this),
                                             queryInfo::init));
            }
        }
    }

    /**
     * Constructs a map of entity name to completed EntityInfo future.
     *
     * @param entityInfoFutures completed futures for entity information.
     * @param x                 failure if any.
     * @return map of entity name to completed EntityInfo future.
     */
    private Map<String, CompletableFuture<EntityInfo>> //
                    allEntityInfoAsMap(Throwable x,
                                       List<CompletableFuture<EntityInfo>> futures) {
        Map<String, CompletableFuture<EntityInfo>> entityInfos = new HashMap<>();
        for (CompletableFuture<EntityInfo> future : futures) {
            if (future.isCompletedExceptionally()) {
                entityInfos.putIfAbsent(EntityInfo.FAILED, future);
            } else if (future.isDone()) {
                EntityInfo entityInfo = future.join();
                CompletableFuture<EntityInfo> conflict = //
                                entityInfos.put(entityInfo.name, future);
                if (entityInfo.recordClass != null && conflict == null) {
                    int end = entityInfo.name.length() -
                              EntityInfo.RECORD_ENTITY_SUFFIX.length();
                    String recordName = entityInfo.name.substring(0, end);
                    conflict = entityInfos.put(recordName, future);
                }
                if (conflict != null) {
                    MappingException conflictX;
                    EntityInfo conflictInfo = conflict.join(); // already completed
                    List<String> classNames = List //
                                    .of(entityInfo.getType().getName(),
                                        conflictInfo.getType().getName());
                    if (entityInfo.recordClass == null &&
                        conflictInfo.recordClass == null) {
                        conflictX = exc(MappingException.class,
                                        "CWWKD1068.entity.name.conflict",
                                        repositoryInterface.getName(),
                                        entityInfo.name,
                                        classNames,
                                        List.of(Entity.class.getName(),
                                                Table.class.getName()));
                    } else { // conflict involving one or more record entity
                        String longerName = entityInfo.name;
                        String shorterName = entityInfo.name;
                        if (conflictInfo.name.length() > longerName.length())
                            longerName = conflictInfo.name;
                        else
                            shorterName = conflictInfo.name;

                        conflictX = exc(MappingException.class,
                                        "CWWKD1069.record.entity.name.conflict",
                                        repositoryInterface.getName(),
                                        shorterName,
                                        classNames,
                                        longerName);
                    }
                    entityInfos.putIfAbsent(EntityInfo.FAILED,
                                            CompletableFuture.failedFuture(conflictX));
                }
            } else {
                entityInfos.putIfAbsent(EntityInfo.FAILED,
                                        CompletableFuture.failedFuture(x));
            }
        }
        return entityInfos;
    }

    /**
     * Invoked when the bean for the repository is disposed.
     */
    public void beanDisposed() {
        isDisposed.set(true);
    }

    /**
     * Execute a repository count query.
     *
     * @param em        entity manager.
     * @param queryInfo query information.
     * @param args      method parameters.
     * @return the count, converted to a type that is compatible with the
     *         method signature.
     * @throws Exception        if an error occurs.
     * @throws MappingException if the count cannot be converted to the
     *                              requested type.
     */
    @Trivial
    private Object count(EntityManager em,
                         QueryInfo queryInfo,
                         Object... args) throws Exception {
        Class<?> type = queryInfo.singleType;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "count", em, queryInfo,
                     // method args have already been logged if loggable
                     "to be returned as " + type.getName());

        TypedQuery<Long> query = em.createQuery(queryInfo.jpql, Long.class);
        queryInfo.setParameters(query, args);

        Long count = query.getSingleResult();

        Object returnValue;
        if (long.class.equals(type) ||
            Long.class.equals(type) ||
            type.isAssignableFrom(Long.class)) {
            returnValue = count;
        } else {
            if (int.class.equals(type) || Integer.class.equals(type))
                if (count > Integer.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              queryInfo.method.getName(),
                              repositoryInterface.getName(),
                              queryInfo.method.getGenericReturnType().getTypeName(),
                              "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ')');
                else
                    returnValue = count.intValue();
            else if (short.class.equals(type) || Short.class.equals(type))
                if (count > Short.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              queryInfo.method.getName(),
                              repositoryInterface.getName(),
                              queryInfo.method.getGenericReturnType().getTypeName(),
                              "Short.MAX_VALUE (" + Short.MAX_VALUE + ')');
                else
                    returnValue = count.shortValue();
            else if (byte.class.equals(type) || Byte.class.equals(type))
                if (count > Byte.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              queryInfo.method.getName(),
                              repositoryInterface.getName(),
                              queryInfo.method.getGenericReturnType().getTypeName(),
                              "Byte.MAX_VALUE (" + Byte.MAX_VALUE + ')');
                else
                    returnValue = count.byteValue();
            else if (BigInteger.class.equals(type))
                returnValue = BigInteger.valueOf(count);
            else if (BigDecimal.class.equals(type))
                returnValue = BigDecimal.valueOf(count);
            else
                throw exc(MappingException.class,
                          "CWWKD1049.count.convert.err",
                          count,
                          queryInfo.method.getName(),
                          repositoryInterface.getName(),
                          queryInfo.method.getGenericReturnType().getTypeName());
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        if (Optional.class.equals(returnType)) {
            returnValue = Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue);
        }

        if (trace && tc.isEntryEnabled())
            if (count == returnValue)
                Tr.exit(this, tc, "count", returnValue);
            else
                Tr.exit(this, tc, "count", count + " converted to " + returnValue);
        return returnValue;
    }

    /**
     * Create a new EmptyResultException.
     *
     * @param method repository method that unexpectedly finds an empty result.
     * @return the EmptyResultException.
     */
    @Trivial
    private EmptyResultException excEmptyResult(Method method) {
        return exc(EmptyResultException.class,
                   "CWWKD1053.empty.result",
                   method.getGenericReturnType().getTypeName(),
                   method.getName(),
                   repositoryInterface.getName(),
                   List.of(List.class.getSimpleName(),
                           Optional.class.getSimpleName(),
                           Page.class.getSimpleName(),
                           CursoredPage.class.getSimpleName(),
                           Stream.class.getSimpleName()));
    }

    /**
     * Create a new NonUniqueResultException.
     *
     * @param method repository method that unexpectedly finds multiple results.
     * @return the NonUniqueResultException.
     */
    @Trivial
    private NonUniqueResultException excNonUniqueResult(QueryInfo queryInfo,
                                                        int numResults) {
        throw exc(NonUniqueResultException.class,
                  queryInfo.method.getName(),
                  repositoryInterface.getName(),
                  queryInfo.method.getGenericReturnType().getTypeName(),
                  numResults,
                  List.of(// In a future release: @Find @First findByX(...)
                          "findFirstByX(...)",
                          "findByX(..., Limit.of(1))"));
    }

    /**
     * Replaces an exception with a Jakarta Data specification-defined exception,
     * chaining the original exception as the cause.
     * This method replaces all exceptions that are not RuntimeExceptions.
     * For RuntimeExceptions, it only replaces those that are
     * jakarta.persistence.PersistenceException (and subclasses).
     *
     * @param original exception to possibly replace.
     * @return exception to replace with, if any. Otherwise, the original.
     */
    @Trivial
    static RuntimeException failure(Exception original) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        RuntimeException x = null;
        if (original instanceof PersistenceException) {
            for (Throwable cause = original; x == null && cause != null; cause = cause.getCause()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "checking " + cause.getClass().getName() + " with message " + cause.getMessage());

                // TODO If this ever becomes real code, it should be delegated to the JDBC integration layer
                // where there is more thorough logic that takes into account configuration and database differences
                if (cause instanceof SQLRecoverableException
                    || cause instanceof SQLNonTransientConnectionException
                    || cause instanceof SQLTransientConnectionException)
                    x = new DataConnectionException(original);
                else if (cause instanceof SQLSyntaxErrorException)
                    x = new MappingException(original);
                else if (cause instanceof SQLIntegrityConstraintViolationException)
                    x = new EntityExistsException(original);
            }
            if (x == null) {
                if (original instanceof OptimisticLockException)
                    x = new OptimisticLockingFailureException(original);
                else if (original instanceof jakarta.persistence.EntityExistsException)
                    x = new EntityExistsException(original);
                else if (original instanceof NoResultException)
                    x = new EmptyResultException(original);
                else if (original instanceof jakarta.persistence.NonUniqueResultException)
                    x = new NonUniqueResultException(original);
                else
                    x = new DataException(original);
            }
        } else if (original instanceof CompletionException) {
            Throwable cause = original.getCause();
            if (cause == null)
                x = new MappingException(original);
            else if (DataException.class.equals(cause.getClass()))
                x = new DataException(cause.getMessage(), original);
            else if (DataConnectionException.class.equals(cause.getClass()))
                x = new DataConnectionException(cause.getMessage(), original);
            else if (EmptyResultException.class.equals(cause.getClass()))
                x = new EmptyResultException(cause.getMessage(), original);
            else if (MappingException.class.equals(cause.getClass()))
                x = new MappingException(cause.getMessage(), original);
            else if (NonUniqueResultException.class.equals(cause.getClass()))
                x = new NonUniqueResultException(cause.getMessage(), original);
            else if (UnsupportedOperationException.class.equals(cause.getClass()))
                x = new UnsupportedOperationException(cause.getMessage(), original);
            else
                x = new MappingException(original);
        } else if (original instanceof IllegalArgumentException) {
            if (original.getCause() == null) // raised by Liberty
                x = (IllegalArgumentException) original;
            else // raised by Jakarta Persistence provider
                 // Example: Problem compiling [SELECT o FROM Account o WHERE (o.accountId>?1)]. The
                 // relationship mapping 'o.accountId' cannot be used in conjunction with the > operator
                x = new MappingException(original);
        } else if (original instanceof RuntimeException) {
            // Per EclipseLink, "This exception is used for any problem that is detected with a descriptor or mapping"
            if ("org.eclipse.persistence.exceptions.DescriptorException".equals(original.getClass().getName()))
                x = new MappingException(original);
            else
                x = (RuntimeException) original;
        } else {
            x = new DataException(original);
        }

        if (trace && tc.isDebugEnabled())
            if (x == original)
                Tr.debug(tc, "Failure occurred: " + x.getClass().getName());
            else
                Tr.debug(tc, original.getClass().getName() + " replaced with " + x.getClass().getName());
        return x;
    }

    /**
     * Finds and updates entities (or records) in the database.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by the repository Update method signature.
     * @throws OptimisticLockingFailureException if an entity is not found in the database.
     * @throws Exception                         if an error occurs.
     */
    private Object findAndUpdate(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        List<Object> results;

        boolean hasSingularEntityParam = false;
        if (queryInfo.entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = new ArrayList<>(length);
            for (int i = 0; i < length; i++)
                results.add(findAndUpdateOne(Array.get(arg, i), queryInfo, em));
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            results = new ArrayList<>();
            if (arg instanceof Iterable) {
                for (Object e : ((Iterable<?>) arg))
                    results.add(findAndUpdateOne(e, queryInfo, em));
            } else {
                hasSingularEntityParam = true;
                results = new ArrayList<>(1);
                results.add(findAndUpdateOne(arg, queryInfo, em));
            }
        }
        em.flush();

        Class<?> returnType = queryInfo.method.getReturnType();
        if (void.class.equals(returnType) || Void.class.equals(returnType))
            return null;

        if (queryInfo.entityInfo.recordClass != null)
            for (int i = 0; i < results.size(); i++)
                results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

        Object returnValue;
        if (queryInfo.returnArrayType != null) {
            Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
            returnValue = results.toArray(newArray);
        } else {
            Class<?> multiType = queryInfo.multiType;
            if (multiType == null)
                if (results.size() == 1)
                    returnValue = results.get(0);
                else if (results.isEmpty())
                    returnValue = null;
                else
                    throw excNonUniqueResult(queryInfo, results.size());
            else if (multiType.isInstance(results))
                returnValue = results;
            else if (Stream.class.equals(multiType))
                returnValue = results.stream();
            else if (Iterable.class.isAssignableFrom(multiType))
                returnValue = queryInfo.convertToIterable(results, null, multiType);
            else if (Iterator.class.equals(multiType))
                returnValue = results.iterator();
            else
                throw exc(MappingException.class,
                          "CWWKD1003.lifecycle.rtrn.err",
                          queryInfo.method.getGenericReturnType().getTypeName(),
                          queryInfo.method.getName(),
                          repositoryInterface.getName(),
                          "Update",
                          getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                              hasSingularEntityParam,
                                              false));
        }

        if (Optional.class.equals(returnType)) {
            returnValue = returnValue == null ? Optional.empty() : Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (returnValue != null && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.lifecycle.rtrn.err",
                      queryInfo.method.getGenericReturnType().getTypeName(),
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      "Update",
                      getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                          hasSingularEntityParam,
                                          false));
        }

        return returnValue;
    }

    /**
     * Finds an entity (or record) in the database, locks it for subsequent update,
     * and updates the entity found in the database to match the desired state of the supplied entity.
     *
     * @param e         the entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the entity that is written to the database. Never null.
     * @throws OptimisticLockingException if the entity is not found.
     * @throws Exception                  if an error occurs.
     */
    private Object findAndUpdateOne(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;

        int versionParamIndex = entityInfo.idClassAttributeAccessors == null //
                        ? 2 //
                        : (entityInfo.idClassAttributeAccessors.size() + 1);
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = queryInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = entityInfo.attributeNames.get(ID);
            id = queryInfo.getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        Class<?> entityClass = queryInfo.singleType.equals(entityInfo.recordClass) //
                        ? entityInfo.entityClass //
                        : queryInfo.singleType;

        TypedQuery<?> query = em.createQuery(jpql, entityClass);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        if (entityInfo.idClassAttributeAccessors == null) {
            int p = 1;
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(id));
                query.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(version));
                query.setParameter(p, version);
            }
        } else {
            queryInfo.setParametersFromIdClassAndVersion(1, query, e, version);
        }

        List<?> results = query.getResultList();

        if (results.isEmpty()) {
            List<String> entityProps = new ArrayList<>(2);
            if (id != null)
                entityProps.add(queryInfo.loggableAppend(idAttributeName,
                                                         "=", id));
            if (entityInfo.versionAttributeName != null && version != null)
                entityProps.add(queryInfo.loggableAppend(entityInfo.versionAttributeName,
                                                         "=", version));
            throw exc(OptimisticLockingFailureException.class,
                      "CWWKD1050.opt.lock.exc",
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      e.getClass().getName(),
                      entityProps,
                      LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
        }

        return em.merge(queryInfo.toEntity(e));
    }

    /**
     * Return a name for the parameter, suitable for display in an NLS message.
     *
     * @param param parameter
     * @param index zero-based method index.
     * @return parameter name.
     */
    @Trivial
    private static final String getName(Parameter param, int index) {
        return param.isNamePresent() //
                        ? param.getName() //
                        : ("(" + (index + 1) + ")");
    }

    /**
     * Request an instance of a resource of the specified type.
     *
     * @param method the repository method.
     * @return instance of the resource. Never null.
     * @throws UnsupportedOperationException if the type of resource is not available.
     */
    private <T> T getResource(Method method) {
        Deque<AutoCloseable> resources = defaultMethodResources.get();
        Object resource = null;
        Class<?> type = method.getReturnType();
        if (EntityManager.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder.createEntityManager();
        else if (DataSource.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder //
                            .getDataSource(method, repositoryInterface);
        else if (Connection.class.equals(type))
            try {
                resource = primaryEntityInfoFuture.join().builder //
                                .getDataSource(method, repositoryInterface) //
                                .getConnection();
            } catch (SQLException x) {
                throw new DataConnectionException(x);
            }

        if (resource == null)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1044.invalid.resource.type",
                      method.getName(),
                      repositoryInterface.getName(),
                      type.getName(),
                      List.of(Connection.class.getName(),
                              DataSource.class.getName(),
                              EntityManager.class.getName()));

        if (resource instanceof AutoCloseable) {
            if (resources == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    String thisClassName = getClass().getName();

                    // skip Thread.getStackTrace and RepositoryImpl in stack,
                    int s = 0;
                    while (++s < stack.length &&
                           stack[s].getClassName().equals(thisClassName));
                    StackTraceElement[] shortened = //
                                    new StackTraceElement[stack.length - s];
                    System.arraycopy(stack, s, shortened, 0, shortened.length);

                    Tr.debug(this, tc,
                             type.getSimpleName() + " accessed outside of repository default method",
                             (Object[]) shortened);
                }
            } else {
                resources.add((AutoCloseable) resource);
            }
        }

        @SuppressWarnings("unchecked")
        T t = (T) resource;
        return t;
    }

    /**
     * Returns some of the more commonly used return types that are valid for a life cycle method.
     *
     * @param singularClassName        simple class name of the entity
     * @param hasSingularEntityParam   if the life cycle method entity parameter is singular (not an Iterable or array)
     * @param includeBooleanAndNumeric whether to include boolean and numeric types as valid.
     * @return some of the more commonly used return types that are valid for a life cycle method.
     */
    private List<String> getValidReturnTypes(String singularClassName, boolean hasSingularEntityParam, boolean includeBooleanAndNumeric) {
        List<String> validReturnTypes = new ArrayList<>();
        if (includeBooleanAndNumeric) {
            validReturnTypes.add("boolean");
            validReturnTypes.add("int");
            validReturnTypes.add("long");
        }

        validReturnTypes.add("void");

        if (hasSingularEntityParam) {
            validReturnTypes.add(singularClassName);
        } else {
            validReturnTypes.add(singularClassName + "[]");
            validReturnTypes.add("List<" + singularClassName + ">");
        }

        return validReturnTypes;
    }

    /**
     * Inserts entities (or records) into the database.
     * An error is raised if any of the entities (or records) already exist in the database.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by Insert method signature.
     * @throws Exception if an error occurs.
     */
    private Object insert(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        boolean resultVoid = void.class.equals(queryInfo.singleType) ||
                             Void.class.equals(queryInfo.singleType);
        ArrayList<Object> results;

        boolean hasSingularEntityParam = false;
        if (queryInfo.entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = resultVoid ? null : new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object entity = queryInfo.toEntity(Array.get(arg, i));
                em.persist(entity);
                if (results != null)
                    results.add(entity);
            }
            em.flush();
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            if (arg instanceof Iterable) {
                results = resultVoid ? null : new ArrayList<>();
                for (Object e : ((Iterable<?>) arg)) {
                    Object entity = queryInfo.toEntity(e);
                    em.persist(entity);
                    if (results != null)
                        results.add(entity);
                }
                em.flush();
            } else {
                hasSingularEntityParam = true;
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = queryInfo.toEntity(arg);
                em.persist(entity);
                em.flush();
                if (results != null)
                    results.add(entity);
            }
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (queryInfo.entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

            if (queryInfo.returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
                returnValue = results.toArray(newArray);
            } else {
                Class<?> multiType = queryInfo.multiType;
                if (multiType == null)
                    if (results.size() == 1)
                        returnValue = results.get(0);
                    else if (results.isEmpty())
                        returnValue = null;
                    else
                        throw excNonUniqueResult(queryInfo, results.size());
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = queryInfo.convertToIterable(results, null, multiType);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw exc(MappingException.class,
                              "CWWKD1003.lifecycle.rtrn.err",
                              queryInfo.method.getGenericReturnType().getTypeName(),
                              queryInfo.method.getName(),
                              repositoryInterface.getName(),
                              "Insert",
                              getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                                  hasSingularEntityParam,
                                                  false));
            }
        }

        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.lifecycle.rtrn.err",
                      queryInfo.method.getGenericReturnType().getTypeName(),
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      "Insert",
                      getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                          hasSingularEntityParam,
                                          false));
        }

        return returnValue;
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CompletableFuture<QueryInfo> queryInfoFuture = queries.get(method);
        boolean isDefaultMethod = false;

        if (queryInfoFuture == null)
            if (method.isDefault()) {
                isDefaultMethod = true;
            } else {
                String methodName = method.getName();
                if (args == null) {
                    if ("hashCode".equals(methodName))
                        return System.identityHashCode(proxy);
                    else if ("toString".equals(methodName))
                        return repositoryInterface.getName() + "(Proxy)@" + Integer.toHexString(System.identityHashCode(proxy));
                } else if (args.length == 1) {
                    if ("equals".equals(methodName))
                        return proxy == args[0];
                }
                throw new UnsupportedOperationException(method.toString());
            }

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(),
                     provider.loggable(repositoryInterface, method, args));
        try {
            if (isDisposed.get())
                throw exc(IllegalStateException.class,
                          "CWWKD1076.repo.disposed",
                          method.getName(),
                          repositoryInterface.getName(),
                          new StringBuilder("RepositoryImpl@") //
                                          .append(Integer.toHexString(hashCode())) //
                                          .append("/(proxy)@") //
                                          .append(Integer.toHexString(System.identityHashCode(proxy))));

            if (isDefaultMethod) {
                Deque<AutoCloseable> resourceStack = defaultMethodResources.get();
                boolean added;
                if (added = (resourceStack == null))
                    defaultMethodResources.set(resourceStack = new LinkedList<>());
                else
                    resourceStack.add(null); // indicator of nested default method
                try {
                    Object returnValue = InvocationHandler.invokeDefault(proxy, method, args);
                    if (trace && tc.isEntryEnabled())
                        Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), returnValue);
                    return returnValue;
                } finally {
                    for (AutoCloseable resource; (resource = resourceStack.pollLast()) != null;)
                        if (!(resource instanceof EntityManager) ||
                            ((EntityManager) resource).isOpen())
                            try {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "close " + resource);
                                resource.close();
                            } catch (Throwable x) {
                                FFDCFilter.processException(x, getClass().getName(), "1827", this);
                            }
                    if (added)
                        defaultMethodResources.remove();
                }
            }

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;
            Type queryType = null;
            boolean startedTransaction = false;

            try {
                QueryInfo queryInfo = queryInfoFuture.join();
                EntityInfo entityInfo = queryInfo.entityInfo;

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, queryInfo.toString());

                if (queryInfo.validateParams)
                    validator.validateParameters(proxy, method, args);

                switch (queryType = queryInfo.type) {
                    case FIND:
                    case COUNT:
                    case EXISTS:
                    case RESOURCE_ACCESS:
                        break;
                    default:
                        if (Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                            suspendedLTC = provider.localTranCurrent.suspend();
                            provider.tranMgr.begin();
                            startedTransaction = true;
                        }
                }

                switch (queryType) {
                    case SAVE: {
                        em = entityInfo.builder.createEntityManager();
                        returnValue = save(args[0], queryInfo, em);
                        break;
                    }
                    case INSERT: {
                        em = entityInfo.builder.createEntityManager();
                        returnValue = insert(args[0], queryInfo, em);
                        break;
                    }
                    case FIND:
                    case FIND_AND_DELETE: {
                        Limit limit = null;
                        PageRequest pagination = null;
                        List<Sort<Object>> sortList = null;

                        // The first method parameters are used as query parameters.
                        // Beyond that, they can have other purposes such as
                        // pagination and sorting.
                        for (int i = queryInfo.paramCount; //
                                        i < (args == null ? 0 : args.length); //
                                        i++) {
                            Object param = args[i];
                            if (param instanceof Limit) {
                                if (limit == null)
                                    limit = (Limit) param;
                                else
                                    throw exc(UnsupportedOperationException.class,
                                              "CWWKD1017.dup.special.param",
                                              method.getName(),
                                              repositoryInterface.getName(),
                                              "Limit");
                            } else if (param instanceof Order) {
                                @SuppressWarnings("unchecked")
                                Iterable<Sort<Object>> order = (Iterable<Sort<Object>>) param;
                                sortList = queryInfo.supplySorts(sortList, order);
                            } else if (param instanceof PageRequest) {
                                if (pagination == null)
                                    pagination = (PageRequest) param;
                                else
                                    throw exc(UnsupportedOperationException.class,
                                              "CWWKD1017.dup.special.param",
                                              method.getName(),
                                              repositoryInterface.getName(),
                                              "PageRequest");
                            } else if (param instanceof Sort) {
                                @SuppressWarnings("unchecked")
                                List<Sort<Object>> newList = queryInfo.supplySorts(sortList, (Sort<Object>) param);
                                sortList = newList;
                            } else if (param instanceof Sort[]) {
                                @SuppressWarnings("unchecked")
                                List<Sort<Object>> newList = queryInfo.supplySorts(sortList, (Sort<Object>[]) param);
                                sortList = newList;
                            } else if (param == null) {
                                // ignore null for empty Sort...
                            } else {
                                throw exc(DataException.class,
                                          "CWWKD1023.extra.param",
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          queryInfo.paramCount,
                                          method.getParameterTypes()[i].getName(),
                                          queryInfo.jpql);
                            }
                        }

                        if (pagination != null && limit != null)
                            throw exc(UnsupportedOperationException.class,
                                      "CWWKD1018.confl.special.param",
                                      method.getName(),
                                      repositoryInterface.getName(),
                                      "Limit",
                                      "PageRequest");

                        if (sortList == null && queryInfo.hasDynamicSortCriteria())
                            sortList = queryInfo.sorts;

                        if (sortList == null || sortList.isEmpty()) {
                            if (pagination != null) {
                                // BasicRepository.findAll(PageRequest, Order) requires NullPointerException when Order is null.
                                if (queryInfo.paramCount == 0 && queryInfo.method.getParameterCount() == 2
                                    && Order.class.equals(queryInfo.method.getParameterTypes()[1]))
                                    throw new NullPointerException("Order: null");
                                // TODO raise a helpful error to prevent some cases of attempted unordered pagination?
                                //else if (!queryInfo.hasOrderBy)
                                //    throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                //                                            repositoryInterface.getName() +
                                //                                            " repository has a PageRequest parameter without a way to " +
                                //                                            " specify a deterministic ordering of results, which is required " +
                                //                                            " when requesting pages. Use the OrderBy annotation or add a " +
                                //                                            " parameter of type Order, Sort, or Sort... to specify an order" +
                                //                                            " for results."); // TODO NLS
                            }
                        } else {
                            boolean forward = pagination == null || pagination.mode() != PageRequest.Mode.CURSOR_PREVIOUS;
                            StringBuilder q = new StringBuilder(queryInfo.jpql);
                            StringBuilder order = null; // ORDER BY clause based on Sorts
                            for (Sort<?> sort : sortList) {
                                queryInfo.validateSort(sort);
                                order = order == null ? new StringBuilder(100).append(" ORDER BY ") : order.append(", ");
                                queryInfo.generateSort(order, sort, forward);
                            }

                            if (pagination == null || pagination.mode() == PageRequest.Mode.OFFSET) {
                                // offset pagination can be a starting point for cursor pagination
                                queryInfo = queryInfo.withJPQL(q.append(order).toString(), sortList);
                            } else { // CURSOR_NEXT or CURSOR_PREVIOUS
                                queryInfo = queryInfo.withJPQL(null, sortList);
                                queryInfo.generateCursorQueries(q, forward ? order : null, forward ? null : order);
                            }
                        }

                        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType)
                                                                      || CompletionStage.class.equals(returnType));

                        Class<?> multiType = queryInfo.multiType;

                        if (CursoredPage.class.equals(multiType)) {
                            returnValue = new CursoredPageImpl<>(queryInfo, pagination, args);
                        } else if (Page.class.equals(multiType)) {
                            PageRequest req = limit == null //
                                            ? pagination //
                                            : queryInfo.toPageRequest(limit);
                            returnValue = new PageImpl<>(queryInfo, req, args);
                        } else if (pagination != null && !PageRequest.Mode.OFFSET.equals(pagination.mode())) {
                            throw exc(IllegalArgumentException.class,
                                      "CWWKD1035.incompat.page.mode",
                                      pagination.mode(),
                                      method.getName(),
                                      repositoryInterface.getName(),
                                      method.getGenericReturnType().getTypeName(),
                                      CursoredPage.class.getSimpleName());
                        } else {
                            em = entityInfo.builder.createEntityManager();

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "createQuery", queryInfo.jpql, entityInfo.entityClass.getName());

                            TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.entityClass);
                            queryInfo.setParameters(query, args);

                            if (queryInfo.type == QueryInfo.Type.FIND_AND_DELETE)
                                query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

                            int maxResults = limit != null ? limit.maxResults() //
                                            : pagination != null ? pagination.size() //
                                                            : queryInfo.maxResults;

                            int startAt = limit != null ? queryInfo.computeOffset(limit) //
                                            : pagination != null ? queryInfo.computeOffset(pagination) //
                                                            : 0;

                            if (maxResults > 0) {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "limit max results to " + maxResults);
                                query.setMaxResults(maxResults);
                            }
                            if (startAt > 0) {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "start at (0-based) position " + startAt);
                                query.setFirstResult(startAt);
                            }

                            if (multiType != null && BaseStream.class.isAssignableFrom(multiType)) {
                                Stream<?> stream = query.getResultStream();
                                if (Stream.class.equals(multiType))
                                    returnValue = stream;
                                else if (IntStream.class.equals(multiType))
                                    returnValue = stream.mapToInt(queryInfo::toInt);
                                else if (LongStream.class.equals(multiType))
                                    returnValue = stream.mapToLong(queryInfo::toLong);
                                else if (DoubleStream.class.equals(multiType))
                                    returnValue = stream.mapToDouble(queryInfo::toDouble);
                                else
                                    throw exc(UnsupportedOperationException.class,
                                              "CWWKD1046.result.convert.err",
                                              List.class.getName(),
                                              method.getName(),
                                              repositoryInterface.getName(),
                                              method.getGenericReturnType().getTypeName());
                            } else {
                                Class<?> singleType = queryInfo.singleType;

                                List<?> results = query.getResultList();

                                if (trace) {
                                    Tr.debug(this, tc, "result list type: " +
                                                       (results == null ? null : results.getClass().toGenericString()));
                                    if (results != null && !results.isEmpty()) {
                                        Object r0 = results.get(0);
                                        Tr.debug(this, tc, "type of first result: " +
                                                           (r0 == null ? null : r0.getClass().toGenericString()));
                                    }
                                }

                                if (queryInfo.type == QueryInfo.Type.FIND_AND_DELETE)
                                    for (Object result : results)
                                        if (result == null) {
                                            throw exc(DataException.class,
                                                      "CWWKD1046.result.convert.err",
                                                      null,
                                                      method.getName(),
                                                      repositoryInterface.getName(),
                                                      method.getGenericReturnType().getTypeName());
                                        } else if (entityInfo.entityClass.isInstance(result)) {
                                            em.remove(result);
                                        } else if (entityInfo.idClassAttributeAccessors != null) {
                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            int numParams = 0;
                                            for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                                                Object value = accessor instanceof Method ? ((Method) accessor).invoke(result) : ((Field) accessor).get(result);
                                                if (trace && tc.isDebugEnabled())
                                                    Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                             "set ?" + (numParams + 1) + ' ' + queryInfo.loggable(value));
                                                delete.setParameter(++numParams, value);
                                            }
                                            delete.executeUpdate();
                                        } else { // is return value the entity or id?
                                            Object value = result;
                                            if (entityInfo.entityClass.isInstance(result) ||
                                                entityInfo.recordClass != null && entityInfo.recordClass.isInstance(result)) {
                                                List<Member> accessors = entityInfo.attributeAccessors.get(entityInfo.attributeNames.get(ID));
                                                if (accessors == null || accessors.isEmpty())
                                                    throw exc(MappingException.class,
                                                              "CWWKD1025.missing.id.prop",
                                                              entityInfo.getType().getName(),
                                                              method.getName(),
                                                              repositoryInterface);
                                                for (Member accessor : accessors)
                                                    value = accessor instanceof Method ? ((Method) accessor).invoke(value) : ((Field) accessor).get(value);
                                            } else if (!entityInfo.idType.isInstance(value)) {
                                                value = queryInfo.convert(result,
                                                                          entityInfo.idType,
                                                                          false);
                                                if (value == result)
                                                    throw exc(MappingException.class,
                                                              "CWWKD1006.delete.rtrn.err",
                                                              method.getGenericReturnType().getTypeName(),
                                                              method.getName(),
                                                              repositoryInterface.getName(),
                                                              entityInfo.getType().getName(),
                                                              entityInfo.idType);
                                            }

                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            if (trace && tc.isDebugEnabled())
                                                Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                         "set ?1 " + queryInfo.loggable(value));
                                            delete.setParameter(1, value);
                                            delete.executeUpdate();
                                        }

                                if (results.isEmpty() && queryInfo.isOptional) {
                                    returnValue = null;
                                } else if (multiType == null && entityInfo.entityClass.equals(singleType)) {
                                    returnValue = oneResult(queryInfo, results);
                                } else if (multiType != null && multiType.isInstance(results) && (results.isEmpty() || !(results.get(0) instanceof Object[]))) {
                                    returnValue = results;
                                } else if (multiType != null && Iterable.class.isAssignableFrom(multiType)) {
                                    returnValue = queryInfo.convertToIterable(results,
                                                                              singleType,
                                                                              multiType);
                                } else if (Iterator.class.equals(multiType)) {
                                    returnValue = results.iterator();
                                } else if (queryInfo.returnArrayType != null) {
                                    int size = results.size();
                                    Object firstNonNullResult = null;
                                    for (Object result : results)
                                        if (result != null) {
                                            firstNonNullResult = result;
                                            break;
                                        }
                                    if (firstNonNullResult == null
                                        || queryInfo.type == QueryInfo.Type.FIND_AND_DELETE
                                        || queryInfo.returnArrayType != Object.class && queryInfo.returnArrayType.isInstance(firstNonNullResult)
                                        || queryInfo.returnArrayType.isPrimitive() && isWrapperClassFor(queryInfo.returnArrayType, firstNonNullResult.getClass())) {
                                        returnValue = Array.newInstance(queryInfo.returnArrayType, size);
                                        int i = 0;
                                        for (Object result : results)
                                            Array.set(returnValue, i++, result);
                                    } else if (firstNonNullResult.getClass().isArray()) {
                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(this, tc, "convert " + firstNonNullResult.getClass().getName() +
                                                               " to " + queryInfo.returnArrayType.getName());
                                        if (queryInfo.returnArrayType.isArray()) {
                                            // convert List<Object[]> to array of array
                                            returnValue = Array.newInstance(queryInfo.returnArrayType, size);
                                            int i = 0;
                                            for (Object result : results)
                                                if (result == null) {
                                                    Array.set(returnValue, i++, result);
                                                } else {
                                                    // Object[] needs conversion to returnArrayType
                                                    Class<?> subarrayType = queryInfo.returnArrayType.getComponentType();
                                                    int len = Array.getLength(result);
                                                    Object subarray = Array.newInstance(subarrayType, len);
                                                    for (int j = 0; j < len; j++) {
                                                        Object element = Array.get(result, j);
                                                        if (!subarrayType.isInstance(element))
                                                            element = queryInfo.convert(element,
                                                                                        subarrayType,
                                                                                        true);
                                                        Array.set(subarray, j, element);
                                                    }
                                                    Array.set(returnValue, i++, subarray);
                                                }
                                        } else if (size == 1) {
                                            // convert size 1 List<Object[]> to array
                                            if (queryInfo.isOptional && firstNonNullResult.getClass().equals(queryInfo.singleType))
                                                returnValue = firstNonNullResult;
                                            else {
                                                int len = Array.getLength(firstNonNullResult);
                                                returnValue = Array.newInstance(queryInfo.returnArrayType, len);
                                                for (int i = 0; i < len; i++) {
                                                    Object element = Array.get(firstNonNullResult, i);
                                                    if (!queryInfo.returnArrayType.isInstance(element))
                                                        element = queryInfo.convert(element,
                                                                                    queryInfo.returnArrayType,
                                                                                    true);
                                                    Array.set(returnValue, i, element);
                                                }
                                            }
                                        } else {
                                            // List<Object[]> with multiple Object[] elements
                                            // cannot convert to a one dimensional array
                                            throw excNonUniqueResult(queryInfo, size);
                                        }
                                    } else {
                                        throw exc(MappingException.class,
                                                  "CWWKD1055.incompat.result",
                                                  queryInfo.loggableAppend(firstNonNullResult.getClass().getName(),
                                                                           " (", firstNonNullResult, ")"),
                                                  method.getGenericReturnType().getTypeName(),
                                                  method.getName(),
                                                  repositoryInterface.getName());
                                    }
                                } else if (results.isEmpty()) {
                                    throw excEmptyResult(method);
                                } else { // single result of other type
                                    returnValue = oneResult(queryInfo, results);
                                    if (returnValue != null &&
                                        !singleType.isAssignableFrom(returnValue.getClass()))
                                        returnValue = queryInfo.convert(returnValue,
                                                                        queryInfo.singleType,
                                                                        true);
                                }
                            }
                        }

                        if (queryInfo.isOptional) {
                            returnValue = returnValue == null
                                          || returnValue instanceof Collection &&
                                             ((Collection<?>) returnValue).isEmpty()
                                          || returnValue instanceof Page
                                             && !((Page<?>) returnValue).hasContent() //
                                                             ? Optional.empty() //
                                                             : Optional.of(returnValue);
                        }

                        if (CompletableFuture.class.equals(returnType) ||
                            CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
                        break;
                    }
                    case DELETE:
                    case UPDATE: {
                        em = entityInfo.builder.createEntityManager();

                        jakarta.persistence.Query update = em.createQuery(queryInfo.jpql);
                        queryInfo.setParameters(update, args);

                        int updateCount = update.executeUpdate();

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case DELETE_WITH_ENTITY_PARAM: {
                        em = entityInfo.builder.createEntityManager();

                        Object arg = args[0] instanceof Stream //
                                        ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) //
                                        : args[0];
                        int updateCount = 0;
                        int numExpected = 0;

                        if (arg instanceof Iterable) {
                            for (Object e : ((Iterable<?>) arg)) {
                                numExpected++;
                                updateCount += remove(e, queryInfo, em);
                            }
                        } else if (queryInfo.entityParamType.isArray()) {
                            numExpected = Array.getLength(arg);
                            for (int i = 0; i < numExpected; i++)
                                updateCount += remove(Array.get(arg, i), queryInfo, em);
                        } else {
                            numExpected = 1;
                            updateCount = remove(arg, queryInfo, em);
                        }

                        if (updateCount < numExpected)
                            if (numExpected == 1)
                                throw exc(OptimisticLockingFailureException.class,
                                          "CWWKD1051.single.opt.lock.exc",
                                          queryInfo.method.getName(),
                                          repositoryInterface.getName(),
                                          queryInfo.entityInfo.entityClass.getName(),
                                          LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
                            else
                                throw exc(OptimisticLockingFailureException.class,
                                          "CWWKD1052.multi.opt.lock.exc",
                                          queryInfo.method.getName(),
                                          repositoryInterface.getName(),
                                          numExpected - updateCount,
                                          numExpected,
                                          queryInfo.entityInfo.entityClass.getName(),
                                          LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case UPDATE_WITH_ENTITY_PARAM: {
                        em = entityInfo.builder.createEntityManager();

                        Object arg = args[0] instanceof Stream //
                                        ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) //
                                        : args[0];
                        int updateCount = 0;
                        int numExpected = 0;

                        if (arg instanceof Iterable) {
                            for (Object e : ((Iterable<?>) arg)) {
                                numExpected++;
                                updateCount += update(e, queryInfo, em);
                            }
                        } else if (queryInfo.entityParamType.isArray()) {
                            numExpected = Array.getLength(arg);
                            for (int i = 0; i < numExpected; i++)
                                updateCount += update(Array.get(arg, i), queryInfo, em);
                        } else {
                            numExpected = 1;
                            updateCount = update(arg, queryInfo, em);
                        }

                        if (updateCount < numExpected)
                            if (numExpected == 1)
                                throw exc(OptimisticLockingFailureException.class,
                                          "CWWKD1051.single.opt.lock.exc",
                                          queryInfo.method.getName(),
                                          repositoryInterface.getName(),
                                          queryInfo.entityInfo.entityClass.getName(),
                                          LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
                            else
                                throw exc(OptimisticLockingFailureException.class,
                                          "CWWKD1052.multi.opt.lock.exc",
                                          queryInfo.method.getName(),
                                          repositoryInterface.getName(),
                                          numExpected - updateCount,
                                          numExpected,
                                          queryInfo.entityInfo.entityClass.getName(),
                                          LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case UPDATE_WITH_ENTITY_PARAM_AND_RESULT: {
                        em = entityInfo.builder.createEntityManager();
                        returnValue = findAndUpdate(args[0], queryInfo, em);
                        break;
                    }
                    case COUNT: {
                        em = entityInfo.builder.createEntityManager();
                        returnValue = count(em, queryInfo, args);
                        break;
                    }
                    case EXISTS: {
                        em = entityInfo.builder.createEntityManager();

                        jakarta.persistence.Query query = em.createQuery(queryInfo.jpql);
                        query.setMaxResults(1);
                        queryInfo.setParameters(query, args);

                        returnValue = !query.getResultList().isEmpty();

                        if (Optional.class.equals(returnType)) {
                            returnValue = Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
                        break;
                    }
                    case RESOURCE_ACCESS: {
                        returnValue = getResource(method);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException(queryInfo.type.name());
                }

                if (queryInfo.validateResult)
                    validator.validateReturnValue(proxy, method, returnValue);

                failed = false;
            } finally {
                if (em != null)
                    em.close();

                try {
                    if (startedTransaction) {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            provider.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.commit();
                    } else {
                        if (failed && Status.STATUS_ACTIVE == provider.tranMgr.getStatus())
                            provider.tranMgr.setRollbackOnly();
                    }
                } finally {
                    if (suspendedLTC != null)
                        provider.localTranCurrent.resume(suspendedLTC);
                }
            }

            if (trace && tc.isEntryEnabled()) {
                boolean hideValue = queryType == Type.FIND ||
                                    queryType == Type.FIND_AND_DELETE ||
                                    queryType == Type.INSERT ||
                                    queryType == Type.SAVE ||
                                    queryType == Type.UPDATE_WITH_ENTITY_PARAM_AND_RESULT;
                Object valueToLog = hideValue //
                                ? provider.loggable(repositoryInterface, method, returnValue) //
                                : returnValue;
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(),
                        valueToLog);
            }
            return returnValue;
        } catch (Throwable x) {
            if (!isDefaultMethod && x instanceof Exception)
                x = failure((Exception) x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), x);
            throw x;
        }
    }

    /**
     * Indicates if the specified class is a wrapper for the primitive class.
     *
     * @param primitive primitive class.
     * @param cl        another class that might be a wrapper class for the primitive class.
     * @return true if the class is the wrapper class for the primitive class, otherwise false.
     */
    private final boolean isWrapperClassFor(Class<?> primitive, Class<?> cl) {
        return primitive == long.class && cl == Long.class ||
               primitive == int.class && cl == Integer.class ||
               primitive == float.class && cl == Float.class ||
               primitive == double.class && cl == Double.class ||
               primitive == char.class && cl == Character.class ||
               primitive == byte.class && cl == Byte.class ||
               primitive == boolean.class && cl == Boolean.class ||
               primitive == short.class && cl == Short.class;
    }

    /**
     * Requires a single result.
     *
     * @param queryInfo information about the query.
     * @param results   list of results that is expected to have exactly 1 result.
     * @return the single result.
     * @throws EmptyResultException     if the list is empty.
     * @throws NonUniqueResultException if the list has more than 1 result.
     */
    @Trivial
    private final Object oneResult(QueryInfo queryInfo, List<?> results) {
        int size = results.size();
        if (size == 1)
            return results.get(0);
        else if (size == 0)
            throw excEmptyResult(queryInfo.method);
        else
            throw excNonUniqueResult(queryInfo, results.size());
    }

    /**
     * Removes the entity (or record) from the database if its attributes match the database.
     *
     * @param e         the entity or record.
     * @param queryInfo query information that is prepopulated for deleteById.
     * @param em        the entity manager.
     * @return the number of entities deleted (1 or 0).
     * @throws Exception if an error occurs or if the repository method return type is void and
     *                       the entity (or correct version of the entity) was not found.
     */
    private int remove(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> entityClass = queryInfo.entityInfo.getType();

        if (e == null)
            throw exc(NullPointerException.class,
                      "CWWKD1015.null.entity.param",
                      queryInfo.method.getName(),
                      repositoryInterface.getName());

        if (!entityClass.isInstance(e))
            throw exc(IllegalArgumentException.class,
                      "CWWKD1016.incompat.entity.param",
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      entityClass.getName(),
                      e.getClass().getName());

        EntityInfo entityInfo = queryInfo.entityInfo;
        String jpql = queryInfo.jpql;

        int versionParamIndex = (entityInfo.idClassAttributeAccessors == null ? 1 : entityInfo.idClassAttributeAccessors.size()) + 1;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = queryInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = queryInfo.getAttributeName(ID, true);
            id = queryInfo.getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> delete = em.createQuery(jpql, entityInfo.entityClass);

        if (entityInfo.idClassAttributeAccessors == null) {
            int p = 1;
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(id));
                delete.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(version));
                delete.setParameter(p, version);
            }
        } else {
            queryInfo.setParametersFromIdClassAndVersion(1, delete, e, version);
        }

        int numDeleted = delete.executeUpdate();

        if (numDeleted == 0) {
            Class<?> returnType = queryInfo.method.getReturnType();
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                if (idAttributeName == null)
                    idAttributeName = ID;
                List<String> entityProps = new ArrayList<>(2);
                if (id != null)
                    entityProps.add(queryInfo.loggableAppend(idAttributeName,
                                                             "=", id));
                if (entityInfo.versionAttributeName != null && version != null)
                    entityProps.add(queryInfo.loggableAppend(entityInfo.versionAttributeName,
                                                             "=", version));
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1050.opt.lock.exc",
                          queryInfo.method.getName(),
                          repositoryInterface.getName(),
                          e.getClass().getName(),
                          entityProps,
                          LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
            }
        } else if (numDeleted > 1) {
            throw new DataException("Found " + numDeleted + " matching entities."); // ought to be unreachable
        }

        return numDeleted;
    }

    /**
     * Converts an update count to the requested return type.
     *
     * @param i          update count value.
     * @param returnType requested return type.
     * @param queryInfo  query information, which must have type DELETE or UPDATE.
     * @return converted value.
     */
    private final Object toReturnValue(int i, Class<?> returnType, QueryInfo queryInfo) {
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
            result = CompletableFuture.completedFuture(toReturnValue(i, queryInfo.singleType, null));
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1007.updel.rtrn.err",
                      queryInfo.method.getGenericReturnType().getTypeName(),
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      queryInfo.type == Type.DELETE ? "Delete" : "Update");
        return result;
    }

    /**
     * Saves entities (or records) to the database, which can involve an update or an insert,
     * depending on whether the entity already exists.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by the repository Save method signature.
     * @throws Exception if an error occurs.
     */
    private Object save(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        boolean resultVoid = void.class.equals(queryInfo.singleType) ||
                             Void.class.equals(queryInfo.singleType);
        List<Object> results;

        boolean hasSingularEntityParam = false;
        if (queryInfo.entityParamType.isArray()) {
            results = new ArrayList<>();
            int length = Array.getLength(arg);
            for (int i = 0; i < length; i++)
                results.add(em.merge(queryInfo.toEntity(Array.get(arg, i))));
            em.flush();
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            if (Iterable.class.isAssignableFrom(queryInfo.entityParamType)) {
                results = new ArrayList<>();
                for (Object e : ((Iterable<?>) arg))
                    results.add(em.merge(queryInfo.toEntity(e)));
                em.flush();
            } else {
                hasSingularEntityParam = true;
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = em.merge(queryInfo.toEntity(arg));
                if (results != null)
                    results.add(entity);
                em.flush();
            }
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (queryInfo.entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

            if (queryInfo.returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
                returnValue = results.toArray(newArray);
            } else {
                Class<?> multiType = queryInfo.multiType;
                if (multiType == null)
                    if (results.size() == 1)
                        returnValue = results.get(0);
                    else if (results.isEmpty())
                        returnValue = null;
                    else
                        throw excNonUniqueResult(queryInfo, results.size());
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = queryInfo.convertToIterable(results, null, multiType);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw exc(MappingException.class,
                              "CWWKD1003.lifecycle.rtrn.err",
                              queryInfo.method.getGenericReturnType().getTypeName(),
                              queryInfo.method.getName(),
                              repositoryInterface.getName(),
                              "Save",
                              getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                                  hasSingularEntityParam,
                                                  false));
            }
        }

        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.lifecycle.rtrn.err",
                      queryInfo.method.getGenericReturnType().getTypeName(),
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      "Save",
                      getValidReturnTypes(results.get(0).getClass().getSimpleName(),
                                          hasSingularEntityParam,
                                          false));
        }

        return returnValue;
    }

    /**
     * Updates the entity (or record) from the database if its attributes match the database.
     *
     * @param e         the entity or record.
     * @param queryInfo query information that is prepopulated for update by id (and possibly version).
     * @param em        the entity manager.
     * @return the number of entities updated (1 or 0).
     * @throws Exception if an error occurs.
     */
    private int update(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> entityClass = queryInfo.entityInfo.getType();

        if (e == null)
            throw exc(NullPointerException.class,
                      "CWWKD1015.null.entity.param",
                      queryInfo.method.getName(),
                      repositoryInterface.getName());

        if (!entityClass.isInstance(e))
            throw exc(IllegalArgumentException.class,
                      "CWWKD1016.incompat.entity.param",
                      queryInfo.method.getName(),
                      repositoryInterface.getName(),
                      entityClass.getName(),
                      e.getClass().getName());

        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;
        Set<String> attrsToUpdate = entityInfo.attributeNamesForEntityUpdate;

        int versionParamIndex = entityInfo.idClassAttributeAccessors == null //
                        ? (attrsToUpdate.size() + 2) //
                        : (attrsToUpdate.size() +
                           entityInfo.idClassAttributeAccessors.size() + 1);
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = queryInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = entityInfo.attributeNames.get(ID);
            id = queryInfo.getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> update = em.createQuery(jpql, entityInfo.entityClass);

        // parameters for entity attributes to update:
        int p = 1;
        for (String attrName : attrsToUpdate)
            queryInfo.setParameter(p++, update, e, attrName);

        // id and version parameters
        if (entityInfo.idClassAttributeAccessors == null) {
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(id));
                update.setParameter(p++, id);
            }

            if (entityInfo.versionAttributeName != null && version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + queryInfo.loggable(version));
                update.setParameter(p++, version);
            }
        } else { // has IdClass
            queryInfo.setParametersFromIdClassAndVersion(p, update, e, version);
        }

        int numUpdated = update.executeUpdate();

        if (numUpdated > 1)
            throw new DataException("Found " + numUpdated + " matching entities."); // ought to be unreachable
        return numUpdated;
    }
}