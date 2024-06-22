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

import static jakarta.data.repository.By.ID;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;
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
import jakarta.data.repository.Query;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Inheritance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final ThreadLocal<Deque<AutoCloseable>> defaultMethodResources = new ThreadLocal<>();

    private final AtomicBoolean isDisposed = new AtomicBoolean();
    final CompletableFuture<EntityInfo> primaryEntityInfoFuture;
    final DataExtensionProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    final Class<R> repositoryInterface;
    final EntityValidator validator;

    public RepositoryImpl(DataExtensionProvider provider, DataExtension extension, FutureEMBuilder futureEMBuilder,
                          Class<R> repositoryInterface, Class<?> primaryEntityClass,
                          Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
        EntityManagerBuilder builder = futureEMBuilder.join();
        // EntityManagerBuilder implementations guarantee that the future
        // in the following map will be completed even if an error occurs
        this.primaryEntityInfoFuture = primaryEntityClass == null ? null : builder.entityInfoMap.computeIfAbsent(primaryEntityClass, EntityInfo::newFuture);
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        Object validation = provider.validationService();
        this.validator = validation == null ? null : EntityValidator.newInstance(validation, repositoryInterface);

        List<CompletableFuture<EntityInfo>> entityInfoFutures = new ArrayList<>();
        List<QueryInfo> entitylessQueryInfos = null;

        for (Entry<Class<?>, List<QueryInfo>> entry : queriesPerEntityClass.entrySet()) {
            Class<?> entityClass = entry.getKey();

            if (Query.class.equals(entityClass)) {
                entitylessQueryInfos = entry.getValue();
            } else {
                boolean inheritance = entityClass.getAnnotation(Inheritance.class) != null; // TODO what do we need to do this with?

                CompletableFuture<EntityInfo> entityInfoFuture = builder.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
                entityInfoFutures.add(entityInfoFuture);

                for (QueryInfo queryInfo : entry.getValue()) {
                    if (queryInfo.type == QueryInfo.Type.RESOURCE_ACCESS) {
                        queryInfo.validateParams = validator != null && validator.isValidatable(queryInfo.method)[1];
                        queries.put(queryInfo.method, CompletableFuture.completedFuture(queryInfo));
                    } else {
                        queries.put(queryInfo.method, entityInfoFuture.thenCombine(CompletableFuture.completedFuture(this),
                                                                                   queryInfo::init));
                    }
                }
            }
        }

        if (entitylessQueryInfos != null) {
            if (entityInfoFutures.isEmpty()) {
                MappingException x = new MappingException("The " + repositoryInterface.getName() + " repository does not specify an entity class." + // TODO NLS
                                                          " To correct this, have the repository interface extend DataRepository" +
                                                          " or another built-in repository interface and supply the entity class as the first parameter.");
                for (QueryInfo queryInfo : entitylessQueryInfos)
                    queries.put(queryInfo.method, CompletableFuture.failedFuture(x));
            }

            CompletableFuture<?>[] futures = entityInfoFutures.toArray(new CompletableFuture<?>[entityInfoFutures.size()]);
            CompletableFuture<Map<String, CompletableFuture<EntityInfo>>> allEntityInfo = CompletableFuture.allOf(futures) //
                            .handle((ignore, x) -> {
                                Map<String, CompletableFuture<EntityInfo>> entityInfos = new HashMap<>();
                                for (CompletableFuture<EntityInfo> future : entityInfoFutures) {
                                    if (future.isCompletedExceptionally()) {
                                        entityInfos.putIfAbsent(EntityInfo.FAILED, future);
                                    } else if (future.isDone()) {
                                        EntityInfo entityInfo = future.join();
                                        CompletableFuture<EntityInfo> conflict = entityInfos.put(entityInfo.name, future);
                                        if (entityInfo.recordClass != null && conflict == null) {
                                            String recordName = entityInfo.name.substring(0, entityInfo.name.length() - EntityInfo.RECORD_ENTITY_SUFFIX.length());
                                            conflict = entityInfos.put(recordName, future);
                                        }
                                        if (conflict != null) {
                                            EntityInfo conflictInfo = conflict.join(); // already completed
                                            List<String> classNames = List.of((entityInfo.recordClass == null ? entityInfo.entityClass : entityInfo.recordClass).getName(),
                                                                              (conflictInfo.recordClass == null ? conflictInfo.entityClass : conflictInfo.recordClass).getName());
                                            // TODO NLS, consider splitting message for records/normal entities
                                            MappingException conflictX = new MappingException("The " + classNames + " entities have conflicting names. " +
                                                                                              "When using records as entities, an entity name consisting of " +
                                                                                              "the record name suffixed with " + EntityInfo.RECORD_ENTITY_SUFFIX +
                                                                                              " is generated.");
                                            entityInfos.putIfAbsent(EntityInfo.FAILED, CompletableFuture.failedFuture(conflictX));
                                        }
                                    } else {
                                        entityInfos.putIfAbsent(EntityInfo.FAILED, CompletableFuture.failedFuture(x));
                                    }
                                }
                                return entityInfos;
                            });
            for (QueryInfo queryInfo : entitylessQueryInfos) {
                queries.put(queryInfo.method,
                            allEntityInfo.thenCombine(CompletableFuture.completedFuture(this),
                                                      queryInfo::init));
            }
        }
    }

    /**
     * Invoked when the bean for the repository is disposed.
     */
    public void beanDisposed() {
        isDisposed.set(true);
    }

    /**
     * Compute the zero-based offset to use as a starting point for a Limit range.
     *
     * @param limit limit that was specified by the application.
     * @return offset value.
     * @throws IllegalArgumentException if the starting point for the limited range
     *                                      is not positive or would overflow Integer.MAX_VALUE.
     */
    static int computeOffset(Limit range) {
        long startIndex = range.startAt() - 1;
        if (startIndex >= 0 && startIndex <= Integer.MAX_VALUE)
            return (int) startIndex;
        else
            throw new IllegalArgumentException("The starting point for " + range + " is not within 1 to Integer.MAX_VALUE (2147483647)."); // TODO
    }

    /**
     * Compute the zero-based offset for the start of a page.
     *
     * @param pagination requested pagination.
     * @return offset for the specified page.
     * @throws IllegalArgumentException if the offset exceeds Integer.MAX_VALUE
     *                                      or the PageRequest requests cursor-based pagination.
     */
    static int computeOffset(PageRequest pagination) {
        if (pagination.mode() != PageRequest.Mode.OFFSET)
            throw new IllegalArgumentException("Cursor-based pagination mode " + pagination.mode() +
                                               " can only be used with repository methods with the following return types: " +
                                               CursoredPage.class.getName() +
                                               ". For offset pagination, use a PageRequest without a cursor."); // TODO NLS
        int maxPageSize = pagination.size();
        long pageIndex = pagination.page() - 1; // zero-based
        if (Integer.MAX_VALUE / maxPageSize >= pageIndex)
            return (int) (pageIndex * maxPageSize);
        else
            throw new IllegalArgumentException("The offset for " + pagination.page() + " pages of size " + maxPageSize +
                                               " exceeds Integer.MAX_VALUE (2147483647)."); // TODO
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
            for (int i = 0; i < length; i++) {
                Object entity = findAndUpdateOne(Array.get(arg, i), queryInfo, em);
                if (entity == null)
                    throw new OptimisticLockingFailureException("A matching entity was not found in the database."); // TODO NLS
                else
                    results.add(entity);
            }
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            results = new ArrayList<>();
            if (arg instanceof Iterable) {
                for (Object e : ((Iterable<?>) arg)) {
                    Object entity = findAndUpdateOne(e, queryInfo, em);
                    if (entity == null)
                        throw new OptimisticLockingFailureException("A matching entity was not found in the database."); // TODO NLS
                    else
                        results.add(entity);
                }
            } else {
                hasSingularEntityParam = true;
                results = new ArrayList<>(1);
                Object entity = findAndUpdateOne(arg, queryInfo, em);
                if (entity == null)
                    throw new OptimisticLockingFailureException("A matching entity was not found in the database."); // TODO NLS
                else
                    results.add(entity);
            }
        }
        em.flush();

        if (queryInfo.entityInfo.recordClass != null)
            for (int i = 0; i < results.size(); i++)
                results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

        Class<?> returnType = queryInfo.method.getReturnType();
        Object returnValue;
        if (queryInfo.returnArrayType != null) {
            Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
            returnValue = results.toArray(newArray);
        } else {
            Class<?> multiType = queryInfo.multiType;
            if (multiType == null)
                returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
            else if (multiType.isInstance(results))
                returnValue = results;
            else if (Stream.class.equals(multiType))
                returnValue = results.stream();
            else if (Iterable.class.isAssignableFrom(multiType))
                returnValue = toIterable(multiType, null, results);
            else if (Iterator.class.equals(multiType))
                returnValue = results.iterator();
            else
                throw new MappingException("The " + returnType.getName() + " return type of the " +
                                           queryInfo.method.getName() + " method of the " +
                                           queryInfo.method.getDeclaringClass().getName() +
                                           " class is not a valid return type for a repository " +
                                           "@Update" + " method. Valid return types include " +
                                           getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
        }

        if (Optional.class.equals(returnType)) {
            returnValue = returnValue == null ? Optional.empty() : Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (returnValue != null && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Update" + " method. Valid return types include " +
                                       getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
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
     * @return the entity that is written to the database. Null if not found.
     * @throws Exception if an error occurs.
     */
    private Object findAndUpdateOne(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;

        int versionParamIndex = 2;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = entityInfo.getAttribute(e, entityInfo.getAttributeName(ID, true));
        if (id == null) {
            jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
            if (version != null)
                jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> query = em.createQuery(jpql, queryInfo.singleType); // TODO for records, use the entity class, not the record class
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        // id parameter(s)

        int p = 0;
        if (entityInfo.idClassAttributeAccessors != null) {
            throw new UnsupportedOperationException(); // TODO
        } else if (id != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + id.getClass().getSimpleName());
            query.setParameter(++p, id);
        }

        // version parameter

        if (entityInfo.versionAttributeName != null && version != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + version.getClass().getSimpleName());
            query.setParameter(++p, version);
        }

        List<?> results = query.getResultList();

        Object entity;
        if (results.isEmpty()) {
            entity = null;
        } else {
            entity = results.get(0);
            entity = em.merge(toEntity(e));
        }
        return entity;
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
     * @param type resource type.
     * @return instance of the resource. Never null.
     * @throws UnsupportedOperationException if the type of resource is not available.
     */
    private <T> T getResource(Class<T> type) {
        Deque<AutoCloseable> resources = defaultMethodResources.get();
        Object resource = null;
        if (EntityManager.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder.createEntityManager();
        else if (DataSource.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder.getDataSource();
        else if (Connection.class.equals(type))
            try {
                resource = primaryEntityInfoFuture.join().builder.getDataSource().getConnection();
            } catch (SQLException x) {
                throw new DataConnectionException(x);
            }

        if (resource == null)
            throw new UnsupportedOperationException("The " + type.getName() + " type of resource is not available from the Jakarta Data provider."); // TODO NLS

        if (resource instanceof AutoCloseable) {
            if (resources == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, type + " accessed outside the scope of repository default method",
                             Arrays.toString(new Exception().getStackTrace())); // TODO log the stack without an exception
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
            validReturnTypes.add("Iterable<" + singularClassName + ">");
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
                Object entity = toEntity(Array.get(arg, i));
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
                    Object entity = toEntity(e);
                    em.persist(entity);
                    if (results != null)
                        results.add(entity);
                }
                em.flush();
            } else {
                hasSingularEntityParam = true;
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = toEntity(arg);
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
                    returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = toIterable(multiType, null, results);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw new MappingException("The " + returnType.getName() + " return type of the " +
                                               queryInfo.method.getName() + " method of the " +
                                               queryInfo.method.getDeclaringClass().getName() +
                                               " class is not a valid return type for a repository " +
                                               "@Insert" + " method. Valid return types include " +
                                               getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
            }
        }

        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Insert" + " method. Valid return types include " +
                                       getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
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
                throw new IllegalStateException("Repository instance " + repositoryInterface.getName() +
                                                "(Proxy)@" + Integer.toHexString(System.identityHashCode(proxy)) +
                                                " is no longer in scope."); // TODO

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

            QueryInfo queryInfo = queryInfoFuture.join();
            EntityInfo entityInfo = queryInfo.entityInfo;

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, queryInfo.toString());

            if (queryInfo.validateParams)
                validator.validateParameters(proxy, method, args);

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;

            boolean requiresTransaction;
            switch (queryInfo.type) {
                case FIND:
                case COUNT:
                case EXISTS:
                case RESOURCE_ACCESS:
                    requiresTransaction = false;
                    break;
                default:
                    requiresTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus();
            }

            try {
                if (requiresTransaction) {
                    suspendedLTC = provider.localTranCurrent.suspend();
                    provider.tranMgr.begin();
                }

                switch (queryInfo.type) {
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

                        // Jakarta Data allows the method parameter positions after those used as query parameters
                        // to be used for purposes such as pagination and sorting.
                        for (int i = queryInfo.paramCount - queryInfo.paramAddedCount; i < (args == null ? 0 : args.length); i++) {
                            Object param = args[i];
                            if (param instanceof Limit) {
                                if (limit == null)
                                    limit = (Limit) param;
                                else
                                    throw new UnsupportedOperationException("Repository method " + method + " cannot have multiple Limit parameters."); // TODO NLS
                            } else if (param instanceof Order) {
                                @SuppressWarnings("unchecked")
                                Iterable<Sort<Object>> order = (Iterable<Sort<Object>>) param;
                                sortList = queryInfo.supplySorts(sortList, order);
                            } else if (param instanceof PageRequest) {
                                if (pagination == null)
                                    pagination = (PageRequest) param;
                                else
                                    throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                                            method.getDeclaringClass().getName() +
                                                                            " repository cannot have multiple PageRequest parameters."); // TODO NLS
                            } else if (param instanceof Sort) {
                                @SuppressWarnings("unchecked")
                                List<Sort<Object>> newList = queryInfo.supplySorts(sortList, (Sort<Object>) param);
                                sortList = newList;
                            } else if (param instanceof Sort[]) {
                                @SuppressWarnings("unchecked")
                                List<Sort<Object>> newList = queryInfo.supplySorts(sortList, (Sort<Object>[]) param);
                                sortList = newList;
                            }
                        }

                        if (pagination != null && limit != null)
                            throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                                    method.getDeclaringClass().getName() +
                                                                    " repository cannot have both Limit and PageRequest as parameters."); // TODO NLS

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
                                //                                            queryInfo.method.getDeclaringClass().getName() +
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
                                queryInfo = queryInfo.withJPQL(q.append(order).toString(), sortList); // offset pagination can be a starting point for keyset pagination
                            } else { // CURSOR_NEXT or CURSOR_PREVIOUS
                                queryInfo = queryInfo.withJPQL(null, sortList);
                                queryInfo.generateKeysetQueries(q, forward ? order : null, forward ? null : order);
                            }
                        }

                        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType)
                                                                      || CompletionStage.class.equals(returnType));

                        Class<?> multiType = queryInfo.multiType;

                        if (CursoredPage.class.equals(multiType)) {
                            returnValue = new CursoredPageImpl<>(queryInfo, limit == null ? pagination : toPageRequest(limit), args);
                        } else if (Page.class.equals(multiType)) {
                            returnValue = new PageImpl<>(queryInfo, limit == null ? pagination : toPageRequest(limit), args);
                        } else if (pagination != null && !PageRequest.Mode.OFFSET.equals(pagination.mode())) {
                            throw new IllegalArgumentException("A PageRequest that specifies the " + pagination.mode() +
                                                               " mode must not be supplied to the " + method.getName() +
                                                               " method of the " + method.getDeclaringClass().getName() +
                                                               " repository because the method returns " + returnType.getName() +
                                                               " rather than " + CursoredPage.class.getName() + "."); // TODO NLS
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

                            int startAt = limit != null ? computeOffset(limit) //
                                            : pagination != null ? computeOffset(pagination) //
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
                                    returnValue = stream.mapToInt(RepositoryImpl::toInt);
                                else if (LongStream.class.equals(multiType))
                                    returnValue = stream.mapToLong(RepositoryImpl::toLong);
                                else if (DoubleStream.class.equals(multiType))
                                    returnValue = stream.mapToDouble(RepositoryImpl::toDouble);
                                else
                                    throw new UnsupportedOperationException("Stream type " + multiType.getName()); // TODO NLS
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
                                            throw new DataException("Unable to delete from the database when the query result includes a null value."); // TODO NLS
                                        } else if (entityInfo.entityClass.isInstance(result)) {
                                            em.remove(result);
                                        } else if (entityInfo.idClassAttributeAccessors != null) {
                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            int numParams = 0;
                                            for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                                                Object value = accessor instanceof Method ? ((Method) accessor).invoke(result) : ((Field) accessor).get(result);
                                                if (trace && tc.isDebugEnabled())
                                                    Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                             "set ?" + (numParams + 1) + ' ' + (value == null ? null : value.getClass().getSimpleName()));
                                                delete.setParameter(++numParams, value);
                                            }
                                            delete.executeUpdate();
                                        } else { // is return value the entity or id?
                                            Object value = result;
                                            if (entityInfo.entityClass.isInstance(result) ||
                                                entityInfo.recordClass != null && entityInfo.recordClass.isInstance(result)) {
                                                List<Member> accessors = entityInfo.attributeAccessors.get(entityInfo.attributeNames.get(ID));
                                                if (accessors == null || accessors.isEmpty())
                                                    throw new MappingException("Unable to find the id attribute on the " + entityInfo.name + " entity."); // TODO NLS
                                                for (Member accessor : accessors)
                                                    value = accessor instanceof Method ? ((Method) accessor).invoke(value) : ((Field) accessor).get(value);
                                            } else if (!entityInfo.idType.isInstance(value)) {
                                                value = to(entityInfo.idType, result, false);
                                                if (value == result) // unable to convert value
                                                    throw new MappingException("Results for find-and-delete repository queries must be the entity class (" +
                                                                               (entityInfo.recordClass == null ? entityInfo.entityClass : entityInfo.recordClass).getName() +
                                                                               ") or the id class (" + entityInfo.idType +
                                                                               "), not the " + result.getClass().getName() + " class."); // TODO NLS
                                            }

                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            if (trace && tc.isDebugEnabled())
                                                Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                         "set ?1 " + (value == null ? null : value.getClass().getSimpleName()));
                                            delete.setParameter(1, value);
                                            delete.executeUpdate();
                                        }

                                if (results.isEmpty() && queryInfo.isOptional) {
                                    returnValue = null;
                                } else if (multiType == null && (entityInfo.entityClass).equals(singleType)) {
                                    returnValue = oneResult(results);
                                } else if (multiType != null && multiType.isInstance(results) && (results.isEmpty() || !(results.get(0) instanceof Object[]))) {
                                    returnValue = results;
                                } else if (multiType != null && Iterable.class.isAssignableFrom(multiType)) {
                                    returnValue = toIterable(multiType, singleType, results);
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
                                                        Array.set(subarray, j, subarrayType.isInstance(element) //
                                                                        ? element : to(subarrayType, element, true));
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
                                                    Array.set(returnValue, i, queryInfo.returnArrayType.isInstance(element) //
                                                                    ? element : to(queryInfo.returnArrayType, element, true));
                                                }
                                            }
                                        } else {
                                            // List<Object[]> with multiple Object[] elements cannot convert to a one dimensional array
                                            throw new NonUniqueResultException(""); // TODO NLS
                                        }
                                    } else {
                                        throw new MappingException("The " + queryInfo.returnArrayType.getName() +
                                                                   " array type that is declared to be returned by the " +
                                                                   queryInfo.method.getName() + " method of the " +
                                                                   queryInfo.method.getDeclaringClass().getName() +
                                                                   " repository is incompatible with the " +
                                                                   firstNonNullResult.getClass().getName() +
                                                                   " type of the observed query results."); // TODO NLS
                                    }
                                } else if (results.isEmpty()) {
                                    throw new EmptyResultException("Query with return type of " + returnType.getName() +
                                                                   " returned no results. If this is expected, specify a return type of array, List, Optional, Page, CursoredPage, or Stream for the repository method.");
                                } else { // single result of other type
                                    returnValue = oneResult(results);
                                    if (returnValue != null && !singleType.isAssignableFrom(returnValue.getClass())) {
                                        // TODO these conversions are not all safe
                                        if (double.class.equals(singleType) || Double.class.equals(singleType))
                                            returnValue = ((Number) returnValue).doubleValue();
                                        else if (float.class.equals(singleType) || Float.class.equals(singleType))
                                            returnValue = ((Number) returnValue).floatValue();
                                        else if (long.class.equals(singleType) || Long.class.equals(singleType))
                                            returnValue = ((Number) returnValue).longValue();
                                        else if (int.class.equals(singleType) || Integer.class.equals(singleType))
                                            returnValue = ((Number) returnValue).intValue();
                                        else if (short.class.equals(singleType) || Short.class.equals(singleType))
                                            returnValue = ((Number) returnValue).shortValue();
                                        else if (byte.class.equals(singleType) || Byte.class.equals(singleType))
                                            returnValue = ((Number) returnValue).byteValue();
                                    }
                                }
                            }
                        }

                        if (Optional.class.equals(returnType)) {
                            returnValue = returnValue == null
                                          || returnValue instanceof Collection && ((Collection<?>) returnValue).isEmpty()
                                          || returnValue instanceof Page && !((Page<?>) returnValue).hasContent() //
                                                          ? Optional.empty() : Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
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
                                throw new OptimisticLockingFailureException("A matching entity was not found in the database."); // TODO NLS
                            else
                                throw new OptimisticLockingFailureException("A matching entity was not found in the database for " +
                                                                            (numExpected - updateCount) + " of the " +
                                                                            numExpected + " entities."); // TODO NLS

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
                                throw new OptimisticLockingFailureException("A matching entity was not found in the database."); // TODO NLS
                            else
                                throw new OptimisticLockingFailureException("A matching entity was not found in the database for " +
                                                                            (numExpected - updateCount) + " of the " +
                                                                            numExpected + " entities."); // TODO NLS

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

                        TypedQuery<Long> query = em.createQuery(queryInfo.jpql, Long.class);
                        queryInfo.setParameters(query, args);

                        Long result = query.getSingleResult();

                        Class<?> type = queryInfo.singleType;

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "result " + result + " to be returned as " + type.getName());

                        returnValue = result;

                        if (!type.isInstance(result) && !type.isAssignableFrom(returnValue.getClass())) {
                            // TODO these conversions are not all safe
                            if (int.class.equals(type) || Integer.class.equals(type))
                                returnValue = result.intValue();
                            else if (short.class.equals(type) || Short.class.equals(type))
                                returnValue = result.shortValue();
                            else if (byte.class.equals(type) || Byte.class.equals(type))
                                returnValue = result.byteValue();
                        }

                        if (Optional.class.equals(returnType)) {
                            returnValue = Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
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
                        returnValue = getResource(returnType);
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

            if (trace && tc.isEntryEnabled()) {
                boolean hideValue = queryInfo.type == Type.FIND
                                    || queryInfo.type == Type.FIND_AND_DELETE
                                    || queryInfo.type == Type.INSERT
                                    || queryInfo.type == Type.SAVE
                                    || queryInfo.type == Type.UPDATE_WITH_ENTITY_PARAM_AND_RESULT;
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

    @Trivial
    private final Object oneResult(List<?> results) {
        int size = results.size();
        if (size == 1)
            return results.get(0);
        else if (size == 0)
            throw new EmptyResultException("Query returned no results. If this is expected, specify a return type of array, List, Optional, Page, CursoredPage, or Stream for the repository method.");
        else
            throw new NonUniqueResultException("Found " + results.size() +
                                               " results. To limit to a single result, specify Limit.of(1) as a parameter or use the findFirstBy name pattern.");
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
        Class<?> entityClass = queryInfo.entityInfo.recordClass == null ? queryInfo.entityInfo.entityClass : queryInfo.entityInfo.recordClass;

        if (e == null)
            throw new NullPointerException("The entity parameter cannot have a null value."); // TODO NLS // required by spec

        if (!entityClass.isInstance(e))
            throw new IllegalArgumentException("The " + (e == null ? null : e.getClass().getName()) +
                                               " parameter does not match the " + entityClass.getName() +
                                               " entity type that is expected for this repository."); // TODO NLS

        EntityInfo entityInfo = queryInfo.entityInfo;
        String jpql = queryInfo.jpql;

        int versionParamIndex = (entityInfo.idClassAttributeAccessors == null ? 1 : entityInfo.idClassAttributeAccessors.size()) + 1;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            id = entityInfo.getAttribute(e, entityInfo.getAttributeName(ID, true));
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
                    Tr.debug(tc, "set ?" + p + ' ' + id.getClass().getSimpleName());
                delete.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + version.getClass().getSimpleName());
                delete.setParameter(p, version);
            }
        } else {
            queryInfo.setParametersFromIdClassAndVersion(delete, e, version);
        }

        int numDeleted = delete.executeUpdate();

        if (numDeleted == 0) {
            Class<?> returnType = queryInfo.method.getReturnType();
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                if (entityInfo.versionAttributeName == null)
                    throw new OptimisticLockingFailureException("Entity was not found."); // TODO NLS
                else
                    //TODO if the version is 0, let the user know the object returned from the insert/save operation must be used, not the original object
                    throw new OptimisticLockingFailureException("Version " + version + " of the entity was not found."); // TODO NLS
            }
        } else if (numDeleted > 1) {
            throw new DataException("Found " + numDeleted + " matching entities."); // ought to be unreachable
        }

        return numDeleted;
    }

    /**
     * Converts to the specified type, raising an error if the conversion cannot be made.
     *
     * @param type               type to convert to.
     * @param item               item to convert.
     * @param failIfNotConverted whether or not to fail if unable to convert the value.
     * @return new instance of the requested type.
     */
    @Trivial // avoid tracing value from customer data
    private static final Object to(Class<?> type, Object item, boolean failIfNotConverted) {
        Object result = item;
        if (item == null) {
            if (type.isPrimitive())
                throw new MappingException("Query returned a null result which is not compatible with the type that is " +
                                           "expected by the repository method signature: " + type.getName()); // TODO NLS
        } else if (item instanceof Number && (type.isPrimitive() || Number.class.isAssignableFrom(type))) {
            Number n = (Number) item;
            if (long.class.equals(type) || Long.class.equals(type))
                result = n.longValue();
            else if (double.class.equals(type) || Double.class.equals(type))
                result = n.doubleValue();
            else if (float.class.equals(type) || Float.class.equals(type))
                result = n.floatValue();
            else if (int.class.equals(type) || Integer.class.equals(type))
                result = n.intValue();
            else if (short.class.equals(type) || Short.class.equals(type))
                result = n.shortValue();
            else if (byte.class.equals(type) || Byte.class.equals(type))
                result = n.byteValue();
            else if (boolean.class.equals(type) || Boolean.class.equals(type))
                result = n.longValue() != 0L;
            else if (failIfNotConverted)
                throw new MappingException("Query returned a result of type " + item.getClass().getName() +
                                           " which is not compatible with the type that is expected by the repository method signature: " +
                                           type.getName()); // TODO
        } else if (type.isAssignableFrom(String.class)) {
            result = item.toString();
        } else if (failIfNotConverted) {
            throw new MappingException("Query returned a result of type " + item.getClass().getName() +
                                       " which is not compatible with the type that is expected by the repository method signature: " +
                                       type.getName()); // TODO
        }
        return result;
    }

    @Trivial
    private static final double toDouble(Object o) {
        if (o instanceof Number)
            return ((Number) o).doubleValue();
        else if (o instanceof String)
            return Double.parseDouble((String) o);
        else
            throw new MappingException("Not representable as a double value: " + o.getClass().getName());
    }

    /**
     * Converts a record to its generated entity equivalent,
     * or does nothing if not a record.
     *
     * @param o entity or a record that needs conversion to an entity.
     * @return entity.
     */
    @Trivial
    private static final Object toEntity(Object o) {
        Object entity = o;
        Class<?> oClass = o == null ? null : o.getClass();
        if (o != null && oClass.isRecord())
            try {
                Class<?> entityClass = oClass.getClassLoader().loadClass(oClass.getName() + "Entity");
                Constructor<?> ctor = entityClass.getConstructor(oClass);
                entity = ctor.newInstance(o);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | //
                            InvocationTargetException | NoSuchMethodException | SecurityException x) {
                throw new MappingException("Unable to convert record " + oClass + " to generated entity class.", //
                                x instanceof InvocationTargetException ? x.getCause() : x); // TODO NLS
            }
        if (entity != o && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "toEntity " + oClass.getName() + " --> " + entity.getClass().getName());
        return entity;
    }

    @Trivial
    private static final int toInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();
        else if (o instanceof String)
            return Integer.parseInt((String) o);
        else
            throw new MappingException("Not representable as an int value: " + o.getClass().getName());
    }

    /**
     * Convert the results list into an Iterable of the specified type.
     *
     * @param iterableType the desired type of Iterable.
     * @param elementType  the type of each element if a find operation. Can be NULL if a save operation.
     * @param results      results of a find or save operation.
     * @return results converted to an Iterable of the specified type.
     */
    @Trivial
    private static final Iterable<?> toIterable(Class<?> iterableType, Class<?> elementType, List<?> results) {
        Collection<Object> list;
        if (iterableType.isInterface()) {
            if (iterableType.isAssignableFrom(ArrayList.class)) // covers Iterable, Collection, List
                list = new ArrayList<>(results.size());
            else if (iterableType.isAssignableFrom(ArrayDeque.class)) // covers Queue, Deque
                list = new ArrayDeque<>(results.size());
            else if (iterableType.isAssignableFrom(LinkedHashSet.class)) // covers Set
                list = new LinkedHashSet<>(results.size());
            else
                throw new UnsupportedOperationException(iterableType + " is an unsupported return type."); // TODO NLS
        } else {
            try {
                @SuppressWarnings("unchecked")
                Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) iterableType.getConstructor();
                list = c.newInstance();
            } catch (NoSuchMethodException x) {
                throw new MappingException("The " + iterableType.getName() + " result type lacks a public zero parameter constructor.", x); // TODO NLS
            } catch (IllegalAccessException | InstantiationException x) {
                throw new MappingException("Unable to access the zero parameter constructor of the " + iterableType.getName() + " result type.", x); // TODO NLS
            } catch (InvocationTargetException x) {
                throw new MappingException("The constructor for the " + iterableType.getName() + " result type raised an error: " + x.getCause().getMessage(), x.getCause()); // TODO NLS
            }
        }
        if (results.size() == 1 && results.get(0) instanceof Object[]) {
            Object[] a = (Object[]) results.get(0);
            for (int i = 0; i < a.length; i++)
                list.add(elementType.isInstance(a[i]) ? a[i] : to(elementType, a[i], true));
        } else {
            list.addAll(results);
        }
        return list;
    }

    @Trivial
    private static final long toLong(Object o) {
        if (o instanceof Number)
            return ((Number) o).longValue();
        else if (o instanceof String)
            return Long.parseLong((String) o);
        else
            throw new MappingException("Not representable as a long value: " + o.getClass().getName());
    }

    /**
     * Converts a Limit to a PageRequest if possible.
     *
     * @param limit Limit.
     * @return PageRequest.
     * @throws IllegalArgumentException if the Limit is a range with a starting point above 1.
     */
    private static final PageRequest toPageRequest(Limit limit) {
        if (limit.startAt() != 1L)
            throw new IllegalArgumentException("Limit with starting point " + limit.startAt() +
                                               ", which is greater than 1, cannot be used to request pages."); // TODO NLS
        return PageRequest.ofSize(limit.maxResults());
    }

    /**
     * Converts an update count to the requested return type.
     *
     * @param i          update count value.
     * @param returnType requested return type.
     * @param queryInfo  query information.
     * @return converted value.
     */
    private static final Object toReturnValue(int i, Class<?> returnType, QueryInfo queryInfo) {
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
        else // TODO queryInfo in message
            throw new UnsupportedOperationException("The " + queryInfo.method.getName() + " method of the " +
                                                    queryInfo.method.getDeclaringClass().getName() + " repository has a return type, " +
                                                    returnType + ", that is not supported for repository Update and Delete operations. " +
                                                    "Supported return types include void (for no result), boolean (to indicate whether " +
                                                    "or not a matching entity was found), or one of the following types to indicate " +
                                                    "how many matching entities were found: " +
                                                    "long, Long, int, Integer, Number" + "."); // TODO NLS

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
                results.add(em.merge(toEntity(Array.get(arg, i))));
            em.flush();
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            if (Iterable.class.isAssignableFrom(queryInfo.entityParamType)) {
                results = new ArrayList<>();
                for (Object e : ((Iterable<?>) arg))
                    results.add(em.merge(toEntity(e)));
                em.flush();
            } else {
                hasSingularEntityParam = true;
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = em.merge(toEntity(arg));
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
                    returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = toIterable(multiType, null, results);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw new MappingException("The " + returnType.getName() + " return type of the " +
                                               queryInfo.method.getName() + " method of the " +
                                               queryInfo.method.getDeclaringClass().getName() +
                                               " class is not a valid return type for a repository " +
                                               "@Save" + " method. Valid return types include " +
                                               getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
            }
        }

        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Save" + " method. Valid return types include " +
                                       getValidReturnTypes(results.get(0).getClass().getSimpleName(), hasSingularEntityParam, false) + "."); // TODO NLS
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
        Class<?> entityClass = queryInfo.entityInfo.recordClass == null ? queryInfo.entityInfo.entityClass : queryInfo.entityInfo.recordClass;

        if (e == null)
            throw new NullPointerException("The entity parameter cannot have a null value."); // TODO NLS // required by spec

        if (!entityClass.isInstance(e))
            throw new IllegalArgumentException("The " + (e == null ? null : e.getClass().getName()) +
                                               " parameter does not match the " + entityClass.getName() +
                                               " entity type that is expected for this repository."); // TODO NLS

        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;
        LinkedHashSet<String> attrsToUpdate = entityInfo.getAttributeNamesForEntityUpdate();

        int versionParamIndex = attrsToUpdate.size() + 2;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = entityInfo.getAttribute(e, entityInfo.getAttributeName(ID, true));
        if (id == null) {
            jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
            if (version != null)
                jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> update = em.createQuery(jpql, entityInfo.entityClass);

        // parameters for entity attributes to update:

        int p = 0;
        for (String attrName : entityInfo.getAttributeNamesForEntityUpdate())
            QueryInfo.setParameter(++p, update, e,
                                   entityInfo.attributeAccessors.get(attrName));

        // id parameter(s)

        if (entityInfo.idClassAttributeAccessors != null) {
            throw new UnsupportedOperationException(); // TODO
        } else if (id != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + id.getClass().getSimpleName());
            update.setParameter(++p, id);
        }

        // version parameter

        if (entityInfo.versionAttributeName != null && version != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + version.getClass().getSimpleName());
            update.setParameter(++p, version);
        }

        int numUpdated = update.executeUpdate();

        if (numUpdated > 1)
            throw new DataException("Found " + numUpdated + " matching entities."); // ought to be unreachable
        return numUpdated;
    }
}